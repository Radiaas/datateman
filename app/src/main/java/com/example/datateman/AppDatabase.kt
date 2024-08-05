package com.example.datateman

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Friend::class],
    version = 2
)
abstract class myDatabase : RoomDatabase() {

    abstract fun friendDao(): FriendDao

    companion object {

        @Volatile
        private var INSTANCE: myDatabase? = null

        fun getInstance(context: Context): myDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }

            val instance = Room.databaseBuilder(
                context.applicationContext,
                myDatabase::class.java,
                "my_database"
            ).fallbackToDestructiveMigration().build()

            INSTANCE = instance
            return instance
        }


    }

}