// 관리자 앱에서 Firebase Realtime Database 연결 설정을 초기화하는 파일
package com.example.hnu_ppe_manager

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

object AdminFirebaseConfig {
    private const val TAG = "AdminFirebaseConfig"

    // 기본 Firebase 앱이 없을 때만 수동 옵션으로 초기화해 중복 초기화를 피합니다.
    fun initialize(context: Context) {
        if (FirebaseApp.getApps(context).isNotEmpty()) return
        if (!hasFirebaseConfig()) {
            Log.e(TAG, "Firebase config is missing. Set HNU_PPE_FIREBASE_* Gradle properties or environment variables.")
            return
        }

        val options = FirebaseOptions.Builder()
            .setDatabaseUrl(BuildConfig.FIREBASE_DATABASE_URL)
            .setApiKey(BuildConfig.FIREBASE_API_KEY)
            .setApplicationId(BuildConfig.FIREBASE_APPLICATION_ID)
            .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
            .build()

        FirebaseApp.initializeApp(context, options)
    }

    private fun hasFirebaseConfig(): Boolean {
        return BuildConfig.FIREBASE_DATABASE_URL.isNotBlank() &&
            BuildConfig.FIREBASE_API_KEY.isNotBlank() &&
            BuildConfig.FIREBASE_APPLICATION_ID.isNotBlank() &&
            BuildConfig.FIREBASE_PROJECT_ID.isNotBlank()
    }
}
