package com.naman.quickdisp

import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {

    private lateinit var quickSQL: QuickSQL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        quickSQL = QuickSQL(this)
        val data = quickSQL.getData()
        quickSQL.close()

        switch_showUsername.isChecked = data.showUserNameOnDialog
        switch_showDeviceName.isChecked = data.showDeviceModelNumberOnDialog
        switch_autoClose.isChecked = data.autoCloseDialog

        bindListeners()
        // start color gradient
        val startColorHex = "#${data.startColor}"
        textView_startColorHex.text = startColorHex
        val colorStart = Integer.parseInt(data.endColor, 16)
        val startColorRed = colorStart shr 16 and 0xFF
        val startColorGreen = colorStart shr 8 and 0xFF
        val startColorBlue = colorStart and 0xFF

        imageView_startColor.setBackgroundColor(Color.rgb(
            startColorRed,
            startColorGreen,
            startColorBlue
        ))

        // end color gradient
        val endColorHex = "#${data.endColor}"
        textView_endColorHex.text = endColorHex
        val colorEnd = Integer.parseInt(data.endColor, 16)
        val endColorRed = colorEnd shr 16 and 0xFF
        val endColorGreen = colorEnd shr 8 and 0xFF
        val endColorBlue = colorEnd and 0xFF

        imageView_endColor.setBackgroundColor(Color.rgb(
            endColorRed,
            endColorGreen,
            endColorBlue
        ))
    }

    private fun bindListeners() {
        switch_showUsername.setOnClickListener {
            QuickSQL(this).apply {
                updateShowUsername(switch_showUsername.isChecked)
                close()
            }
            Toast.makeText(this, when (switch_showUsername.isChecked) {
                true -> "Username will be shown"
                else -> "Username will not be shown"
            },
                Toast.LENGTH_LONG).show()
        }

        switch_showDeviceName.setOnClickListener {
            QuickSQL(this).apply {
                updateShowDeviceName(switch_showDeviceName.isChecked)
                close()
            }
            Toast.makeText(this, when (switch_showDeviceName.isChecked) {
                true -> "Device Name will be shown"
                else -> "Device Name will not be shown"
            },
                Toast.LENGTH_LONG).show()
        }

        switch_autoClose.setOnClickListener {
            QuickSQL(this).apply {
                Log.e("witcher", switch_autoClose.isChecked.toString())
                updateAutoClose(switch_autoClose.isChecked)
                close()
            }
            Toast.makeText(this, when (switch_autoClose.isChecked) {
                true -> "Dialog will be closed on selecting any option"
                else -> "Username will not be closed until user select the close button"
            },
                Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>?,
        grantResults: IntArray?
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            1234 -> {
                when (grantResults?.get(0)) {
                    PackageManager.PERMISSION_GRANTED -> {
                        Toast.makeText(
                            this,
                            "${permissions?.get(0)} Granted",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else -> Toast.makeText(
                        this,
                        "${permissions?.get(0)} Not Granted",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
