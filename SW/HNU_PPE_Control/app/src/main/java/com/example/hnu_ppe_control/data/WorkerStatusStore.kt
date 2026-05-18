// 작업자 최신 상태를 앱 내부와 SharedPreferences에 보관하는 파일
package com.example.hnu_ppe_control.data

import android.content.Context
import android.content.Intent
import java.util.concurrent.CopyOnWriteArraySet

object WorkerStatusStore {
    const val ACTION_WORKER_DETAIL_UPDATED = "com.example.hnu_ppe_control.WORKER_DETAIL_UPDATED"

    private val listeners = CopyOnWriteArraySet<(WorkerDetailSnapshot) -> Unit>()

    @Volatile
    var latestSnapshot: WorkerDetailSnapshot? = null
        private set

    fun update(snapshot: WorkerDetailSnapshot, context: Context? = null) {
        latestSnapshot = snapshot
        listeners.forEach { listener -> listener(snapshot) }
        context?.sendBroadcast(Intent(ACTION_WORKER_DETAIL_UPDATED).setPackage(context.packageName))
    }

    fun addListener(listener: (WorkerDetailSnapshot) -> Unit) {
        listeners.add(listener)
        latestSnapshot?.let(listener)
    }

    fun removeListener(listener: (WorkerDetailSnapshot) -> Unit) {
        listeners.remove(listener)
    }
}
