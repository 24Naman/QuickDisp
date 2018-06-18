package com.naman.quickdisp

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
    private val autoCloseDialog = "autoCloseDialog"
    private val showUserNameOnDialog = "showUserNameOnDialog"
    private val showDeviceModelNumberOnDialog = "showDeviceModelNumberOnDialog"

    override fun onCreate(p0: SQLiteDatabase?) {

        val createTableStatement = """
            create table $tableName (
                $startColor  string DEFAULT #FFFFFF,
                $endColor    string DEFAULT #FFFFFF,
                $autoCloseDialog int DEFAULT 0,
                $showUserNameOnDialog    int DEFAULT 0,
                $showDeviceModelNumberOnDialog   int DEFAULT 0
            )
        """.trimIndent()

        p0?.execSQL(createTableStatement)
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
        p0?.execSQL("DROP TABLE IF EXISTS $tableName")
        onCreate(p0)
    }

    fun getData(): QuickSQLData {
        val cursor = this.readableDatabase.query(
            this@QuickSQL.tableName,
            arrayOf(
                startColor,
                endColor,
                autoCloseDialog,
                showUserNameOnDialog,
                showDeviceModelNumberOnDialog
            ),
            null,
            null,
            null,
            null,
            null
        )

        return cursor.use {
            it.moveToFirst()
            QuickSQLData(
                it.getString(0),
                it.getString(1),
                when(it.getInt(2)) {
                    1 -> true
                    else -> false
                },
                when(it.getInt(3)) {
                    1 -> true
                    else -> false
                },
                when(it.getInt(4)) {
                    1 -> true
                    else -> false
                }
            )
        }
    }
}
