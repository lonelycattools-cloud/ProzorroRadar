package ua.gov.prozorro.radar.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import ua.gov.prozorro.radar.model.DashboardMetrics
import ua.gov.prozorro.radar.model.ReportRow
import ua.gov.prozorro.radar.model.ScanLog
import ua.gov.prozorro.radar.model.Tender

class ProzorroDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_TENDERS (
                $COL_ID TEXT PRIMARY KEY,
                $COL_TENDER_ID TEXT,
                $COL_TITLE TEXT,
                $COL_DESCRIPTION TEXT,
                $COL_STATUS TEXT,
                $COL_DATE_MODIFIED TEXT,
                $COL_ENTITY_NAME TEXT,
                $COL_ENTITY_EDRPOU TEXT,
                $COL_VALUE_AMOUNT REAL,
                $COL_VALUE_CURRENCY TEXT,
                $COL_CPV_CODES TEXT,
                $COL_DEADLINE TEXT,
                $COL_TENDER_URL TEXT,
                $COL_EMAIL_STATUS TEXT,
                $COL_IS_FAVORITE INTEGER DEFAULT 0,
                $COL_FOUND_AT INTEGER
            )
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX idx_tenders_status ON $TABLE_TENDERS ($COL_STATUS)")
        db.execSQL("CREATE INDEX idx_tenders_edrpou ON $TABLE_TENDERS ($COL_ENTITY_EDRPOU)")
        db.execSQL("CREATE INDEX idx_tenders_found_at ON $TABLE_TENDERS ($COL_FOUND_AT)")

        db.execSQL(
            """
            CREATE TABLE $TABLE_LOGS (
                $COL_LOG_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_LOG_TIMESTAMP INTEGER,
                $COL_LOG_EVENT TEXT,
                $COL_LOG_COUNT INTEGER,
                $COL_LOG_DETAILS TEXT
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Simple migration strategy for local dev
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TENDERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOGS")
        onCreate(db)
    }

    // --- Tenders Operations ---

    fun clearAllTenders() {
        val db = writableDatabase
        db.delete(TABLE_TENDERS, null, null)
    }

    fun insertOrUpdateTender(tender: Tender): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_ID, tender.id)
            put(COL_TENDER_ID, tender.tenderId)
            put(COL_TITLE, tender.title)
            put(COL_DESCRIPTION, tender.description)
            put(COL_STATUS, tender.status)
            put(COL_DATE_MODIFIED, tender.dateModified)
            put(COL_ENTITY_NAME, tender.procuringEntityName)
            put(COL_ENTITY_EDRPOU, tender.procuringEntityEdrpou)
            put(COL_VALUE_AMOUNT, tender.valueAmount)
            put(COL_VALUE_CURRENCY, tender.valueCurrency)
            put(COL_CPV_CODES, tender.cpvCodes)
            put(COL_DEADLINE, tender.deadline)
            put(COL_TENDER_URL, tender.tenderUrl)
            put(COL_EMAIL_STATUS, tender.emailStatus)
            put(COL_IS_FAVORITE, if (tender.isFavorite) 1 else 0)
            put(COL_FOUND_AT, tender.foundAtTimestamp)
        }
        val result = db.insertWithOnConflict(TABLE_TENDERS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        return result != -1L
    }

    fun getAllTenders(
        query: String = "",
        statusFilter: String = "",
        favoritesOnly: Boolean = false,
        cpvPrefixes: List<String> = emptyList(),
        limit: Int = 1000
    ): List<Tender> {
        val db = readableDatabase
        val whereClauses = mutableListOf<String>()
        val args = mutableListOf<String>()

        if (query.isNotBlank()) {
            whereClauses.add("($COL_TITLE LIKE ? OR $COL_ENTITY_NAME LIKE ? OR $COL_ENTITY_EDRPOU LIKE ? OR $COL_CPV_CODES LIKE ?)")
            val param = "%${query.trim()}%"
            args.add(param)
            args.add(param)
            args.add(param)
            args.add(param)
        }

        if (statusFilter.isNotBlank() && statusFilter != "all") {
            whereClauses.add("$COL_STATUS = ?")
            args.add(statusFilter)
        }

        if (favoritesOnly) {
            whereClauses.add("$COL_IS_FAVORITE = 1")
        }

        if (cpvPrefixes.isNotEmpty()) {
            val cpvClauses = cpvPrefixes.map { "$COL_CPV_CODES LIKE ?" }
            whereClauses.add("(${cpvClauses.joinToString(" OR ")})")
            cpvPrefixes.forEach { args.add("%$it%") }
        }

        val whereStr = if (whereClauses.isNotEmpty()) whereClauses.joinToString(" AND ") else null
        val cursor = db.query(
            TABLE_TENDERS,
            null,
            whereStr,
            if (args.isNotEmpty()) args.toTypedArray() else null,
            null,
            null,
            "$COL_FOUND_AT DESC",
            limit.toString()
        )

        val list = mutableListOf<Tender>()
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(cursorToTender(c))
            }
        }
        return list
    }

    fun getExistingIds(cpvPrefixes: List<String> = emptyList()): Set<String> {
        val db = readableDatabase
        val set = mutableSetOf<String>()
        val whereClause = if (cpvPrefixes.isNotEmpty()) {
            val clauses = cpvPrefixes.map { "$COL_CPV_CODES LIKE ?" }
            clauses.joinToString(" OR ")
        } else null
        val args = if (cpvPrefixes.isNotEmpty()) {
            cpvPrefixes.map { "%$it%" }.toTypedArray()
        } else null

        val cursor = db.query(TABLE_TENDERS, arrayOf(COL_ID), whereClause, args, null, null, null)
        cursor.use { c ->
            while (c.moveToNext()) {
                set.add(c.getString(0))
            }
        }
        return set
    }

    fun toggleFavorite(id: String, current: Boolean) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_IS_FAVORITE, if (!current) 1 else 0)
        }
        db.update(TABLE_TENDERS, cv, "$COL_ID = ?", arrayOf(id))
    }

    fun updateEmailStatus(id: String, newStatus: String) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_EMAIL_STATUS, newStatus)
        }
        db.update(TABLE_TENDERS, cv, "$COL_ID = ?", arrayOf(id))
    }

    fun getMetrics(cpvPrefixes: List<String> = emptyList()): DashboardMetrics {
        val db = readableDatabase
        var total = 0
        var totalSum = 0.0
        var active = 0
        var newToday = 0

        val now = System.currentTimeMillis()
        val oneDayAgo = now - 24 * 60 * 60 * 1000L

        val cpvFilterSql = if (cpvPrefixes.isNotEmpty()) {
            val clauses = cpvPrefixes.joinToString(" OR ") { "$COL_CPV_CODES LIKE ?" }
            "AND ($clauses)"
        } else ""

        val rawArgs = mutableListOf<String>()
        rawArgs.add(oneDayAgo.toString())
        if (cpvPrefixes.isNotEmpty()) {
            cpvPrefixes.forEach { rawArgs.add("%$it%") }
        }

        val baseQuery = if (cpvPrefixes.isNotEmpty()) {
            val clauses = cpvPrefixes.joinToString(" OR ") { "$COL_CPV_CODES LIKE ?" }
            "WHERE ($clauses)"
        } else ""

        val cursor = if (cpvPrefixes.isEmpty()) {
            db.rawQuery(
                """
                SELECT 
                    COUNT(*), 
                    COALESCE(SUM($COL_VALUE_AMOUNT), 0),
                    COUNT(CASE WHEN $COL_STATUS = 'active.tendering' THEN 1 END),
                    COUNT(CASE WHEN $COL_FOUND_AT >= ? THEN 1 END)
                FROM $TABLE_TENDERS
                """.trimIndent(),
                arrayOf(oneDayAgo.toString())
            )
        } else {
            val argsList = mutableListOf<String>()
            cpvPrefixes.forEach { argsList.add("%$it%") }
            argsList.add(oneDayAgo.toString())
            cpvPrefixes.forEach { argsList.add("%$it%") }

            val cpvOr = cpvPrefixes.joinToString(" OR ") { "$COL_CPV_CODES LIKE ?" }
            db.rawQuery(
                """
                SELECT 
                    COUNT(*), 
                    COALESCE(SUM($COL_VALUE_AMOUNT), 0),
                    COUNT(CASE WHEN $COL_STATUS = 'active.tendering' THEN 1 END),
                    COUNT(CASE WHEN $COL_FOUND_AT >= ? AND ($cpvOr) THEN 1 END)
                FROM $TABLE_TENDERS
                WHERE ($cpvOr)
                """.trimIndent(),
                argsList.toTypedArray()
            )
        }

        cursor.use { c ->
            if (c.moveToFirst()) {
                total = c.getInt(0)
                totalSum = c.getDouble(1)
                active = c.getInt(2)
                newToday = c.getInt(3)
            }
        }

        // Urgent deadlines count
        val urgentCursor = if (cpvPrefixes.isEmpty()) {
            db.rawQuery(
                """
                SELECT COUNT(*) FROM $TABLE_TENDERS 
                WHERE $COL_DEADLINE != '-' AND $COL_STATUS = 'active.tendering'
                """.trimIndent(),
                null
            )
        } else {
            val cpvOr = cpvPrefixes.joinToString(" OR ") { "$COL_CPV_CODES LIKE ?" }
            val args = cpvPrefixes.map { "%$it%" }.toTypedArray()
            db.rawQuery(
                """
                SELECT COUNT(*) FROM $TABLE_TENDERS 
                WHERE $COL_DEADLINE != '-' AND $COL_STATUS = 'active.tendering' AND ($cpvOr)
                """.trimIndent(),
                args
            )
        }

        var urgentCount = 0
        urgentCursor.use { c ->
            if (c.moveToFirst()) {
                urgentCount = c.getInt(0)
            }
        }

        return DashboardMetrics(
            totalTenders = total,
            newToday = newToday,
            totalBudget = totalSum,
            activeTendersCount = active,
            urgentDeadlinesCount = urgentCount
        )
    }

    // --- Analytics / Reports ---

    fun getTopProcuringEntities(limit: Int = 10, cpvPrefixes: List<String> = emptyList()): List<ReportRow> {
        val db = readableDatabase
        val list = mutableListOf<ReportRow>()
        val whereClause = if (cpvPrefixes.isNotEmpty()) {
            val or = cpvPrefixes.joinToString(" OR ") { "$COL_CPV_CODES LIKE ?" }
            "WHERE ($or)"
        } else ""
        val args = mutableListOf<String>()
        if (cpvPrefixes.isNotEmpty()) {
            cpvPrefixes.forEach { args.add("%$it%") }
        }
        args.add(limit.toString())

        val cursor = db.rawQuery(
            """
            SELECT $COL_ENTITY_NAME, $COL_ENTITY_EDRPOU, COUNT(*) as cnt, SUM($COL_VALUE_AMOUNT) as sm
            FROM $TABLE_TENDERS
            $whereClause
            GROUP BY $COL_ENTITY_NAME
            ORDER BY sm DESC
            LIMIT ?
            """.trimIndent(),
            args.toTypedArray()
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(0) ?: "Невідомий замовник"
                val edrpou = c.getString(1) ?: ""
                val count = c.getInt(2)
                val sum = c.getDouble(3)
                list.add(
                    ReportRow(
                        title = name,
                        subtitle = if (edrpou.isNotEmpty()) "ЄДРПОУ: $edrpou" else "Без коду",
                        count = count,
                        totalAmount = sum,
                        extra = "$count тенд."
                    )
                )
            }
        }
        return list
    }

    fun getTopCpvCategories(limit: Int = 10, cpvPrefixes: List<String> = emptyList()): List<ReportRow> {
        val db = readableDatabase
        val list = mutableListOf<ReportRow>()
        val whereClause = if (cpvPrefixes.isNotEmpty()) {
            val or = cpvPrefixes.joinToString(" OR ") { "$COL_CPV_CODES LIKE ?" }
            "WHERE $COL_CPV_CODES != '' AND ($or)"
        } else "WHERE $COL_CPV_CODES != ''"
        val args = mutableListOf<String>()
        if (cpvPrefixes.isNotEmpty()) {
            cpvPrefixes.forEach { args.add("%$it%") }
        }
        args.add(limit.toString())

        val cursor = db.rawQuery(
            """
            SELECT $COL_CPV_CODES, COUNT(*) as cnt, SUM($COL_VALUE_AMOUNT) as sm
            FROM $TABLE_TENDERS
            $whereClause
            GROUP BY $COL_CPV_CODES
            ORDER BY cnt DESC
            LIMIT ?
            """.trimIndent(),
            args.toTypedArray()
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                val cpv = c.getString(0) ?: "-"
                val count = c.getInt(1)
                val sum = c.getDouble(2)
                list.add(
                    ReportRow(
                        title = cpv,
                        subtitle = "Категорія ДК 021:2015",
                        count = count,
                        totalAmount = sum,
                        extra = String.format("%,.0f грн", sum)
                    )
                )
            }
        }
        return list
    }

    fun getTendersByEdrpou(edrpou: String): List<Tender> {
        val db = readableDatabase
        val list = mutableListOf<Tender>()
        val cursor = db.query(
            TABLE_TENDERS,
            null,
            "$COL_ENTITY_EDRPOU = ? OR $COL_ENTITY_EDRPOU LIKE ?",
            arrayOf(edrpou.trim(), "%${edrpou.trim()}%"),
            null,
            null,
            "$COL_FOUND_AT DESC"
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(cursorToTender(c))
            }
        }
        return list
    }

    fun getTendersByCpvAndPeriod(cpvPrefix: String, fromTimestamp: Long, toTimestamp: Long): List<Tender> {
        val db = readableDatabase
        val list = mutableListOf<Tender>()
        val cursor = db.query(
            TABLE_TENDERS,
            null,
            "$COL_CPV_CODES LIKE ? AND $COL_FOUND_AT >= ? AND $COL_FOUND_AT <= ?",
            arrayOf("%${cpvPrefix.trim()}%", fromTimestamp.toString(), toTimestamp.toString()),
            null,
            null,
            "$COL_FOUND_AT DESC"
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(cursorToTender(c))
            }
        }
        return list
    }

    // --- Logging ---

    fun insertLog(event: String, count: Int = 0, details: String = "") {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_LOG_TIMESTAMP, System.currentTimeMillis())
            put(COL_LOG_EVENT, event)
            put(COL_LOG_COUNT, count)
            put(COL_LOG_DETAILS, details)
        }
        db.insert(TABLE_LOGS, null, cv)
    }

    fun getLogs(limit: Int = 100): List<ScanLog> {
        val db = readableDatabase
        val list = mutableListOf<ScanLog>()
        val cursor = db.query(
            TABLE_LOGS,
            null,
            null,
            null,
            null,
            null,
            "$COL_LOG_TIMESTAMP DESC",
            limit.toString()
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    ScanLog(
                        id = c.getLong(c.getColumnIndexOrThrow(COL_LOG_ID)),
                        timestamp = c.getLong(c.getColumnIndexOrThrow(COL_LOG_TIMESTAMP)),
                        event = c.getString(c.getColumnIndexOrThrow(COL_LOG_EVENT)),
                        count = c.getInt(c.getColumnIndexOrThrow(COL_LOG_COUNT)),
                        details = c.getString(c.getColumnIndexOrThrow(COL_LOG_DETAILS)) ?: ""
                    )
                )
            }
        }
        return list
    }

    private fun cursorToTender(c: Cursor): Tender {
        return Tender(
            id = c.getString(c.getColumnIndexOrThrow(COL_ID)),
            tenderId = c.getString(c.getColumnIndexOrThrow(COL_TENDER_ID)),
            title = c.getString(c.getColumnIndexOrThrow(COL_TITLE)),
            description = c.getString(c.getColumnIndexOrThrow(COL_DESCRIPTION)) ?: "",
            status = c.getString(c.getColumnIndexOrThrow(COL_STATUS)),
            dateModified = c.getString(c.getColumnIndexOrThrow(COL_DATE_MODIFIED)),
            procuringEntityName = c.getString(c.getColumnIndexOrThrow(COL_ENTITY_NAME)),
            procuringEntityEdrpou = c.getString(c.getColumnIndexOrThrow(COL_ENTITY_EDRPOU)),
            valueAmount = c.getDouble(c.getColumnIndexOrThrow(COL_VALUE_AMOUNT)),
            valueCurrency = c.getString(c.getColumnIndexOrThrow(COL_VALUE_CURRENCY)),
            cpvCodes = c.getString(c.getColumnIndexOrThrow(COL_CPV_CODES)),
            deadline = c.getString(c.getColumnIndexOrThrow(COL_DEADLINE)),
            tenderUrl = c.getString(c.getColumnIndexOrThrow(COL_TENDER_URL)),
            emailStatus = c.getString(c.getColumnIndexOrThrow(COL_EMAIL_STATUS)),
            isFavorite = c.getInt(c.getColumnIndexOrThrow(COL_IS_FAVORITE)) == 1,
            foundAtTimestamp = c.getLong(c.getColumnIndexOrThrow(COL_FOUND_AT))
        )
    }

    companion object {
        const val DATABASE_NAME = "prozorro_radar.db"
        const val DATABASE_VERSION = 1

        const val TABLE_TENDERS = "tenders"
        const val COL_ID = "id"
        const val COL_TENDER_ID = "tender_id"
        const val COL_TITLE = "title"
        const val COL_DESCRIPTION = "description"
        const val COL_STATUS = "status"
        const val COL_DATE_MODIFIED = "date_modified"
        const val COL_ENTITY_NAME = "procuring_entity_name"
        const val COL_ENTITY_EDRPOU = "procuring_entity_edrpou"
        const val COL_VALUE_AMOUNT = "value_amount"
        const val COL_VALUE_CURRENCY = "value_currency"
        const val COL_CPV_CODES = "cpv_codes"
        const val COL_DEADLINE = "deadline"
        const val COL_TENDER_URL = "tender_url"
        const val COL_EMAIL_STATUS = "email_status"
        const val COL_IS_FAVORITE = "is_favorite"
        const val COL_FOUND_AT = "found_at"

        const val TABLE_LOGS = "scan_logs"
        const val COL_LOG_ID = "id"
        const val COL_LOG_TIMESTAMP = "timestamp"
        const val COL_LOG_EVENT = "event"
        const val COL_LOG_COUNT = "count"
        const val COL_LOG_DETAILS = "details"
    }
}
