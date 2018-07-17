package com.naman.quickdisp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Color

data class QuickSQLData(
    val startColor: String,
    val endColor: String,
    val autoCloseDialog: Boolean,
    val showUserNameOnDialog: Boolean,
    val showDeviceModelNumberOnDialog: Boolean
) {
    private fun hexToRgb(color: Int): Int = Color.rgb(
        color shr 16 and 0xFF,
        color shr 8 and 0xFF,
        color and 0xFF
    )

    val startColorHex: String
        get() = "#$startColor"

    val endColorHex: String
        get() = "#$endColor"

    val startColorRgb: Int
        get() = hexToRgb(Integer.parseInt(startColor, 16))

    val endColorRgb: Int
        get() = hexToRgb(Integer.parseInt(endColor, 16))
}

class QuickSQL(context: Context) : SQLiteOpenHelper(context, "app_settings.db", null, 1) {

    private var tableName = "quicksql"

    // column names
    private val startColor = "startColor"
    private val endColor = "endColor"
    private val firstRun = "firstRun"
    private val autoCloseDialog = "autoCloseDialog"
    private val showUserNameOnDialog = "showUserNameOnDialog"
    private val showDeviceModelNumberOnDialog = "showDeviceModelNumberOnDialog"

    override fun onCreate(sqLiteDatabase: SQLiteDatabase?) {

        val createTableStatement = """
            create table $tableName (
                $startColor  string,
                $endColor    string,
                $firstRun   int,
                $autoCloseDialog int,
                $showUserNameOnDialog    int,
                $showDeviceModelNumberOnDialog   int
            )
        """.trimIndent()

        sqLiteDatabase?.execSQL(createTableStatement)

        sqLiteDatabase?.insert(
            tableName,
            null,
            ContentValues().apply {
                put(startColor, "FFFFFF")
                put(endColor, "FFFFFF")
                put(firstRun, true.oneOrZero())
                put(autoCloseDialog, false.oneOrZero())
                put(showUserNameOnDialog, false.oneOrZero())
                put(showDeviceModelNumberOnDialog, false.oneOrZero())
            }
        )
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) = Unit

    fun getData(): QuickSQLData {
        val cursor = this.readableDatabase.rawQuery(
            "select * from ${this@QuickSQL.tableName};",
            arrayOf()
        )
        return with(cursor) {
            when {
                count > 0 -> {
                    moveToFirst()
                    val quickSQLData = QuickSQLData(
                        getString(getColumnIndex(startColor)),
                        getString(getColumnIndex(endColor)),
                        getInt(getColumnIndex(autoCloseDialog)).trueOrFalse(),
                        getInt(getColumnIndex(showUserNameOnDialog)).trueOrFalse(),
                        getInt(getColumnIndex(showDeviceModelNumberOnDialog)).trueOrFalse()
                    )
                    close()
                    quickSQLData
                }
                else -> {
                    close()
                    QuickSQLData(
                        startColor = "FFFFFF",
                        endColor = "FFFFFF",
                        autoCloseDialog = false,
                        showUserNameOnDialog = false,
                        showDeviceModelNumberOnDialog = false
                    )
                }
            }
        }
    }

    fun resetData() {
        with(writableDatabase) {
            this.update(
                tableName,
                ContentValues().apply {
                    put(startColor, "FFFFFF")
                    put(endColor, "FFFFFF")
                    put(autoCloseDialog, false.oneOrZero())
                    put(showUserNameOnDialog, false.oneOrZero())
                    put(showDeviceModelNumberOnDialog, false.oneOrZero())
                },
                null,
                null
            )
            close()
        }
    }

    var gradientStartColor: String =""
        set(value) = with(writableDatabase) {
            this.update(
                tableName,
                ContentValues().apply {
                    put(startColor, value.subSequence(1, 7).toString().toUpperCase())
                },
                null,
                null
            )
            close()
        }

    var gradientEndColor: String =""
        set(value) = with(writableDatabase) {
            this.update(
                tableName,
                ContentValues().apply {
                    put(endColor, value.subSequence(1, 7).toString().toUpperCase())
                },
                null,
                null
            )
            close()
        }

    var showUsername: Boolean = false
        set(value) = with(writableDatabase) {
            this.update(
                tableName,
                ContentValues().apply {
                    put(showUserNameOnDialog, value.oneOrZero())
                },
                null,
                null
            )
            close()
        }

    var showDeviceName: Boolean = false
        set(value) = with(writableDatabase) {
            this.update(
                tableName,
                ContentValues().apply {
                    put(showDeviceModelNumberOnDialog, value.oneOrZero())
                },
                null,
                null
            )
            close()
        }

    var dialogAutoClose: Boolean = false
        set(value) = with(writableDatabase) {
            this.update(
                tableName,
                ContentValues().apply {
                    put(autoCloseDialog, value.oneOrZero())
                },
                null,
                null
            )
            close()
        }
}

private fun Boolean.oneOrZero(): Int = when(this) {
    true -> 1
    else -> 0
}

private fun Int.trueOrFalse(): Boolean = when(this) {
    1 -> true
    else -> false
}

