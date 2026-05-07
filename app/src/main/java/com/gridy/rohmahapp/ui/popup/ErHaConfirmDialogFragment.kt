package com.gridy.rohmahapp.ui.popup

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.gridy.rohmahapp.R
import com.gridy.rohmahapp.databinding.DialogErhaConfirmBinding
import com.gridy.rohmahapp.databinding.ItemErhaConfirmDetailRowBinding

/**
 * Dialog dua tombol — pola UI mengikuti `referensi/popupKonfirmasi.html`.
 */
class ErHaConfirmDialogFragment : DialogFragment() {

    private var _binding: DialogErhaConfirmBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_ROWS = "rows"
        private const val ARG_PRIMARY = "primary"
        private const val ARG_NEGATIVE = "negative"
        private const val ARG_DISCLAIMER = "disclaimer"
        private const val ARG_ICON_RES = "icon_res"

        const val TAG_CONFIRM = "ErHaConfirmDialog"

        /** Hasil: extra [RESULT_EXTRA_CONFIRMED] = true / false */
        const val REQUEST_KEY_CONFIRM = "erha_confirm_dialog_request"
        const val RESULT_EXTRA_CONFIRMED = "confirmed"

        fun newInstance(
            title: String,
            rows: List<ErHaConfirmDetailRow> = emptyList(),
            primaryButtonText: String,
            negativeButtonText: String,
            disclaimer: String? = null,
            headerIconResId: Int = R.drawable.ic_erha_popup_confirm
        ): ErHaConfirmDialogFragment = ErHaConfirmDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                @Suppress("DEPRECATION")
                putSerializable(ARG_ROWS, ArrayList(rows))
                putString(ARG_PRIMARY, primaryButtonText)
                putString(ARG_NEGATIVE, negativeButtonText)
                putString(ARG_DISCLAIMER, disclaimer)
                putInt(ARG_ICON_RES, headerIconResId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        super.onCreateDialog(savedInstanceState).apply {
            window?.requestFeature(Window.FEATURE_NO_TITLE)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogErhaConfirmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = arguments ?: return

        binding.tvConfirmTitle.text = args.getString(ARG_TITLE).orEmpty()
        val iconRes = args.getInt(ARG_ICON_RES, R.drawable.ic_erha_popup_confirm)
        binding.ivConfirmHeaderIcon.setImageResource(iconRes)
        binding.ivConfirmHeaderIcon.imageTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.primary)

        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        val rows = args.getSerializable(ARG_ROWS) as? ArrayList<ErHaConfirmDetailRow>
            ?: arrayListOf()

        binding.llConfirmRows.removeAllViews()
        if (rows.isEmpty()) {
            binding.cardConfirmDetails.visibility = View.GONE
        } else {
            binding.cardConfirmDetails.visibility = View.VISIBLE
            val inflater = LayoutInflater.from(requireContext())
            val gapPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                8f,
                resources.displayMetrics
            ).toInt()
            rows.forEachIndexed { index, row ->
                val rowBinding = ItemErhaConfirmDetailRowBinding.inflate(
                    inflater,
                    binding.llConfirmRows,
                    false
                )
                rowBinding.tvRowLabel.text = row.label
                rowBinding.tvRowPrimary.text = row.primaryText
                if (!row.secondaryText.isNullOrBlank()) {
                    rowBinding.tvRowSecondary.visibility = View.VISIBLE
                    rowBinding.tvRowSecondary.text = row.secondaryText
                } else {
                    rowBinding.tvRowSecondary.visibility = View.GONE
                }
                val resId = row.iconResId.takeIf { it != 0 } ?: R.drawable.ic_erha_popup_confirm
                rowBinding.ivRowIcon.setImageResource(resId)
                rowBinding.ivRowIcon.imageTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.primary)
                rowBinding.ivRowIcon.scaleType = ImageView.ScaleType.FIT_CENTER

                binding.llConfirmRows.addView(rowBinding.root)
                if (index < rows.lastIndex) {
                    binding.llConfirmRows.addView(
                        View(requireContext()).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                gapPx
                            )
                        }
                    )
                }
            }
        }

        val disclaimer = args.getString(ARG_DISCLAIMER)
        if (!disclaimer.isNullOrBlank()) {
            binding.tvConfirmDisclaimer.visibility = View.VISIBLE
            binding.tvConfirmDisclaimer.text = disclaimer
        } else {
            binding.tvConfirmDisclaimer.visibility = View.GONE
        }

        binding.btnConfirmPositive.text = args.getString(ARG_PRIMARY).orEmpty()
        binding.btnConfirmNegative.text = args.getString(ARG_NEGATIVE).orEmpty()

        binding.btnConfirmPositive.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY_CONFIRM,
                bundleOf(RESULT_EXTRA_CONFIRMED to true)
            )
            dismiss()
        }
        binding.btnConfirmNegative.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY_CONFIRM,
                bundleOf(RESULT_EXTRA_CONFIRMED to false)
            )
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val w = (resources.displayMetrics.widthPixels * 0.92f).toInt()
            setLayout(w, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
