package devs.org.ultrafocus.extentions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.room.TypeConverter
import java.io.ByteArrayOutputStream

class DrawableConverter {

    @TypeConverter
    fun fromDrawable(drawable: Drawable?): ByteArray? {
        if (drawable == null) return null

        val bitmap = if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    @TypeConverter
    fun toDrawable(byteArray: ByteArray?): Drawable? {
        if (byteArray == null) return null

        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        return BitmapDrawable(null, bitmap)
    }
}

// Update your AppDatabase to include the converter
/*
@Database(
    entities = [AppInfo::class, BlockedApp::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DrawableConverter::class)
abstract class AppDatabase : RoomDatabase() {
    // ... rest of your database code
}
*/