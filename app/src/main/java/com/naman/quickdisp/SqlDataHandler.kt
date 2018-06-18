package com.naman.quickdisp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

data class QuickSQLData(
    val startColor: String,
    val endColor: String,
    val autoCloseDialog: Boolean,
    val showUserNameOnDialog: Boolean,
    val showDeviceModelNumberOnDialog: Boolean
)

class QuickSQL(context: Context) : SQLiteOpenHelper(context, "app_settings", null, 1) {

    private var tableName = "quicksql"

    // column name
    private val startColor = "startColor"
    private val endColor = "endColor"
    private val autoCloseDialog = "autoCloseDialog"
    private val showUserNameOnDialog = "showUserNameOnDialog"
    private val showDeviceModelNumberOnDialog = "showDeviceModelNumberOnDialog"

    override fun onCreate(p0: SQLiteDatabase?) {
        Log.v("SQL", "onCreate")

        val createTableStatement = """
            create table $tableName (
                $startColor  string,
                $endColor    string,
                $autoCloseDialog int,
                $showUserNameOnDialog    int,
                $showDeviceModelNumberOnDialog   int
            )
        """.trimIndent()

        p0?.execSQL(createTableStatement)

        insertData(
            QuickSQLData(
                "#FFFFFF",
                "#FFFFFF",
                false,
                false,
                false
            )
        )
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
        p0?.execSQL("DROP TABLE IF EXISTS contacts")
        onCreate(p0)
    }

    private fun insertData(quickSQLData: QuickSQLData): Boolean {
        /* return */ this.writableDatabase.use {
            when (
            it.insert(
                tableName,
                null,
                ContentValues().let {
                    it.put(startColor, quickSQLData.startColor)
                    it.put(endColor, quickSQLData.endColor)
                    it.put(
                        autoCloseDialog, when (quickSQLData.autoCloseDialog) {
                            true -> 1
                            else -> 0
                        }
                    )
                    it.put(
                        showUserNameOnDialog, when (quickSQLData.showUserNameOnDialog) {
                            true -> 1
                            else -> 0
                        }
                    )
                    it.put(
                        showDeviceModelNumberOnDialog,
                        when (quickSQLData.showDeviceModelNumberOnDialog) {
                            true -> 1
                            else -> 0
                        }
                    )
                    it
                }
            )
            ) {
                -1L -> false
                else -> true
            }
        }
        return true
    }

    fun printData() {
        val temp = this.readableDatabase.use {
            it.query(
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
        }
        println(temp)
        Log.v("naman", temp.toString())
    }
}
