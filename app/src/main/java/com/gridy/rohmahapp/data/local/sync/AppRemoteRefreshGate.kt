package com.gridy.rohmahapp.data.local.sync

import java.util.EnumSet

/**
 * Kapan boleh memanggil API untuk memperbarui cache Room — selain swipe refresh manual:
 * pembukaan sesi utama MainActivity, atau kembalinya proses app ke foreground setelah ON_STOP.
 *
 * Setiap [RemoteRefreshSlot] boleh dikonsumsi sekali per peristiwa tersebut (idempoten per slot).
 * Swipe refresh tidak memakai gate; kirim `forceRemoteRefresh = true` langsung dari UI.
 */
object AppRemoteRefreshGate {

    private val lock = Any()

    private var sessionEventPending: Boolean = false

    private val pendingSlots: EnumSet<RemoteRefreshSlot> =
        EnumSet.noneOf(RemoteRefreshSlot::class.java)

    fun markRemoteRefreshExpectedForNewSession() {
        synchronized(lock) {
            sessionEventPending = true
        }
    }

    fun notifyAppEnteredForegroundAfterBeingStopped() {
        synchronized(lock) {
            sessionEventPending = true
        }
    }

    /** `true` sekali per slot sampai peristiwa sesi berikutnya mengisi ulang semua slot. */
    fun consumeRemoteRefresh(slot: RemoteRefreshSlot): Boolean {
        synchronized(lock) {
            if (sessionEventPending) {
                sessionEventPending = false
                pendingSlots.clear()
                pendingSlots.addAll(EnumSet.allOf(RemoteRefreshSlot::class.java))
            }
            return pendingSlots.remove(slot)
        }
    }
}
