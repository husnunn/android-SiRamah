package com.gridy.rohmahapp

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.gridy.rohmahapp.data.local.sync.AppRemoteRefreshGate
import com.gridy.rohmahapp.di.AppComponent
import com.gridy.rohmahapp.di.DaggerAppComponent
import com.gridy.rohmahapp.BuildConfig
import com.gridy.rohmahapp.di.module.AppModule
import com.gridy.rohmahapp.di.module.NetworkModule
import java.lang.ref.WeakReference
import timber.log.Timber

class RohMahApp : Application() {

    lateinit var appComponent: AppComponent
        private set

    private var resumedActivityRef: WeakReference<Activity>? = null

    private var processWasInBackground: Boolean = false

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    processWasInBackground = true
                }

                override fun onStart(owner: LifecycleOwner) {
                    if (processWasInBackground) {
                        AppRemoteRefreshGate.notifyAppEnteredForegroundAfterBeingStopped()
                        processWasInBackground = false
                    }
                }
            },
        )

        appComponent = DaggerAppComponent.builder()
            .appModule(AppModule(this))
            .networkModule(NetworkModule())
            .build()

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                resumedActivityRef = WeakReference(activity)
            }

            override fun onActivityPaused(activity: Activity) {
                if (resumedActivityRef?.get() === activity) resumedActivityRef = null
            }

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                if (resumedActivityRef?.get() === activity) resumedActivityRef = null
            }
        })
    }

    /** Aktivitas foreground untuk popup sesi berakhir (401). */
    internal fun currentForegroundActivity(): Activity? =
        resumedActivityRef?.get()?.takeIf { !it.isFinishing && !it.isDestroyed }
}