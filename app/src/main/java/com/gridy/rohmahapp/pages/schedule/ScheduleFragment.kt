// File: /app/src/main/java/com/gridy/rohmahapp/pages/schedule/ScheduleFragment.kt
package com.gridy.rohmahapp.pages.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.gridy.rohmahapp.di.Injection
import com.gridy.rohmahapp.data.state.UiState
import com.gridy.rohmahapp.databinding.FragmentScheduleRedesignBinding
import com.gridy.rohmahapp.pages.BaseFragment
import com.gridy.rohmahapp.viewmodel.ScheduleViewModel
import com.gridy.rohmahapp.viewmodel.factory.ScheduleViewModelFactory
import com.gridy.rohmahapp.utils.PreferenceClass
import java.util.Calendar
import java.util.Locale

class ScheduleFragment : BaseFragment() {

    private var _binding: FragmentScheduleRedesignBinding? = null
    private val binding get() = _binding!!

    private lateinit var scheduleAdapter: ScheduleAdapter

    /** Penting guru: menghindari memicu reload ganda ketika menyelaraskan tab hari. */
    private var suppressDayRadioListener = false

    override fun isSwipeRefreshEnabled(): Boolean = true

    override fun onRefreshData() {
        beginTrackedSwipeRefresh(1)
        loadSchedule(currentSelectedDay(), manualRefresh = true)
    }

    private val viewModel: ScheduleViewModel by viewModels {
        ScheduleViewModelFactory(
            Injection.provideScheduleRepository(requireContext()),
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentScheduleRedesignBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSwipeRefresh(binding.swipeRefresh)
        setupRecyclerView()
        setupDaySelector()
        observeScheduleScreen()
        applyStaticHeaderForRole()
        selectTodayRadiobuttonInitially()
    }

    private fun setupRecyclerView() {
        scheduleAdapter = ScheduleAdapter()
        binding.rvSchedule.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = scheduleAdapter
        }
    }

    private fun setupDaySelector() {
        binding.rgDays.setOnCheckedChangeListener { _, checkedId ->
            if (suppressDayRadioListener) return@setOnCheckedChangeListener
            val selectedDay = when (checkedId) {
                binding.rbMonday.id -> 1
                binding.rbTuesday.id -> 2
                binding.rbWednesday.id -> 3
                binding.rbThursday.id -> 4
                binding.rbFriday.id -> 5
                binding.rbSaturday.id -> 6
                binding.rbSunday.id -> 7
                else -> currentDayOfWeekNumber()
            }
            loadSchedule(selectedDay)
            if (roleIsTeacher()) {
                updateTeacherDayBannerTitle()
            }
        }
    }

    private fun applyStaticHeaderForRole() {
        binding.tvClassInfo.text = buildClassInfoSubtitle()
        if (!roleIsTeacher()) {
            binding.tvDayName.text = DEFAULT_TITLE.uppercase(Locale.ROOT)
        }
    }

    private fun selectTodayRadiobuttonInitially() {
        val dow = currentDayOfWeekNumber()
        withRadioSuppress {
            radioForDayOfWeek(dow)?.let { rb ->
                binding.rgDays.check(rb.id)
            }
        }
        loadSchedule(dow)
        if (roleIsTeacher()) {
            updateTeacherDayBannerTitle()
        }
    }

    private fun loadSchedule(day: Int, manualRefresh: Boolean = false) {
        viewModel.loadSchedules(day = day, manualRefresh = manualRefresh)
    }

    private fun roleIsTeacher(): Boolean =
        PreferenceClass(requireContext()).getString(PreferenceClass.KEY_USER_ROLE) == "teacher"

    /** Untuk guru, arahkan pemilih ke hari mengajar jika tab saat ini di luar kumpulan hari tersebut. */
    private fun reconcileTeacherTeachingDays(payload: ScheduleScreenData): Boolean {
        val meta = payload.teacherTeachingDays ?: return false
        if (meta.isEmpty()) return false

        val current = currentSelectedDay()
        if (current in meta) return false

        val fallback = nearestTeachingDayPreferring(meta, pivot = current)
        radioForDayOfWeek(fallback) ?: return false
        withRadioSuppress {
            radioForDayOfWeek(fallback)?.let { rb -> binding.rgDays.check(rb.id) }
        }
        loadSchedule(fallback)
        return true
    }

    /** Pilih pivot terdekat pada lingkar 1 … 7. */
    private fun nearestTeachingDayPreferring(meta: List<Int>, pivot: Int): Int {
        val piv = weekdayIndexMon0(pivot)
        return meta.minByOrNull { d ->
            val c = weekdayIndexMon0(d)
            val forwards = (c - piv + 7) % 7
            val backwards = (piv - c + 7) % 7
            minOf(forwards, backwards)
        } ?: meta.first()
    }

    /** 0 = Senin … 6 = Minggu. */
    private fun weekdayIndexMon0(dow: Int): Int =
        ((dow - 1 + 7) % 7)

    private fun applyTeachingDaysChrome(teachingDays: List<Int>?) {
        resetAllDayRadiosVisibility()
        binding.scrollDays.visibility = View.VISIBLE

        if (teachingDays == null) {
            binding.rbSunday.visibility = View.VISIBLE
            binding.tvDayName.visibility = View.VISIBLE
            return
        }

        binding.rbSunday.visibility = View.GONE
        val set = teachingDays.toSet()
        for (d in 1..6) {
            radioForDayOfWeek(d)?.visibility = if (d in set) View.VISIBLE else View.GONE
        }

        binding.tvHelperTeacherDays.visibility =
            if (teachingDays.isEmpty()) View.VISIBLE else View.GONE

        binding.tvDayName.visibility = View.VISIBLE
    }

    private fun resetAllDayRadiosVisibility() {
        for (d in 1..7) {
            radioForDayOfWeek(d)?.visibility = View.VISIBLE
        }
        binding.tvHelperTeacherDays.visibility = View.GONE
    }

    private fun radioForDayOfWeek(dow: Int): RadioButton? = when (dow) {
        1 -> binding.rbMonday
        2 -> binding.rbTuesday
        3 -> binding.rbWednesday
        4 -> binding.rbThursday
        5 -> binding.rbFriday
        6 -> binding.rbSaturday
        7 -> binding.rbSunday
        else -> null
    }

    private inline fun withRadioSuppress(block: () -> Unit) {
        suppressDayRadioListener = true
        try {
            block()
        } finally {
            binding.root.post { suppressDayRadioListener = false }
        }
    }

    private fun currentSelectedDay(): Int {
        return when {
            binding.rbMonday.isChecked -> 1
            binding.rbTuesday.isChecked -> 2
            binding.rbWednesday.isChecked -> 3
            binding.rbThursday.isChecked -> 4
            binding.rbFriday.isChecked -> 5
            binding.rbSaturday.isChecked -> 6
            binding.rbSunday.isChecked -> 7
            else -> currentDayOfWeekNumber()
        }
    }

    private fun observeScheduleScreen() {
        viewModel.scheduleScreenState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Idle -> showLoading(false)
                is UiState.Loading -> showLoading(true)
                is UiState.Success -> {
                    showLoading(false)
                    val payload = state.data

                    if (roleIsTeacher()) {
                        if (reconcileTeacherTeachingDays(payload)) {
                            swipeRefreshStepDone()
                            return@observe
                        }
                    }

                    applyTeachingDaysChrome(payload.teacherTeachingDays)

                    if (roleIsTeacher()) {
                        updateTeacherDayBannerTitle(payload)
                    } else {
                        binding.tvDayName.text = DEFAULT_TITLE.uppercase(Locale.ROOT)
                    }

                    val items = payload.items
                    scheduleAdapter.submitList(items)

                    binding.tvEmpty.text = resolveEmptyCaption(roleIsTeacher(), items.isEmpty())
                    binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvSchedule.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
                    swipeRefreshStepDone()
                }

                is UiState.Error -> {
                    showLoading(false)
                    showToast(state.message)
                    binding.tvEmpty.text = resolveEmptyCaption(roleIsTeacher(), true)
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.rvSchedule.visibility = View.GONE
                    swipeRefreshStepDone()
                }
            }
        }
    }

    private fun updateTeacherDayBannerTitle(payload: ScheduleScreenData? = null) {
        val dow = currentSelectedDay()
        val title =
            payload?.teacherDayNameByNumber?.get(dow)?.uppercase(Locale.ROOT)
                ?: defaultIndonesianDayTitle(dow)
        binding.tvDayName.text = title
    }

    private fun defaultIndonesianDayTitle(dow: Int): String = (
        when (dow) {
            1 -> "Senin"
            2 -> "Selasa"
            3 -> "Rabu"
            4 -> "Kamis"
            5 -> "Jumat"
            6 -> "Sabtu"
            7 -> "Minggu"
            else -> DEFAULT_TITLE
        }
        ).uppercase(Locale.ROOT)

    private fun resolveEmptyCaption(isTeacher: Boolean, emptySlots: Boolean): String {
        return if (!isTeacher) {
            DEFAULT_EMPTY_STUDENT
        } else {
            when {
                binding.tvHelperTeacherDays.visibility == View.VISIBLE ->
                    EMPTY_TEACHER_NO_WEEK_SLOTS
                emptySlots ->
                    EMPTY_TEACHER_NO_DAY_SLOTS
                else -> DEFAULT_EMPTY_STUDENT
            }
        }
    }

    private fun buildClassInfoSubtitle(): String {
        val pref = PreferenceClass(requireContext())
        return when (pref.getString(PreferenceClass.KEY_USER_ROLE)) {
            "student" -> "Jadwal Pelajaran Siswa"
            "teacher" -> "Menampilkan hari Anda mengajar"
            else -> "Jadwal Hari Ini"
        }
    }

    private fun currentDayOfWeekNumber(): Int {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val DEFAULT_TITLE = "JADWAL"
        const val DEFAULT_EMPTY_STUDENT = "Belum ada jadwal"
        const val EMPTY_TEACHER_NO_DAY_SLOTS =
            "Tidak ada jadwal untuk hari ini (pilih hari lain)."
        const val EMPTY_TEACHER_NO_WEEK_SLOTS =
            "Belum ada jadwal minggu ini (cek semester atau event akademik)."
    }
}
