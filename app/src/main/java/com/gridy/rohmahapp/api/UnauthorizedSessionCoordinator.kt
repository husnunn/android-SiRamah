package com.gridy.rohmahapp.api

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.gridy.rohmahapp.R
import com.gridy.rohmahapp.RohMahApp
import com.gridy.rohmahapp.pages.auth.LoginActivity
import com.gridy.rohmahapp.utils.PreferenceClass
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Respons 401 dari endpoint selain login: popup + hapus sesi + buka [LoginActivity].
 */
object UnauthorizedSessionCoordinator {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val busy = AtomicBoolean(false)

    fun notifyUnauthorized(app: RohMahApp, pref: PreferenceClass, message: String?) {
        if (!busy.compareAndSet(false, true)) return
        mainHandler.post {
            try {
                val act = app.currentForegroundActivity()
                if (act is LoginActivity) {
                    busy.set(false)
                    return@post
                }

                val displayMsg = message?.takeIf { it.isNotBlank() }
                    ?: app.getString(R.string.session_expired_message)

                val host = act?.takeIf { !it.isFinishing && !it.isDestroyed }

                if (host != null) {
                    MaterialAlertDialogBuilder(host)
                        .setTitle(R.string.session_expired_title)
                        .setMessage(displayMsg)
                        .setCancelable(false)
                        .setPositiveButton(R.string.session_expired_ok) { _, _ ->
                            pref.clear()
                            navigateLogin(app)
                            busy.set(false)
                        }
                        .show()
                } else {
                    pref.clear()
                    navigateLogin(app)
                    busy.set(false)
                }
            } catch (_: Throwable) {
                busy.set(false)
            }
        }
    }

    private fun navigateLogin(app: RohMahApp) {
        val intent = Intent(app, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        app.startActivity(intent)
    }
}
