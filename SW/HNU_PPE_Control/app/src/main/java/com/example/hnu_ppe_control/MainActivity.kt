// 작업자 앱의 BLE 연결, 센서 처리, 위험도 판단, Firebase 업로드 흐름을 제어하는 파일
package com.example.hnu_ppe_control

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.hnu_ppe_control.alert.AlertManager
import com.example.hnu_ppe_control.ble.BleConstants
import com.example.hnu_ppe_control.ble.BleManager
import com.example.hnu_ppe_control.ble.BlePermissionHelper
import com.example.hnu_ppe_control.data.BleSignalLevel
import com.example.hnu_ppe_control.data.RiskLevel
import com.example.hnu_ppe_control.data.SensorData
import com.example.hnu_ppe_control.data.TempBaselineSnapshot
import com.example.hnu_ppe_control.data.WeatherSnapshot
import com.example.hnu_ppe_control.data.WorkerDetailSnapshot
import com.example.hnu_ppe_control.data.WorkerStatusStore
import com.example.hnu_ppe_control.data.WorkLocation
import com.example.hnu_ppe_control.data.WorkSessionState
import com.example.hnu_ppe_control.firebase.FirebaseStatusUploader
import com.example.hnu_ppe_control.firebase.RiskLogPolicy
import com.example.hnu_ppe_control.parser.SensorDataParser
import com.example.hnu_ppe_control.risk.HeatstrokeAnalyzer
import com.example.hnu_ppe_control.risk.RiskCommandMapper
import com.example.hnu_ppe_control.service.ForegroundServiceController
import com.example.hnu_ppe_control.test.FakeSensorDataProvider
import com.example.hnu_ppe_control.ui.MainUiController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity(), BleManager.Listener {

    companion object {
        private const val TAG = "SmartShieldBLE"
        private const val REQUEST_NOTIFICATION_PERMISSION = 2001
        private const val TEMP_STABILIZATION_SECONDS = 30L
        private const val TEMP_BASELINE_COLLECTION_SECONDS = 60L
        private const val TEMP_MIN_VALID_SAMPLE_COUNT = 20
        private const val TEMP_TREND_WINDOW_SECONDS = 10L
        private const val TEMP_TREND_MIN_SAMPLE_COUNT = 5
    }

    private lateinit var bleManager: BleManager
    private lateinit var alertManager: AlertManager
    private lateinit var ui: MainUiController
    private lateinit var riskLogPolicy: RiskLogPolicy
    private lateinit var foregroundServiceController: ForegroundServiceController

    private val foundDeviceList = ArrayList<BleManager.BleDeviceInfo>()
    private var dialogDeviceAdapter: ArrayAdapter<String>? = null
    private var dialogStatusText: TextView? = null
    private var dialogConnectButton: Button? = null
    private var dialogSelectedDeviceIndex = -1
    private var pendingWorkLocation: WorkLocation? = null
    private var connectDialog: AlertDialog? = null

    private var appSessionActive = false
    private var workSessionState = WorkSessionState.IDLE
    private var selectedWorkLocation: WorkLocation? = null
    private var workStartedAtMillis: Long? = null
    private var workEndedAtMillis: Long? = null
    private var baselinePosture: String? = null
    private var baselineTemp: Double? = null
    private var baselineTempReady = false
    private var baselineStartTimeMillis: Long? = null
    private val baselineTempSamples = mutableListOf<Double>()
    private val recentDeltaTempSamples = mutableListOf<Pair<Long, Double>>()
    private var latestDeltaTemp: Double? = null
    private var latestStableDeltaTemp: Double? = null
    private var tempBaselineStatus = "SENSOR_ERROR"
    private var baselineHr: Int? = null
    private var bleSignalLevel = BleSignalLevel.NOT_CONNECTED
    private var bleRssi: Int? = null
    private var currentBleState = "연결 전"
    private var weatherSnapshot = WeatherSnapshot()
    private var lastSensorData: SensorData? = null
    private var lastRiskLevel: RiskLevel = RiskLevel.SAFE
    private var lastRiskCommand: String = "RISK:SAFE"
    private var lastUpdatedAt: String = "-"
    private var fakeTestMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 관리자 객체와 기본 화면 상태 준비
        initManagers()
        initUi()
        requestNotificationPermissionIfNeeded()
        prepareWeatherPlaceholder()
        FirebaseStatusUploader.startRealtimeConnection { success, message ->
            showFirebaseUploadResult(success, message)
        }
    }

    private fun initManagers() {
        bleManager = BleManager(this, this)
        alertManager = AlertManager(this)
        riskLogPolicy = RiskLogPolicy()
        foregroundServiceController = ForegroundServiceController(this)
    }

    private fun initUi() {
        ui = MainUiController(this)
        ui.showDefault(bleManager.isBluetoothAvailable())
        ui.bindActions(
            onWorkButtonClicked = { handleWorkButtonClicked() },
            onDisconnectClicked = { showEndWorkDialog() },
            onFakeDataClicked = { handleFakeDataClicked() },
            onDetailClicked = { openWorkerDetail() }
        )
    }

    private fun handleWorkButtonClicked() {
        when (workSessionState) {
            WorkSessionState.IDLE,
            WorkSessionState.ENDED -> showBleConnectDialog()
            WorkSessionState.CONNECTING -> Toast.makeText(this, "이미 연결을 시도하고 있습니다.", Toast.LENGTH_SHORT).show()
            WorkSessionState.WORKING,
            WorkSessionState.RECONNECTING -> showEndWorkDialog()
        }
    }

    private fun showBleConnectDialog() {
        val canScan = ensureBleReady(requestPermission = true)

        // 새 연결 시도마다 선택 상태 초기화
        foundDeviceList.clear()
        dialogSelectedDeviceIndex = -1
        pendingWorkLocation = null

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ble_connect, null)
        val locationGroup: RadioGroup = dialogView.findViewById(R.id.groupWorkLocation)
        val listDialogBle: ListView = dialogView.findViewById(R.id.listDialogBle)
        val btnDialogFakeData: Button = dialogView.findViewById(R.id.btnDialogFakeData)
        dialogStatusText = dialogView.findViewById(R.id.txtDialogStatus)

        WorkLocation.OPTIONS.forEachIndexed { index, location ->
            val radioButton = RadioButton(this)
            radioButton.id = index + 1
            radioButton.text = location.name
            radioButton.textSize = 17f
            locationGroup.addView(radioButton)
        }

        dialogDeviceAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, ArrayList<String>())
        listDialogBle.adapter = dialogDeviceAdapter
        listDialogBle.choiceMode = ListView.CHOICE_MODE_SINGLE

        locationGroup.setOnCheckedChangeListener { _, checkedId ->
            pendingWorkLocation = WorkLocation.OPTIONS.getOrNull(checkedId - 1)
            updateDialogConnectEnabled()
        }
        listDialogBle.setOnItemClickListener { _, _, position, _ ->
            dialogSelectedDeviceIndex = position
            updateDialogConnectEnabled()
        }
        btnDialogFakeData.setOnClickListener { enableFakeDataButtonFromDialog() }

        connectDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("취소") { dialog, _ ->
                bleManager.stopScan()
                dialog.dismiss()
            }
            .setPositiveButton("연결", null)
            .create()

        connectDialog?.setOnShowListener { dialogInterface ->
            val dialog = dialogInterface as AlertDialog
            dialogConnectButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            dialogConnectButton?.isEnabled = false
            dialogConnectButton?.setOnClickListener { connectSelectedDevice() }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.ss_header))
            dialogConnectButton?.setTextColor(ContextCompat.getColor(this, R.color.ss_header))
            startDialogScan(canScan)
        }
        connectDialog?.setOnDismissListener {
            dialogDeviceAdapter = null
            dialogStatusText = null
            dialogConnectButton = null
            connectDialog = null
        }
        connectDialog?.show()
    }

    private fun enableFakeDataButtonFromDialog() {
        fakeTestMode = true
        pendingWorkLocation?.let { selectedWorkLocation = it }
        selectedWorkLocation?.let { ui.showWorkLocation(it.name) }
        bleManager.stopScan()
        connectDialog?.dismiss()
        ui.showFakeDataButton(true)
        Toast.makeText(this, "Fake 데이터 테스트 버튼이 활성화되었습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun ensureBleReady(requestPermission: Boolean = true): Boolean {
        if (!BlePermissionHelper.hasBlePermission(this)) {
            if (requestPermission) BlePermissionHelper.requestBlePermission(this)
            dialogStatusText?.text = "BLE 권한이 필요합니다."
            return false
        }
        if (!bleManager.isBluetoothAvailable()) {
            Toast.makeText(this, "BLE를 사용할 수 없는 기기입니다.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!bleManager.isBluetoothEnabled()) {
            Toast.makeText(this, "블루투스를 먼저 켜주세요.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun startDialogScan(canScan: Boolean) {
        setWorkSessionState(WorkSessionState.IDLE)
        if (!canScan) {
            dialogStatusText?.text = "BLE 권한이 필요합니다. 권한 허용 후 작업 시작을 다시 눌러주세요."
            return
        }
        dialogStatusText?.text = "주변 Smart Shield 기기를 검색 중입니다."
        bleManager.startScan()
    }

    private fun updateDialogConnectEnabled() {
        val hasLocation = pendingWorkLocation != null
        val hasDevice = dialogSelectedDeviceIndex in foundDeviceList.indices
        dialogConnectButton?.isEnabled = hasLocation && hasDevice
        dialogStatusText?.text = when {
            !hasLocation && !hasDevice -> "작업 위치와 BLE 기기를 선택하세요."
            !hasLocation -> "작업 위치를 선택하세요."
            !hasDevice -> "BLE 기기를 선택하세요."
            else -> "연결할 준비가 되었습니다."
        }
    }

    private fun connectSelectedDevice() {
        if (!ensureBleReady()) return
        val location = pendingWorkLocation ?: return
        val deviceInfo = foundDeviceList.getOrNull(dialogSelectedDeviceIndex) ?: return

        // 선택 위치와 장치 기준 연결 준비 전환
        selectedWorkLocation = location
        workStartedAtMillis = null
        workEndedAtMillis = null
        baselinePosture = null
        resetTempBaselineState()
        baselineHr = null
        appSessionActive = false
        fakeTestMode = false
        bleSignalLevel = BleSignalLevel.DISCONNECTED
        bleRssi = null

        ui.showWorkLocation(location.name)
        ui.showWorkStartedAt(null)
        ui.showBleSignal(bleSignalLevel)
        setWorkSessionState(WorkSessionState.CONNECTING)
        currentBleState = "연결 시도 중"
        ui.showBleState(currentBleState)

        connectDialog?.dismiss()
        bleManager.connect(deviceInfo.device)
    }

    private fun showEndWorkDialog() {
        AlertDialog.Builder(this)
            .setTitle("작업을 종료하시겠습니까?")
            .setMessage("작업을 종료하면 BLE 연결이 해제되고 현재 작업 세션이 종료됩니다.")
            .setNegativeButton("취소", null)
            .setPositiveButton("작업 종료") { _, _ -> endWorkManually() }
            .show()
    }

    private fun endWorkManually() {
        appSessionActive = false
        workEndedAtMillis = System.currentTimeMillis()
        bleManager.disconnectManually()
        foregroundServiceController.stop()
        currentBleState = "연결 끊김"
        bleSignalLevel = BleSignalLevel.DISCONNECTED
        bleRssi = null
        uploadLastStatus(bleConnected = false, appSessionActive = false)
        resetTempBaselineState()
        baselineHr = null
        baselinePosture = null
        ui.showBleState(currentBleState)
        ui.showBleSignal(bleSignalLevel)
        setWorkSessionState(WorkSessionState.ENDED)
    }

    private fun handleFakeDataClicked() {
        if (selectedWorkLocation == null) selectedWorkLocation = WorkLocation.OPTIONS.first()
        fakeTestMode = true
        appSessionActive = true
        if (workStartedAtMillis == null) workStartedAtMillis = System.currentTimeMillis()
        if (baselineStartTimeMillis == null) resetTempBaselineState()
        workEndedAtMillis = null
        currentBleState = "연결 전"
        bleSignalLevel = BleSignalLevel.NOT_CONNECTED
        bleRssi = null

        ui.showWorkLocation(selectedWorkLocation?.name)
        ui.showWorkStartedAt(formatMillis(workStartedAtMillis))
        ui.showBleState(currentBleState)
        ui.showBleSignal(bleSignalLevel)
        setWorkSessionState(WorkSessionState.WORKING)

        val fakeData = FakeSensorDataProvider.nextPayload()
        Log.d(TAG, "Fake data generated: $fakeData")
        handleReceivedData(fakeData)
    }

    override fun onScanStarted() {
        currentBleState = "연결 전"
        ui.showBleState(currentBleState)
        ui.showReconnectStatus("스캔 중")
        ui.showNoConnectedDevice()
    }

    override fun onScanStopped() {
        dialogStatusText?.text = if (foundDeviceList.isEmpty()) "검색된 Smart Shield 기기가 없습니다." else "기기를 선택하세요."
    }

    override fun onScanFailed(errorCode: Int) {
        currentBleState = "연결 전"
        ui.showBleState(currentBleState)
        dialogStatusText?.text = "BLE 스캔 실패($errorCode)"
    }

    override fun onDeviceFound(deviceInfo: BleManager.BleDeviceInfo) {
        runOnUiThread {
            if (foundDeviceList.any { it.address == deviceInfo.address }) return@runOnUiThread
            foundDeviceList.add(deviceInfo)
            dialogDeviceAdapter?.add("${deviceInfo.name}\n${deviceInfo.address}")
            dialogDeviceAdapter?.notifyDataSetChanged()
            dialogStatusText?.text = "기기를 선택하세요."
        }
    }

    override fun onBleStatusChanged(message: String) {
        currentBleState = normalizeBleState(message)
        ui.showBleState(currentBleState)
        if (currentBleState == "연결 끊김") {
            bleSignalLevel = BleSignalLevel.DISCONNECTED
            ui.showBleSignal(bleSignalLevel)
        }
    }

    override fun onReconnectStatusChanged(message: String) {
        ui.showReconnectStatus(message)
        if (message.contains("재연결")) {
            setWorkSessionState(WorkSessionState.RECONNECTING)
            currentBleState = "재연결 중"
            ui.showBleState(currentBleState)
        }
    }

    override fun onConnected(deviceName: String, address: String) {
        appSessionActive = true
        if (workStartedAtMillis == null) workStartedAtMillis = System.currentTimeMillis()
        if (baselineStartTimeMillis == null) baselineStartTimeMillis = workStartedAtMillis
        workEndedAtMillis = null
        foregroundServiceController.startIfAllowed()

        currentBleState = "연결됨"
        bleSignalLevel = BleSignalLevel.NORMAL
        ui.showBleState(currentBleState)
        ui.showBleSignal(bleSignalLevel)
        ui.showReconnectStatus("대기")
        ui.showConnectedDevice(deviceName, address)
        ui.showWorkStartedAt(formatMillis(workStartedAtMillis))
        setWorkSessionState(WorkSessionState.WORKING)
        updateWorkerDetailSnapshot()
        uploadLastStatus(bleConnected = true, appSessionActive = true)
    }

    override fun onDisconnected(manual: Boolean) {
        appSessionActive = !manual
        currentBleState = if (manual) "연결 끊김" else "재연결 중"
        bleSignalLevel = BleSignalLevel.DISCONNECTED
        bleRssi = null
        uploadLastStatus(bleConnected = false, appSessionActive = appSessionActive)
        ui.showBleState(currentBleState)
        ui.showBleSignal(bleSignalLevel)
        ui.showReconnectStatus(if (manual) "중지" else "재연결 중")
        if (manual) {
            foregroundServiceController.stop()
            setWorkSessionState(WorkSessionState.ENDED)
        } else {
            setWorkSessionState(WorkSessionState.RECONNECTING)
        }
        updateWorkerDetailSnapshot()
    }

    override fun onReconnectFailed() {
        appSessionActive = false
        workEndedAtMillis = System.currentTimeMillis()
        currentBleState = "재연결 실패"
        bleSignalLevel = BleSignalLevel.DISCONNECTED
        bleRssi = null
        uploadLastStatus(bleConnected = false, appSessionActive = false)
        ui.showBleState(currentBleState)
        ui.showBleSignal(bleSignalLevel)
        ui.showReconnectStatus("재연결 실패")
        ui.showNoConnectedDevice()
        foregroundServiceController.stop()
        setWorkSessionState(WorkSessionState.ENDED)
    }

    override fun onNotifyReady() {
        currentBleState = "연결됨"
        ui.showBleState(currentBleState)
        ui.showRiskCommand("Write 준비 완료")
    }

    override fun onDataReceived(rawData: String) {
        handleReceivedData(rawData)
    }

    override fun onWriteResult(command: String, started: Boolean, reason: String?) {
        ui.showWriteResult(command, started, reason)
    }

    override fun onRssiUpdated(rssi: Int) {
        bleRssi = rssi
        bleSignalLevel = BleSignalLevel.fromRssi(rssi)
        ui.showBleSignal(bleSignalLevel)
        updateWorkerDetailSnapshot()
    }

    private fun handleReceivedData(rawData: String) {
        val cleanedRawData = rawData.trim()
        Log.d(TAG, "Raw sensor data: $cleanedRawData")

        // BLE Notify 한 줄을 위험도, UI, Firebase, ESP32 명령에 반영
        if (cleanedRawData.isEmpty()) {
            showParseError(rawData)
            return
        }

        val sensorData = SensorDataParser.parse(cleanedRawData)
        if (sensorData == null) {
            showParseError(cleanedRawData)
            return
        }

        Log.d(TAG, "[TEMP] raw=${sensorData.temp} valid=${sensorData.tempValid} source=${sensorData.tempSource}")
        val tempSnapshot = updateTempBaseline(sensorData)
        val riskLevel = HeatstrokeAnalyzer.analyze(
            data = sensorData,
            baselineTemp = HeatstrokeAnalyzer.sanitizeBaselineTemp(baselineTemp),
            baselineHR = HeatstrokeAnalyzer.sanitizeBaselineHr(baselineHr),
            tempBaselineReady = tempSnapshot.baselineTempReady,
            stableDeltaTemp = tempSnapshot.stableDeltaTemp
        )
        val command = RiskCommandMapper.toCommand(riskLevel)
        lastUpdatedAt = formatNow()

        lastSensorData = sensorData
        lastRiskLevel = riskLevel
        lastRiskCommand = command

        // 작업 시작 직후 정상 자세 기준값 저장
        if (baselinePosture == null) baselinePosture = sensorData.posture
        if (baselineHr == null && sensorData.posture.equals("NORMAL", ignoreCase = true)) {
            baselineHr = sensorData.hr
        }

        ui.showSensorData(sensorData, riskLevel, lastUpdatedAt, tempSnapshot)
        ui.showRisk(riskLevel)
        ui.showRiskCommand(command)
        updateWorkerDetailSnapshot()

        bleManager.writeRiskCommand(command)
        uploadCurrentStatus(sensorData, riskLevel, command, tempSnapshot)
        uploadRiskLogIfNeeded(sensorData, riskLevel, command, tempSnapshot)
        alertManager.handleRisk(riskLevel)
    }

    private fun updateTempBaseline(sensorData: SensorData): TempBaselineSnapshot {
        val sessionStart = workStartedAtMillis ?: System.currentTimeMillis().also {
            workStartedAtMillis = it
        }
        if (baselineStartTimeMillis == null) baselineStartTimeMillis = sessionStart

        if (!sensorData.tempValid) {
            tempBaselineStatus = "SENSOR_ERROR"
            latestDeltaTemp = null
            latestStableDeltaTemp = null
            recentDeltaTempSamples.clear()
            Log.d(TAG, "[TEMP_ERROR] sensor invalid, excluded from risk calculation")
            return currentTempSnapshot()
        }

        val elapsedMillis = System.currentTimeMillis() - (baselineStartTimeMillis ?: sessionStart)
        val stabilizationMillis = TEMP_STABILIZATION_SECONDS * 1000L
        val collectionMillis = TEMP_BASELINE_COLLECTION_SECONDS * 1000L

        if (!baselineTempReady) {
            if (elapsedMillis < stabilizationMillis) {
                tempBaselineStatus = "STABILIZING"
                Log.d(TAG, "[TEMP_BASELINE] status=STABILIZING")
                return currentTempSnapshot()
            }

            tempBaselineStatus = "COLLECTING"
            if (sensorData.posture.equals("NORMAL", ignoreCase = true)) {
                baselineTempSamples.add(sensorData.temp)
            }
            Log.d(TAG, "[TEMP_BASELINE] collecting count=${baselineTempSamples.size}")

            val collectionElapsed = elapsedMillis - stabilizationMillis
            if (
                collectionElapsed >= collectionMillis &&
                baselineTempSamples.size >= TEMP_MIN_VALID_SAMPLE_COUNT
            ) {
                baselineTemp = median(baselineTempSamples)
                baselineTempReady = true
                tempBaselineStatus = "READY"
                Log.d(TAG, "[TEMP_BASELINE] ready baseline=${baselineTemp}")
            }
            return currentTempSnapshot()
        }

        tempBaselineStatus = "READY"
        val baseline = baselineTemp
        if (baseline != null) {
            latestDeltaTemp = sensorData.temp - baseline
            updateStableDeltaTemp(latestDeltaTemp ?: 0.0)
            Log.d(TAG, "[TEMP_TREND] delta=${latestDeltaTemp} stableDelta=${latestStableDeltaTemp}")
        }
        return currentTempSnapshot()
    }

    private fun updateStableDeltaTemp(deltaTemp: Double) {
        val now = System.currentTimeMillis()
        recentDeltaTempSamples.add(now to deltaTemp)
        val cutoff = now - (TEMP_TREND_WINDOW_SECONDS * 1000L)
        recentDeltaTempSamples.removeAll { it.first < cutoff }
        latestStableDeltaTemp = if (recentDeltaTempSamples.size >= TEMP_TREND_MIN_SAMPLE_COUNT) {
            median(recentDeltaTempSamples.map { it.second })
        } else {
            null
        }
    }

    private fun currentTempSnapshot(): TempBaselineSnapshot {
        return TempBaselineSnapshot(
            baselineTemp = baselineTemp,
            baselineTempReady = baselineTempReady,
            deltaTemp = latestDeltaTemp,
            stableDeltaTemp = latestStableDeltaTemp,
            status = tempBaselineStatus
        )
    }

    private fun resetTempBaselineState() {
        baselineTemp = null
        baselineTempReady = false
        baselineStartTimeMillis = System.currentTimeMillis()
        baselineTempSamples.clear()
        recentDeltaTempSamples.clear()
        latestDeltaTemp = null
        latestStableDeltaTemp = null
        tempBaselineStatus = "STABILIZING"
    }

    private fun median(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        } else {
            sorted[middle]
        }
    }

    private fun uploadCurrentStatus(
        sensorData: SensorData,
        riskLevel: RiskLevel,
        command: String,
        tempSnapshot: TempBaselineSnapshot
    ) {
        ui.showFirebaseState("currentStatus 업로드 중")
        FirebaseStatusUploader.uploadCurrentStatus(
            workerId = sensorData.id,
            deviceName = bleManager.connectedDeviceName,
            temp = sensorData.temp,
            tempValid = sensorData.tempValid,
            tempSource = sensorData.tempSource,
            baselineTemp = tempSnapshot.baselineTemp,
            baselineTempReady = tempSnapshot.baselineTempReady,
            deltaTemp = tempSnapshot.deltaTemp,
            stableDeltaTemp = tempSnapshot.stableDeltaTemp,
            tempBaselineStatus = tempSnapshot.status,
            hr = sensorData.hr,
            spo2 = sensorData.spo2,
            env = sensorData.env,
            hum = sensorData.hum.toDouble(),
            lux = sensorData.lux,
            ax = sensorData.ax,
            ay = sensorData.ay,
            az = sensorData.az,
            posture = sensorData.posture,
            riskLevel = riskLevel.label,
            riskCommand = command,
            bleConnected = bleManager.isBleConnected,
            appSessionActive = appSessionActive,
            workLocationCode = selectedWorkLocation?.code,
            workLocationName = selectedWorkLocation?.name,
            workStartedAt = workStartedAtMillis,
            workStartedAtText = formatMillisOrNull(workStartedAtMillis),
            workEndedAt = workEndedAtMillis,
            workEndedAtText = formatMillisOrNull(workEndedAtMillis),
            bleSignalLevel = bleSignalLevel.label,
            bleRssi = bleRssi,
            weatherAlert = weatherSnapshot.alert,
            todayMaxTemp = weatherSnapshot.todayMaxTemp,
            weatherRegion = weatherSnapshot.region,
            baselinePosture = baselinePosture,
            callback = { success, message -> showFirebaseUploadResult(success, message) }
        )
    }

    private fun uploadRiskLogIfNeeded(
        sensorData: SensorData,
        riskLevel: RiskLevel,
        command: String,
        tempSnapshot: TempBaselineSnapshot
    ) {
        if (!riskLogPolicy.shouldUpload(riskLevel)) return
        FirebaseStatusUploader.uploadRiskLog(
            workerId = sensorData.id,
            riskLevel = riskLevel.label,
            riskCommand = command,
            temp = sensorData.temp,
            tempValid = sensorData.tempValid,
            tempSource = sensorData.tempSource,
            baselineTemp = tempSnapshot.baselineTemp,
            baselineTempReady = tempSnapshot.baselineTempReady,
            deltaTemp = tempSnapshot.deltaTemp,
            stableDeltaTemp = tempSnapshot.stableDeltaTemp,
            tempBaselineStatus = tempSnapshot.status,
            hr = sensorData.hr,
            spo2 = sensorData.spo2,
            env = sensorData.env,
            hum = sensorData.hum.toDouble(),
            lux = sensorData.lux,
            ax = sensorData.ax,
            ay = sensorData.ay,
            az = sensorData.az,
            posture = sensorData.posture,
            message = riskLogPolicy.messageFor(riskLevel),
            workLocationCode = selectedWorkLocation?.code,
            workLocationName = selectedWorkLocation?.name,
            workStartedAt = workStartedAtMillis,
            workStartedAtText = formatMillisOrNull(workStartedAtMillis),
            bleSignalLevel = bleSignalLevel.label,
            bleRssi = bleRssi,
            weatherAlert = weatherSnapshot.alert,
            todayMaxTemp = weatherSnapshot.todayMaxTemp,
            callback = { success, message -> showFirebaseUploadResult(success, message) }
        )
    }

    private fun uploadLastStatus(bleConnected: Boolean, appSessionActive: Boolean) {
        val sensorData = lastSensorData
        if (sensorData != null) {
            FirebaseStatusUploader.uploadCurrentStatus(
                workerId = sensorData.id,
                deviceName = bleManager.connectedDeviceName,
                temp = sensorData.temp,
                tempValid = sensorData.tempValid,
                tempSource = sensorData.tempSource,
                baselineTemp = baselineTemp,
                baselineTempReady = baselineTempReady,
                deltaTemp = latestDeltaTemp,
                stableDeltaTemp = latestStableDeltaTemp,
                tempBaselineStatus = tempBaselineStatus,
                hr = sensorData.hr,
                spo2 = sensorData.spo2,
                env = sensorData.env,
                hum = sensorData.hum.toDouble(),
                lux = sensorData.lux,
                ax = sensorData.ax,
                ay = sensorData.ay,
                az = sensorData.az,
                posture = sensorData.posture,
                riskLevel = lastRiskLevel.label,
                riskCommand = lastRiskCommand,
                bleConnected = bleConnected,
                appSessionActive = appSessionActive,
                workLocationCode = selectedWorkLocation?.code,
                workLocationName = selectedWorkLocation?.name,
                workStartedAt = workStartedAtMillis,
                workStartedAtText = formatMillisOrNull(workStartedAtMillis),
                workEndedAt = workEndedAtMillis,
                workEndedAtText = formatMillisOrNull(workEndedAtMillis),
                bleSignalLevel = bleSignalLevel.label,
                bleRssi = bleRssi,
                weatherAlert = weatherSnapshot.alert,
                todayMaxTemp = weatherSnapshot.todayMaxTemp,
                weatherRegion = weatherSnapshot.region,
                baselinePosture = baselinePosture,
                callback = { success, message -> showFirebaseUploadResult(success, message) }
            )
            return
        }

        val workerId = workerIdFromConnectedDeviceName()
        if (workerId == null) {
            Log.e(TAG, "sessionStatus upload skipped. workerId cannot be extracted from deviceName=${bleManager.connectedDeviceName}")
            return
        }

        FirebaseStatusUploader.uploadSessionStatusOnly(
            workerId = workerId,
            deviceName = bleManager.connectedDeviceName,
            workLocationCode = selectedWorkLocation?.code,
            workLocationName = selectedWorkLocation?.name,
            workStartedAt = workStartedAtMillis,
            workStartedAtText = formatMillisOrNull(workStartedAtMillis),
            workEndedAt = workEndedAtMillis,
            workEndedAtText = formatMillisOrNull(workEndedAtMillis),
            bleConnected = bleConnected,
            bleSignalLevel = bleSignalLevel.label,
            bleRssi = bleRssi,
            appSessionActive = appSessionActive,
            weatherAlert = weatherSnapshot.alert,
            todayMaxTemp = weatherSnapshot.todayMaxTemp,
            weatherRegion = weatherSnapshot.region,
            baselinePosture = baselinePosture,
            callback = { success, message -> showFirebaseUploadResult(success, message) }
        )
    }

    private fun showFirebaseUploadResult(success: Boolean, message: String) {
        runOnUiThread {
            ui.showFirebaseState(message)
            if (!success) Log.e(TAG, message)
        }
    }

    private fun openWorkerDetail() {
        updateWorkerDetailSnapshot()
        startActivity(Intent(this, WorkerDetailActivity::class.java))
    }

    private fun updateWorkerDetailSnapshot() {
        WorkerStatusStore.update(buildWorkerDetailSnapshot(), applicationContext)
    }

    private fun buildWorkerDetailSnapshot(): WorkerDetailSnapshot {
        val sensorData = lastSensorData
        return WorkerDetailSnapshot(
            workerId = sensorData?.id ?: workerIdFromConnectedDeviceName() ?: "-",
            workLocationCode = selectedWorkLocation?.code ?: "-",
            workLocationName = selectedWorkLocation?.name ?: "-",
            workStartedAt = formatMillis(workStartedAtMillis),
            bleState = currentBleState,
            bleSignalLevel = bleSignalLevel.label,
            bleRssi = bleRssi?.toString() ?: "-",
            riskLevel = lastRiskLevel.label,
            temp = sensorData?.let { formatTempForDetail(it) } ?: "-",
            hr = sensorData?.hr?.let { "$it bpm" } ?: "-",
            spo2 = sensorData?.spo2?.let { "$it %" } ?: "-",
            env = sensorData?.env?.let { "%.1f ℃".format(it) } ?: "-",
            hum = sensorData?.hum?.let { "$it %" } ?: "-",
            lux = sensorData?.lux?.let { "$it lx" } ?: "-",
            axis = sensorData?.let { formatAxis(it) } ?: "-",
            posture = sensorData?.posture ?: "-",
            baselinePosture = baselinePosture ?: "-",
            weatherAlert = weatherSnapshot.alert,
            weatherRegion = weatherSnapshot.region,
            todayMaxTemp = weatherSnapshot.todayMaxTemp?.let { "%.1f ℃".format(it) } ?: "-",
            lastUpdatedAt = lastUpdatedAt
        )
    }

    private fun formatAxis(sensorData: SensorData): String {
        val ax = sensorData.ax?.let { "%.2f".format(it) } ?: "-"
        val ay = sensorData.ay?.let { "%.2f".format(it) } ?: "-"
        val az = sensorData.az?.let { "%.2f".format(it) } ?: "-"
        return "X:$ax, Y:$ay, Z:$az"
    }

    private fun formatTempForDetail(sensorData: SensorData): String {
        if (!sensorData.tempValid) return "측정 불가"
        val stableDelta = latestStableDeltaTemp
        return if (baselineTempReady && stableDelta != null) {
            "%.1f ℃ / 기준 대비 %+.1f ℃".format(sensorData.temp, stableDelta)
        } else {
            "%.1f ℃ (기준값 측정 중)".format(sensorData.temp)
        }
    }

    private fun setWorkSessionState(state: WorkSessionState) {
        workSessionState = state
        ui.updateWorkSessionState(state)
    }

    private fun showParseError(rawData: String) {
        Log.w(TAG, "Sensor parse failed. rawData=$rawData")
        ui.showParseError(rawData)
    }

    private fun normalizeBleState(message: String): String {
        return when {
            message.contains("재연결 실패") -> "재연결 실패"
            message.contains("재연결") -> "재연결 중"
            message.contains("시도") -> "연결 시도 중"
            message.contains("끊김") || message.contains("오프라인") -> "연결 끊김"
            message.contains("연결됨") || message.contains("수신") -> "연결됨"
            else -> message
        }
    }

    private fun prepareWeatherPlaceholder() {
        // 기상청 연동 전 지역 단위 참고값 표시
        weatherSnapshot = WeatherSnapshot(alert = "연결 전", todayMaxTemp = null, region = "대전")
        ui.showWeather(weatherSnapshot.alert, weatherSnapshot.todayMaxTemp, weatherSnapshot.region)
    }

    private fun workerIdFromConnectedDeviceName(): String? {
        return Regex("^SS_(\\d{4})$").find(bleManager.connectedDeviceName)?.groupValues?.getOrNull(1)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATION_PERMISSION
            )
        }
    }

    private fun formatNow(): String {
        return koreaDateFormat().format(Date())
    }

    private fun formatMillis(millis: Long?): String {
        return formatMillisOrNull(millis) ?: "-"
    }

    private fun formatMillisOrNull(millis: Long?): String? {
        return millis?.let { koreaDateFormat().format(Date(it)) }
    }

    private fun koreaDateFormat(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).apply {
            timeZone = TimeZone.getTimeZone("Asia/Seoul")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appSessionActive = false
        if (workEndedAtMillis == null) workEndedAtMillis = System.currentTimeMillis()
        uploadLastStatus(bleConnected = false, appSessionActive = false)
        bleManager.release()
        foregroundServiceController.stop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            BleConstants.REQUEST_BLE_PERMISSION -> {
                val isGranted = grantResults.isNotEmpty() &&
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                Toast.makeText(
                    this,
                    if (isGranted) "BLE 권한이 허용되었습니다. 작업 시작을 다시 눌러주세요." else "BLE 권한이 필요합니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            REQUEST_NOTIFICATION_PERMISSION -> {
                Log.d(TAG, "Notification permission result=${grantResults.toList()}")
            }
        }
    }
}
