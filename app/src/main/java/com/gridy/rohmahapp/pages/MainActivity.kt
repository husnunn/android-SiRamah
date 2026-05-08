package com.gridy.rohmahapp.pages

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.gridy.rohmahapp.R
import com.gridy.rohmahapp.data.local.sync.AppRemoteRefreshGate
import com.gridy.rohmahapp.databinding.ActivityMainRedesignBinding
import com.gridy.rohmahapp.di.Injection
import com.gridy.rohmahapp.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainRedesignBinding
    private lateinit var navController: NavController
    private val ioScope = CoroutineScope(Dispatchers.IO)

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) registerFcmTokenForLoggedInUser()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainRedesignBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInsets()
        setupNavigation()
        setupNotification()
        handleNotificationIntent(intent)

        AppRemoteRefreshGate.markRemoteRefreshExpectedForNewSession()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
    }

    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(binding.navHostFragment.id) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)
    }

    private fun setupNotification() {
        NotificationHelper.createChannel(this)
        askNotificationPermissionIfNeeded()
        registerFcmTokenForLoggedInUser()
    }

    private fun askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val type = intent?.getStringExtra(NotificationHelper.EXTRA_NOTIFICATION_TYPE) ?: return
        when (type) {
            "teacher_schedule_start_reminder",
            "teacher_schedule_end_reminder",
            "teacher_schedule_ended",
            "student_first_schedule_reminder" -> {
                if (!::navController.isInitialized) return
                if (navController.currentDestination?.id != R.id.scheduleFragment) {
                    navController.navigate(R.id.scheduleFragment)
                }
                binding.bottomNavigation.selectedItemId = R.id.scheduleFragment
            }
        }
        intent.removeExtra(NotificationHelper.EXTRA_NOTIFICATION_TYPE)
    }

    private fun registerFcmTokenForLoggedInUser() {
        ioScope.launch {
            Injection.provideDeviceTokenRepository(this@MainActivity)
                .registerCurrentTokenIfLoggedIn()
        }
    }
}