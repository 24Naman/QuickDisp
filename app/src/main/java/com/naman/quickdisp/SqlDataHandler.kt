package com.naman.quickdisp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Color

data class QuickSQLData(
    val bgColor: String,
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

    val bgColorHex: String
        get() = "#$bgColor"

    val startColorRgb: Int
        get() = hexToRgb(Integer.parseInt(startColor, 16))

    val endColorRgb: Int
        get() = hexToRgb(Integer.parseInt(endColor, 16))

    val bgColorRgb: Int
        get() = hexToRgb(Integer.parseInt(bgColor, 16))
}

private const val whiteColor = "FFFFFF"
private const val dbVersion = 2

class QuickSQL(context: Context) : SQLiteOpenHelper(context, "app_settings.db", null, dbVersion) {
    private var tableName = "quicksql"

    // column names
    private val startColor = "startColor"
    private val endColor = "endColor"
    private val autoCloseDialog = "autoCloseDialog"
    private val showUserNameOnDialog = "showUserNameOnDialog"
    private val showDeviceModelNumberOnDialog = "showDeviceModelNumberOnDialog"
    // dbVersion 2
    private val bgColor = "bgColor"

    override fun onCreate(sqLiteDatabase: SQLiteDatabase?) {

        val createTableStatement = """
            create table $tableName (
                $bgColor   string,
                $startColor  string,
                $endColor    string,
                $autoCloseDialog int,
                $showUserNameOnDialog    int,
                $showDeviceModelNumberOnDialog   int
            )
        """.trimIndent()

        sqLiteDatabase?.apply {
            execSQL(createTableStatement)

            insert(
                tableName,
                null,
                ContentValues().apply {
                    put(bgColor, whiteColor)
                    put(startColor, whiteColor)
                    put(endColor, whiteColor)
                    put(autoCloseDialog, false.oneOrZero())
                    put(showUserNameOnDialog, false.oneOrZero())
                    put(showDeviceModelNumberOnDialog, false.oneOrZero())
                }
            )
        }
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
        when {
            p1 < 2 -> {
                p0?.execSQL("ALTER TABLE $tableName ADD COLUMN $bgColor TEXT DEFAULT $whiteColor")
            }
        }
    }

    fun getData(): QuickSQLData {
        val cursor = readableDatabase.rawQuery(
            "select * from ${this@QuickSQL.tableName};",
            arrayOf()
        )
        return with(cursor) {
            when {
                count > 0 -> {
                    moveToFirst()
                    val quickSQLData = QuickSQLData(
                        getString(getColumnIndex(bgColor)),
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
                        bgColor = whiteColor,
                        startColor = whiteColor,
                        endColor = whiteColor,
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
            updateAllRecords(
                ContentValues().apply {
                    put(bgColor, whiteColor)
                    put(startColor, whiteColor)
                    put(endColor, whiteColor)
                    put(autoCloseDialog, false.oneOrZero())
                    put(showUserNameOnDialog, false.oneOrZero())
                    put(showDeviceModelNumberOnDialog, false.oneOrZero())
                }
            )
            close()
        }
    }

    var dialogBgColor: String = ""
        set(value) = with(writableDatabase) {
            updateAllRecords(
                ContentValues().apply {
                    put(bgColor, value.subSequence(1, 7).toString().toUpperCase())
                }
            )
            close()
        }

    var gradientStartColor: String = ""
        set(value) = with(writableDatabase) {
            updateAllRecords(
                ContentValues().apply {
                    put(startColor, value.subSequence(1, 7).toString().toUpperCase())
                }
            )
            close()
        }

    var gradientEndColor: String = ""
        set(value) = with(writableDatabase) {
            updateAllRecords(
                ContentValues().apply {
                    put(endColor, value.subSequence(1, 7).toString().toUpperCase())
                }
            )
            close()
        }

    var showUsername: Boolean = false
        set(value) = with(writableDatabase) {
            updateAllRecords(
                ContentValues().apply {
                    put(showUserNameOnDialog, value.oneOrZero())
                }
            )
            close()
        }

    var showDeviceName: Boolean = false
        set(value) = with(writableDatabase) {
            updateAllRecords(
                ContentValues().apply {
                    put(showDeviceModelNumberOnDialog, value.oneOrZero())
                }
            )
            close()
        }

    var dialogAutoClose: Boolean = false
        set(value) = with(writableDatabase) {
            updateAllRecords(
                ContentValues().apply {
                    put(autoCloseDialog, value.oneOrZero())
                }
            )
            close()
        }

    private fun SQLiteDatabase.updateAllRecords(data: ContentValues) {
        this.update(tableName, data, null, null)
    }

    private fun Boolean.oneOrZero(): Int = when(this) {
        true -> 1
        else -> 0
    }

    private fun Int.trueOrFalse(): Boolean = when(this) {
        1 -> true
        else -> false
    }
}