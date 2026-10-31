package com.vexedit.studio.data.project

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ProjectEntity::class], version = 1, exportSchema = false)
abstract class VexDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile private var INSTANCE: VexDatabase? = null

        fun getInstance(context: Context): VexDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VexDatabase::class.java,
                    "vex_edit_studio.db"
                ).build().also { INSTANCE = it }
            }
    }
}
