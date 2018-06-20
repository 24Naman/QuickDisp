package com.naman.quickdisp

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Switch
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {

    private lateinit var quickSQL: QuickSQL
    private lateinit var data: QuickSQLData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val deviceModel = "${android.os.Build.BRAND} ${android.os.Build.MODEL}"
        textView_deviceName.text = deviceModel

        quickSQL = QuickSQL(this)
        data = quickSQL.getData()
        quickSQL.close()

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
                Toast.LENGTH_SHORT).show()

            // update the User name textView
            (it as Switch).updateTextViews()
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
                Toast.LENGTH_SHORT).show()

            // update the Device name textView
            (it as Switch).updateTextViews()
        }

        switch_autoClose.setOnClickListener {
            QuickSQL(this).apply {
                updateAutoClose(switch_autoClose.isChecked)
                close()
            }
            Toast.makeText(this, when (switch_autoClose.isChecked) {
                true -> "Dialog will be closed on selecting any option"
                else -> "Username will not be closed until user select the close button"
            },
                Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()

        switch_showUsername.isChecked = data.showUserNameOnDialog
        switch_showDeviceName.isChecked = data.showDeviceModelNumberOnDialog
        switch_autoClose.isChecked = data.autoCloseDialog

        switch_showUsername.updateTextViews()
        switch_showDeviceName.updateTextViews()
    }

    private fun Switch.updateTextViews() {
        when (this) {
            switch_showUsername -> textView_userName.text = when (this.isChecked) {
                true -> {
                    when (checkSelfPermission(Manifest.permission.READ_CONTACTS)) {
                        PackageManager.PERMISSION_GRANTED -> {
                            with(contentResolver) {
                                this.query(
                                    ContactsContract.Profile.CONTENT_URI,
                                    null,
                                    null,
                                    null,
                                    null
                                ).apply {
                                    moveToFirst()
                                    getString(getColumnIndex(ContactsContract.Profile.DISPLAY_NAME))
                                }
                            }
                        }
                        PackageManager.PERMISSION_DENIED -> {
                            when (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                                false -> {
                                    requestPermissions(
                                        arrayOf(Manifest.permission.READ_CONTACTS),
                                        1234
                                    )
                                }
                                true -> {
                                    // Dialog for requesting permission
                                    AlertDialog.Builder(this@MainActivity).apply {
                                        this.setTitle("Permission to use Contacts?")
                                        this.setIcon(
                                            resources.getDrawable(
                                                R.mipmap.ic_launcher,
                                                context.theme
                                            )
                                        )
                                        this.setPositiveButton(R.string.dialog_yes) { _: DialogInterface, _: Int ->
                                            setOnClickListener {
                                                requestPermissions(
                                                    arrayOf(Manifest.permission.READ_CONTACTS),
                                                    1234
                                                )
                                            }
                                        }
                                        this.setNegativeButton(R.string.dialog_no) { _: DialogInterface, _: Int ->
                                            setOnClickListener {
                                                this@MainActivity.switch_showUsername.isChecked = false
                                            }
                                        }
                                        create()
                                    }.show()
                                }
                            }
                        }
                    }
                    ""
                }
                false -> {
                    ""
                }
            }
            switch_showDeviceName -> {
                textView_deviceName.text = when (this.isChecked) {
                    true -> "${android.os.Build.BRAND} ${android.os.Build.MODEL}"
                    else -> ""
                }
            }
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
                        switch_showUsername.updateTextViews()
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