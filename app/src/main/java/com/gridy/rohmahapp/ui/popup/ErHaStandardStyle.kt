package com.gridy.rohmahapp.ui.popup

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.gridy.rohmahapp.R

enum class ErHaStandardStyle(
    @StringRes val defaultTitleRes: Int,
    @DrawableRes val altIconRes: Int
) {
    SUCCESS(R.string.erha_popup_title_success, R.drawable.ic_erha_popup_success),
    INFO(R.string.erha_popup_title_info, R.drawable.ic_erha_popup_info),
    WARNING(R.string.erha_popup_title_warning, R.drawable.ic_erha_popup_warning),
    ERROR(R.string.erha_popup_title_error, R.drawable.ic_erha_popup_error);

    companion object {
        fun fromName(name: String?): ErHaStandardStyle =
            entries.find { it.name.equals(name, ignoreCase = true) } ?: INFO
    }
}
