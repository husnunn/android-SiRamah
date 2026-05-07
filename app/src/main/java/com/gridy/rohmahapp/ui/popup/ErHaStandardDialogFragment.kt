package com.gridy.rohmahapp.ui.popup

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.gridy.rohmahapp.R
import com.gridy.rohmahapp.databinding.DialogErhaStandardBinding

/**
 * Popup satu tombol — pola UI mengikuti `referensi/popupStandart.html`,
 * dengan area pesan scroll untuk respons backend yang panjang.
 */
class ErHaStandardDialogFragment : DialogFragment() {

    private var _binding: DialogErhaStandardBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_STYLE = "style"
        private const val ARG_POSITIVE = "positive"
        private const val ARG_FOOTER = "footer"

        const val TAG_STANDARD = "ErHaStandardDialog"

        fun newInstance(
            message: String,
            title: String? = null,
            style: ErHaStandardStyle = ErHaStandardStyle.INFO,
            positiveButtonText: String? = null,
            footer: String? = null
        ): ErHaStandardDialogFragment = ErHaStandardDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_MESSAGE, message)
                putString(ARG_TITLE, title)
                putString(ARG_STYLE, style.name)
                putString(ARG_POSITIVE, positiveButtonText)
                putString(ARG_FOOTER, footer)
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
        _binding = DialogErhaStandardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val message = arguments?.getString(ARG_MESSAGE).orEmpty()
        val titleArg = arguments?.getString(ARG_TITLE)
        val style = ErHaStandardStyle.fromName(arguments?.getString(ARG_STYLE))
        val positive =
            arguments?.getString(ARG_POSITIVE) ?: getString(R.string.erha_popup_close)
        val footer = arguments?.getString(ARG_FOOTER)

        binding.tvStandardMessage.text = message

        val resolvedTitle = titleArg?.takeIf { it.isNotBlank() }
            ?: getString(style.defaultTitleRes)
        binding.tvStandardTitle.text = resolvedTitle

        applyHeaderStyle(style)

        binding.btnStandardPositive.text = positive
        binding.btnStandardPositive.setOnClickListener { dismiss() }

        if (!footer.isNullOrBlank()) {
            binding.tvStandardFooter.visibility = View.VISIBLE
            binding.tvStandardFooter.text = footer
        } else {
            binding.tvStandardFooter.visibility = View.GONE
        }
    }

    private fun applyHeaderStyle(style: ErHaStandardStyle) {
        val ctx = requireContext()
        when (style) {
            ErHaStandardStyle.SUCCESS -> {
                binding.ivStandardIconAlt.visibility = View.GONE
                binding.flStandardPrimaryBadge.visibility = View.VISIBLE
                binding.ivStandardIconSuccess.visibility = View.VISIBLE
                binding.tvStandardTitle.setTextColor(ContextCompat.getColor(ctx, R.color.primary))
            }
            ErHaStandardStyle.INFO -> {
                binding.flStandardPrimaryBadge.visibility = View.GONE
                binding.ivStandardIconSuccess.visibility = View.GONE
                binding.ivStandardIconAlt.visibility = View.VISIBLE
                binding.ivStandardIconAlt.setImageResource(style.altIconRes)
                binding.ivStandardIconAlt.imageTintList = null
                binding.tvStandardTitle.setTextColor(ContextCompat.getColor(ctx, R.color.secondary))
            }
            ErHaStandardStyle.WARNING -> {
                binding.flStandardPrimaryBadge.visibility = View.GONE
                binding.ivStandardIconSuccess.visibility = View.GONE
                binding.ivStandardIconAlt.visibility = View.VISIBLE
                binding.ivStandardIconAlt.setImageResource(style.altIconRes)
                binding.ivStandardIconAlt.imageTintList =
                    ContextCompat.getColorStateList(ctx, R.color.tertiary)
                binding.tvStandardTitle.setTextColor(ContextCompat.getColor(ctx, R.color.on_surface))
            }
            ErHaStandardStyle.ERROR -> {
                binding.flStandardPrimaryBadge.visibility = View.GONE
                binding.ivStandardIconSuccess.visibility = View.GONE
                binding.ivStandardIconAlt.visibility = View.VISIBLE
                binding.ivStandardIconAlt.setImageResource(style.altIconRes)
                binding.ivStandardIconAlt.imageTintList = null
                binding.tvStandardTitle.setTextColor(ContextCompat.getColor(ctx, R.color.error))
            }
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
