// File: /app/src/main/java/com/gridy/rohmahapp/pages/attendance/AttendanceFragment.kt
package com.gridy.rohmahapp.pages.attendance

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.gridy.rohmahapp.R
import com.gridy.rohmahapp.data.model.AttendanceMapUi
import com.gridy.rohmahapp.data.model.AttendanceSiteOptionUi
import com.gridy.rohmahapp.data.model.AttendanceTodayUi
import com.gridy.rohmahapp.data.state.UiState
import com.gridy.rohmahapp.databinding.FragmentAttendanceBinding
import com.gridy.rohmahapp.di.Injection
import com.gridy.rohmahapp.pages.BaseFragment
import com.gridy.rohmahapp.ui.popup.ErHaStandardStyle
import com.gridy.rohmahapp.ui.popup.showErHaFeedback
import com.gridy.rohmahapp.utils.AttendanceMapComposer
import com.gridy.rohmahapp.viewmodel.AttendanceViewModel
import com.gridy.rohmahapp.viewmodel.factory.AttendanceViewModelFactory
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceFragment : BaseFragment() {

    private var _binding: FragmentAttendanceBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AttendanceViewModel

    /** Logika Maps radius absensi dipisah agar Fragment tidak menjadi monolit. */
    private lateinit var radiusMapController: AttendanceRadiusMapController

    private var cachedUserLat: Double? = null
    private var cachedUserLng: Double? = null

    /** -1 = belum dapat `Success`; mencegah `onResume` memicu kedua muatan saat pembuka pertama. */
    private var lastAttendanceSuccessElapsedRealtimeMs = -1L

    override fun isSwipeRefreshEnabled(): Boolean = true

    override fun onRefreshData() {
        if (!hasLocationPermission()) {
            stopRefreshing()
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }
        beginTrackedSwipeRefresh(1)
        viewModel.loadTodayAttendance(hasLocationPermission(), manualRefresh = true)
    }

    private var boundAttendanceSiteId: Int? = null

    private val timeHandler = Handler(Looper.getMainLooper())
    private val timeRunnable = object : Runnable {
        override fun run() {
            updateCurrentDateTime()
            timeHandler.postDelayed(this, 1000L)
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.loadTodayAttendance(hasLocationPermission(), manualRefresh = false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupSwipeRefresh(binding.swipeRefresh)
        setupView()
        setupClickListener()
        radiusMapController = AttendanceRadiusMapController(this, R.id.mapAttendance)
        radiusMapController.attachMapFragment(
            childFragmentManager,
            binding.cardAttendanceMap,
            binding.mapAttendance
        )
        radiusMapController.installNestedScrollPassthrough(
            binding.mapAttendance,
            binding.scrollContent
        )
        observeViewModel()
        ensurePermissionAndLoad()
    }

    private fun setupViewModel() {
        val factory: AttendanceViewModelFactory =
            Injection.provideAttendanceViewModelFactory(requireContext())

        viewModel = ViewModelProvider(this, factory)[AttendanceViewModel::class.java]
    }

    private fun setupView() {
        updateCurrentDateTime()
        binding.tvLocationStatus.text = "Memeriksa kondisi perangkat..."
        binding.tvCheckInHistory.text = "Belum Presensi Masuk"
        binding.tvCheckOutHistory.text = "Belum Presensi Pulang"
    }

    private fun setupClickListener() {
        binding.btnCheckIn.setOnClickListener {
            submitWithResolvedSite("check_in")
        }

        binding.btnCheckOut.setOnClickListener {
            submitWithResolvedSite("check_out")
        }
    }

    private fun submitWithResolvedSite(attendanceType: String) {
        val siteId = boundAttendanceSiteId
        if (siteId == null) {
            showToast("Titik absensi belum tersedia. Muat ulang atau pilih titik.")
            return
        }
        viewModel.submitAttendance(
            attendanceType = attendanceType,
            attendanceSiteId = siteId,
            hasLocationPermission = hasLocationPermission()
        )
    }

    private fun observeViewModel() {
        viewModel.todayAttendanceState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Idle -> Unit
                is UiState.Loading -> {
                    binding.tvLocationStatus.text = "Memeriksa kondisi perangkat..."
                }
                is UiState.Success -> {
                    lastAttendanceSuccessElapsedRealtimeMs = SystemClock.elapsedRealtime()
                    bindTodayAttendance(state.data)
                    swipeRefreshStepDone()
                }
                is UiState.Error -> {
                    showErHaFeedback(state.message, ErHaStandardStyle.ERROR)
                    swipeRefreshStepDone()
                }
            }
        }

        viewModel.submitAttendanceState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Idle -> Unit
                is UiState.Loading -> Unit
                is UiState.Success ->
                    showErHaFeedback(state.data.message, ErHaStandardStyle.SUCCESS)
                is UiState.Error ->
                    showErHaFeedback(state.message, ErHaStandardStyle.ERROR)
            }
        }
    }

    private fun bindTodayAttendance(data: AttendanceTodayUi) {
        binding.tvLocationStatus.text = data.locationStatusText
        binding.tvCheckInHistory.text = data.checkInHistoryText
        binding.tvCheckOutHistory.text = data.checkOutHistoryText

        val policyText = data.policyInfoText
        if (!policyText.isNullOrBlank()) {
            binding.cardAttendancePolicy.visibility = View.VISIBLE
            binding.tvAttendancePolicyInfo.text = policyText
        } else {
            binding.cardAttendancePolicy.visibility = View.GONE
        }

        val showCard =
            (data.showStatusBadge && data.dailyStatusLabel != null) ||
                !data.infoMessage.isNullOrBlank()
        binding.cardDailyStatus.visibility = if (showCard) View.VISIBLE else View.GONE

        if (data.showStatusBadge && data.dailyStatusLabel != null) {
            binding.tvDailyStatusBadge.visibility = View.VISIBLE
            binding.tvDailyStatusBadge.text = data.dailyStatusLabel
            applyDailyBadgeColors(data.dailyStatusCode)
        } else {
            binding.tvDailyStatusBadge.visibility = View.GONE
        }

        val msg = data.infoMessage
        if (!msg.isNullOrBlank()) {
            binding.tvDailyStatusMessage.visibility = View.VISIBLE
            binding.tvDailyStatusMessage.text = msg
        } else {
            binding.tvDailyStatusMessage.visibility = View.GONE
        }

        val overrideNotes = data.overrideUiNotes
        if (data.overrideActive && !overrideNotes.isNullOrBlank()) {
            binding.cardAttendanceOverride.visibility = View.VISIBLE
            binding.tvOverrideBody.text = overrideNotes
        } else {
            binding.cardAttendanceOverride.visibility = View.GONE
        }

        binding.btnCheckIn.isEnabled = data.canCheckIn
        binding.btnCheckOut.isEnabled = data.canCheckOut

        binding.btnCheckIn.alpha = if (data.canCheckIn) 1f else 0.5f
        binding.btnCheckOut.alpha = if (data.canCheckOut) 1f else 0.5f

        cachedUserLat = data.mapUi.userLatitude
        cachedUserLng = data.mapUi.userLongitude
        bindAttendanceSitePicker(data)
        logFinalAttendanceMapUiState(data, phase = "after bindAttendanceSitePicker")
        applyAttendanceMapPresentation(data.mapUi)
    }

    private fun logFinalAttendanceMapUiState(data: AttendanceTodayUi, phase: String) {
        val m = data.mapUi
        Timber.tag(AttendanceRadiusMapController.LOG_TAG).d("--- final UI map state [%s] ---", phase)
        Timber.tag(AttendanceRadiusMapController.LOG_TAG).d("showMapSection=%s", m.showMapSection)
        Timber.tag(AttendanceRadiusMapController.LOG_TAG).d(
            "site lat/lng/radius_m = %s / %s / %s",
            m.siteLatitude,
            m.siteLongitude,
            m.radiusMeter
        )
        Timber.tag(AttendanceRadiusMapController.LOG_TAG).d(
            "user lat/lng = %s / %s",
            m.userLatitude,
            m.userLongitude
        )
        val sitesLine = data.attendanceSites.joinToString("; ") { site ->
            "${site.id}:${site.name} lat=${site.latitude} lng=${site.longitude} r=${site.radiusM}"
        }
        Timber.tag(AttendanceRadiusMapController.LOG_TAG).d(
            "sites after mapping count=%s [%s]",
            data.attendanceSites.size,
            sitesLine
        )
    }


    private fun applyAttendanceMapPresentation(mapUi: AttendanceMapUi) {
        val show = mapUi.showMapSection
        binding.cardAttendanceMap.visibility = if (show) View.VISIBLE else View.GONE
        binding.tvMapRadiusStatus.text = mapUi.radiusStatusText
        binding.tvMapRadiusStatus.applyMapRadiusAccent(mapUi.isInsideRadius)
        if (mapUi.distanceText.isNotBlank()) {
            binding.tvMapDistance.visibility = View.VISIBLE
            binding.tvMapDistance.text = mapUi.distanceText
        } else {
            binding.tvMapDistance.visibility = View.GONE
        }

        if (show) {
            radiusMapController.present(
                mapUi,
                binding.cardAttendanceMap,
                binding.mapAttendance
            )
        } else {
            radiusMapController.clearOverlayState()
        }
    }

    private fun composeMapForSite(site: AttendanceSiteOptionUi): AttendanceMapUi =
        AttendanceMapComposer.compose(
            site = site,
            userLat = cachedUserLat,
            userLng = cachedUserLng,
            hasLocationPermission = hasLocationPermission(),
            locationEnabled = isDeviceLocationServicesEnabled()
        )



    private fun bindAttendanceSitePicker(data: AttendanceTodayUi) {
        binding.cardAttendanceSite.visibility = View.VISIBLE

        val sites = data.attendanceSites
        boundAttendanceSiteId = data.resolvedAttendanceSiteId

        if (sites.isEmpty()) {
            binding.rowAttendanceSiteAndBssid.visibility = View.GONE
            binding.tvAttendanceSiteEmpty.visibility = View.VISIBLE
            binding.tvAttendanceSiteSingle.visibility = View.GONE
            binding.spinnerAttendanceSite.visibility = View.GONE
            return
        }

        binding.rowAttendanceSiteAndBssid.visibility = View.VISIBLE
        binding.tvAttendanceSiteBssid.text = data.wifiBssidDisplay ?: "—"

        binding.tvAttendanceSiteEmpty.visibility = View.GONE

        if (sites.size == 1) {
            binding.tvAttendanceSiteSingle.visibility = View.VISIBLE
            binding.spinnerAttendanceSite.visibility = View.GONE
            binding.tvAttendanceSiteSingle.text = formatAttendanceSiteLine(sites.first())
            boundAttendanceSiteId = sites.first().id
            return
        }

        binding.tvAttendanceSiteSingle.visibility = View.GONE
        binding.spinnerAttendanceSite.visibility = View.VISIBLE

        val labels = sites.map { formatAttendanceSiteLine(it) }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            labels
        )
        binding.spinnerAttendanceSite.adapter = adapter

        val preferredIdx =
            sites.indexOfFirst { it.id == data.resolvedAttendanceSiteId }.takeIf { it >= 0 } ?: 0

        binding.spinnerAttendanceSite.onItemSelectedListener = null
        binding.spinnerAttendanceSite.setSelection(preferredIdx)
        boundAttendanceSiteId = sites[preferredIdx].id

        binding.spinnerAttendanceSite.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val site = sites[position]
                    boundAttendanceSiteId = site.id
                    viewModel.persistAttendanceSiteSelection(site.id)
                    applyAttendanceMapPresentation(composeMapForSite(site))
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun formatAttendanceSiteLine(s: AttendanceSiteOptionUi): String {
        val r = s.radiusM?.let { rad ->
            String.format(Locale.getDefault(), "%.0f m", rad)
        }
        return buildString {
            append(s.name)
            if (r != null) {
                append(" • radius ")
                append(r)
            }
        }
    }

    private fun applyDailyBadgeColors(statusCode: String?) {
        val bg = when (statusCode) {
            "present" -> R.color.primary_fixed
            "late" -> R.color.tertiary_fixed
            "excused" -> R.color.secondary_container
            "sick" -> R.color.surface_variant
            "dispensation" -> R.color.tertiary_container
            "absent" -> R.color.error
            "holiday" -> R.color.outline_variant
            else -> R.color.surface_container_high
        }
        val fg = when (statusCode) {
            "present" -> R.color.on_surface
            "late" -> R.color.on_tertiary_container
            "excused" -> R.color.on_secondary_container
            "sick" -> R.color.error
            "dispensation" -> R.color.on_tertiary_container
            "absent" -> R.color.on_brand
            "holiday" -> R.color.on_surface_variant
            else -> R.color.on_surface
        }
        val ctx = requireContext()
        binding.tvDailyStatusBadge.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(ctx, bg))
        binding.tvDailyStatusBadge.setTextColor(ContextCompat.getColor(ctx, fg))
    }

    private fun ensurePermissionAndLoad() {
        if (hasLocationPermission()) {
            viewModel.loadTodayAttendance(hasLocationPermission(), manualRefresh = false)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    private fun isDeviceLocationServicesEnabled(): Boolean {
        val lm = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun updateCurrentDateTime() {
        val now = Date()
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale("id", "ID"))
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))

        binding.tvCurrentTime.text = timeFormat.format(now)
        binding.tvCurrentDate.text = dateFormat.format(now).replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale("id", "ID")) else char.toString()
        }
    }

    override fun onResume() {
        super.onResume()
        timeHandler.post(timeRunnable)
        if (::viewModel.isInitialized &&
            hasLocationPermission() &&
            shouldSilentlyReloadAttendanceAfterPause()
        ) {
            viewModel.loadTodayAttendance(hasLocationPermission(), manualRefresh = false)
        }
    }

    private fun shouldSilentlyReloadAttendanceAfterPause(): Boolean {
        val last = lastAttendanceSuccessElapsedRealtimeMs
        if (last < 0L) return false
        return SystemClock.elapsedRealtime() - last >= SILENT_RESUME_REFRESH_AFTER_MS
    }

    override fun onPause() {
        timeHandler.removeCallbacks(timeRunnable)
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timeHandler.removeCallbacks(timeRunnable)
        if (::radiusMapController.isInitialized) {
            radiusMapController.onDestroyView()
        }
        _binding = null
    }

    companion object {
        private const val SILENT_RESUME_REFRESH_AFTER_MS = 45_000L
    }
}

private fun android.widget.TextView.applyMapRadiusAccent(isInside: Boolean?) {
    val c = context
    val colorRes = when (isInside) {
        true -> R.color.primary
        false -> R.color.error
        null -> R.color.on_surface
    }
    setTextColor(ContextCompat.getColor(c, colorRes))
}
