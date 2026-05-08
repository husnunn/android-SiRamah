// File: /app/src/main/java/com/gridy/rohmahapp/pages/dashboard/DashboardFragment.kt
package com.gridy.rohmahapp.pages.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.gridy.rohmahapp.di.Injection
import com.gridy.rohmahapp.api.PrayerApiClient
import com.gridy.rohmahapp.data.state.UiState
import com.gridy.rohmahapp.databinding.FragmentDashboardRedesignBinding
import com.gridy.rohmahapp.pages.BaseFragment
import com.gridy.rohmahapp.repository.PrayerRepository
import com.gridy.rohmahapp.utils.PrayerReminderScheduler
import com.gridy.rohmahapp.utils.PreferenceClass
import com.gridy.rohmahapp.viewmodel.PrayerViewModel
import com.gridy.rohmahapp.viewmodel.ScheduleViewModel
import com.gridy.rohmahapp.viewmodel.factory.PrayerViewModelFactory
import com.gridy.rohmahapp.viewmodel.factory.ScheduleViewModelFactory

class DashboardFragment : BaseFragment() {

    private var _binding: FragmentDashboardRedesignBinding? = null
    private val binding get() = _binding!!

    override fun isSwipeRefreshEnabled(): Boolean = true

    override fun onRefreshData() {
        beginTrackedSwipeRefresh(2)
        bindGreeting()
        loadPrayerSchedule()
        loadNearestSchedule(manualRefresh = true)
    }

    private val prayerViewModel: PrayerViewModel by viewModels {
        PrayerViewModelFactory(
            PrayerRepository(
                PrayerApiClient.create()
            )
        )
    }

    private val scheduleViewModel: ScheduleViewModel by viewModels {
        ScheduleViewModelFactory(
            Injection.provideScheduleRepository(requireContext()),
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardRedesignBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSwipeRefresh(binding.swipeRefresh)
        observePrayerSchedule()
        observeNearestSchedule()
        setupView()
    }

    private fun setupView() {
        bindGreeting()
        bindAttendanceCard()
//        bindDhuhaAlarm()
        loadPrayerSchedule()
        loadNearestSchedule()
    }

    private fun loadPrayerSchedule() {
        prayerViewModel.loadPrayerSchedule(
            latitude = -7.2575,
            longitude = 112.7521
        )
    }

    private fun loadNearestSchedule(manualRefresh: Boolean = false) {
        scheduleViewModel.loadNearestSchedule(manualRefresh = manualRefresh)
    }

    private fun bindGreeting() {
        val pref = PreferenceClass(requireContext())
        val userName = pref.getString(PreferenceClass.KEY_USER_NAME, "Pengguna")

        binding.tvGreeting.text = "Assalamu'alaikum,\n$userName"
        binding.tvGreetingTime.text = getGreetingTime()
    }

    private fun bindAttendanceCard() {
        binding.tvWifiStatus.text = "Terhubung Wi-Fi Sekolah"
        binding.tvAttendanceTitle.text = "Siap Absen"
        binding.tvAttendanceSubtitle.text = "Sistem kehadiran tersinkronisasi"
    }

//    private fun bindDhuhaAlarm() {
//        val pref = PreferenceClass(requireContext())
//        val enabled = pref.getBoolean("dhuha_alarm_enabled", true)
//
//        binding.switchDhuhaAlarm.isChecked = enabled
//        binding.switchDhuhaAlarm.setOnCheckedChangeListener { _, isChecked ->
//            pref.putBoolean("dhuha_alarm_enabled", isChecked)
//        }
//    }

    private fun getGreetingTime(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 0..10 -> "SELAMAT PAGI"
            in 11..14 -> "SELAMAT SIANG"
            in 15..17 -> "SELAMAT SORE"
            else -> "SELAMAT MALAM"
        }
    }

    private fun observePrayerSchedule() {
        prayerViewModel.prayerState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Idle -> Unit
                is UiState.Loading -> Unit

                is UiState.Success -> {
                    val data = state.data
                    binding.tvFajrTime.text = data.fajr
                    binding.tvDhuhaTime.text = data.dhuha
                    binding.tvDhuhrTime.text = data.dhuhr
                    binding.tvAsrTime.text = data.asr
                    binding.tvMaghribTime.text = data.maghrib
                    binding.tvIshaTime.text = data.isha
                    PrayerReminderScheduler.scheduleAfterFetch(requireContext(), data)
                    swipeRefreshStepDone()
                }

                is UiState.Error -> {
                    showToast(state.message)
                    swipeRefreshStepDone()
                }
            }
        }
    }

    private fun observeNearestSchedule() {
        scheduleViewModel.nearestScheduleState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Idle -> Unit
                is UiState.Loading -> Unit

                is UiState.Success -> {
                    val data = state.data
                    if (data == null) {
                        binding.tvNearestSubject.text = "Belum ada jadwal hari ini"
                        binding.tvNearestTeacher.text = "-"
                        binding.tvNearestTime.text = "-"
                        binding.tvNearestRoom.text = "-"
                    } else {
                        binding.tvNearestSubject.text = data.subjectName
                        binding.tvNearestTeacher.text = data.teacherName
                        binding.tvNearestTime.text = data.startTime
                        binding.tvNearestRoom.text = data.room
                    }
                    swipeRefreshStepDone()
                }

                is UiState.Error -> {
                    binding.tvNearestSubject.text = "Gagal memuat jadwal"
                    binding.tvNearestTeacher.text = "-"
                    binding.tvNearestTime.text = "-"
                    binding.tvNearestRoom.text = "-"
                    showToast(state.message)
                    swipeRefreshStepDone()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}