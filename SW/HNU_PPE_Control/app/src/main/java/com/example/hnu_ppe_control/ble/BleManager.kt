// Smart Shield BLE 스캔, 연결, Notify 수신, Write 전송, 자동 재연결을 담당하는 파일
package com.example.hnu_ppe_control.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.nio.charset.StandardCharsets

class BleManager(
    private val context: Context,
    private val listener: Listener
) {
    interface Listener {
        fun onScanStarted()
        fun onScanStopped()
        fun onScanFailed(errorCode: Int)
        fun onDeviceFound(deviceInfo: BleDeviceInfo)
        fun onBleStatusChanged(message: String)
        fun onReconnectStatusChanged(message: String)
        fun onConnected(deviceName: String, address: String)
        fun onDisconnected(manual: Boolean)
        fun onReconnectFailed()
        fun onNotifyReady()
        fun onDataReceived(rawData: String)
        fun onWriteResult(command: String, started: Boolean, reason: String?)
        fun onRssiUpdated(rssi: Int)
    }

    data class BleDeviceInfo(
        val device: BluetoothDevice,
        val name: String,
        val address: String
    )

    companion object {
        private const val TAG = "SmartShieldBLE"
        private const val RECONNECT_INTERVAL_MS = 3000L
        private const val RECONNECT_MAX_DURATION_MS = 10 * 60 * 1000L
        private const val UNSTABLE_TIMEOUT_MS = 10 * 1000L
        private const val OFFLINE_TIMEOUT_MS = 30 * 1000L
        private const val REQUESTED_MTU = 128
        private const val RSSI_READ_INTERVAL_MS = 5000L
        private val ALLOWED_RISK_COMMANDS = setOf(
            "RISK:SAFE",
            "RISK:CAUTION",
            "RISK:DANGER",
            "RISK:EMERGENCY"
        )
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    private var bluetoothGatt: BluetoothGatt? = null
    private var isScanning = false
    private var isManualDisconnect = false
    private var isReconnecting = false
    private var reconnectStartTime = 0L
    private var lastDataReceivedTime = 0L
    private var isOfflineCheckerRunning = false
    private var isRssiReaderRunning = false
    private var isConnecting = false
    private var lastConnectedDevice: BluetoothDevice? = null
    private var lastSentRiskCommand: String? = null
    private val notifyBuffer = StringBuilder()

    var isBleConnected = false
        private set
    var isServiceDiscovered = false
        private set
    var isNotifyReady = false
        private set
    var connectedDeviceName: String = "Unknown"
        private set
    var connectedDeviceAddress: String? = null
        private set

    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    fun startScan() {
        // Smart Shield 장치만 선별
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

        if (bluetoothLeScanner == null) {
            listener.onBleStatusChanged("사용 불가")
            Log.e(TAG, "BLE scanner is null")
            return
        }
        if (isScanning) {
            listener.onBleStatusChanged("스캔 중")
            return
        }
        if (!BlePermissionHelper.hasScanPermission(context)) {
            listener.onBleStatusChanged("스캔 권한 없음")
            return
        }

        isScanning = true
        listener.onScanStarted()
        try {
            bluetoothLeScanner?.startScan(scanCallback)
            Log.d(TAG, "BLE scan started")
        } catch (e: SecurityException) {
            isScanning = false
            Log.e(TAG, "startScan failed by permission", e)
            listener.onBleStatusChanged("스캔 권한 오류")
            return
        }

        mainHandler.postDelayed({
            if (isScanning) stopScan()
        }, BleConstants.SCAN_PERIOD)
    }

    fun stopScan() {
        if (bluetoothLeScanner != null && isScanning && BlePermissionHelper.hasScanPermission(context)) {
            try {
                bluetoothLeScanner?.stopScan(scanCallback)
                Log.d(TAG, "BLE scan stopped")
            } catch (e: SecurityException) {
                Log.e(TAG, "stopScan failed by permission", e)
            }
        }

        if (isScanning) {
            isScanning = false
            listener.onScanStopped()
        }
    }

    fun connect(device: BluetoothDevice) {
        // 중복 연결 방지와 재연결 대상 저장
        if (!BlePermissionHelper.hasConnectPermission(context)) {
            listener.onBleStatusChanged("연결 권한 없음")
            return
        }

        val address = readDeviceAddress(device) ?: return
        val name = readDeviceName(device) ?: "Unknown"

        if (isConnecting || isBleConnected) {
            Log.d(TAG, "Duplicate connect blocked. isConnecting=$isConnecting, isBleConnected=$isBleConnected, address=$address")
            return
        }

        stopScan()
        resetConnectionFlags()
        isConnecting = true
        isManualDisconnect = false
        lastConnectedDevice = device
        connectedDeviceAddress = address
        connectedDeviceName = name

        listener.onBleStatusChanged("연결 시도 중")
        Log.d(TAG, "Connecting to device: $address / $name")

        try {
            bluetoothGatt?.close()
            bluetoothGatt = null
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
        } catch (e: SecurityException) {
            isConnecting = false
            Log.e(TAG, "connectGatt failed by permission", e)
            listener.onBleStatusChanged("연결 권한 오류")
        }
    }

    fun disconnectManually() {
        // 수동 종료 시 자동 재연결 중지와 GATT 정리
        isConnecting = false
        isManualDisconnect = true
        stopReconnect()
        stopOfflineChecker()
        stopRssiReader()

        try {
            if (BlePermissionHelper.hasConnectPermission(context)) {
                bluetoothGatt?.disconnect()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Manual disconnect failed by permission", e)
        }

        closeGatt()
        resetConnectionFlags()
        listener.onDisconnected(manual = true)
    }

    fun writeRiskCommand(command: String) {
        // 약속된 위험도 명령만 전송
        if (!ALLOWED_RISK_COMMANDS.contains(command)) {
            listener.onWriteResult(command, started = false, reason = "invalid_command")
            return
        }
        if (lastSentRiskCommand == command) {
            listener.onWriteResult(command, started = false, reason = "duplicate")
            return
        }
        if (!isBleConnected || !isServiceDiscovered) {
            listener.onWriteResult(command, started = false, reason = "not_ready")
            return
        }

        val gatt = bluetoothGatt
        if (gatt == null) {
            listener.onWriteResult(command, started = false, reason = "gatt_null")
            return
        }
        if (!BlePermissionHelper.hasConnectPermission(context)) {
            listener.onWriteResult(command, started = false, reason = "permission")
            return
        }

        val service = gatt.getService(BleConstants.TARGET_SERVICE_UUID)
        val characteristic = service?.getCharacteristic(BleConstants.CONTROL_CHARACTERISTIC_UUID)
        if (service == null || characteristic == null) {
            listener.onWriteResult(command, started = false, reason = "characteristic_null")
            return
        }

        val started = writeCharacteristic(gatt, characteristic, command.toByteArray(StandardCharsets.UTF_8))
        if (started) lastSentRiskCommand = command
        listener.onWriteResult(command, started, if (started) null else "write_failed")
    }

    fun release() {
        stopScan()
        stopReconnect()
        stopOfflineChecker()
        stopRssiReader()
        closeGatt()
        resetConnectionFlags()
        lastConnectedDevice = null
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            val device = result.device ?: return
            val scanRecord = result.scanRecord ?: return
            val name = readDeviceName(device) ?: return
            val address = readDeviceAddress(device) ?: return

            if (!name.matches(Regex("^SS_\\d{4}$"))) return
            if (!hasTargetServiceUuid(scanRecord)) return

            listener.onDeviceFound(BleDeviceInfo(device, name, address))
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            isScanning = false
            listener.onScanFailed(errorCode)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> handleGattConnected(gatt)
                BluetoothProfile.STATE_DISCONNECTED -> handleGattDisconnected(status)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            Log.d(TAG, "onMtuChanged: mtu=$mtu, status=$status")
            discoverServicesAfterMtu(gatt)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            handleServicesDiscovered(gatt, status)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            handleDescriptorWrite(descriptor, status)
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            handleNotifyValue(characteristic, characteristic.value)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            handleNotifyValue(characteristic, value)
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                listener.onRssiUpdated(rssi)
            }
        }
    }

    private fun handleGattConnected(gatt: BluetoothGatt) {
        // MTU 협상, 서비스 탐색, Notify 준비 순서
        isConnecting = false
        isBleConnected = true
        isServiceDiscovered = false
        isNotifyReady = false
        lastSentRiskCommand = null
        notifyBuffer.clear()

        stopReconnect()
        startOfflineChecker()
        startRssiReader()
        listener.onConnected(connectedDeviceName, connectedDeviceAddress ?: "")

        if (!BlePermissionHelper.hasConnectPermission(context)) {
            listener.onBleStatusChanged("서비스 탐색 권한 없음")
            return
        }
        requestMtuBeforeServiceDiscovery(gatt)
    }

    private fun requestMtuBeforeServiceDiscovery(gatt: BluetoothGatt) {
        val started = try {
            if (BlePermissionHelper.hasConnectPermission(context)) gatt.requestMtu(REQUESTED_MTU) else false
        } catch (e: SecurityException) {
            Log.e(TAG, "requestMtu failed", e)
            false
        }

        if (!started) discoverServicesAfterMtu(gatt)
    }

    private fun discoverServicesAfterMtu(gatt: BluetoothGatt) {
        if (isServiceDiscovered) return
        try {
            if (BlePermissionHelper.hasConnectPermission(context)) {
                val started = gatt.discoverServices()
                Log.d(TAG, "discoverServices started=$started")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "discoverServices failed", e)
        }
    }

    private fun handleGattDisconnected(status: Int) {
        // 비정상 해제 재연결, 수동 해제 즉시 정리
        Log.w(TAG, "BLE disconnected. status=$status")
        isConnecting = false
        isBleConnected = false
        isServiceDiscovered = false
        isNotifyReady = false
        lastSentRiskCommand = null
        notifyBuffer.clear()
        stopRssiReader()
        listener.onDisconnected(manual = isManualDisconnect)

        if (isManualDisconnect) {
            stopOfflineChecker()
            closeGatt()
        } else {
            startReconnect()
        }
    }

    private fun handleServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        // HW 공통 Service/Characteristic UUID 확인
        if (status != BluetoothGatt.GATT_SUCCESS) {
            isServiceDiscovered = false
            isNotifyReady = false
            listener.onBleStatusChanged("서비스 탐색 실패")
            return
        }

        val service = gatt.getService(BleConstants.TARGET_SERVICE_UUID)
        val notifyCharacteristic = service?.getCharacteristic(BleConstants.DATA_CHARACTERISTIC_UUID)
        val writeCharacteristic = service?.getCharacteristic(BleConstants.CONTROL_CHARACTERISTIC_UUID)

        if (service == null) {
            listener.onBleStatusChanged("대상 서비스 없음")
            return
        }
        isServiceDiscovered = true

        if (notifyCharacteristic == null) {
            listener.onBleStatusChanged("Notify 특성 없음")
            return
        }
        if (writeCharacteristic == null) {
            listener.onWriteResult("", started = false, reason = "write_characteristic_null")
        }

        enableNotify(gatt, notifyCharacteristic)
    }

    private fun enableNotify(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        // setCharacteristicNotification과 CCCD 쓰기 모두 필요
        if (!BlePermissionHelper.hasConnectPermission(context)) {
            listener.onBleStatusChanged("Notify 권한 없음")
            return
        }

        val notifyEnabled = try {
            gatt.setCharacteristicNotification(characteristic, true)
        } catch (e: SecurityException) {
            Log.e(TAG, "setCharacteristicNotification failed", e)
            false
        }

        val descriptor = characteristic.getDescriptor(BleConstants.CLIENT_CHARACTERISTIC_CONFIG_UUID)
        if (descriptor == null) {
            isNotifyReady = false
            listener.onBleStatusChanged("CCCD 없음")
            return
        }

        val descriptorWriteStarted = writeNotifyDescriptor(gatt, descriptor)
        isNotifyReady = false
        if (notifyEnabled && descriptorWriteStarted) {
            Log.d(TAG, "Notify descriptor write started")
        } else {
            Log.e(TAG, "Notify descriptor write start failed. notifyEnabled=$notifyEnabled, descriptorWriteStarted=$descriptorWriteStarted")
            listener.onBleStatusChanged("Notify 설정 실패")
        }
    }

    private fun handleDescriptorWrite(descriptor: BluetoothGattDescriptor, status: Int) {
        if (descriptor.uuid != BleConstants.CLIENT_CHARACTERISTIC_CONFIG_UUID) return

        if (status == BluetoothGatt.GATT_SUCCESS) {
            isNotifyReady = true
            Log.d(TAG, "Notify descriptor write success")
            listener.onNotifyReady()
        } else {
            isNotifyReady = false
            Log.e(TAG, "Notify descriptor write failed. status=$status")
            listener.onBleStatusChanged("Notify 설정 실패")
        }
    }

    private fun handleNotifyValue(characteristic: BluetoothGattCharacteristic, value: ByteArray?) {
        if (characteristic.uuid != BleConstants.DATA_CHARACTERISTIC_UUID) return
        val rawData = String(value ?: return, StandardCharsets.UTF_8)
        lastDataReceivedTime = System.currentTimeMillis()
        requestRemoteRssi()
        Log.d(TAG, "Notify chunk received: $rawData")
        handleNotifyChunk(rawData)
    }

    private fun handleNotifyChunk(chunk: String) {
        // 줄바꿈 기준 payload 조각 복원
        notifyBuffer.append(chunk)
        while (true) {
            val lineEnd = notifyBuffer.indexOf("\n")
            if (lineEnd < 0) break

            val line = notifyBuffer.substring(0, lineEnd).trim()
            notifyBuffer.delete(0, lineEnd + 1)
            if (line.isNotEmpty()) {
                Log.d(TAG, "Notify complete payload: $line")
                listener.onDataReceived(line)
            }
        }

        if (notifyBuffer.length > 512) {
            Log.w(TAG, "Notify buffer overflow. Clearing buffer=$notifyBuffer")
            notifyBuffer.clear()
        }
    }

    private fun writeNotifyDescriptor(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ==
                    BluetoothStatusCodes.SUCCESS
            } catch (e: SecurityException) {
                Log.e(TAG, "writeDescriptor failed", e)
                false
            }
        } else {
            try {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            } catch (e: SecurityException) {
                Log.e(TAG, "writeDescriptor legacy failed", e)
                false
            }
        }
    }

    private fun writeCharacteristic(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        sendData: ByteArray
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                gatt.writeCharacteristic(
                    characteristic,
                    sendData,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                ) == BluetoothStatusCodes.SUCCESS
            } catch (e: SecurityException) {
                Log.e(TAG, "writeCharacteristic failed", e)
                false
            }
        } else {
            try {
                @Suppress("DEPRECATION")
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                @Suppress("DEPRECATION")
                characteristic.value = sendData
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(characteristic)
            } catch (e: SecurityException) {
                Log.e(TAG, "writeCharacteristic legacy failed", e)
                false
            }
        }
    }

    private fun startReconnect() {
        // 제한 시간 내 주기적 재연결
        if (lastConnectedDevice == null || isReconnecting) return
        isReconnecting = true
        reconnectStartTime = System.currentTimeMillis()
        listener.onReconnectStatusChanged("재연결 중")
        mainHandler.post(reconnectRunnable)
    }

    private fun stopReconnect() {
        isReconnecting = false
        mainHandler.removeCallbacks(reconnectRunnable)
    }

    private val reconnectRunnable = object : Runnable {
        override fun run() {
            if (!isReconnecting) return
            val elapsed = System.currentTimeMillis() - reconnectStartTime
            if (elapsed > RECONNECT_MAX_DURATION_MS) {
                stopReconnect()
                closeGatt()
                resetConnectionFlags()
                listener.onReconnectFailed()
                return
            }

            if (!isConnecting && !isBleConnected) {
                lastConnectedDevice?.let {
                    val address = readDeviceAddress(it) ?: "unknown"
                    Log.d(TAG, "Reconnect attempt started. address=$address")
                    connect(it)
                }
            } else {
                Log.d(TAG, "Reconnect skipped. isConnecting=$isConnecting, isBleConnected=$isBleConnected")
            }
            mainHandler.postDelayed(this, RECONNECT_INTERVAL_MS)
        }
    }

    private fun startOfflineChecker() {
        // 마지막 Notify 수신 시각 기준 상태 표시
        lastDataReceivedTime = System.currentTimeMillis()
        if (isOfflineCheckerRunning) return
        isOfflineCheckerRunning = true
        mainHandler.post(offlineCheckRunnable)
    }

    private fun stopOfflineChecker() {
        isOfflineCheckerRunning = false
        mainHandler.removeCallbacks(offlineCheckRunnable)
    }

    private val offlineCheckRunnable = object : Runnable {
        override fun run() {
            if (!isOfflineCheckerRunning) return
            val elapsed = System.currentTimeMillis() - lastDataReceivedTime

            when {
                elapsed >= OFFLINE_TIMEOUT_MS -> listener.onBleStatusChanged("연결 끊김")
                elapsed >= UNSTABLE_TIMEOUT_MS -> listener.onBleStatusChanged("수신 불안정")
                isNotifyReady -> listener.onBleStatusChanged("연결됨")
            }

            mainHandler.postDelayed(this, 1000L)
        }
    }

    private fun startRssiReader() {
        if (isRssiReaderRunning) return
        isRssiReaderRunning = true
        mainHandler.post(rssiRunnable)
    }

    private fun stopRssiReader() {
        isRssiReaderRunning = false
        mainHandler.removeCallbacks(rssiRunnable)
    }

    private val rssiRunnable = object : Runnable {
        override fun run() {
            if (!isRssiReaderRunning) return
            requestRemoteRssi()
            mainHandler.postDelayed(this, RSSI_READ_INTERVAL_MS)
        }
    }

    private fun requestRemoteRssi() {
        // RSSI 읽기 주기와 실패 처리 위치
        val gatt = bluetoothGatt ?: return
        if (!isBleConnected || !BlePermissionHelper.hasConnectPermission(context)) return
        try {
            gatt.readRemoteRssi()
        } catch (e: SecurityException) {
            Log.e(TAG, "readRemoteRssi failed", e)
        }
    }

    private fun hasTargetServiceUuid(scanRecord: ScanRecord): Boolean {
        val serviceUuids = scanRecord.serviceUuids ?: return false
        return serviceUuids.any { it.uuid == BleConstants.TARGET_SERVICE_UUID }
    }

    private fun readDeviceName(device: BluetoothDevice): String? {
        return try {
            if (!BlePermissionHelper.hasConnectPermission(context)) null
            else device.name?.takeIf { it.isNotBlank() }
        } catch (e: SecurityException) {
            null
        }
    }

    private fun readDeviceAddress(device: BluetoothDevice): String? {
        return try {
            if (!BlePermissionHelper.hasConnectPermission(context)) null else device.address
        } catch (e: SecurityException) {
            null
        }
    }

    private fun resetConnectionFlags() {
        isConnecting = false
        isBleConnected = false
        isServiceDiscovered = false
        isNotifyReady = false
        lastSentRiskCommand = null
        lastDataReceivedTime = 0L
        notifyBuffer.clear()
    }

    private fun closeGatt() {
        isConnecting = false
        val gatt = bluetoothGatt ?: return
        try {
            if (BlePermissionHelper.hasConnectPermission(context)) gatt.close()
        } catch (e: SecurityException) {
            Log.e(TAG, "BluetoothGatt close failed", e)
        }
        bluetoothGatt = null
    }
}
