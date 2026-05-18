// 관리자 앱에서 Firebase Realtime Database 연결 정보를 초기화하는 파일
package com.example.hnu_ppe_manager

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

// google-services.json 없이 기존 작업자 앱 Firebase 프로젝트를 직접 연결
object AdminFirebaseConfig {

    // 기존 작업자 앱과 같은 Firebase 프로젝트 정보
    private const val DATABASE_URL = "https://hnu-ppe-default-rtdb.asia-southeast1.firebasedatabase.app"
    private const val API_KEY = "AIzaSyADnfratDB5OxcCpPPTofH1_eQwmlxKmDs"
    private const val APPLICATION_ID = "1:653300192739:android:8d0a8a16e83dc25cd7fcd6"
    private const val PROJECT_ID = "hnu-ppe"

    // FirebaseApp 중복 초기화 방지
    fun initialize(context: Context) {
        if (FirebaseApp.getApps(context).isNotEmpty()) return

        val options = FirebaseOptions.Builder()
            .setDatabaseUrl(DATABASE_URL)
            .setApiKey(API_KEY)
            .setApplicationId(APPLICATION_ID)
            .setProjectId(PROJECT_ID)
            .build()

        FirebaseApp.initializeApp(context, options)
    }
}
