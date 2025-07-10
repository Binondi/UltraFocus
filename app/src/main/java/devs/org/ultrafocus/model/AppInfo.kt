package devs.org.ultrafocus.model

import android.graphics.drawable.Drawable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "AppInfo")
data class AppInfo(


    @PrimaryKey(autoGenerate = false)val packageName: String,
    val appName: String,
    val icon: Drawable,
    val blockedAt: Long = System.currentTimeMillis(),
    val isBlocked : Boolean = false,
    val fromTime: String? = null, // e.g., "08:00"
    val toTime: String? = null,   // e.g., "17:00"
    val repeatMode: String? = null // e.g., "DAILY", "WEEKLY", "MONTHLY"
)

