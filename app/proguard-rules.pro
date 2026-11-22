# Keep serialized models and Room generated adapters.
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class org.libraryofarts.**$$serializer { *; }
-keepclassmembers class org.libraryofarts.** {
    *** Companion;
}
-keepclasseswithmembers class org.libraryofarts.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class * extends androidx.work.ListenableWorker { <init>(...); }
-keep class * extends android.service.wallpaper.WallpaperService { *; }
-keep class * extends android.service.dreams.DreamService { *; }
-keep class * extends android.service.quicksettings.TileService { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-dontwarn org.slf4j.**
-dontwarn com.google.ar.core.**
