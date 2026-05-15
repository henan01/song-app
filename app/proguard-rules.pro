# Keep Room entities & Compose
-keep class com.songapp.ktv.data.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-dontwarn org.bouncycastle.**
