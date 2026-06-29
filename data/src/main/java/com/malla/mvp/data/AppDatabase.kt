package com.malla.mvp.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.malla.mvp.data.dao.ConversationDao
import com.malla.mvp.data.dao.MessageDao
import com.malla.mvp.data.dao.StoryDao
import com.malla.mvp.data.dao.PollDao
import com.malla.mvp.data.dao.IdentityDao
import com.malla.mvp.data.dao.ContactDao
import com.malla.mvp.data.entity.ConversationEntity
import com.malla.mvp.data.entity.MessageEntity
import com.malla.mvp.data.entity.StoryEntity
import com.malla.mvp.data.entity.PollEntity
import com.malla.mvp.data.entity.PollOptionEntity
import com.malla.mvp.data.entity.UserIdentityEntity
import com.malla.mvp.data.entity.ContactEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        StoryEntity::class,
        PollEntity::class,
        PollOptionEntity::class,
        UserIdentityEntity::class,
        ContactEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun storyDao(): StoryDao
    abstract fun pollDao(): PollDao
    abstract fun identityDao(): IdentityDao
    abstract fun contactDao(): ContactDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase? {
            return try {
                INSTANCE ?: synchronized(this) {
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "malla_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                        .also { INSTANCE = it }
                }
            } catch (e: Exception) {
                Log.e("AppDatabase", "Error creando la base de datos", e)
                null
            }
        }
    }
}
