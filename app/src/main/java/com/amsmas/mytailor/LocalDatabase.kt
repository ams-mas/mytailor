package com.amsmas.mytailor

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

/**
 * On-device database. Records remain available after the app is closed or the phone restarts.
 * Backup/export and restore/import can be added later without changing the app's data model.
 */
internal class LocalDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE workshop_skills (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE COLLATE NOCASE)")
        db.execSQL("CREATE TABLE tailors (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, phone TEXT NOT NULL, address TEXT NOT NULL DEFAULT '', skills_json TEXT NOT NULL)")
        db.execSQL("CREATE TABLE garment_models (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE COLLATE NOCASE, measurements_json TEXT NOT NULL)")
        createOrdersTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS garment_models (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE COLLATE NOCASE, measurements_json TEXT NOT NULL)")
        }
        if (oldVersion < 3) createOrdersTable(db)
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

    fun getGarmentModels(): List<GarmentModel> {
        val result = mutableListOf<GarmentModel>()
        readableDatabase.rawQuery("SELECT name, measurements_json FROM garment_models ORDER BY id", null).use { cursor ->
            while (cursor.moveToNext()) {
                val measurements = JSONArray(cursor.getString(1))
                result.add(GarmentModel(cursor.getString(0), (0 until measurements.length()).map { measurements.getString(it) }))
            }
        }
        return result
    }

    fun saveGarmentModels(models: List<GarmentModel>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("garment_models", null, null)
            models.forEach { model ->
                val name = model.name.trim()
                if (name.isNotBlank()) {
                    db.insertOrThrow("garment_models", null, ContentValues().apply {
                        put("name", name)
                        put("measurements_json", JSONArray(model.measurements.map { it.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }).toString())
                    })
                }
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


    private fun createOrdersTable(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS orders (id INTEGER PRIMARY KEY AUTOINCREMENT, customer_name TEXT NOT NULL, phone TEXT NOT NULL, garment_model TEXT NOT NULL, measurements_json TEXT NOT NULL, notes TEXT NOT NULL DEFAULT '', delivery_date TEXT NOT NULL, price REAL NOT NULL, deposit REAL NOT NULL, paid REAL NOT NULL, remaining REAL NOT NULL, status TEXT NOT NULL DEFAULT 'incomplete')")
    }

    fun addOrder(order: TailorOrder) {
        writableDatabase.insertOrThrow("orders", null, ContentValues().apply {
            put("customer_name", order.customerName)
            put("phone", order.phone)
            put("garment_model", order.garmentModel)
            put("measurements_json", JSONObject(order.measurements).toString())
            put("notes", order.notes)
            put("delivery_date", order.deliveryDate)
            put("price", order.price)
            put("deposit", order.deposit)
            put("paid", order.paid)
            put("remaining", (order.price - order.deposit - order.paid).coerceAtLeast(0.0))
            put("status", "incomplete")
        })
    }

    fun getOrderCounts(): Triple<Int, Int, Int> {
        fun count(selection: String? = null): Int {
            val sql = if (selection == null) "SELECT COUNT(*) FROM orders" else "SELECT COUNT(*) FROM orders WHERE $selection"
            readableDatabase.rawQuery(sql, null).use { cursor ->
                if (cursor.moveToFirst()) return cursor.getInt(0)
            }
            return 0
        }
        return Triple(count(), count("status = 'ready'"), count("status != 'ready'"))
    }

    fun createBackupJson(): String {
        val root = JSONObject()
        root.put("format", "mytailor-backup")
        root.put("version", 1)
        root.put("skills", JSONArray(getSkills()))
        val tailorsJson = JSONArray()
        getTailors().forEach { tailor ->
            tailorsJson.put(JSONObject().apply {
                put("name", tailor.name)
                put("phone", tailor.phone)
                put("address", tailor.address)
                put("skills", JSONArray(tailor.skills))
            })
        }
        root.put("tailors", tailorsJson)
        val modelsJson = JSONArray()
        getGarmentModels().forEach { model ->
            modelsJson.put(JSONObject().apply {
                put("name", model.name)
                put("measurements", JSONArray(model.measurements))
            })
        }
        root.put("garmentModels", modelsJson)
        return root.toString(2)
    }

    fun restoreBackupJson(jsonText: String) {
        val root = JSONObject(jsonText)
        require(root.optString("format") == "mytailor-backup") { "این فایل، نسخه پشتیبان MyTailor نیست." }
        require(root.optInt("version", -1) == 1) { "نسخه این فایل پشتیبان پشتیبانی نمی‌شود." }
        val skillsArray = root.getJSONArray("skills")
        val tailorsArray = root.getJSONArray("tailors")
        val modelsArray = root.optJSONArray("garmentModels") ?: JSONArray()
        val garmentModels = (0 until modelsArray.length()).map { i ->
            val item = modelsArray.getJSONObject(i)
            val measurements = item.optJSONArray("measurements") ?: JSONArray()
            GarmentModel(item.getString("name"), (0 until measurements.length()).map { measurements.getString(it) })
        }
        val skills = (0 until skillsArray.length()).map { skillsArray.getString(it).trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }
        val tailors = (0 until tailorsArray.length()).map { i ->
            val item = tailorsArray.getJSONObject(i)
            val itemSkills = item.getJSONArray("skills")
            TailorRecord(
                item.getString("name"),
                item.getString("phone"),
                item.optString("address", ""),
                (0 until itemSkills.length()).map { itemSkills.getString(it) }
            )
        }

        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("tailors", null, null)
            db.delete("workshop_skills", null, null)
            db.delete("garment_models", null, null)
            skills.forEach { name ->
                db.insertOrThrow("workshop_skills", null, ContentValues().apply { put("name", name) })
            }
            tailors.forEach { tailor ->
                db.insertOrThrow("tailors", null, ContentValues().apply {
                    put("name", tailor.name)
                    put("phone", tailor.phone)
                    put("address", tailor.address)
                    put("skills_json", JSONArray(tailor.skills).toString())
                })
            }
            garmentModels.forEach { model ->
                db.insertOrThrow("garment_models", null, ContentValues().apply {
                    put("name", model.name)
                    put("measurements_json", JSONArray(model.measurements).toString())
                })
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    companion object {
        private const val DATABASE_NAME = "mytailor_local.db"
        private const val DATABASE_VERSION = 3
    }
}
