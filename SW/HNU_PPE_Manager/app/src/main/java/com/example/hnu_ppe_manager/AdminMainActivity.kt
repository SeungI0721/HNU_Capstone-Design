// 관리자 메인 화면에서 작업자 상태와 응급 작업자 목록 표시
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

// Firebase 상태를 화면에 반영하는 Activity
class AdminMainActivity : AppCompatActivity() {

    // 화면 이동과 갱신 주기 상수
    companion object {
        const val EXTRA_WORKER_ID = "extra_worker_id"
        private const val LOCATION_ALL = "전체"
        private const val REFRESH_INTERVAL_MILLIS = 15_000L
    }

    // 메인 화면 View 참조
    private lateinit var btnMonitoring: TextView
    private lateinit var btnRefresh: TextView
    private lateinit var txtDangerEmpty: TextView
    private lateinit var txtWorkerEmpty: TextView
    private lateinit var layoutLocationFilters: LinearLayout
    private lateinit var recyclerDangerWorkers: RecyclerView
    private lateinit var dangerAdapter: AdminWorkerAdapter
    private lateinit var workerAdapter: AdminWorkerAdapter

    // 작업자 목록과 리스너 상태
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val workers = ArrayList<AdminWorkerStatus>()
    private val riskWorkers = ArrayList<AdminWorkerStatus>()
    private var selectedLocation = LOCATION_ALL
    private var monitoring = false
    private var riskRealtimeListener: ValueEventListener? = null

    // 전체 작업자 목록 15초 갱신
    private val refreshRunnable = object : Runnable {
        override fun run() {
            readWorkersOnce()
            refreshHandler.postDelayed(this, REFRESH_INTERVAL_MILLIS)
        }
    }

    // 화면 초기화
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

    // XML View 연결
    private fun bindViews() {
        btnMonitoring = findViewById(R.id.btnMonitoring)
        btnRefresh = findViewById(R.id.btnRefresh)
        txtDangerEmpty = findViewById(R.id.txtDangerEmpty)
        txtWorkerEmpty = findViewById(R.id.txtWorkerEmpty)
        layoutLocationFilters = findViewById(R.id.layoutLocationFilters)
        recyclerDangerWorkers = findViewById(R.id.recyclerDangerWorkers)
    }

    // 작업자 목록 RecyclerView 연결
    private fun bindRecyclerViews() {
        dangerAdapter = AdminWorkerAdapter { openWorkerDetail(it) }
        workerAdapter = AdminWorkerAdapter { openWorkerDetail(it) }

        recyclerDangerWorkers.apply {
            layoutManager = LinearLayoutManager(this@AdminMainActivity)
            adapter = dangerAdapter
        }

        findViewById<RecyclerView>(R.id.recyclerWorkerDevices).apply {
            layoutManager = LinearLayoutManager(this@AdminMainActivity)
            adapter = workerAdapter
        }
    }

    // 버튼 이벤트 연결
    private fun bindActions() {
        btnMonitoring.setOnClickListener {
            if (monitoring) stopMonitoring() else startMonitoring()
        }
        btnRefresh.setOnClickListener { readWorkersOnce() }
    }

    // 모니터링 시작
    private fun startMonitoring() {
        if (monitoring) return

        monitoring = true
        btnMonitoring.text = "모니터링 종료"
        readWorkersOnce()
        startRiskRealtimeListener()
        refreshHandler.removeCallbacks(refreshRunnable)
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MILLIS)
    }

    // 모니터링 종료
    private fun stopMonitoring() {
        monitoring = false
        btnMonitoring.text = "모니터링 시작"
        refreshHandler.removeCallbacks(refreshRunnable)
        stopRiskRealtimeListener()
    }

    // 일반 작업자 목록 1회 읽기
    private fun readWorkersOnce() {
        FirebaseDatabase.getInstance()
            .reference
            .child("workers")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val loadedWorkers = ArrayList<AdminWorkerStatus>()

                    for (workerSnapshot in snapshot.children) {
                        val currentStatus = workerSnapshot.child("currentStatus")
                        if (!currentStatus.exists()) continue
                        loadedWorkers.add(
                            AdminWorkerStatus.fromSnapshot(
                                workerSnapshot.key ?: "-",
                                currentStatus
                            )
                        )
                    }

                    workers.clear()
                    workers.addAll(loadedWorkers)
                    riskWorkers.clear()
                    riskWorkers.addAll(loadedWorkers.filter { it.riskPriority == 4 })
                    renderLocationFilters()
                    renderWorkers()
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

    // 응급 작업자 실시간 리스너 시작
    private fun startRiskRealtimeListener() {
        if (riskRealtimeListener != null) return

        val workersReference = FirebaseDatabase.getInstance()
            .reference
            .child("workers")

        riskRealtimeListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val loadedRiskWorkers = ArrayList<AdminWorkerStatus>()

                for (workerSnapshot in snapshot.children) {
                    val currentStatus = workerSnapshot.child("currentStatus")
                    if (!currentStatus.exists()) continue

                    val status = AdminWorkerStatus.fromSnapshot(
                        workerSnapshot.key ?: "-",
                        currentStatus
                    )
                    if (status.riskPriority == 4) loadedRiskWorkers.add(status)
                }

                riskWorkers.clear()
                riskWorkers.addAll(loadedRiskWorkers)
                renderDangerWorkers()
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

    // 응급 작업자 실시간 리스너 제거
    private fun stopRiskRealtimeListener() {
        val listener = riskRealtimeListener ?: return
        FirebaseDatabase.getInstance()
            .reference
            .child("workers")
            .removeEventListener(listener)
        riskRealtimeListener = null
    }

    // 구역 필터 표시 갱신
    private fun renderLocationFilters() {
        val filters = buildLocationFilters()
        if (filters.none { it.name == selectedLocation }) selectedLocation = LOCATION_ALL

        layoutLocationFilters.removeAllViews()
        filters.forEach { filter ->
            layoutLocationFilters.addView(createLocationChip(filter))
        }
    }

    // 작업자 데이터에서 구역 목록 생성
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

    // 구역 선택 칩 생성
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

    // 구역별 작업자 목록 갱신
    private fun renderWorkers() {
        val locationWorkers = workers
            .filter { selectedLocation == LOCATION_ALL || it.displayLocation == selectedLocation || it.workLocationCode == selectedLocation }
            .sortedWith(AdminWorkerStatus.compareByRiskAndTime())

        renderDangerWorkers()
        workerAdapter.submitList(locationWorkers)
        txtWorkerEmpty.visibility = if (locationWorkers.isEmpty()) View.VISIBLE else View.GONE
    }

    // 응급 작업자 목록 갱신
    private fun renderDangerWorkers() {
        val emergencyWorkers = riskWorkers
            .sortedWith(AdminWorkerStatus.compareByRiskAndTime())

        dangerAdapter.submitList(emergencyWorkers)
        recyclerDangerWorkers.visibility = if (emergencyWorkers.isEmpty()) View.GONE else View.VISIBLE
        txtDangerEmpty.visibility = if (emergencyWorkers.isEmpty()) View.VISIBLE else View.GONE
    }

    // 작업자 상세 화면 이동
    private fun openWorkerDetail(worker: AdminWorkerStatus) {
        val intent = Intent(this, AdminWorkerDetailActivity::class.java)
        intent.putExtra(EXTRA_WORKER_ID, worker.workerId)
        startActivity(intent)
    }

    // dp를 px로 변환
    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    // 화면 종료 정리
    override fun onDestroy() {
        refreshHandler.removeCallbacks(refreshRunnable)
        stopRiskRealtimeListener()
        super.onDestroy()
    }
}
