// 관리자 앱의 작업자 목록, 위험 작업자, 위치 필터 모니터링 화면을 제어하는 파일
package com.example.hnu_ppe_manager

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminMainActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_WORKER_ID = "extra_worker_id"
        private const val LOCATION_ALL = "전체"
        private const val REFRESH_INTERVAL_MILLIS = 15_000L
    }

    private lateinit var btnMonitoring: TextView
    private lateinit var btnRefresh: TextView
    private lateinit var txtDangerEmpty: TextView
    private lateinit var txtWorkerEmpty: TextView
    private lateinit var layoutLocationFilters: LinearLayout
    private lateinit var recyclerDangerWorkers: RecyclerView
    private lateinit var dangerAdapter: AdminWorkerAdapter
    private lateinit var workerAdapter: AdminWorkerAdapter

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val workers = ArrayList<AdminWorkerStatus>()
    private val riskWorkers = ArrayList<AdminWorkerStatus>()
    private var selectedLocation = LOCATION_ALL
    private var monitoring = false
    private var monitoringStartedAtMillis = 0L
    private var riskRealtimeListener: ValueEventListener? = null

    // 실시간 리스너와 별도로 주기 조회를 유지해 일시적인 이벤트 누락에 대비합니다.
    private val refreshRunnable = object : Runnable {
        override fun run() {
            readWorkersOnce()
            refreshHandler.postDelayed(this, REFRESH_INTERVAL_MILLIS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminFirebaseConfig.initialize(this)
        setContentView(R.layout.activity_admin_main)

        bindViews()
        bindRecyclerViews()
        bindActions()
        renderLocationFilters()
        renderWorkers()
    }

    private fun bindViews() {
        btnMonitoring = findViewById(R.id.btnMonitoring)
        btnRefresh = findViewById(R.id.btnRefresh)
        txtDangerEmpty = findViewById(R.id.txtDangerEmpty)
        txtWorkerEmpty = findViewById(R.id.txtWorkerEmpty)
        layoutLocationFilters = findViewById(R.id.layoutLocationFilters)
        recyclerDangerWorkers = findViewById(R.id.recyclerDangerWorkers)
    }

    private fun bindRecyclerViews() {
        dangerAdapter = AdminWorkerAdapter(showFirstEmergencyLog = true) { openWorkerDetail(it) }
        workerAdapter = AdminWorkerAdapter { openWorkerDetail(it) }

        recyclerDangerWorkers.apply {
            layoutManager = LinearLayoutManager(this@AdminMainActivity)
            adapter = dangerAdapter
            isNestedScrollingEnabled = false
        }

        findViewById<RecyclerView>(R.id.recyclerWorkerDevices).apply {
            layoutManager = LinearLayoutManager(this@AdminMainActivity)
            adapter = workerAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun bindActions() {
        btnMonitoring.setOnClickListener {
            if (monitoring) stopMonitoring() else startMonitoring()
        }
        btnRefresh.setOnClickListener { readWorkersOnce() }
    }

    private fun startMonitoring() {
        if (monitoring) return

        // 모니터링 시작 이후 생성된 상태와 응급 로그만 현재 세션 데이터로 표시합니다.
        monitoring = true
        monitoringStartedAtMillis = System.currentTimeMillis()
        btnMonitoring.text = "모니터링 종료"
        clearWorkerLists()
        readWorkersOnce()
        startRiskRealtimeListener()
        refreshHandler.removeCallbacks(refreshRunnable)
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MILLIS)
    }

    private fun stopMonitoring() {
        monitoring = false
        btnMonitoring.text = "모니터링 시작"
        refreshHandler.removeCallbacks(refreshRunnable)
        stopRiskRealtimeListener()
    }

    private fun readWorkersOnce() {
        // Firebase workers 전체를 한 번 읽어 목록과 위험 작업자 영역을 갱신합니다.
        FirebaseDatabase.getInstance()
            .reference
            .child("workers")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    updateWorkersFromSnapshot(snapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@AdminMainActivity,
                        "Firebase 읽기 실패: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun startRiskRealtimeListener() {
        if (riskRealtimeListener != null) return

        // 위험 상태 변화는 목록 우선순위에 바로 영향을 주므로 workers 노드 변경을 구독합니다.
        val workersReference = FirebaseDatabase.getInstance()
            .reference
            .child("workers")

        riskRealtimeListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                updateWorkersFromSnapshot(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@AdminMainActivity,
                    "위험 상태 실시간 읽기 실패: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        workersReference.addValueEventListener(riskRealtimeListener as ValueEventListener)
    }

    private fun updateWorkersFromSnapshot(snapshot: DataSnapshot) {
        val loadedWorkers = ArrayList<AdminWorkerStatus>()

        // currentStatus가 없는 작업자는 앱에 표시할 현재 상태가 없으므로 제외합니다.
        for (workerSnapshot in snapshot.children) {
            val currentStatus = workerSnapshot.child("currentStatus")
            if (!currentStatus.exists()) continue

            val status = AdminWorkerStatus.fromSnapshot(
                workerSnapshot.key ?: "-",
                currentStatus,
                workerSnapshot.child("riskLogs"),
                monitoringStartedAtMillis
            )
            if (isNewMonitoringData(status)) loadedWorkers.add(status)
        }

        workers.clear()
        workers.addAll(loadedWorkers)
        riskWorkers.clear()
        riskWorkers.addAll(loadedWorkers.filter { it.riskPriority >= 3 })
        renderLocationFilters()
        renderWorkers()
    }

    private fun isNewMonitoringData(status: AdminWorkerStatus): Boolean {
        return monitoringStartedAtMillis > 0L && status.lastUpdatedMillis >= monitoringStartedAtMillis
    }

    private fun clearWorkerLists() {
        workers.clear()
        riskWorkers.clear()
        selectedLocation = LOCATION_ALL
        renderLocationFilters()
        renderWorkers()
    }

    private fun stopRiskRealtimeListener() {
        val listener = riskRealtimeListener ?: return
        FirebaseDatabase.getInstance()
            .reference
            .child("workers")
            .removeEventListener(listener)
        riskRealtimeListener = null
    }

    private fun renderLocationFilters() {
        val filters = buildLocationFilters()
        if (filters.none { it.name == selectedLocation }) selectedLocation = LOCATION_ALL

        // 작업 위치는 Firebase에 올라온 실제 작업자 데이터 기준으로 동적으로 구성합니다.
        layoutLocationFilters.removeAllViews()
        filters.forEach { filter ->
            layoutLocationFilters.addView(createLocationChip(filter))
        }
    }

    private fun buildLocationFilters(): List<WorkLocationFilter> {
        val filters = ArrayList<WorkLocationFilter>()
        filters.add(WorkLocationFilter(LOCATION_ALL, LOCATION_ALL))

        workers
            .map { WorkLocationFilter(it.workLocationCode, it.displayLocation) }
            .filter { it.name.isNotBlank() && it.name != "-" }
            .distinctBy { "${it.code}_${it.name}" }
            .sortedBy { it.name }
            .forEach { filters.add(it) }

        return filters
    }

    private fun createLocationChip(filter: WorkLocationFilter): TextView {
        val selected = filter.name == selectedLocation
        return TextView(this).apply {
            text = filter.name
            textSize = 22f
            setTextColor(ContextCompat.getColor(this@AdminMainActivity, if (selected) android.R.color.white else R.color.ss_dark))
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = ContextCompat.getDrawable(
                this@AdminMainActivity,
                if (selected) R.drawable.bg_location_chip_selected else R.drawable.bg_location_chip
            )
            isClickable = true
            isFocusable = true
            setPadding(dp(24), 0, dp(24), 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(56)
            ).apply {
                marginEnd = dp(10)
            }
            setOnClickListener {
                selectedLocation = filter.name
                renderLocationFilters()
                renderWorkers()
            }
        }
    }

    private fun renderWorkers() {
        // 선택한 위치의 작업자만 표시하고 위험도와 갱신 시각 순서로 정렬합니다.
        val locationWorkers = workers
            .filter { selectedLocation == LOCATION_ALL || it.displayLocation == selectedLocation || it.workLocationCode == selectedLocation }
            .sortedWith(AdminWorkerStatus.compareByRiskAndTime())

        renderDangerWorkers()
        workerAdapter.submitList(locationWorkers)
        txtWorkerEmpty.visibility = if (locationWorkers.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun renderDangerWorkers() {
        val emergencyWorkers = riskWorkers
            .sortedWith(AdminWorkerStatus.compareByRiskAndTime())

        dangerAdapter.submitList(emergencyWorkers)
        recyclerDangerWorkers.visibility = if (emergencyWorkers.isEmpty()) View.GONE else View.VISIBLE
        txtDangerEmpty.visibility = if (emergencyWorkers.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openWorkerDetail(worker: AdminWorkerStatus) {
        val intent = Intent(this, AdminWorkerDetailActivity::class.java)
        intent.putExtra(EXTRA_WORKER_ID, worker.workerId)
        startActivity(intent)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        refreshHandler.removeCallbacks(refreshRunnable)
        stopRiskRealtimeListener()
        super.onDestroy()
    }
}
