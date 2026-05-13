package com.example.hnu_ppe_control.firebase

import android.util.Log
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase

object FirebaseStatusUploader {

    private const val TAG = "SmartShieldFirebase"
    private val database = FirebaseDatabase.getInstance()

    fun uploadCurrentStatus(
        workerId: String,
        deviceName: String,
        temp: Double,
        hr: Int,
        spo2: Int?,
        env: Double,
        hum: Double,
        lux: Int,
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
        baselinePosture: String?
    ) {
        if (workerId.isBlank()) {
            Log.e(TAG, "currentStatus upload FAILED. workerId is blank")
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

        data["temp"] = temp
        data["hr"] = hr
        data["env"] = env
        data["hum"] = hum
        data["lux"] = lux
        data["directSunlight"] = lux >= 50000
        data["posture"] = posture
        data["riskLevel"] = riskLevel
        data["riskCommand"] = riskCommand
        spo2?.let { data["spo2"] = it }

        uploadCurrentStatusMap(workerId, data)
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
        baselinePosture: String?
    ) {
        if (workerId.isBlank()) {
            Log.e(TAG, "sessionStatus upload FAILED. workerId is blank")
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

        uploadCurrentStatusMap(workerId, data)
    }

    fun uploadRiskLog(
        workerId: String,
        riskLevel: String,
        riskCommand: String,
        temp: Double,
        hr: Int,
        spo2: Int?,
        env: Double,
        hum: Double,
        lux: Int,
        posture: String,
        message: String,
        workLocationCode: String?,
        workLocationName: String?,
        workStartedAt: Long?,
        workStartedAtText: String?,
        bleSignalLevel: String?,
        bleRssi: Int?,
        weatherAlert: String?,
        todayMaxTemp: Double?
    ) {
        if (workerId.isBlank()) {
            Log.e(TAG, "riskLog upload FAILED. workerId is blank")
            return
        }

        FirebaseDatabase.getInstance().goOnline()

        val ref = database.getReference("workers")
            .child(workerId)
            .child("riskLogs")
            .push()

        val data = mutableMapOf<String, Any>(
            "workerId" to workerId,
            "riskLevel" to riskLevel,
            "riskCommand" to riskCommand,
            "temp" to temp,
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
            if (error == null) Log.d(TAG, "riskLog upload SUCCESS")
            else Log.e(TAG, "riskLog upload FAILED. code=${error.code}, message=${error.message}, details=${error.details}")
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

    private fun uploadCurrentStatusMap(workerId: String, data: Map<String, Any>) {
        FirebaseDatabase.getInstance().goOnline()

        val ref = database.getReference("workers")
            .child(workerId)
            .child("currentStatus")

        Log.d(TAG, "currentStatus upload START path=workers/$workerId/currentStatus data=$data")

        ref.updateChildren(data) { error: DatabaseError?, _ ->
            if (error == null) Log.d(TAG, "currentStatus upload SUCCESS")
            else Log.e(TAG, "currentStatus upload FAILED. code=${error.code}, message=${error.message}, details=${error.details}")
        }
    }

    private fun putIfNotBlank(data: MutableMap<String, Any>, key: String, value: String?) {
        if (!value.isNullOrBlank()) data[key] = value
    }
}
