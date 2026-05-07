package com.gridy.rohmahapp.data.local.sync

/**
 * Slot refresh jaringan per-domain (buka sesi / kembali dari STOP / swipe manual).
 * Lihat `databaseLocal.md`.
 */
enum class RemoteRefreshSlot {
    PROFILE,
    SCHEDULE_NEAREST,
    SCHEDULE_LIST,
    ATTENDANCE,
}
