package com.example.moneypad.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.moneypad.data.dao.MoneyPadDao
import com.example.moneypad.data.model.*

@Database(
    entities = [
        User::class,
        Story::class,
        StoryPart::class,
        Transaction::class,
        Conversation::class,
        Follow::class,
        Review::class,
        UserReadPart::class,
        UserStoryLike::class,
        PartAnnotation::class
    ],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun moneyPadDao(): MoneyPadDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "moneypad_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
