package devs.org.ultrafocus.repository

import android.content.Context
import android.content.pm.PackageManager
import devs.org.ultrafocus.database.AppDatabase
import devs.org.ultrafocus.model.AppInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AppRepository(private val db: AppDatabase) {

    // This doesn't need to be suspend since it's not doing database operations
    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        return apps.filter {
            pm.getLaunchIntentForPackage(it.packageName) != null
        }.map {
            AppInfo(
                appName = it.loadLabel(pm).toString(),
                packageName = it.packageName,
                icon = it.loadIcon(pm)
            )
        }
    }

    // Get blocked packages as a one-time list
    suspend fun getBlockedPackages(context: Context): List<String> {
        return db.blockedAppDao().getAll().first().map { it.packageName }
    }

    // Get blocked packages as a Flow for real-time updates
    fun getBlockedPackagesFlow(context: Context): Flow<List<String>> {
        return db.blockedAppDao().getAll().map { apps ->
            apps.map { it.packageName }
        }
    }

    // Get blocked apps (full objects) as a one-time list
    suspend fun getBlockedApps(): List<AppInfo> {
        return db.blockedAppDao().getAll().first()
    }

    // Get blocked apps (full objects) as a Flow for real-time updates
    fun getBlockedAppsFlow(): Flow<List<AppInfo>> {
        return db.blockedAppDao().getAll()
    }

    // Add a blocked app
    suspend fun addBlockedApp(blockedApp: AppInfo) {
        db.blockedAppDao().insert(blockedApp)
    }

    // Remove a blocked app
    suspend fun removeBlockedApp(blockedApp: AppInfo) {
        db.blockedAppDao().delete(blockedApp)
    }

    // Remove blocked app by package name
    suspend fun removeBlockedAppByPackage(packageName: String) {
        db.blockedAppDao().deleteByPackageName(packageName)
    }

    // Check if an app is blocked
    suspend fun isAppBlocked(packageName: String): Boolean {
        return db.blockedAppDao().isBlocked(packageName)
    }
}