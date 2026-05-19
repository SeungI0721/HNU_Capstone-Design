// Smart Shield 작업자 앱이 백그라운드에서도 실행 중임을 알림으로 유지하는 Foreground Service 파일
package com.example.hnu_ppe_control.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.hnu_ppe_control.MainActivity
import com.example.hnu_ppe_control.R

class SmartShieldForegroundService : Service() {

    companion object {
        const val ACTION_START = "com.example.hnu_ppe_control.action.START_FOREGROUND"
        const val ACTION_STOP = "com.example.hnu_ppe_control.action.STOP_FOREGROUND"

        private const val CHANNEL_ID = "smart_shield_worker_channel"
        private const val CHANNEL_NAME = "Smart Shield 작업자 상태"
        private const val NOTIFICATION_ID = 100
    }

    override fun onCreate() {
        super.onCreate()
        // Android 8 이상에서 Foreground Service 알림을 띄우기 위한 채널
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START 알림 표시, STOP 알림 제거와 서비스 종료
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            else -> {
                startForeground(NOTIFICATION_ID, buildNotification())
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        // 사용자가 알림을 누르면 작업자 앱 화면으로 돌아옵니다.
        val contentIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Smart Shield 실행 중")
            .setContentText("BLE 센서 수신과 Firebase 업로드를 유지합니다.")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        // 낮은 중요도 알림으로 지속 실행 표시
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Smart Shield 작업자 앱 백그라운드 실행 알림"
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
