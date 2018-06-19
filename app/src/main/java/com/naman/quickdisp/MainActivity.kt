package com.naman.quickdisp

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {

    private lateinit var quickSQL: QuickSQL

    private val permissionRetry = 1234
    private val permissionRetryAgain = 12345

    private fun prepareHome(
        showName: Boolean=false,
        showDeviceName: Boolean=false,
        fromSwitch: Boolean=false
    ) {
        when (showName) {
            true -> when (checkSelfPermission(Manifest.permission.READ_CONTACTS)) {
                PackageManager.PERMISSION_GRANTED -> {
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
                            else -> "Set Username in the contacts"
                        }
                    }
                }
                PackageManager.PERMISSION_DENIED -> {
                    when (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                        // request for the permission
                        true -> ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.READ_CONTACTS),
                            permissionRetry
                        )
                        else -> {
                            when {
                                !fromSwitch -> return
                                else -> with(AlertDialog.Builder(this)) {
                                    setTitle("Contacts Permission Required To Show User Name")
                                    setMessage("Do you want to give it now?")
                                    setCancelable(true)
                                    setPositiveButton("Yes") { _: DialogInterface?, _: Int ->
                                        run {
                                            ActivityCompat.requestPermissions(
                                                this@MainActivity,
                                                arrayOf(Manifest.permission.READ_CONTACTS),
                                                permissionRetryAgain
                                            )
                                        }
                                    }
                                    setNegativeButton("No") { _: DialogInterface, _: Int ->
                                        return@setNegativeButton
                                    }
                                    setNeutralButton("Open App Info") { _: DialogInterface, _: Int ->
                                        with(Intent(
                                            Settings.ACTION_APPLICATION_SETTINGS,
                                            Uri.parse("package:$packageName")
                                        )) {
                                            addCategory(Intent.CATEGORY_DEFAULT)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    }
                                    create()
                                }
                            }
                        }
                    }
                }
            }
        }

        textView_deviceName.text = when (showDeviceName) {
            true -> {
                "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
            }
            else -> {
                ""      // show empty string
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        quickSQL = QuickSQL(this)
        val data = quickSQL.getData()
        quickSQL.close()

        switch_showUsername.isChecked = data.showUserNameOnDialog
        switch_showDeviceName.isChecked = data.showDeviceModelNumberOnDialog
        switch_autoClose.isChecked = data.autoCloseDialog

        prepareHome(data.showUserNameOnDialog, data.showDeviceModelNumberOnDialog)

        bindListeners()
        // start color gradient
        val startColorHex = "#${data.startColor}"
        textView_startColorHex.text = startColorHex
        val colorStart = Integer.parseInt(data.startColor,16)
        val startColorRed = colorStart shr 16 and 0xFF
        val startColorGreen = colorStart and 0xFF
        val startColorBlue = colorStart shr 24 and 0xFF

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
        val endColorGreen = colorEnd and 0xFF
        val endColorBlue = colorEnd shr 24 and 0xFF

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
                        prepareHome()
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
