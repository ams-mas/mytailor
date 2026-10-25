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
internal data class CustomerSummary(val name: String, val phone: String, val activeOrderCount: Int, val orderCount: Int, val remaining: Double, val lastDeliveryDate: String)
internal data class CustomerOrderHistory(val id: Int, val registeredAt: String, val deliveryDate: String, val garmentModel: String, val measurementsJson: String, val notes: String, val price: Double, val deposit: Double, val paid: Double, val remaining: Double, val status: String)
internal data class DeliveryOrder(val id: Int, val customerName: String, val phone: String, val garmentModel: String, val deliveryDate: String, val price: Double, val paid: Double, val remaining: Double, val status: String)

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
        if (oldVersion >= 3 && oldVersion < 4) db.execSQL("ALTER TABLE orders ADD COLUMN registered_at TEXT NOT NULL DEFAULT ''")
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
        db.execSQL("CREATE TABLE IF NOT EXISTS orders (id INTEGER PRIMARY KEY AUTOINCREMENT, customer_name TEXT NOT NULL, phone TEXT NOT NULL, garment_model TEXT NOT NULL, measurements_json TEXT NOT NULL, notes TEXT NOT NULL DEFAULT '', delivery_date TEXT NOT NULL, registered_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, price REAL NOT NULL, deposit REAL NOT NULL, paid REAL NOT NULL, remaining REAL NOT NULL, status TEXT NOT NULL DEFAULT 'incomplete')")
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

    /**
     * Adds a fixed set of demo customers and orders without deleting or changing existing records.
     * Re-running this method will not duplicate the demo customers.
     */
    fun addSampleCustomers(): Int {
        val samples = listOf(
            Triple("نمونه - احمد محمدی", "0799000101", listOf(Triple("پیراهن", 1800.0, 500.0), Triple("شلوار", 1200.0, 1200.0))),
            Triple("نمونه - فاطمه احمدی", "0799000102", listOf(Triple("لباس افغانی", 4500.0, 2000.0), Triple("چادر", 2200.0, 700.0))),
            Triple("نمونه - محمود رحیمی", "0799000103", listOf(Triple("دریشی", 6500.0, 3000.0), Triple("پیراهن", 1600.0, 0.0))),
            Triple("نمونه - مریم کریمی", "0799000104", listOf(Triple("لباس مجلسی", 8000.0, 8000.0))),
            Triple("نمونه - حامد نوری", "0799000105", listOf(Triple("کرتی", 3500.0, 1000.0), Triple("شلوار", 1400.0, 400.0))),
            Triple("نمونه - زینب حیدری", "0799000106", listOf(Triple("لباس افغانی", 5200.0, 2500.0))),
            Triple("نمونه - بلال صادقی", "0799000107", listOf(Triple("پیراهن", 1700.0, 1700.0), Triple("دریشی", 7000.0, 3500.0))),
            Triple("نمونه - لیلا سادات", "0799000108", listOf(Triple("لباس مجلسی", 9000.0, 3000.0))),
            Triple("نمونه - جاوید اکبری", "0799000109", listOf(Triple("کرتی", 3800.0, 1000.0), Triple("پیراهن", 1500.0, 500.0))),
            Triple("نمونه - سمیرا عظیمی", "0799000110", listOf(Triple("لباس افغانی", 4800.0, 1800.0))),
            Triple("نمونه - ناصر احمدی", "0799000111", listOf(Triple("دریشی", 7200.0, 2500.0), Triple("شلوار", 1300.0, 1300.0))),
            Triple("نمونه - شبانه حبیبی", "0799000112", listOf(Triple("لباس مجلسی", 6800.0, 0.0))),
            Triple("نمونه - فرهاد یوسفی", "0799000113", listOf(Triple("پیراهن", 1900.0, 900.0))),
            Triple("نمونه - رویا امینی", "0799000114", listOf(Triple("لباس افغانی", 5600.0, 4000.0), Triple("چادر", 2400.0, 1200.0))),
            Triple("نمونه - کریم شاه", "0799000115", listOf(Triple("کرتی", 3200.0, 500.0)))
        )
        val db = writableDatabase
        var added = 0
        db.beginTransaction()
        try {
            samples.forEachIndexed { customerIndex, sample ->
                val (name, phone, orders) = sample
                val exists = db.rawQuery("SELECT 1 FROM orders WHERE customer_name = ? AND phone = ? LIMIT 1", arrayOf(name, phone)).use { it.moveToFirst() }
                if (!exists) {
                    orders.forEachIndexed { orderIndex, item ->
                        val (model, price, totalPaid) = item
                        val deposit = if (orderIndex == 0) totalPaid / 2.0 else 0.0
                        val paid = totalPaid - deposit
                        val ready = (customerIndex + orderIndex) % 4 == 0
                        val day = ((customerIndex + orderIndex) % 25) + 1
                        db.insertOrThrow("orders", null, ContentValues().apply {
                            put("customer_name", name)
                            put("phone", phone)
                            put("garment_model", model)
                            put("measurements_json", JSONObject().toString())
                            put("notes", "داده نمونه برای آزمایش برنامه")
                            put("delivery_date", "۱۴۰۵/۰۸/" + day.toString().padStart(2, '0'))
                            put("price", price)
                            put("deposit", deposit)
                            put("paid", paid)
                            put("remaining", (price - totalPaid).coerceAtLeast(0.0))
                            put("status", if (ready) "ready" else "incomplete")
                        })
                    }
                    added++
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return added
    }

    fun getCustomers(search: String = ""): List<CustomerSummary> {
        val result = mutableListOf<CustomerSummary>()
        val term = search.trim()
        val where = if (term.isBlank()) "" else "WHERE customer_name LIKE ? OR phone LIKE ?"
        val args = if (term.isBlank()) null else arrayOf("%$term%", "%$term%")
        val sql = """
            SELECT customer_name, phone,
                   SUM(CASE WHEN status NOT IN ('ready', 'delivered') THEN 1 ELSE 0 END) AS active_order_count,
                   COUNT(*) AS order_count,
                   COALESCE(SUM(remaining), 0) AS total_remaining,
                   (SELECT o2.delivery_date FROM orders o2
                    WHERE o2.customer_name = orders.customer_name AND o2.phone = orders.phone
                    ORDER BY o2.id DESC LIMIT 1) AS last_delivery_date
            FROM orders
            $where
            GROUP BY customer_name, phone
            ORDER BY MAX(id) DESC
        """.trimIndent()
        readableDatabase.rawQuery(sql, args).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(CustomerSummary(
                    name = cursor.getString(0) ?: "",
                    phone = cursor.getString(1) ?: "",
                    activeOrderCount = cursor.getInt(2),
                    orderCount = cursor.getInt(3),
                    remaining = cursor.getDouble(4),
                    lastDeliveryDate = cursor.getString(5) ?: ""
                ))
            }
        }
        return result
    }

    fun getCustomerOrders(name: String, phone: String, activeOnly: Boolean): List<CustomerOrderHistory> {
        val result = mutableListOf<CustomerOrderHistory>()
        val statusFilter = if (activeOnly) " AND status NOT IN ('ready', 'delivered')" else ""
        val sql = "SELECT id, registered_at, delivery_date, garment_model, measurements_json, notes, price, deposit, paid, remaining, status FROM orders WHERE customer_name = ? AND phone = ?$statusFilter ORDER BY id DESC"
        readableDatabase.rawQuery(sql, arrayOf(name, phone)).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(CustomerOrderHistory(cursor.getInt(0), cursor.getString(1) ?: "", cursor.getString(2) ?: "", cursor.getString(3) ?: "", cursor.getString(4) ?: "{}", cursor.getString(5) ?: "", cursor.getDouble(6), cursor.getDouble(7), cursor.getDouble(8), cursor.getDouble(9), cursor.getString(10) ?: ""))
            }
        }
        return result
    }

    fun updateCustomerDetails(oldName: String, oldPhone: String, newName: String, newPhone: String) {
        writableDatabase.update(
            "orders",
            ContentValues().apply { put("customer_name", newName.trim()); put("phone", newPhone.trim()) },
            "customer_name = ? AND phone = ?",
            arrayOf(oldName, oldPhone)
        )
    }

    fun deleteCustomerOrders(name: String, phone: String) {
        writableDatabase.delete("orders", "customer_name = ? AND phone = ?", arrayOf(name, phone))
    }

    fun getActiveOrders(search: String = ""): List<DeliveryOrder> {
        val result = mutableListOf<DeliveryOrder>()
        val term = search.trim()
        val sql = """
            SELECT id, customer_name, phone, garment_model, delivery_date, price, paid, remaining, status
            FROM orders
            WHERE status NOT IN ('ready', 'delivered')
              AND (? = '' OR customer_name LIKE ? OR phone LIKE ?)
            ORDER BY id DESC
        """.trimIndent()
        readableDatabase.rawQuery(sql, arrayOf(term, "%$term%", "%$term%")).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(DeliveryOrder(
                    id = cursor.getInt(0),
                    customerName = cursor.getString(1),
                    phone = cursor.getString(2),
                    garmentModel = cursor.getString(3),
                    deliveryDate = cursor.getString(4),
                    price = cursor.getDouble(5),
                    paid = cursor.getDouble(6),
                    remaining = cursor.getDouble(7),
                    status = cursor.getString(8)
                ))
            }
        }
        return result
    }

    fun getReadyForDeliveryOrders(search: String = ""): List<DeliveryOrder> {
        val result = mutableListOf<DeliveryOrder>()
        val term = search.trim()
        val sql = """
            SELECT id, customer_name, phone, garment_model, delivery_date, price, paid, remaining, status
            FROM orders
            WHERE status = 'ready'
              AND (? = '' OR customer_name LIKE ? OR phone LIKE ?)
            ORDER BY id DESC
        """.trimIndent()
        readableDatabase.rawQuery(sql, arrayOf(term, "%$term%", "%$term%")).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(DeliveryOrder(
                    id = cursor.getInt(0),
                    customerName = cursor.getString(1),
                    phone = cursor.getString(2),
                    garmentModel = cursor.getString(3),
                    deliveryDate = cursor.getString(4),
                    price = cursor.getDouble(5),
                    paid = cursor.getDouble(6),
                    remaining = cursor.getDouble(7),
                    status = cursor.getString(8)
                ))
            }
        }
        return result
    }

    fun collectRemainingAndDeliver(orderId: Int, paymentAmount: Double): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val current = db.rawQuery("SELECT remaining, paid, status FROM orders WHERE id = ?", arrayOf(orderId.toString())).use { cursor ->
                if (!cursor.moveToFirst() || cursor.getString(2) != "ready") return false
                Triple(cursor.getDouble(0), cursor.getDouble(1), cursor.getString(2))
            }
            if (paymentAmount < 0 || paymentAmount > current.first + 0.0001) return false
            val newRemaining = (current.first - paymentAmount).coerceAtLeast(0.0)
            if (newRemaining > 0.0001) return false
            db.update("orders", ContentValues().apply {
                put("paid", current.second + paymentAmount)
                put("remaining", 0.0)
                put("status", "delivered")
            }, "id = ? AND status = 'ready'", arrayOf(orderId.toString()))
            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    fun updateOrderStatus(orderId: Int, status: String) {
        writableDatabase.update("orders", ContentValues().apply { put("status", status) }, "id = ?", arrayOf(orderId.toString()))
    }

    fun updateOrderMeasurements(orderId: Int, measurements: Map<String, String>) {
        val values = ContentValues().apply {
            put("measurements_json", JSONObject(measurements).toString())
        }
        writableDatabase.update("orders", values, "id = ?", arrayOf(orderId.toString()))
    }

    fun getOrderCounts(): Triple<Int, Int, Int> {
        fun count(selection: String? = null): Int {
            val sql = if (selection == null) "SELECT COUNT(*) FROM orders" else "SELECT COUNT(*) FROM orders WHERE $selection"
            readableDatabase.rawQuery(sql, null).use { cursor ->
                if (cursor.moveToFirst()) return cursor.getInt(0)
            }
            return 0
        }
        return Triple(count(), count("status IN ('ready', 'delivered')"), count("status NOT IN ('ready', 'delivered')"))
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
        private const val DATABASE_VERSION = 4
    }
}
