package com.naman.quickdisp

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.support.v4.app.ActivityCompat
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
                        true -> ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.READ_CONTACTS),
                            permissionRetry
                        )
                        else -> {
                            with (AlertDialog.Builder(this)) {
                                setTitle("Contacts Permission Required To Show User Name")
                                setMessage("Do you want to give it now?")
                                setCancelable(true)
                                setPositiveButton("Yes") {
                                        _: DialogInterface?, _: Int -> run {
                                        ActivityCompat.requestPermissions(
                                            this@MainActivity,
                                            arrayOf(Manifest.permission.READ_CONTACTS),
                                            permissionRetryAgain
                                        )
                                    }
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

        prepareHome(data.showUserNameOnDialog, data.showDeviceModelNumberOnDialog)

        bindListeners()
        // start color gradient
        textView_startColorHex.text = data.startColor
        textView_endColorHex.text = data.endColor
        //        val colorStart = Integer.parseInt(
//            data.startColor.subSequence(0, data.startColor.length-1).toString(),
//            16
//        )
//        val startColorRed = colorStart shr 16 and 0xFF
//        val startColorGreen = colorStart and 0xFF
//        val startColorBlue = colorStart shr 24 and 0xFF
//
//        imageView_startColor.setBackgroundColor(Color.rgb(
//            startColorRed,
//            startColorGreen,
//            startColorBlue
//        ))
//
//        // end color gradient
//        textView_startColorHex.text = data.endColor
//        val colorEnd = Integer.parseInt(
//            data.endColor.subSequence(1, data.startColor.length-1).toString(),
//            16
//        )
//        val endColorRed = colorEnd shr 16 and 0xFF
//        val endColorGreen = colorEnd and 0xFF
//        val endColorBlue = colorEnd shr 24 and 0xFF
//
//        imageView_startColor.setBackgroundColor(Color.rgb(
//            endColorRed,
//            endColorGreen,
//            endColorBlue
//        ))
    }

    private fun bindListeners() {
        switch_showUsername.setOnClickListener {

        }

        switch_showDeviceName.setOnClickListener {

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
