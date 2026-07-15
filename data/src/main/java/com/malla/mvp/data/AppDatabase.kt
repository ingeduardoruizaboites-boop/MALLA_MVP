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
import java.io.File

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
        val CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL("INSERT INTO conversations (id, title, lastMessage, timestamp, unreadCount, isGroup) VALUES ('self_chat', 'Yo (Mensajes guardados)', 'Toca para guardar notas, imágenes, documentos...', strftime('%s','now'), 0, 0)")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase? {
            return try {
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: try {
                        Room.databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            "malla_database"
                        )
                            .fallbackToDestructiveMigration()
                            .addCallback(AppDatabase.CALLBACK)
                            .build()
                            .also { INSTANCE = it }
                    } catch (e: Exception) {
                        Log.e("AppDatabase", "Error al crear BD, eliminando y reintentando...", e)
                        val dbFile = context.getDatabasePath("malla_database")
                        if (dbFile.exists()) {
                            dbFile.delete()
                            dbFile.parentFile?.listFiles()?.filter { it.name.startsWith("malla_database") }?.forEach { it.delete() }
                        }
                        val instance = Room.databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            "malla_database"
                        )
                            .fallbackToDestructiveMigration()
                            .addCallback(AppDatabase.CALLBACK)
                            .build()
                        INSTANCE = instance
                        instance
                    }
                }
            } catch (e: Exception) {
                Log.e("AppDatabase", "No se pudo crear la base de datos después de reintentar", e)
                null
            }
        }
    }
}
