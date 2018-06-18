package com.naman.quickdisp

import android.app.Activity
import android.os.Bundle
import android.provider.ContactsContract
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {

    private lateinit var quickSQL: QuickSQL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        quickSQL = QuickSQL(this)

        contentResolver.query(
            ContactsContract.Profile.CONTENT_URI,
            null,
            null,
            null,
            null
        ).use {
            when {
                it.count > 0 -> {
                    it.moveToFirst()
                    val dispName = it.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME)
                    textView_userName.text = it.getString(dispName)
                }
            }
        }

        val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        textView_deviceName.text = deviceName

        quickSQL.printData()
    }
}
