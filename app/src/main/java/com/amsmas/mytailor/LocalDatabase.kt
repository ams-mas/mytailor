package com.amsmas.mytailor

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray

/**
 * On-device database. Records remain available after the app is closed or the phone restarts.
 * Backup/export and restore/import can be added later without changing the app's data model.
 */
internal class LocalDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE workshop_skills (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE COLLATE NOCASE)")
        db.execSQL("CREATE TABLE tailors (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, phone TEXT NOT NULL, address TEXT NOT NULL DEFAULT '', skills_json TEXT NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future database changes should use migrations so existing user data is preserved.
    }

    fun getSkills(): List<String> {
        val result = mutableListOf<String>()
        readableDatabase.rawQuery("SELECT name FROM workshop_skills ORDER BY name COLLATE NOCASE", null).use { cursor ->
            while (cursor.moveToNext()) result.add(cursor.getString(0))
        }
        return result
    }

    fun replaceSkills(skills: List<String>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("workshop_skills", null, null)
            skills.map { it.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }.forEach { name ->
                val values = ContentValues().apply { put("name", name) }
                db.insertOrThrow("workshop_skills", null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getTailors(): List<TailorRecord> {
        val result = mutableListOf<TailorRecord>()
        readableDatabase.rawQuery("SELECT name, phone, address, skills_json FROM tailors ORDER BY id DESC", null).use { cursor ->
            while (cursor.moveToNext()) {
                val names = mutableListOf<String>()
                val json = JSONArray(cursor.getString(3))
                for (i in 0 until json.length()) names.add(json.getString(i))
                result.add(TailorRecord(cursor.getString(0), cursor.getString(1), cursor.getString(2), names))
            }
        }
        return result
    }

    fun addTailor(tailor: TailorRecord) {
        val values = ContentValues().apply {
            put("name", tailor.name)
            put("phone", tailor.phone)
            put("address", tailor.address)
            put("skills_json", JSONArray(tailor.skills).toString())
        }
        writableDatabase.insertOrThrow("tailors", null, values)
    }

    companion object {
        private const val DATABASE_NAME = "mytailor_local.db"
        private const val DATABASE_VERSION = 1
    }
}
