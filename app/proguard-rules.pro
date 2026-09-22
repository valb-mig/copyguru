# Room generates implementations reflectively referenced by the generated DAOs.
-keep class androidx.room.RoomDatabase { *; }

# The overlay service is started by name from the app and by the system on restart.
-keep class com.valb.copyguru.overlay.BubbleService { *; }
