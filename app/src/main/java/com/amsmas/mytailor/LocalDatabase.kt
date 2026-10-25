package com.amsmas.mytailor

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.compose.runtime.mutableIntStateOf
import org.json.JSONArray
import org.json.JSONObject

private const val DATABASE_NAME = "mytailor_local.db"
private const val DATABASE_VERSION = 6

/**
 * On-device database. Records remain available after the app is closed or the phone restarts.
 * Backup/export and restore/import can be added later without changing the app's data model.
 */
internal data class CustomerSummary(val name: String, val phone: String, val activeOrderCount: Int, val orderCount: Int, val remaining: Double, val lastDeliveryDate: String, val address: String = "")
internal data class CustomerOrderHistory(val id: Int, val registeredAt: String, val deliveryDate: String, val garmentModel: String, val measurementsJson: String, val notes: String, val price: Double, val deposit: Double, val paid: Double, val remaining: Double, val status: String)
internal data class DeliveryOrder(val id: Int, val customerName: String, val phone: String, val garmentModel: String, val deliveryDate: String, val price: Double, val paid: Double, val remaining: Double, val status: String)
internal data class WorkerRecord(val id: Int = 0, val name: String, val phone: String = "", val address: String = "", val payType: String = "piece_rate", val payRate: Double = 0.0, val active: Boolean = true)
internal data class WorkflowStepRecord(val id: Int, val name: String, val stepOrder: Int)
internal data class OrderWorkflowRecord(val assignmentId: Int, val orderId: Int, val stepId: Int, val stepName: String, val workerId: Int?, val workerName: String, val status: String, val completedAt: String)
internal data class CustomerMeasurementRecord(val orderType: String, val fieldName: String, val value: String, val recordedAt: String)
internal data class WorkerPayrollRecord(val workerId: Int, val name: String, val payType: String, val payRate: Double, val units: Int, val earned: Double, val paid: Double) { val due: Double get() = (earned - paid).coerceAtLeast(0.0) }

internal class LocalDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    // Compose screens observe this counter to refresh database-backed lists immediately.
    val revision = mutableIntStateOf(0)
    private fun notifyChanged() { revision.intValue++ }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE workshop_skills (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE COLLATE NOCASE)")
        db.execSQL("CREATE TABLE tailors (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, phone TEXT NOT NULL, address TEXT NOT NULL DEFAULT '', skills_json TEXT NOT NULL)")
        db.execSQL("CREATE TABLE garment_models (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE COLLATE NOCASE, measurements_json TEXT NOT NULL)")
        createOrdersTable(db)
        createOrderPaymentsTable(db)
        createBlueprintTables(db)
        seedDefaultWorkflow(db)
    }

    private fun createOrderPaymentsTable(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS order_payments (id INTEGER PRIMARY KEY AUTOINCREMENT, order_id INTEGER NOT NULL, amount REAL NOT NULL CHECK(amount > 0), payment_date TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, note TEXT NOT NULL DEFAULT '', FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_order_payments_date ON order_payments(payment_date)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS garment_models (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE COLLATE NOCASE, measurements_json TEXT NOT NULL)")
        }
        if (oldVersion < 3) createOrdersTable(db)
        if (oldVersion >= 3 && oldVersion < 4) db.execSQL("ALTER TABLE orders ADD COLUMN registered_at TEXT NOT NULL DEFAULT ''")
        if (oldVersion < 5) {
            createOrderPaymentsTable(db)
            // Preserve historical totals when upgrading existing installations.
            db.execSQL("""
                INSERT INTO order_payments (order_id, amount, payment_date, note)
                SELECT id, deposit + paid, COALESCE(NULLIF(registered_at, ''), CURRENT_TIMESTAMP), 'ثبت تاریخی هنگام به‌روزرسانی'
                FROM orders WHERE deposit + paid > 0
            """.trimIndent())
        }
        if (oldVersion < 6) {
            createBlueprintTables(db)
            seedDefaultWorkflow(db)
            migrateLegacyData(db)
        }
    }

    private fun createBlueprintTables(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS customers (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, phone TEXT NOT NULL DEFAULT '', address TEXT NOT NULL DEFAULT '', created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE(name, phone))")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_customers_phone ON customers(phone)")
        db.execSQL("CREATE TABLE IF NOT EXISTS order_types (id INTEGER PRIMARY KEY AUTOINCREMENT, type_name TEXT NOT NULL UNIQUE COLLATE NOCASE)")
        db.execSQL("CREATE TABLE IF NOT EXISTS measurement_fields (id INTEGER PRIMARY KEY AUTOINCREMENT, order_type_id INTEGER NOT NULL, field_name TEXT NOT NULL, field_order INTEGER NOT NULL DEFAULT 0, UNIQUE(order_type_id, field_name), FOREIGN KEY(order_type_id) REFERENCES order_types(id) ON DELETE CASCADE)")
        db.execSQL("CREATE TABLE IF NOT EXISTS customer_measurements (id INTEGER PRIMARY KEY AUTOINCREMENT, customer_id INTEGER NOT NULL, order_type_id INTEGER NOT NULL, field_id INTEGER NOT NULL, value TEXT NOT NULL, recorded_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, order_id INTEGER, FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE CASCADE, FOREIGN KEY(order_type_id) REFERENCES order_types(id), FOREIGN KEY(field_id) REFERENCES measurement_fields(id), FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE SET NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_customer_measurements_history ON customer_measurements(customer_id, recorded_at DESC)")
        db.execSQL("CREATE TABLE IF NOT EXISTS workers (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, phone TEXT NOT NULL DEFAULT '', address TEXT NOT NULL DEFAULT '', pay_type TEXT NOT NULL DEFAULT 'piece_rate', pay_rate REAL NOT NULL DEFAULT 0, active INTEGER NOT NULL DEFAULT 1)")
        db.execSQL("CREATE TABLE IF NOT EXISTS workflow_steps (id INTEGER PRIMARY KEY AUTOINCREMENT, step_name TEXT NOT NULL UNIQUE COLLATE NOCASE, step_order INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("CREATE TABLE IF NOT EXISTS order_step_assignments (id INTEGER PRIMARY KEY AUTOINCREMENT, order_id INTEGER NOT NULL, step_id INTEGER NOT NULL, worker_id INTEGER, step_status TEXT NOT NULL DEFAULT 'pending', completed_at TEXT, payout_amount REAL NOT NULL DEFAULT 0, UNIQUE(order_id, step_id), FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE, FOREIGN KEY(step_id) REFERENCES workflow_steps(id), FOREIGN KEY(worker_id) REFERENCES workers(id) ON DELETE SET NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_order_steps_status ON order_step_assignments(step_status, order_id)")
        db.execSQL("CREATE TABLE IF NOT EXISTS worker_attendance (id INTEGER PRIMARY KEY AUTOINCREMENT, worker_id INTEGER NOT NULL, work_date TEXT NOT NULL, note TEXT NOT NULL DEFAULT '', UNIQUE(worker_id, work_date), FOREIGN KEY(worker_id) REFERENCES workers(id) ON DELETE CASCADE)")
        db.execSQL("CREATE TABLE IF NOT EXISTS worker_payouts (id INTEGER PRIMARY KEY AUTOINCREMENT, worker_id INTEGER NOT NULL, amount REAL NOT NULL CHECK(amount > 0), payout_date TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, period_start TEXT NOT NULL DEFAULT '', period_end TEXT NOT NULL DEFAULT '', note TEXT NOT NULL DEFAULT '', FOREIGN KEY(worker_id) REFERENCES workers(id) ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_worker_payouts_date ON worker_payouts(worker_id, payout_date)")
    }

    private fun seedDefaultWorkflow(db: SQLiteDatabase) {
        if (db.rawQuery("SELECT COUNT(*) FROM workflow_steps", null).use { it.moveToFirst(); it.getInt(0) } == 0) {
            listOf("برش", "دوخت", "اتوکاری", "تکمیل‌کاری").forEachIndexed { index, name ->
                db.insert("workflow_steps", null, ContentValues().apply { put("step_name", name); put("step_order", index + 1) })
            }
        }
    }

    private fun migrateLegacyData(db: SQLiteDatabase) {
        // Import existing customer/order records into the normalized blueprint tables.
        db.rawQuery("SELECT id, customer_name, phone, garment_model, measurements_json, registered_at FROM orders ORDER BY id", null).use { cursor ->
            while (cursor.moveToNext()) {
                val orderId = cursor.getInt(0)
                val name = cursor.getString(1) ?: ""
                val phone = cursor.getString(2) ?: ""
                val model = cursor.getString(3) ?: "لباس"
                val measurements = runCatching { JSONObject(cursor.getString(4) ?: "{}") }.getOrDefault(JSONObject())
                val recordedAt = cursor.getString(5)?.takeIf { it.isNotBlank() } ?: "CURRENT_TIMESTAMP"
                db.insertWithOnConflict("customers", null, ContentValues().apply { put("name", name); put("phone", phone) }, SQLiteDatabase.CONFLICT_IGNORE)
                val customerId = db.rawQuery("SELECT id FROM customers WHERE name = ? AND phone = ?", arrayOf(name, phone)).use { c -> if (c.moveToFirst()) c.getInt(0) else -1 }
                db.insertWithOnConflict("order_types", null, ContentValues().apply { put("type_name", model) }, SQLiteDatabase.CONFLICT_IGNORE)
                val typeId = db.rawQuery("SELECT id FROM order_types WHERE type_name = ?", arrayOf(model)).use { c -> if (c.moveToFirst()) c.getInt(0) else -1 }
                if (customerId > 0 && typeId > 0) {
                    val keys = measurements.keys()
                    while (keys.hasNext()) {
                        val fieldName = keys.next()
                        db.insertWithOnConflict("measurement_fields", null, ContentValues().apply { put("order_type_id", typeId); put("field_name", fieldName); put("field_order", measurements.length()) }, SQLiteDatabase.CONFLICT_IGNORE)
                        val fieldId = db.rawQuery("SELECT id FROM measurement_fields WHERE order_type_id = ? AND field_name = ?", arrayOf(typeId.toString(), fieldName)).use { c -> if (c.moveToFirst()) c.getInt(0) else -1 }
                        if (fieldId > 0) {
                            db.insert("customer_measurements", null, ContentValues().apply {
                                put("customer_id", customerId); put("order_type_id", typeId); put("field_id", fieldId)
                                put("value", measurements.optString(fieldName)); put("recorded_at", recordedAt); put("order_id", orderId)
                            })
                        }
                    }
                }
                val assigned = db.rawQuery("SELECT COUNT(*) FROM order_step_assignments WHERE order_id = ?", arrayOf(orderId.toString())).use { c -> c.moveToFirst(); c.getInt(0) }
                if (assigned == 0) {
                    db.rawQuery("SELECT id FROM workflow_steps ORDER BY step_order", null).use { steps ->
                        while (steps.moveToNext()) db.insert("order_step_assignments", null, ContentValues().apply { put("order_id", orderId); put("step_id", steps.getInt(0)); put("step_status", "pending") })
                    }
                }
            }
        }
        db.rawQuery("SELECT name, phone, address, skills_json FROM tailors ORDER BY id", null).use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(0) ?: continue
                db.insertWithOnConflict("workers", null, ContentValues().apply {
                    put("name", name); put("phone", cursor.getString(1) ?: ""); put("address", cursor.getString(2) ?: "")
                    put("pay_type", "piece_rate"); put("pay_rate", 0.0)
                }, SQLiteDatabase.CONFLICT_IGNORE)
            }
        }
    }

    private fun syncOrderBlueprintData(db: SQLiteDatabase, orderId: Long, order: TailorOrder) {
        db.insertWithOnConflict("customers", null, ContentValues().apply {
            put("name", order.customerName.trim()); put("phone", order.phone.trim()); put("address", order.address.trim())
        }, SQLiteDatabase.CONFLICT_IGNORE)
        if (order.address.isNotBlank()) db.update("customers", ContentValues().apply { put("address", order.address.trim()) }, "name = ? AND phone = ?", arrayOf(order.customerName.trim(), order.phone.trim()))
        val customerId = db.rawQuery("SELECT id FROM customers WHERE name = ? AND phone = ?", arrayOf(order.customerName.trim(), order.phone.trim())).use { c -> if (c.moveToFirst()) c.getInt(0) else -1 }
        db.insertWithOnConflict("order_types", null, ContentValues().apply { put("type_name", order.garmentModel.trim()) }, SQLiteDatabase.CONFLICT_IGNORE)
        val typeId = db.rawQuery("SELECT id FROM order_types WHERE type_name = ?", arrayOf(order.garmentModel.trim())).use { c -> if (c.moveToFirst()) c.getInt(0) else -1 }
        if (customerId > 0 && typeId > 0) {
            order.measurements.forEachIndexed { index, entry ->
                db.insertWithOnConflict("measurement_fields", null, ContentValues().apply { put("order_type_id", typeId); put("field_name", entry.key); put("field_order", index + 1) }, SQLiteDatabase.CONFLICT_IGNORE)
                val fieldId = db.rawQuery("SELECT id FROM measurement_fields WHERE order_type_id = ? AND field_name = ?", arrayOf(typeId.toString(), entry.key)).use { c -> if (c.moveToFirst()) c.getInt(0) else -1 }
                if (fieldId > 0 && entry.value.isNotBlank()) db.insert("customer_measurements", null, ContentValues().apply {
                    put("customer_id", customerId); put("order_type_id", typeId); put("field_id", fieldId); put("value", entry.value); put("order_id", orderId)
                })
            }
        }
        db.rawQuery("SELECT id FROM workflow_steps ORDER BY step_order", null).use { steps ->
            while (steps.moveToNext()) db.insertWithOnConflict("order_step_assignments", null, ContentValues().apply {
                put("order_id", orderId); put("step_id", steps.getInt(0)); put("step_status", "pending")
            }, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    fun getWorkers(includeInactive: Boolean = false): List<WorkerRecord> {
        val rows = mutableListOf<WorkerRecord>()
        val sql = if (includeInactive) "SELECT id,name,phone,address,pay_type,pay_rate,active FROM workers ORDER BY active DESC,name" else "SELECT id,name,phone,address,pay_type,pay_rate,active FROM workers WHERE active=1 ORDER BY name"
        readableDatabase.rawQuery(sql, null).use { c ->
            while (c.moveToNext()) rows.add(WorkerRecord(c.getInt(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getDouble(5), c.getInt(6) == 1))
        }
        return rows
    }

    fun saveWorker(worker: WorkerRecord) {
        val values = ContentValues().apply {
            put("name", worker.name.trim()); put("phone", worker.phone.trim()); put("address", worker.address.trim())
            put("pay_type", worker.payType); put("pay_rate", worker.payRate); put("active", if (worker.active) 1 else 0)
        }
        if (worker.id > 0) writableDatabase.update("workers", values, "id = ?", arrayOf(worker.id.toString()))
        else writableDatabase.insertOrThrow("workers", null, values)
        notifyChanged()
    }

    fun getWorkflowSteps(): List<WorkflowStepRecord> {
        val rows = mutableListOf<WorkflowStepRecord>()
        readableDatabase.rawQuery("SELECT id,step_name,step_order FROM workflow_steps ORDER BY step_order,id", null).use { c ->
            while (c.moveToNext()) rows.add(WorkflowStepRecord(c.getInt(0), c.getString(1), c.getInt(2)))
        }
        return rows
    }

    fun saveWorkflowStep(name: String, order: Int) {
        val clean = name.trim()
        if (clean.isBlank()) return
        val db = writableDatabase
        val existingId = db.rawQuery("SELECT id FROM workflow_steps WHERE step_name = ?", arrayOf(clean)).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
        if (existingId > 0) db.update("workflow_steps", ContentValues().apply { put("step_order", order) }, "id = ?", arrayOf(existingId.toString()))
        else db.insertOrThrow("workflow_steps", null, ContentValues().apply { put("step_name", clean); put("step_order", order) })
        notifyChanged()
    }

    fun getOrderWorkflow(orderId: Int): List<OrderWorkflowRecord> {
        val rows = mutableListOf<OrderWorkflowRecord>()
        readableDatabase.rawQuery("""
            SELECT a.id,a.order_id,a.step_id,s.step_name,a.worker_id,COALESCE(w.name,''),a.step_status,COALESCE(a.completed_at,'')
            FROM order_step_assignments a JOIN workflow_steps s ON s.id=a.step_id
            LEFT JOIN workers w ON w.id=a.worker_id WHERE a.order_id=? ORDER BY s.step_order,a.id
        """.trimIndent(), arrayOf(orderId.toString())).use { c ->
            while (c.moveToNext()) rows.add(OrderWorkflowRecord(c.getInt(0),c.getInt(1),c.getInt(2),c.getString(3),if(c.isNull(4)) null else c.getInt(4),c.getString(5),c.getString(6),c.getString(7)))
        }
        return rows
    }

    fun assignWorkflowStep(assignmentId: Int, workerId: Int?) {
        writableDatabase.update("order_step_assignments", ContentValues().apply { if (workerId == null) putNull("worker_id") else put("worker_id", workerId) }, "id=?", arrayOf(assignmentId.toString()))
        notifyChanged()
    }

    fun updateWorkflowStepStatus(assignmentId: Int, status: String) {
        val valid = status in listOf("pending", "in_progress", "done")
        if (!valid) return
        writableDatabase.update("order_step_assignments", ContentValues().apply {
            put("step_status", status)
            if (status == "done") put("completed_at", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date()))
            else putNull("completed_at")
        }, "id=?", arrayOf(assignmentId.toString()))
        notifyChanged()
    }

    fun getCustomerMeasurementHistory(customerName: String, phone: String): List<CustomerMeasurementRecord> {
        val rows = mutableListOf<CustomerMeasurementRecord>()
        readableDatabase.rawQuery("""
            SELECT t.type_name,f.field_name,m.value,m.recorded_at
            FROM customer_measurements m JOIN customers c ON c.id=m.customer_id
            JOIN order_types t ON t.id=m.order_type_id JOIN measurement_fields f ON f.id=m.field_id
            WHERE c.name=? AND c.phone=? ORDER BY m.recorded_at DESC,m.id DESC
        """.trimIndent(), arrayOf(customerName, phone)).use { c ->
            while (c.moveToNext()) rows.add(CustomerMeasurementRecord(c.getString(0),c.getString(1),c.getString(2),c.getString(3)))
        }
        return rows
    }

    fun recordWorkerAttendance(workerId: Int, workDate: String, note: String = "") {
        writableDatabase.insertWithOnConflict("worker_attendance", null, ContentValues().apply { put("worker_id", workerId); put("work_date", workDate); put("note", note) }, SQLiteDatabase.CONFLICT_IGNORE)
        notifyChanged()
    }

    fun getWorkerPayroll(startDate: String, endDate: String): List<WorkerPayrollRecord> {
        val rows = mutableListOf<WorkerPayrollRecord>()
        val workers = getWorkers()
        val start = startDate.ifBlank { java.text.SimpleDateFormat("yyyy-MM-01", java.util.Locale.US).format(java.util.Date()) }
        val end = endDate.ifBlank { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) }
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val startMillis = runCatching { dateFormat.parse(start)?.time }.getOrNull() ?: System.currentTimeMillis()
        val endMillis = runCatching { dateFormat.parse(end)?.time }.getOrNull() ?: startMillis
        val days = (java.util.concurrent.TimeUnit.MILLISECONDS.toDays(kotlin.math.abs(endMillis - startMillis)).toInt() + 1).coerceAtLeast(1)
        val startCalendar = java.util.Calendar.getInstance().apply { timeInMillis = startMillis }
        val endCalendar = java.util.Calendar.getInstance().apply { timeInMillis = endMillis }
        val months = ((endCalendar.get(java.util.Calendar.YEAR) - startCalendar.get(java.util.Calendar.YEAR)) * 12 +
            endCalendar.get(java.util.Calendar.MONTH) - startCalendar.get(java.util.Calendar.MONTH) + 1).coerceAtLeast(1)
        workers.forEach { worker ->
            val units: Int
            val earned: Double
            when (worker.payType) {
                "daily" -> {
                    units = readableDatabase.rawQuery("SELECT COUNT(*) FROM worker_attendance WHERE worker_id=? AND work_date BETWEEN ? AND ?", arrayOf(worker.id.toString(), start, end)).use { it.moveToFirst(); it.getInt(0) }
                    earned = units * worker.payRate
                }
                "weekly" -> { units = ((days + 6) / 7); earned = units * worker.payRate }
                "monthly" -> { units = months; earned = units * worker.payRate }
                else -> {
                    units = readableDatabase.rawQuery("SELECT COUNT(*) FROM order_step_assignments WHERE worker_id=? AND step_status='done' AND substr(COALESCE(completed_at,''),1,10) BETWEEN ? AND ?", arrayOf(worker.id.toString(), start, end)).use { it.moveToFirst(); it.getInt(0) }
                    earned = units * worker.payRate
                }
            }
            val paid = readableDatabase.rawQuery("SELECT COALESCE(SUM(amount),0) FROM worker_payouts WHERE worker_id=? AND COALESCE(NULLIF(period_start,''),substr(payout_date,1,10)) <= ? AND COALESCE(NULLIF(period_end,''),substr(payout_date,1,10)) >= ?", arrayOf(worker.id.toString(), end, start)).use { it.moveToFirst(); it.getDouble(0) }
            rows.add(WorkerPayrollRecord(worker.id, worker.name, worker.payType, worker.payRate, units, earned, paid))
        }
        return rows
    }

    fun markWorkerPaid(workerId: Int, amount: Double, startDate: String, endDate: String, note: String = ""): Boolean {
        if (!amount.isFinite() || amount <= 0) return false
        writableDatabase.insertOrThrow("worker_payouts", null, ContentValues().apply {
            put("worker_id", workerId); put("amount", amount); put("period_start", startDate); put("period_end", endDate); put("note", note)
        })
        notifyChanged()
        return true
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
        notifyChanged()
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
                    val fields = model.measurements.map { it.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }
                    db.insertOrThrow("garment_models", null, ContentValues().apply {
                        put("name", name)
                        put("measurements_json", JSONArray(fields).toString())
                    })
                    db.insertWithOnConflict("order_types", null, ContentValues().apply { put("type_name", name) }, SQLiteDatabase.CONFLICT_IGNORE)
                    val typeId = db.rawQuery("SELECT id FROM order_types WHERE type_name = ?", arrayOf(name)).use { c -> if (c.moveToFirst()) c.getInt(0) else -1 }
                    if (typeId > 0) {
                        db.delete("measurement_fields", "order_type_id = ?", arrayOf(typeId.toString()))
                        fields.forEachIndexed { index, field ->
                            db.insert("measurement_fields", null, ContentValues().apply {
                                put("order_type_id", typeId); put("field_name", field); put("field_order", index + 1)
                            })
                        }
                    }
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        notifyChanged()
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
        notifyChanged()
    }


    private fun createOrdersTable(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS orders (id INTEGER PRIMARY KEY AUTOINCREMENT, customer_name TEXT NOT NULL, phone TEXT NOT NULL, garment_model TEXT NOT NULL, measurements_json TEXT NOT NULL, notes TEXT NOT NULL DEFAULT '', delivery_date TEXT NOT NULL, registered_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, price REAL NOT NULL, deposit REAL NOT NULL, paid REAL NOT NULL, remaining REAL NOT NULL, status TEXT NOT NULL DEFAULT 'incomplete')")
    }

    fun addOrder(order: TailorOrder) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val orderId = db.insertOrThrow("orders", null, ContentValues().apply {
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
            val initialPayment = order.deposit + order.paid
            if (initialPayment > 0) {
                db.insertOrThrow("order_payments", null, ContentValues().apply {
                    put("order_id", orderId)
                    put("amount", initialPayment)
                    put("note", "پیش‌پرداخت و پرداخت هنگام ثبت سفارش")
                })
            }
            syncOrderBlueprintData(db, orderId, order)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        notifyChanged()
    }

    /**
     * Records a customer payment against their oldest outstanding orders first.
     * The payment and order balances are updated atomically.
     */
    fun recordCustomerPayment(customerName: String, phone: String, amount: Double): Boolean {
        if (!amount.isFinite() || amount <= 0) return false
        val db = writableDatabase
        db.beginTransaction()
        try {
            val outstanding = mutableListOf<Triple<Int, Double, Double>>()
            db.rawQuery(
                "SELECT id, remaining, paid FROM orders WHERE customer_name = ? AND phone = ? AND remaining > 0 ORDER BY id ASC",
                arrayOf(customerName, phone)
            ).use { cursor ->
                while (cursor.moveToNext()) outstanding.add(Triple(cursor.getInt(0), cursor.getDouble(1), cursor.getDouble(2)))
            }
            val totalDue = outstanding.sumOf { it.second }
            if (totalDue <= 0 || amount > totalDue + 0.0001) return false
            var left = amount
            outstanding.forEach { (orderId, due, paid) ->
                if (left > 0.0001) {
                    val applied = minOf(left, due)
                    val remaining = (due - applied).coerceAtLeast(0.0)
                    db.update("orders", ContentValues().apply {
                        put("paid", paid + applied)
                        put("remaining", remaining)
                    }, "id = ?", arrayOf(orderId.toString()))
                    db.insertOrThrow("order_payments", null, ContentValues().apply {
                        put("order_id", orderId)
                        put("amount", applied)
                        put("note", "پرداخت مشتری")
                    })
                    left -= applied
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        notifyChanged()
        return true
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
        if (added > 0) notifyChanged()
        if (added > 0) notifyChanged()
        return added
    }

    fun getCustomers(search: String = ""): List<CustomerSummary> {
        val result = mutableListOf<CustomerSummary>()
        val term = search.trim()
        val sql = """
            SELECT c.name,c.phone,c.address,
                   COALESCE(SUM(CASE WHEN o.status NOT IN ('ready','delivered') THEN 1 ELSE 0 END),0) AS active_order_count,
                   COUNT(o.id) AS order_count,
                   COALESCE(SUM(o.remaining),0) AS total_remaining,
                   COALESCE(MAX(o.delivery_date),'') AS last_delivery_date
            FROM customers c
            LEFT JOIN orders o ON o.customer_name=c.name AND o.phone=c.phone
            WHERE (? = '' OR c.name LIKE ? OR c.phone LIKE ?)
            GROUP BY c.id,c.name,c.phone,c.address
            ORDER BY c.id DESC
        """.trimIndent()
        readableDatabase.rawQuery(sql, arrayOf(term, "%$term%", "%$term%")).use { cursor ->
            while (cursor.moveToNext()) result.add(CustomerSummary(
                name = cursor.getString(0) ?: "",
                phone = cursor.getString(1) ?: "",
                address = cursor.getString(2) ?: "",
                activeOrderCount = cursor.getInt(3),
                orderCount = cursor.getInt(4),
                remaining = cursor.getDouble(5),
                lastDeliveryDate = cursor.getString(6) ?: ""
            ))
        }
        return result
    }

    fun saveCustomer(name: String, phone: String, address: String = ""): Boolean {
        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        if (cleanName.isBlank()) return false
        val db = writableDatabase
        db.insertWithOnConflict("customers", null, ContentValues().apply {
            put("name", cleanName); put("phone", cleanPhone); put("address", address.trim())
        }, SQLiteDatabase.CONFLICT_IGNORE)
        db.update("customers", ContentValues().apply { put("address", address.trim()) }, "name=? AND phone=?", arrayOf(cleanName, cleanPhone))
        notifyChanged()
        return true
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

    fun updateCustomerDetails(oldName: String, oldPhone: String, newName: String, newPhone: String, newAddress: String = "") {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.update("orders", ContentValues().apply { put("customer_name", newName.trim()); put("phone", newPhone.trim()) }, "customer_name = ? AND phone = ?", arrayOf(oldName, oldPhone))
            db.update("customers", ContentValues().apply { put("name", newName.trim()); put("phone", newPhone.trim()); put("address", newAddress.trim()) }, "name = ? AND phone = ?", arrayOf(oldName, oldPhone))
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        notifyChanged()
    }

    fun deleteCustomerOrders(name: String, phone: String) {
        writableDatabase.delete("orders", "customer_name = ? AND phone = ?", arrayOf(name, phone))
        notifyChanged()
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
            if (paymentAmount > 0) {
                db.insertOrThrow("order_payments", null, ContentValues().apply {
                    put("order_id", orderId)
                    put("amount", paymentAmount)
                    put("note", "پرداخت هنگام تحویل سفارش")
                })
            }
            db.setTransactionSuccessful()
            notifyChanged()
            return true
        } finally {
            db.endTransaction()
        }
    }

    fun updateOrderStatus(orderId: Int, status: String) {
        writableDatabase.update("orders", ContentValues().apply { put("status", status) }, "id = ?", arrayOf(orderId.toString()))
        notifyChanged()
    }

    fun updateOrderMeasurements(orderId: Int, measurements: Map<String, String>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.update("orders", ContentValues().apply { put("measurements_json", JSONObject(measurements).toString()) }, "id = ?", arrayOf(orderId.toString()))
            db.rawQuery("SELECT customer_name,phone,garment_model,notes,delivery_date,price,deposit,paid FROM orders WHERE id=?", arrayOf(orderId.toString())).use { c ->
                if (c.moveToFirst()) {
                    val order = TailorOrder(c.getString(0), c.getString(1), c.getString(2), measurements, c.getString(3), c.getString(4), c.getDouble(5), c.getDouble(6), c.getDouble(7))
                    syncOrderBlueprintData(db, orderId.toLong(), order)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        notifyChanged()
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
        notifyChanged()
    }


    fun getFinancialReport(): Pair<Double, Double> {
        readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(price), 0), COALESCE(SUM(remaining), 0) FROM orders",
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) return cursor.getDouble(0) to cursor.getDouble(1)
        }
        return 0.0 to 0.0
    }


    fun getIncomeForPeriod(period: String, startDate: String = "", endDate: String = ""): Double {
        val where = when (period) {
            "week" -> "date(payment_date) >= date('now', '-6 days') AND date(payment_date) <= date('now')"
            "month" -> "strftime('%Y-%m', payment_date) = strftime('%Y-%m', 'now')"
            "year" -> "strftime('%Y', payment_date) = strftime('%Y', 'now')"
            "custom" -> "date(payment_date) >= date(?) AND date(payment_date) <= date(?)"
            else -> "date(payment_date) = date('now')"
        }
        val args = if (period == "custom") arrayOf(startDate, endDate) else null
        readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(amount), 0) FROM order_payments WHERE $where",
            args
        ).use { cursor -> return if (cursor.moveToFirst()) cursor.getDouble(0) else 0.0 }
    }

    fun getDebtCustomers(): List<Pair<CustomerSummary, Double>> {
        val result = mutableListOf<Pair<CustomerSummary, Double>>()
        readableDatabase.rawQuery(
            """
            SELECT customer_name, phone,
                   SUM(CASE WHEN status NOT IN ('ready', 'delivered') THEN 1 ELSE 0 END),
                   COUNT(*), SUM(remaining),
                   (SELECT o2.delivery_date FROM orders o2
                    WHERE o2.customer_name = orders.customer_name AND o2.phone = orders.phone
                    ORDER BY o2.id DESC LIMIT 1)
            FROM orders
            GROUP BY customer_name, phone
            HAVING SUM(remaining) > 0
            ORDER BY SUM(remaining) DESC
            """.trimIndent(),
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val customer = CustomerSummary(
                    name = cursor.getString(0),
                    phone = cursor.getString(1),
                    activeOrderCount = cursor.getInt(2),
                    orderCount = cursor.getInt(3),
                    remaining = cursor.getDouble(4),
                    lastDeliveryDate = cursor.getString(5) ?: ""
                )
                result.add(customer to customer.remaining)
            }
        }
        return result
    }

}
