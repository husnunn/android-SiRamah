package com.gridy.rohmahapp.data.local

import com.gridy.rohmahapp.utils.PreferenceClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Titik pusat menghapus data lokal setelah logout (Room + Preferences), sesuai databaseLocal.md.
 */
class UserLocalCacheCleaner(
    private val db: RohMahAppDatabase,
    private val pref: PreferenceClass,
) {

    suspend fun wipeUserScopedCachesOnly() =
        withContext(Dispatchers.IO) {
            db.clearAllTables()
            pref.clear()
        }

    suspend fun wipeRoomOnly() =
        withContext(Dispatchers.IO) {
            db.clearAllTables()
        }
}
