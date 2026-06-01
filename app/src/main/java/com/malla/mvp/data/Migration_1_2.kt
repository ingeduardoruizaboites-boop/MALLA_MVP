package com.malla.mvp.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE polls ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
        database.execSQL("UPDATE polls SET createdAt = strftime('%s', 'now') * 1000 WHERE createdAt = 0")
    }
}
