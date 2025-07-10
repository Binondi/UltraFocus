package devs.org.ultrafocus.database

import androidx.room.*
import devs.org.ultrafocus.model.AppInfo
import kotlinx.coroutines.flow.Flow

// Entity class for AppInfo


@Dao
interface BlockedAppDao {
    @Query("SELECT * FROM AppInfo")
    fun getAll(): Flow<List<AppInfo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: AppInfo)

    @Delete
    suspend fun delete(app: AppInfo)

    // Additional useful methods
    @Query("DELETE FROM AppInfo WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("SELECT COUNT(*) > 0 FROM AppInfo WHERE packageName = :packageName")
    suspend fun isBlocked(packageName: String): Boolean

    @Query("SELECT packageName FROM AppInfo")
    fun getAllPackageNames(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM AppInfo")
    suspend fun getAppInfoCount(): Int

    @Query("DELETE FROM AppInfo")
    suspend fun deleteAll()
}