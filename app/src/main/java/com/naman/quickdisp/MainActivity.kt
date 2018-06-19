package com.naman.quickdisp

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {

    private lateinit var quickSQL: QuickSQL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // show user name from the contacts
        with (contentResolver.query (
            ContactsContract.Profile.CONTENT_URI,
            null,
            null,
            null,
            null)) {
            textView_userName.text = when {
                this.count > 0 -> {
                    this.moveToFirst()
                    val dispName = this.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME)
                    this.getString(dispName)
                }
                else -> "Set Username in the profile"
            }
        }

        val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        textView_deviceName.text = deviceName

        quickSQL = QuickSQL(this)
        val data = quickSQL.getData()
        quickSQL.close()

        switch_showUsername.isChecked = data.showUserNameOnDialog
        switch_showDeviceName.isChecked = data.showDeviceModelNumberOnDialog

        // start color gradient
        textView_startColorHex.text = data.startColor
        val colorStart = Integer.parseInt(
            data.startColor.subSequence(1, data.startColor.length-1).toString(),
            16
        )
        val startColorRed = colorStart shr 16 and 0xFF
        val startColorGreen = colorStart and 0xFF
        val startColorBlue = colorStart shr 24 and 0xFF

        imageView_startColor.setBackgroundColor(Color.rgb(
            startColorRed,
            startColorGreen,
            startColorBlue
        ))

        // end color gradient
        textView_startColorHex.text = data.endColor
        val colorEnd = Integer.parseInt(
            data.endColor.subSequence(1, data.startColor.length-1).toString(),
            16
        )
        val endColorRed = colorEnd shr 16 and 0xFF
        val endColorGreen = colorEnd and 0xFF
        val endColorBlue = colorEnd shr 24 and 0xFF

        imageView_startColor.setBackgroundColor(Color.rgb(
            endColorRed,
            endColorGreen,
            endColorBlue
        ))

        val message = data.autoCloseDialog.toString() + data.endColor +
                data.showDeviceModelNumberOnDialog + data.showUserNameOnDialog + data.startColor
        println(message)
        Log.v("witcher", message)
    }
}
