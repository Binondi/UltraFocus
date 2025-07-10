package devs.org.ultrafocus.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import devs.org.ultrafocus.model.AppInfo
import devs.org.ultrafocus.repository.AppRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

class MainViewModel(private val repository: AppRepository) : ViewModel() {

    // UI State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    // Flows for real-time updates
    fun getBlockedAppsFlow(): Flow<List<AppInfo>> = repository.getBlockedAppsFlow()

    fun getBlockedPackagesFlow(context: Context): Flow<List<String>> =
        repository.getBlockedPackagesFlow(context)

    // Combined flow for apps with their blocked status
    fun getAppsWithBlockedStatus(context: Context): Flow<List<Pair<AppInfo, Boolean>>> {
        return combine(
            installedApps,
            getBlockedPackagesFlow(context)
        ) { apps, blockedPackages ->
            apps.map { app ->
                app to blockedPackages.contains(app.packageName)
            }
        }
    }

    // Load all installed apps
    suspend fun loadInstalledApps(context: Context) {

            try {
                _isLoading.value = true
                _error.value = null

                val apps = repository.getInstalledApps(context)
                _installedApps.value = apps

            } catch (e: Exception) {
                _error.value = "Failed to load installed apps: ${e.message}"
            } finally {
                _isLoading.value = false
            }
    }

    // Get all apps (non-suspend version for compatibility)
    fun listAllApps(context: Context): List<AppInfo> = repository.getInstalledApps(context)

    // Get blocked apps as a one-time list
    suspend fun getBlockedApps(): MutableList<AppInfo>{
        val list = mutableListOf<AppInfo>()
        list.addAll(repository.getBlockedApps())
        return list
    }

    // Get blocked packages as a one-time list
    suspend fun getBlockedPackages(context: Context): List<String> =
        repository.getBlockedPackages(context)

    // Block an app
     suspend fun blockApp(blockedApp: AppInfo, reason: String = "Blocked by user") {

            try {
                repository.addBlockedApp(blockedApp)
            } catch (e: Exception) {
                _error.value = "Failed to block app: ${e.message}"
            }
    }

    // Unblock an app by AppInfo
    suspend fun unblockApp(appInfo: AppInfo) {

            try {
                repository.removeBlockedAppByPackage(appInfo.packageName)
            } catch (e: Exception) {
                _error.value = "Failed to unblock app: ${e.message}"
            }

    }

    // Unblock an app by package name
    suspend fun unblockAppByPackage(packageName: String) {

            try {
                repository.removeBlockedAppByPackage(packageName)
            } catch (e: Exception) {
                _error.value = "Failed to unblock app: ${e.message}"
            }
    }

    // Remove a blocked app by BlockedApp object
    suspend fun removeBlockedApp(blockedApp: AppInfo) {
            try {
                repository.removeBlockedApp(blockedApp)
            } catch (e: Exception) {
                _error.value = "Failed to remove blocked app: ${e.message}"
            }
    }

    // Toggle app blocked status
    suspend fun toggleAppBlockStatus(appInfo: AppInfo, reason: String = "Blocked by user") {
            try {
                val isBlocked = repository.isAppBlocked(appInfo.packageName)
                if (isBlocked) {
                    repository.removeBlockedAppByPackage(appInfo.packageName)
                } else {
                    repository.addBlockedApp(appInfo)
                }
            } catch (e: Exception) {
                _error.value = "Failed to toggle app status: ${e.message}"
            }
    }

    // Check if an app is blocked
    suspend fun isAppBlocked(packageName: String): Boolean =
        repository.isAppBlocked(packageName)

    // Clear error message
    fun clearError() {
        _error.value = null
    }

    // Search/filter apps
    fun searchApps(query: String): List<AppInfo> {
        return _installedApps.value.filter { app ->
            app.appName.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
        }
    }

    // Get apps by blocked status
    fun getAppsByBlockedStatus(context: Context, blocked: Boolean): Flow<List<AppInfo>> {
        return combine(
            installedApps,
            getBlockedPackagesFlow(context)
        ) { apps, blockedPackages ->
            apps.filter { app ->
                val isBlocked = blockedPackages.contains(app.packageName)
                if (blocked) isBlocked else !isBlocked
            }
        }
    }

    suspend fun updateBlockedAppsTimePeriod(fromTime: String?, toTime: String?, repeatMode: String?) {
        val blockedApps = getBlockedApps()
        blockedApps.forEach { app ->
            val updatedApp = app.copy(fromTime = fromTime, toTime = toTime, repeatMode = repeatMode)
            repository.addBlockedApp(updatedApp)
        }
    }

    suspend fun updateSingleAppTimePeriod(appInfo: AppInfo, fromTime: String?, toTime: String?, repeatMode: String?) {
        val updatedApp = appInfo.copy(fromTime = fromTime, toTime = toTime, repeatMode = repeatMode)
        repository.addBlockedApp(updatedApp)
    }
}