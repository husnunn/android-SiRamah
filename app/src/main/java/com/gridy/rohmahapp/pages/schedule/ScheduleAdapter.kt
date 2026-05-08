package com.gridy.rohmahapp.pages.schedule

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gridy.rohmahapp.databinding.ItemScheduleRedesignBinding

class ScheduleAdapter : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    private val items = mutableListOf<ScheduleUi>()

    fun submitList(data: List<ScheduleUi>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    inner class ScheduleViewHolder(
        private val binding: ItemScheduleRedesignBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ScheduleUi) {
            binding.tvStartTime.text = item.start_time
            binding.tvEndTime.text = item.end_time
            binding.tvSubjectName.text = item.subject_name
            val code = item.subject_code_hint?.trim()?.takeIf { it.isNotEmpty() }
            if (code != null) {
                binding.tvSubjectCodeHint.visibility = View.VISIBLE
                binding.tvSubjectCodeHint.text = code.uppercase()
            } else {
                binding.tvSubjectCodeHint.visibility = View.GONE
            }
            binding.tvTeacherOrClass.text = item.teacher_or_class
            val notes = item.notes?.trim()?.takeIf { it.isNotEmpty() }
            if (notes != null) {
                binding.tvScheduleNotes.visibility = View.VISIBLE
                binding.tvScheduleNotes.text = notes
            } else {
                binding.tvScheduleNotes.visibility = View.GONE
            }
            binding.tvRoom.text = item.room
            binding.tvDay.text = item.day
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val binding = ItemScheduleRedesignBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ScheduleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}