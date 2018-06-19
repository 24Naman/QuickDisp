package com.naman.quickdisp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class QuickSQLData(
    val startColor: String,
    val endColor: String,
    val autoCloseDialog: Boolean,
    val showUserNameOnDialog: Boolean,
    val showDeviceModelNumberOnDialog: Boolean
)

class QuickSQL(context: Context) : SQLiteOpenHelper(context, "app_settings.db", null, 1) {

    private var tableName = "quicksql"

    // column name
    private val startColor = "startColor"
    private val endColor = "endColor"
    private val firstRun = "firstRun"
    private val autoCloseDialog = "autoCloseDialog"
    private val showUserNameOnDialog = "showUserNameOnDialog"
    private val showDeviceModelNumberOnDialog = "showDeviceModelNumberOnDialog"

    override fun onCreate(p0: SQLiteDatabase?) {

        val createTableStatement = """
            create table $tableName (
                $startColor  string,
                $endColor    string,
                $firstRun   int
                $autoCloseDialog int,
                $showUserNameOnDialog    int,
                $showDeviceModelNumberOnDialog   int
            )
        """.trimIndent()

        p0?.execSQL(createTableStatement)

        p0?.insert(
            tableName,
            null,
            ContentValues().let {
                it.put(startColor, "FFFFFF")
                it.put(endColor, "FFFFFF")
                it.put(firstRun, 1)
                it.put(autoCloseDialog, 0)
                it.put(showUserNameOnDialog, 0)
                it.put(showDeviceModelNumberOnDialog, 0)
                it
            }
        )
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
    }

    fun getData(): QuickSQLData {
        val cursor = this.readableDatabase.rawQuery(
            "select * from ${this@QuickSQL.tableName};",
            arrayOf()
        )
        if (cursor.count > 0) {
            cursor.moveToFirst()
            cursor.moveToFirst()
            val quickSQLData = QuickSQLData(
                cursor.getString(cursor.getColumnIndex(startColor)),
                cursor.getString(cursor.getColumnIndex(endColor)),
                cursor.getInt(cursor.getColumnIndex(autoCloseDialog)).toBoolean(),
                cursor.getInt(cursor.getColumnIndex(showUserNameOnDialog)).toBoolean(),
                cursor.getInt(cursor.getColumnIndex(showDeviceModelNumberOnDialog)).toBoolean()
            )
            cursor.close()
            return quickSQLData
        }
        cursor.close()
        return QuickSQLData(
            "000000",
            "000000",
            false,
            false,
            false
        )
    }
}

private fun Int.toBoolean(): Boolean {
    return when(this) {
        1 -> true
        else -> false
    }
}
