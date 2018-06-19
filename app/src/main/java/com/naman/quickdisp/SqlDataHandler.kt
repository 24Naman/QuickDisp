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

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) = Unit

    fun getData(): QuickSQLData {
        val cursor = this.readableDatabase.rawQuery(
            "select * from ${this@QuickSQL.tableName};",
            arrayOf()
        )
        with(cursor) {
            if (count > 0) {
                moveToFirst()
                val quickSQLData = QuickSQLData(
                    getString(getColumnIndex(startColor)),
                    getString(getColumnIndex(endColor)),
                    getInt(getColumnIndex(autoCloseDialog)).trueOrFalse(),
                    getInt(getColumnIndex(showUserNameOnDialog)).trueOrFalse(),
                    getInt(getColumnIndex(showDeviceModelNumberOnDialog)).trueOrFalse()
                )
                close()
                return quickSQLData
            }
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

    fun updateShowUsername(state: Boolean) {
        with(writableDatabase) {
            this.update(
                tableName,
                ContentValues().apply {
                    put(showUserNameOnDialog, state.oneOrZero())
                },
                null,
                null
            )
            close()
        }
    }

    fun updateShowDeviceName(state: Boolean) {
        with(writableDatabase) {
            this.update(
                tableName,
                ContentValues().apply {
                    put(showDeviceModelNumberOnDialog, state.oneOrZero())
                },
                null,
                null
            )
            close()
        }
    }

    fun updateAutoClose(state: Boolean) {
        with(writableDatabase) {
            this.update(
                tableName,
                ContentValues().apply {
                    put(autoCloseDialog, state.oneOrZero())
                },
                null,
                null
            )
            close()
        }
    }
}

private fun Boolean.oneOrZero(): Int {
    return when(this) {
        true -> 1
        else -> 0
    }
}

private fun Int.trueOrFalse(): Boolean {
    return when(this) {
        1 -> true
        else -> false
    }
}
