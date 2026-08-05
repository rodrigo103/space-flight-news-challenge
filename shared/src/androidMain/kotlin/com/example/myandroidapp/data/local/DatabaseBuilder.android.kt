package com.example.myandroidapp.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/** Igual que arriba: devuelve el tipo propio del módulo, sin exponer Room a los consumidores. */
fun createDatabase(context: Context): AppDatabase = buildDatabase(databaseBuilder(context))

fun databaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath(DATABASE_NAME)
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    )
}
