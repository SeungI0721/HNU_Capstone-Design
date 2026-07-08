// Firebase Realtime Database에 작업자 현재 상태와 위험 로그를 업로드하는 파일
package com.example.hnu_ppe_control.firebase

import android.util.Log
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase

private typealias UploadCallback = (success: Boolean, message: String) -> Unit
private data class PendingCurrentStatusUpload(
    val workerId: String,
    val data: Map<String, Any>,
    val callback: UploadCallback?
)

object FirebaseStatusUploader {

    private const val TAG = "SmartShieldFirebase"
    private const val DATABASE_URL = "https://hnu-ppe-default-rtdb.asia-southeast1.firebasedatabase.app"
    private val database = FirebaseDatabase.getInstance(DATABASE_URL)
    private var currentStatusUploadInFlight = false
    private var pendingCurrentStatusUpload: PendingCurrentStatusUpload? = null

    // Firebase 연결 상태를 앱 시작 시 확인해 업로드 실패 원인을 UI에 표시할 수 있게 합니다.
    fun startRealtimeConnection(callback: UploadCallback? = null) {
        database.goOnline()
        database.getReference(".info/connected").get()
            .addOnSuccessListener { snapshot ->
                callback?.invoke(true, "Firebase 연결 준비: ${snapshot.value}")
            }
            .addOnFailureListener { error ->
                callback?.invoke(false, "Firebase 연결 확인 실패: ${error.message}")
            }
    }

    fun uploadCurrentStatus(
        workerId: String,
        deviceName: String,
        temp: Double,
        tempValid: Boolean,
        tempSource: String,
        baselineTemp: Double?,
        baselineTempReady: Boolean,
        deltaTemp: Double?,
        stableDeltaTemp: Double?,
        tempBaselineStatus: String,
        hr: Int,
        spo2: Int?,
        env: Double,
        hum: Double,
        lux: Int,
        ax: Double?,
        ay: Double?,
        az: Double?,
        posture: String,
        riskLevel: String,
        riskCommand: String,
        bleConnected: Boolean,
        appSessionActive: Boolean,
        workLocationCode: String?,
        workLocationName: String?,
        workStartedAt: Long?,
        workStartedAtText: String?,
        workEndedAt: Long?,
        workEndedAtText: String?,
        bleSignalLevel: String?,
        bleRssi: Int?,
        weatherAlert: String?,
        todayMaxTemp: Double?,
        weatherRegion: String?,
        baselinePosture: String?,
        callback: UploadCallback? = null
    ) {
        // workerId는 Firebase 경로에 직접 사용되므로 빈 값이면 업로드를 중단합니다.
        if (workerId.isBlank()) {
            Log.e(TAG, "currentStatus upload FAILED. workerId is blank")
            callback?.invoke(false, "currentStatus 실패: workerId 없음")
            return
        }

        val data = baseSessionMap(
            workerId = workerId,
            deviceName = deviceName,
            workLocationCode = workLocationCode,
            workLocationName = workLocationName,
            workStartedAt = workStartedAt,
            workStartedAtText = workStartedAtText,
            workEndedAt = workEndedAt,
            workEndedAtText = workEndedAtText,
            bleConnected = bleConnected,
            bleSignalLevel = bleSignalLevel,
            bleRssi = bleRssi,
            appSessionActive = appSessionActive,
            weatherAlert = weatherAlert,
            todayMaxTemp = todayMaxTemp,
            weatherRegion = weatherRegion,
            baselinePosture = baselinePosture
        )

        // 유효하지 않은 피부 온도는 숫자 필드 호환성을 위해 0.0으로 저장하고 별도 플래그로 구분합니다.
        data["temp"] = if (tempValid) temp else 0.0
        data["tempValid"] = tempValid
        data["tempSource"] = tempSource
        data["baselineTempReady"] = baselineTempReady
        data["tempBaselineStatus"] = tempBaselineStatus
        baselineTemp?.let { data["baselineTemp"] = it }
        deltaTemp?.let { data["deltaTemp"] = it }
        stableDeltaTemp?.let { data["stableDeltaTemp"] = it }
        data["hr"] = hr
        data["env"] = env
        data["hum"] = hum
        data["lux"] = lux
        data["directSunlight"] = lux >= 50000
        data["posture"] = posture
        data["riskLevel"] = riskLevel
        data["riskCommand"] = riskCommand
        spo2?.let { data["spo2"] = it }
        ax?.let { data["ax"] = it }
        ay?.let { data["ay"] = it }
        az?.let { data["az"] = it }

        uploadCurrentStatusMap(workerId, data, callback)
    }

    fun uploadSessionStatusOnly(
        workerId: String,
        deviceName: String,
        workLocationCode: String?,
        workLocationName: String?,
        workStartedAt: Long?,
        workStartedAtText: String?,
        workEndedAt: Long?,
        workEndedAtText: String?,
        bleConnected: Boolean,
        bleSignalLevel: String?,
        bleRssi: Int?,
        appSessionActive: Boolean,
        weatherAlert: String?,
        todayMaxTemp: Double?,
        weatherRegion: String?,
        baselinePosture: String?,
        callback: UploadCallback? = null
    ) {
        if (workerId.isBlank()) {
            Log.e(TAG, "sessionStatus upload FAILED. workerId is blank")
            callback?.invoke(false, "sessionStatus 실패: workerId 없음")
            return
        }

        val data = baseSessionMap(
            workerId = workerId,
            deviceName = deviceName,
            workLocationCode = workLocationCode,
            workLocationName = workLocationName,
            workStartedAt = workStartedAt,
            workStartedAtText = workStartedAtText,
            workEndedAt = workEndedAt,
            workEndedAtText = workEndedAtText,
            bleConnected = bleConnected,
            bleSignalLevel = bleSignalLevel,
            bleRssi = bleRssi,
            appSessionActive = appSessionActive,
            weatherAlert = weatherAlert,
            todayMaxTemp = todayMaxTemp,
            weatherRegion = weatherRegion,
            baselinePosture = baselinePosture
        )

        uploadCurrentStatusMap(workerId, data, callback)
    }

    fun uploadRiskLog(
        workerId: String,
        riskLevel: String,
        riskCommand: String,
        temp: Double,
        tempValid: Boolean,
        tempSource: String,
        baselineTemp: Double?,
        baselineTempReady: Boolean,
        deltaTemp: Double?,
        stableDeltaTemp: Double?,
        tempBaselineStatus: String,
        hr: Int,
        spo2: Int?,
        env: Double,
        hum: Double,
        lux: Int,
        ax: Double?,
        ay: Double?,
        az: Double?,
        posture: String,
        message: String,
        workLocationCode: String?,
        workLocationName: String?,
        workStartedAt: Long?,
        workStartedAtText: String?,
        bleSignalLevel: String?,
        bleRssi: Int?,
        weatherAlert: String?,
        todayMaxTemp: Double?,
        callback: UploadCallback? = null
    ) {
        // 위험 로그는 currentStatus와 달리 이벤트 기록이므로 push key로 누적 저장합니다.
        if (workerId.isBlank()) {
            Log.e(TAG, "riskLog upload FAILED. workerId is blank")
            callback?.invoke(false, "riskLogs 실패: workerId 없음")
            return
        }

        database.goOnline()

        val ref = database.getReference("workers")
            .child(workerId)
            .child("riskLogs")
            .push()

        val data = mutableMapOf<String, Any>(
            "workerId" to workerId,
            "riskLevel" to riskLevel,
            "riskCommand" to riskCommand,
            "riskMessage" to message,
            "temp" to if (tempValid) temp else 0.0,
            "tempValid" to tempValid,
            "tempSource" to tempSource,
            "baselineTempReady" to baselineTempReady,
            "tempBaselineStatus" to tempBaselineStatus,
            "hr" to hr,
            "env" to env,
            "hum" to hum,
            "lux" to lux,
            "directSunlight" to (lux >= 50000),
            "posture" to posture,
            "message" to message,
            "createdAt" to System.currentTimeMillis()
        )

        spo2?.let { data["spo2"] = it }
        baselineTemp?.let { data["baselineTemp"] = it }
        deltaTemp?.let { data["deltaTemp"] = it }
        stableDeltaTemp?.let { data["stableDeltaTemp"] = it }
        ax?.let { data["ax"] = it }
        ay?.let { data["ay"] = it }
        az?.let { data["az"] = it }
        putIfNotBlank(data, "workLocationCode", workLocationCode)
        putIfNotBlank(data, "workLocationName", workLocationName)
        workStartedAt?.let { data["workStartedAt"] = it }
        putIfNotBlank(data, "workStartedAtText", workStartedAtText)
        putIfNotBlank(data, "bleSignalLevel", bleSignalLevel)
        bleRssi?.let { data["bleRssi"] = it }
        putIfNotBlank(data, "weatherAlert", weatherAlert)
        todayMaxTemp?.let { data["todayMaxTemp"] = it }

        Log.d(TAG, "riskLog upload START path=workers/$workerId/riskLogs data=$data")

        ref.setValue(data) { error: DatabaseError?, _ ->
            if (error == null) {
                Log.d(TAG, "riskLog upload SUCCESS")
                callback?.invoke(true, "riskLogs 업로드 성공")
            } else {
                val message = "riskLogs 실패: ${error.message}"
                Log.e(TAG, "riskLog upload FAILED. code=${error.code}, message=${error.message}, details=${error.details}")
                callback?.invoke(false, message)
            }
        }
    }

    private fun baseSessionMap(
        workerId: String,
        deviceName: String,
        workLocationCode: String?,
        workLocationName: String?,
        workStartedAt: Long?,
        workStartedAtText: String?,
        workEndedAt: Long?,
        workEndedAtText: String?,
        bleConnected: Boolean,
        bleSignalLevel: String?,
        bleRssi: Int?,
        appSessionActive: Boolean,
        weatherAlert: String?,
        todayMaxTemp: Double?,
        weatherRegion: String?,
        baselinePosture: String?
    ): MutableMap<String, Any> {
        val data = mutableMapOf<String, Any>(
            "workerId" to workerId,
            "deviceName" to deviceName,
            "bleConnected" to bleConnected,
            "appSessionActive" to appSessionActive,
            "updatedAt" to System.currentTimeMillis()
        )

        putIfNotBlank(data, "workLocationCode", workLocationCode)
        putIfNotBlank(data, "workLocationName", workLocationName)
        workStartedAt?.let { data["workStartedAt"] = it }
        putIfNotBlank(data, "workStartedAtText", workStartedAtText)
        workEndedAt?.let { data["workEndedAt"] = it }
        putIfNotBlank(data, "workEndedAtText", workEndedAtText)
        putIfNotBlank(data, "bleSignalLevel", bleSignalLevel)
        bleRssi?.let { data["bleRssi"] = it }
        putIfNotBlank(data, "weatherAlert", weatherAlert)
        todayMaxTemp?.let { data["todayMaxTemp"] = it }
        putIfNotBlank(data, "weatherRegion", weatherRegion)
        putIfNotBlank(data, "baselinePosture", baselinePosture)
        return data
    }

    private fun uploadCurrentStatusMap(
        workerId: String,
        data: Map<String, Any>,
        callback: UploadCallback? = null
    ) {
        database.goOnline()

        // 센서 Notify가 빠르게 들어올 때는 마지막 상태만 보존해 Firebase 쓰기 중복을 줄입니다.
        synchronized(this) {
            if (currentStatusUploadInFlight) {
                pendingCurrentStatusUpload = PendingCurrentStatusUpload(workerId, data, callback)
                Log.d(TAG, "currentStatus upload COALESCED path=workers/$workerId/currentStatus data=$data")
                return
            }
            currentStatusUploadInFlight = true
        }

        val ref = database.getReference("workers")
            .child(workerId)
            .child("currentStatus")

        Log.d(TAG, "currentStatus upload START path=workers/$workerId/currentStatus data=$data")

        ref.updateChildren(data) { error: DatabaseError?, _ ->
            if (error == null) {
                Log.d(TAG, "currentStatus upload SUCCESS")
                callback?.invoke(true, "currentStatus 업로드 성공")
            } else {
                val message = "currentStatus 실패: ${error.message}"
                Log.e(TAG, "currentStatus upload FAILED. code=${error.code}, message=${error.message}, details=${error.details}")
                callback?.invoke(false, message)
            }
            uploadPendingCurrentStatusIfNeeded()
        }
    }

    private fun uploadPendingCurrentStatusIfNeeded() {
        val pending = synchronized(this) {
            currentStatusUploadInFlight = false
            val next = pendingCurrentStatusUpload
            pendingCurrentStatusUpload = null
            next
        }
        if (pending != null) {
            uploadCurrentStatusMap(pending.workerId, pending.data, pending.callback)
        }
    }

    private fun putIfNotBlank(data: MutableMap<String, Any>, key: String, value: String?) {
        if (!value.isNullOrBlank()) data[key] = value
    }
}
