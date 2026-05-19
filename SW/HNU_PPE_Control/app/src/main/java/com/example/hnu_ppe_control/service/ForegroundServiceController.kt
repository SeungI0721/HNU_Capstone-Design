// Smart Shield Foreground Service 시작과 종료 요청을 Activity 밖에서 관리하는 파일
package com.example.hnu_ppe_control.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.hnu_ppe_control.ble.BlePermissionHelper

class ForegroundServiceController(
    private val context: Context
) {
    companion object {
        private const val TAG = "SmartShieldBLE"
    }

    fun startIfAllowed() {
        // Android 12 이상 connectedDevice 권한 확인
        if (!BlePermissionHelper.hasConnectPermission(context)) {
            Log.w(TAG, "Foreground service start skipped. BLUETOOTH_CONNECT permission is missing.")
            return
        }

        val intent = Intent(context, SmartShieldForegroundService::class.java).apply {
            action = SmartShieldForegroundService.ACTION_START
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        } catch (e: RuntimeException) {
            Log.e(TAG, "Foreground service start failed", e)
        }
    }

    fun stop() {
        val intent = Intent(context, SmartShieldForegroundService::class.java).apply {
            action = SmartShieldForegroundService.ACTION_STOP
        }

        try {
            context.startService(intent)
        } catch (e: RuntimeException) {
            Log.e(TAG, "Foreground service stop failed", e)
        }
    }
}
