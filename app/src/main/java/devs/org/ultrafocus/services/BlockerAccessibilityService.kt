package devs.org.ultrafocus.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import devs.org.ultrafocus.activities.BlockedAppActivity
import devs.org.ultrafocus.database.AppDatabase
import devs.org.ultrafocus.repository.AppRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class BlockerAccessibilityService : AccessibilityService() {

    private var blockedApps: List<String> = emptyList()
    private lateinit var appRepository: AppRepository
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isServiceReady = false

    // Debouncing mechanism
    private var lastBlockedPackage: String? = null
    private var lastBlockTime: Long = 0
    private val blockCooldownMs = 2000L // 2 seconds cooldown

    // Track currently blocked apps to prevent multiple instances
    private val currentlyBlockedApps = mutableSetOf<String>()
    private var blockedAppInfos: List<devs.org.ultrafocus.model.AppInfo> = emptyList()

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("BlockerService", "onServiceConnected called")

        try {
            val db = AppDatabase.getDatabase(this)
            appRepository = AppRepository(db)

            // Load and observe blocked apps
            loadBlockedApps()

            // Mark service as ready
            isServiceReady = true
            Log.d("BlockerService", "Service is now ready")

        } catch (e: Exception) {
            Log.e("BlockerService", "Error in onServiceConnected", e)
        }
    }

    private fun loadBlockedApps() {
        serviceScope.launch {
            try {
                Log.d("BlockerService", "Starting to load blocked apps")
                appRepository.getBlockedAppsFlow()
                    .collectLatest { appInfos ->
                        blockedAppInfos = appInfos
                        blockedApps = appInfos.map { it.packageName }
                        Log.d("BlockerService", "Updated blocked apps: ${blockedApps.size} apps")
                        Log.d("BlockerService", "Blocked apps list: $blockedApps")
                    }
                Toast.makeText(this@BlockerAccessibilityService, "Service Connected", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("BlockerService", "Error loading blocked apps", e)
                blockedApps = emptyList()
                blockedAppInfos = emptyList()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Add more detailed logging
        Log.d("BlockerService", "onAccessibilityEvent triggered - eventType: ${event?.eventType}, packageName: ${event?.packageName}")

        if (!isServiceReady) {
            Log.d("BlockerService", "Service not ready yet, ignoring event")
            return
        }

        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChanged(event)
            }
            else -> {
                Log.d("BlockerService", "Ignoring event type: ${event?.eventType}")
            }
        }
    }

    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString()
        if (packageName.isNullOrEmpty()) {
            Log.d("BlockerService", "Package name is null or empty")
            return
        }
        Log.d("BlockerService", "Window state changed for: $packageName")
        if (packageName == this.packageName || packageName == "com.android.systemui") {
            Log.d("BlockerService", "Ignoring system or own app: $packageName")
            return
        }
        val appInfo = blockedAppInfos.find { it.packageName == packageName }
        if (appInfo != null && shouldBlockNow(appInfo)) {
            Log.d("BlockerService", "Attempting to block app: $packageName")
            blockApp(packageName)
        } else {
            Log.d("BlockerService", "App $packageName is not in blocked list or not in block time")
            currentlyBlockedApps.remove(packageName)
        }
    }

    private fun shouldBlockNow(appInfo: devs.org.ultrafocus.model.AppInfo): Boolean {
        val from = appInfo.fromTime
        val to = appInfo.toTime
        val repeat = appInfo.repeatMode
        if (from.isNullOrEmpty() || to.isNullOrEmpty() || repeat.isNullOrEmpty()) return true // If not set, block always
        val now = java.util.Calendar.getInstance()
        val nowMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
        val fromParts = from.split(":").map { it.toIntOrNull() ?: 0 }
        val toParts = to.split(":").map { it.toIntOrNull() ?: 0 }
        val fromMinutes = fromParts[0] * 60 + fromParts[1]
        val toMinutes = toParts[0] * 60 + toParts[1]
        val inTimeRange = if (fromMinutes <= toMinutes) {
            nowMinutes in fromMinutes..toMinutes
        } else {
            nowMinutes >= fromMinutes || nowMinutes <= toMinutes // Overnight
        }
        val inRepeat = when (repeat) {
            "DAILY" -> true
            "WEEKLY" -> now.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.MONDAY // Example: block on Mondays
            "MONTHLY" -> now.get(java.util.Calendar.DAY_OF_MONTH) == 1 // Example: block on 1st
            else -> true
        }
        return inTimeRange && inRepeat
    }

    private fun blockApp(packageName: String) {
        try {
            val currentTime = System.currentTimeMillis()

            // Check if we recently blocked this same app
            if (lastBlockedPackage == packageName &&
                currentTime - lastBlockTime < blockCooldownMs) {
                Log.d("BlockerService", "Ignoring rapid block attempt for: $packageName")
                return
            }

            // Check if app is currently being blocked
            if (currentlyBlockedApps.contains(packageName)) {
                Log.d("BlockerService", "App $packageName is already being blocked")
                return
            }

            Log.d("BlockerService", "Blocking app: $packageName")

            // Add to currently blocked set
            currentlyBlockedApps.add(packageName)

            // Update last block info
            lastBlockedPackage = packageName
            lastBlockTime = currentTime

            // Move the blocked app to the background by launching home
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(homeIntent)

            // Small delay to ensure home is launched
            serviceScope.launch {
                delay(200) // Slightly longer delay

                try {
                    val intent = Intent(this@BlockerAccessibilityService, BlockedAppActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                                Intent.FLAG_ACTIVITY_NO_HISTORY or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra("blocked_package", packageName)
                    }
                    startActivity(intent)
                    Log.d("BlockerService", "Successfully blocked app: $packageName")

                    // Remove from currently blocked set after a delay
                    delay(1000)
                    currentlyBlockedApps.remove(packageName)

                } catch (e: Exception) {
                    Log.e("BlockerService", "Error starting BlockedAppActivity", e)
                    currentlyBlockedApps.remove(packageName)
                }
            }

        } catch (e: Exception) {
            Log.e("BlockerService", "Error blocking app: $packageName", e)
            currentlyBlockedApps.remove(packageName)
        }
    }

    override fun onInterrupt() {
        Log.d("BlockerService", "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        isServiceReady = false
        currentlyBlockedApps.clear()
        Log.d("BlockerService", "Service destroyed")
    }

    // Method to check if service is working
    fun isServiceWorking(): Boolean {
        return isServiceReady && blockedApps.isNotEmpty()
    }

    // Method to manually refresh blocked apps (for testing)
    fun refreshBlockedApps() {
        if (::appRepository.isInitialized) {
            loadBlockedApps()
        }
    }
}