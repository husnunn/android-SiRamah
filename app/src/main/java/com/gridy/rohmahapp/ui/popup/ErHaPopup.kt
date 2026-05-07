package com.gridy.rohmahapp.ui.popup

import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

/**
 * Titik masuk untuk popup reusable (ErHa / RohMah).
 *
 * - [ErHaStandardDialogFragment]: satu tombol, pesan bisa panjang (scroll).
 * - [ErHaConfirmDialogFragment]: dua tombol + baris detail opsional.
 *
 * Untuk umpan balik singkat vs popup: gunakan [showFeedback]; set [preferPopupOverToast] false jika toast masih diinginkan untuk pesan pendek.
 */
object ErHaPopup {

    /** Di atas ambang ini atau banyak baris → popup lebih cocok daripada toast. */
    const val SHORT_MESSAGE_MAX_CHARS = 120

    const val SHORT_MESSAGE_MAX_LINES = 4

    fun showStandard(
        fragment: Fragment,
        message: String,
        title: String? = null,
        style: ErHaStandardStyle = ErHaStandardStyle.INFO,
        positiveButtonText: String? = null,
        footer: String? = null
    ) {
        if (!fragment.isAdded) return
        ErHaStandardDialogFragment.newInstance(
            message = message,
            title = title,
            style = style,
            positiveButtonText = positiveButtonText,
            footer = footer
        ).show(fragment.childFragmentManager, ErHaStandardDialogFragment.TAG_STANDARD)
    }

    fun showStandard(
        activity: FragmentActivity,
        message: String,
        title: String? = null,
        style: ErHaStandardStyle = ErHaStandardStyle.INFO,
        positiveButtonText: String? = null,
        footer: String? = null
    ) {
        ErHaStandardDialogFragment.newInstance(
            message = message,
            title = title,
            style = style,
            positiveButtonText = positiveButtonText,
            footer = footer
        ).show(activity.supportFragmentManager, ErHaStandardDialogFragment.TAG_STANDARD)
    }

    fun showConfirm(
        fragment: Fragment,
        title: String,
        rows: List<ErHaConfirmDetailRow> = emptyList(),
        primaryButtonText: String,
        negativeButtonText: String,
        disclaimer: String? = null,
        headerIconResId: Int = com.gridy.rohmahapp.R.drawable.ic_erha_popup_confirm
    ) {
        if (!fragment.isAdded) return
        ErHaConfirmDialogFragment.newInstance(
            title = title,
            rows = rows,
            primaryButtonText = primaryButtonText,
            negativeButtonText = negativeButtonText,
            disclaimer = disclaimer,
            headerIconResId = headerIconResId
        ).show(fragment.childFragmentManager, ErHaConfirmDialogFragment.TAG_CONFIRM)
    }

    /**
     * @param preferPopupOverToast `true` (default): selalu popup; `false`: toast untuk pesan pendek & sedikit baris.
     */
    fun showFeedback(
        fragment: Fragment,
        message: String,
        style: ErHaStandardStyle,
        preferPopupOverToast: Boolean = true
    ) {
        if (!preferPopupOverToast && isShortMessage(message)) {
            Toast.makeText(fragment.requireContext(), message, Toast.LENGTH_SHORT).show()
            return
        }
        showStandard(fragment, message = message, title = null, style = style)
    }

    fun showFeedback(
        activity: FragmentActivity,
        message: String,
        style: ErHaStandardStyle,
        preferPopupOverToast: Boolean = true
    ) {
        if (!preferPopupOverToast && isShortMessage(message)) {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            return
        }
        showStandard(activity, message = message, title = null, style = style)
    }

    fun isShortMessage(message: String): Boolean {
        val lineCount = message.count { it == '\n' } + 1
        return message.length <= SHORT_MESSAGE_MAX_CHARS &&
            lineCount < SHORT_MESSAGE_MAX_LINES
    }
}

/** Popup satu tombol — pesan panjang tetap nyaman (scroll). */
fun Fragment.showErHaStandardMessage(
    message: String,
    title: String? = null,
    style: ErHaStandardStyle = ErHaStandardStyle.INFO,
    positiveButtonText: String? = null,
    footer: String? = null
) {
    ErHaPopup.showStandard(
        fragment = this,
        message = message,
        title = title,
        style = style,
        positiveButtonText = positiveButtonText,
        footer = footer
    )
}

/**
 * Umpan balik dari API / validasi: default pakai popup (sesuai referensi UI).
 * Set [preferPopupOverToast] false untuk tetap memakai toast pada pesan pendek.
 */
fun Fragment.showErHaFeedback(
    message: String,
    style: ErHaStandardStyle = ErHaStandardStyle.INFO,
    preferPopupOverToast: Boolean = true
) {
    ErHaPopup.showFeedback(this, message, style, preferPopupOverToast)
}

fun Fragment.showErHaConfirm(
    title: String,
    rows: List<ErHaConfirmDetailRow> = emptyList(),
    primaryButtonText: String,
    negativeButtonText: String,
    disclaimer: String? = null,
    headerIconResId: Int = com.gridy.rohmahapp.R.drawable.ic_erha_popup_confirm
) {
    ErHaPopup.showConfirm(
        fragment = this,
        title = title,
        rows = rows,
        primaryButtonText = primaryButtonText,
        negativeButtonText = negativeButtonText,
        disclaimer = disclaimer,
        headerIconResId = headerIconResId
    )
}

fun FragmentActivity.showErHaFeedback(
    message: String,
    style: ErHaStandardStyle = ErHaStandardStyle.INFO,
    preferPopupOverToast: Boolean = true
) {
    ErHaPopup.showFeedback(this, message, style, preferPopupOverToast)
}

fun FragmentActivity.showErHaStandardMessage(
    message: String,
    title: String? = null,
    style: ErHaStandardStyle = ErHaStandardStyle.INFO,
    positiveButtonText: String? = null,
    footer: String? = null
) {
    ErHaPopup.showStandard(
        activity = this,
        message = message,
        title = title,
        style = style,
        positiveButtonText = positiveButtonText,
        footer = footer
    )
}
