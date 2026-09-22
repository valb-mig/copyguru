package com.valb.copyguru.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Segment::class, Copy::class], version = 1, exportSchema = true)
abstract class CopyGuruDatabase : RoomDatabase() {

    abstract fun dao(): CopyGuruDao

    companion object {
        @Volatile
        private var instance: CopyGuruDatabase? = null

        fun get(context: Context): CopyGuruDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CopyGuruDatabase::class.java,
                    "copyguru.db"
                ).build().also { instance = it }
            }
    }
}
