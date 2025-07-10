# UltraFocus ProGuard rules

# Keep model/data classes (Room, Gson, etc.)
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}


# Room (Database)
-keep class androidx.room.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    *;
}
-keepclassmembers class * extends androidx.room.RoomDatabase$Builder {
    *;
}
-dontwarn androidx.room.**

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** { *; }
-dontwarn com.bumptech.glide.**

# Coroutines
-dontwarn kotlinx.coroutines.**

# Material Components
-dontwarn com.google.android.material.**

# Keep ViewBinding
-keep class **ViewBinding { *; }

# Keep all annotations
-keep @interface *

# General Android rules
-keep class * extends android.app.Activity
-keep class * extends android.app.Service
-keep class * extends android.app.Application
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider

# Keep all Parcelable implementations
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# Keep all classes with @Entity, @Dao, @Database, @TypeConverters
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep @androidx.room.Database class *
-keep @androidx.room.TypeConverters class *

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep MainActivity and services
-keep class devs.org.ultrafocus.activities.MainActivity { *; }
-keep class devs.org.ultrafocus.services.** { *; }

# Add any additional rules for libraries you use below