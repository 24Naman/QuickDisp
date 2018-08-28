package com.naman.quickdisp

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.database.CursorIndexOutOfBoundsException
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.SeekBar
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.color_picker_dialog.*
import kotlinx.android.synthetic.main.quick_display_title.*

class MainActivity : Activity() {

    private lateinit var quickSQL: QuickSQL     // object of the SQLiteOpenHelper class
    private lateinit var data: QuickSQLData     // object of the QuickSQLData, data class

    private fun settingPermission() {
        /*
        * Permission for changing device settings
        * */
        when {
            !Settings.System.canWrite(applicationContext) -> {
                val intent = Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, 200)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settingPermission()

        quickSQL = QuickSQL(this)
        data = quickSQL.getData()
        quickSQL.close()

        bindListeners()
        initialize()
    }

    override fun onStart() {
        super.onStart()

        initializeSwitches()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menuItem_resetButton -> {
                AlertDialog.Builder(this@MainActivity).apply {
                    setTitle("Reset App Settings")
                    setMessage("This will change the preferences and color to the default " +
                            "setting permanently.")
                    setIcon(
                        resources.getDrawable(
                            R.drawable.ic_reset_settings,
                            context.theme
                        )
                    )
                    val createdDialog = create()
                    setPositiveButton(R.string.dialog_yes) { _: DialogInterface, _: Int ->
                        com.naman.quickdisp.QuickSQL(this@MainActivity).apply {
                            resetData()
                            close()
                        }
                        initialize()
                    }
                    setNegativeButton(R.string.dialog_no) { _: DialogInterface, _: Int ->
                        createdDialog.dismiss()
                    }
                }.show()
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            1234 -> when (grantResults[0]) {
                PackageManager.PERMISSION_GRANTED -> {
                    raiseLongToast("${permissions[0]} Granted")
                    switch_showUsername.isChecked = true
                }
                else -> {
                    raiseLongToast("${permissions[0]} Not Granted")
                    switch_showUsername.isChecked = false
                }
            }
        }
    }


    private fun initializeSwitches() {
        /*
        * Initialize and change the state of switches on application run
        * */
        quickSQL = QuickSQL(this)
        data = quickSQL.getData()

        // setting the state of switches
        data.apply {
            switch_showUsername.isChecked = showUserNameOnDialog
            switch_showDeviceName.isChecked = showDeviceModelNumberOnDialog
            switch_autoClose.isChecked = autoCloseDialog
        }
    }

    private fun initializeDetailsCard() {
        /*
        * initialize the details on the title CardView
        * */
        quickSQL = QuickSQL(this)
        data = quickSQL.getData()

        // start gradient color
        textView_startColorHex.text = data.startColorHex
        imageView_startColor.setBackgroundColor(data.startColorRgb)

        // end gradient color
        textView_endColorHex.text = data.endColorHex
        imageView_endColor.setBackgroundColor(data.endColorRgb)

        data.apply {
            with (cardView_details) {
                background = android.graphics.drawable.GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(data.startColorRgb, data.endColorRgb)
                ).apply {
                    this.cornerRadius = 20F
                }
                cardElevation = 20F
                this.radius = 20F
                setContentPadding(5, 5, 5, 5)
            }
        }
        quickSQL.close()
    }

    private fun initialize() {
        initializeSwitches()
        initializeDetailsCard()
    }

    private fun bindListeners() {
        /*
        * Binding the listeners to the appropriate widgets
        * */

        switch_autoClose.setOnClickListener {
            QuickSQL(this).apply {
                dialogAutoClose = switch_autoClose.isChecked
                close()
            }
            raiseShortToast(
                when (switch_autoClose.isChecked) {
                    true -> "Dialog will be closed on selecting any option"
                    else -> "Username will not be closed until user select the close button"
                }
            )
        }

        switch_showDeviceName.setOnCheckedChangeListener { _, boolean ->
            QuickSQL(this).apply {
                showDeviceName = switch_showDeviceName.isChecked
                close()
            }
            textView_deviceName.text = when (boolean) {
                true -> when (android.os.Build.MODEL.startsWith(android.os.Build.BRAND)) {
                    true -> android.os.Build.MODEL
                    else -> "${android.os.Build.BRAND} ${android.os.Build.MODEL}"
                }
                else -> resources.getString(R.string.device_name)
            }
            raiseShortToast(
                when (switch_showDeviceName.isChecked) {
                    true -> "Device Name will be shown"
                    else -> "Device Name will not be shown"
                }
            )
        }

        switch_showUsername.setOnCheckedChangeListener { _, boolean ->
            QuickSQL(this).apply {
                showUsername = boolean
                close()
            }
            raiseShortToast(
                when (boolean) {
                    true -> "Username will be shown"
                    else -> "Username will not be shown"
                }
            )

            var username = resources.getString(R.string.username)
            textView_userName.text = when (boolean) {
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
                                ).let {
                                    it?.moveToFirst()
                                    username = try {
                                        it?.getString(it.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME)) ?: "My"
                                    } catch (e: CursorIndexOutOfBoundsException) {
                                        raiseLongToast("Username not available")
                                        "My"
                                    }
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
                                    /**
                                    * Dialog for requesting permission
                                    * It will be shown in case of user has denied the READ_CONTACTS
                                     * permission permanently and has request to show the user name in the dialog
                                    */
                                    AlertDialog.Builder(this@MainActivity).apply {
                                        setTitle("Permission to use Contacts?")
                                        setIcon(
                                            resources.getDrawable(
                                                R.mipmap.ic_launcher,
                                                context.theme
                                            )
                                        )
                                        setPositiveButton(R.string.dialog_yes) { _: DialogInterface, _: Int ->
                                                requestPermissions(
                                                    arrayOf(Manifest.permission.READ_CONTACTS),
                                                    1234
                                                )
                                        }
                                        setNegativeButton(R.string.dialog_no) { _: DialogInterface, _: Int ->
                                                this@MainActivity.switch_showUsername.isChecked = false
                                        }
                                        create()
                                    }.show()
                                }
                            }
                        }
                    }
                    username
                }
                false -> {
                    username
                }
            }
        }

        /*
        * Gradient Start Color: START
        * */
        button_changeStartColor.setOnClickListener {
            /*
            * updating gradient start color
            * */
            with(Dialog(this)) {
                setContentView(R.layout.color_picker_dialog)

                setTitle("Select Gradient Start Color")

                data = quickSQL.getData()

                // converting HexCode to RGB value
                seekBar_redComponent.progress = Integer.parseInt(data.startColor, 16) shr 16 and 0xFF
                seekBar_greenComponent.progress = Integer.parseInt(data.startColor, 16) shr 8 and 0xFF
                seekBar_blueComponent.progress = Integer.parseInt(data.startColor, 16) and 0xFF

                cardView_finalColor.setCardBackgroundColor(Color.rgb(
                    seekBar_redComponent.progress,
                    seekBar_greenComponent.progress,
                    seekBar_blueComponent.progress
                ))

                // setting the initial value of color components in the TextViews
                textView_redComponent.text = String.format(
                    "#%02X",
                    seekBar_redComponent.progress
                )
                textView_greenComponent.text = String.format(
                    "#%02X",
                    seekBar_greenComponent.progress
                )
                textView_blueComponent.text = String.format(
                    "#%02X",
                    seekBar_blueComponent.progress
                )
                textView_hexCode.text = String.format(
                    "#%02X%02X%02X",
                    seekBar_redComponent.progress,
                    seekBar_greenComponent.progress,
                    seekBar_blueComponent.progress
                )

                /*
                * Gradient Start RED Component: RED_START
                * */
                seekBar_redComponent.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        imageView_redComponent.setBackgroundColor(Color.rgb(
                            seekBar_redComponent.progress, 0,0
                        ))

                        cardView_finalColor.setCardBackgroundColor(Color.rgb(
                            seekBar_redComponent.progress,
                            seekBar_greenComponent.progress,
                            seekBar_blueComponent.progress
                        ))
                        textView_redComponent.text = String.format(
                            "#%02X",
                            seekBar_redComponent.progress
                        )
                        textView_hexCode.text = String.format(
                            "#%02X%02X%02X",
                            seekBar_redComponent.progress,
                            seekBar_greenComponent.progress,
                            seekBar_blueComponent.progress
                        )
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) = Unit

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                        cardView_finalColor.setCardBackgroundColor(Color.rgb(
                            seekBar_redComponent.progress,
                            seekBar_greenComponent.progress,
                            seekBar_blueComponent.progress
                        ))
                        textView_redComponent.text = String.format(
                            "#%02X",
                            seekBar_redComponent.progress
                        )
                        textView_hexCode.text = String.format(
                            "#%02X%02X%02X",
                            seekBar_redComponent.progress,
                            seekBar_greenComponent.progress,
                            seekBar_blueComponent.progress
                        )
                    }
                })
                /*
                * Gradient Start RED Component: RED_END
                * */

                /*
                * Gradient Start GREEN Component: GREEN_START
                * */
                seekBar_greenComponent.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        imageView_greenComponent.setBackgroundColor(Color.rgb(
                            0, seekBar_greenComponent.progress,0
                        ))

                        cardView_finalColor.setCardBackgroundColor(Color.rgb(
                            seekBar_redComponent.progress,
                            seekBar_greenComponent.progress,
                            seekBar_blueComponent.progress
                        ))
                        textView_greenComponent.text = String.format(
                            "#%02X",
                            seekBar_greenComponent.progress
                        )
                        textView_hexCode.text = String.format(
                            "#%02X%02X%02X",
                            seekBar_redComponent.progress,
                            seekBar_greenComponent.progress,
                            seekBar_blueComponent.progress
                        )
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) = Unit

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                        cardView_finalColor.setCardBackgroundColor(Color.rgb(
                            seekBar_redComponent.progress,
                            seekBar_greenComponent.progress,
                            seekBar_blueComponent.progress
                        ))
                        textView_greenComponent.text = String.format(
                            "#%02X",
                            seekBar_greenComponent.progress
                        ).toUpperCase()
                        textView_hexCode.text = String.format(
                            "#%02X%02X%02X",
                            seekBar_redComponent.progress,
                            seekBar_greenComponent.progress,
                            seekBar_blueComponent.progress
                        )
                    }
                })
                /*
                * Gradient Start GREEN Component: GREEN_END
                * */

                /*
                * Gradient Start BLUE Component: BLUE_START
                * */
                seekBar_blueComponent.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        imageView_blueComponent.setBackgroundColor(Color.rgb(
                            0, 0,seekBar_blueComponent.progress
                        ))

                        cardView_finalColor.setCardBackgroundColor(Color.rgb(
                            seekBar_redComponent.progress,
                            seekBar_greenComponent.progress,
                            seekBar_blueComponent.progress
                        ))
                        textView_blueComponent.text = String.format(
                            "#%02X",
                            seekBar_blueComponent.progress
                        )
                        textView_hexCode.text = String.format(
                            "#%02X%02X%02X",
                            seekBar_redComponent.progress,
                            seekBar_greenComponent.progress,
                            seekBar_blueComponent.progress
                        )
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) = Unit

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                        cardView_finalColor.setCardBackgroundColor(Color.rgb(
                            seekBar_redComponent.progress,
                            seekBar_greenComponent.progress,
                            seekBar_blueComponent.progress
                        ))
                        textView_blueComponent.text = String.format(
                            "#%02X",
                            seekBar_blueComponent.progress
                        )
                        textView_hexCode.text = String.format(
                            "#%02X%02X%02X",
                            seekBar_redComponent.progress,
                            seekBar_greenComponent.progress,
                            seekBar_blueComponent.progress
                        )
                    }
                })
                /*
                * Gradient Start BLUE Component: BLUE_END
                * */

                button_colorPickerCancel.setOnClickListener { _ ->
                    dismiss()
                }

                button_colorPickerOk.setOnClickListener { _ ->
                    with(QuickSQL(this@MainActivity)) {
                        gradientStartColor = textView_hexCode.text as String
                        val hexCode = "#${textView_hexCode.text.subSequence(1, 7).toString().toUpperCase()}"
                        this@MainActivity.textView_startColorHex.text = hexCode
                        this@MainActivity.imageView_startColor.setBackgroundColor(Color.rgb(
                            seekBar_redComponent.progress,
                            seekBar_greenComponent.progress,
                            seekBar_blueComponent.progress
                        ))
                        initializeDetailsCard()
                        close()
                    }
                    dismiss()
                }
                show()
            }
        }
        /*
        * Gradient Start Color: END
        * */

        /*
        * Gradient End Color: START
        * */
        button_changeEndColor.setOnClickListener {
            /*
            * updating gradient start color
            * */
            with(Dialog(this)) {
                setContentView(R.layout.color_picker_dialog)

                setTitle("Select Gradient End Color")

                data = quickSQL.getData()

                // converting HexCode to RGB value
                seekBar_redComponent.progress = Integer.parseInt(data.endColor, 16) shr 16 and 0xFF
                seekBar_greenComponent.progress = Integer.parseInt(data.endColor, 16) shr 8 and 0xFF
                seekBar_blueComponent.progress = Integer.parseInt(data.endColor, 16) and 0xFF

                // setting the initial value of color components in the TextViews
                textView_redComponent.text = String.format(
                    "#%02X",
                    seekBar_redComponent.progress
                )
                textView_greenComponent.text = String.format(
                    "#%02X",
                    seekBar_greenComponent.progress
                )
                textView_blueComponent.text = String.format(
                    "#%02X",
                    seekBar_blueComponent.progress
                )
                textView_hexCode.text = String.format(
                    "#%02X%02X%02X",
                    seekBar_redComponent.progress,
                    seekBar_greenComponent.progress,
                    seekBar_blueComponent.progress
                )

                cardView_finalColor.setCardBackgroundColor(Color.rgb(
                    seekBar_redComponent.progress,
                    seekBar_greenComponent.progress,
                    seekBar_blueComponent.progress
                ))
                /*
                * Gradient Start RED Component: RED_START
                * */
                seekBar_redComponent.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        imageView_redComponent.setBackgroundColor(Color.rgb(
                            seekBar_redComponent.progress, 0,0
                        ))

                        cardView_finalColor.setCardBackgroundColor(Color.rgb(
                            seekBar_redComponent.progress,
                            seekBar_greenComponent.progress,
                            seekBar_blueComponent.progress
                        ))
                        textView_redComponent.text = String.format(
                            "#%02X",
                            seekBar_redComponent.progress
                        )
                        textView_hexCode.text = String.format(
                            "#%02X%02X%02X",
                            seekBar_redComponent.progress,
                            seekBar_greenComponent.progress,
                            seekBar_blueComponent.progress
                        )
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) = Unit

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                        cardView_finalColor.setCardBackgroundColor(Color.rgb(
                            seekBar_redComponent.progress,
                            seekBar_greenComponent.progress,
                            seekBar_blueComponent.progress
                        ))
                        textView_redComponent.text = String.format(
                            "#%02X",
                            seekBar_redComponent.progress
                        )
                        textView_hexCode.text = String.format(
                            "#%02X%02X%02X",
                            seekBar_redComponent.progress,
                            seekBar_greenComponent.progress,
                            seekBar_blueComponent.progress
                        )
                    }
                })
                /*
                * Gradient Start RED Component: RED_END
                * */

                /*
                * Gradient Start GREEN Component: GREEN_START
                * */
                seekBar_greenComponent.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        imageView_greenComponent.setBackgroundColor(Color.rgb(
                            0, seekBar_greenComponent.progress,0
                        ))

                        cardView_finalColor.setCardBackgroundColor(Color.rgb(
                            seekBar_redComponent.progress,
                            seekBar_greenComponent.progress,
                            seekBar_blueComponent.progress
                        ))
                        textView_greenComponent.text = String.format(
                            "#%02X",
                            seekBar_greenComponent.progress
                        )
                        textView_hexCode.text = String.format(
                            "#%02X%02X%02X",
                            seekBar_redComponent.progress,
                            seekBar_greenComponent.progress,
                            seekBar_blueComponent.progress
                        )
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) = Unit

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                        cardView_finalColor.setCardBackgroundColor(Color.rgb(
                            seekBar_redComponent.progress,
                            seekBar_greenComponent.progress,
                            seekBar_blueComponent.progress
                        ))
                        textView_greenComponent.text = String.format(
                            "#%02X",
                            seekBar_greenComponent.progress
                        )
                        textView_hexCode.text = String.format(
                            "#%02X%02X%02X",
                            seekBar_redComponent.progress,
                            seekBar_greenComponent.progress,
                            seekBar_blueComponent.progress
                        )
                    }
                })
                /*
                * Gradient Start GREEN Component: GREEN_END
                * */

                /*
                * Gradient Start BLUE Component: BLUE_START
                * */
                seekBar_blueComponent.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        imageView_blueComponent.setBackgroundColor(Color.rgb(
                            0, 0,seekBar_blueComponent.progress
                        ))

                        cardView_finalColor.setCardBackgroundColor(Color.rgb(
                            seekBar_redComponent.progress,
                            seekBar_greenComponent.progress,
                            seekBar_blueComponent.progress
                        ))
                        textView_blueComponent.text = String.format(
                            "#%02X",
                            seekBar_blueComponent.progress
                        )
                        textView_hexCode.text = String.format(
                            "#%02X%02X%02X",
                            seekBar_redComponent.progress,
                            seekBar_greenComponent.progress,
                            seekBar_blueComponent.progress
                        )
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) = Unit

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                        cardView_finalColor.setCardBackgroundColor(Color.rgb(
                            seekBar_redComponent.progress,
                            seekBar_greenComponent.progress,
                            seekBar_blueComponent.progress
                        ))
                        textView_blueComponent.text = String.format(
                            "#%02X",
                            seekBar_blueComponent.progress
                        )
                        textView_hexCode.text = String.format(
                            "#%02X%02X%02X",
                            seekBar_redComponent.progress,
                            seekBar_greenComponent.progress,
                            seekBar_blueComponent.progress
                        )
                    }
                })
                /*
                * Gradient Start BLUE Component: BLUE_END
                * */

                button_colorPickerCancel.setOnClickListener { _ ->
                    dismiss()
                }

                button_colorPickerOk.setOnClickListener { _ ->
                    with(QuickSQL(this@MainActivity)) {
                        gradientEndColor = textView_hexCode.text as String
                        val hexCode = "#${textView_hexCode.text.subSequence(1, 7).toString().toUpperCase()}"
                        this@MainActivity.textView_endColorHex.text = hexCode
                        this@MainActivity.imageView_endColor.setBackgroundColor(Color.rgb(
                            seekBar_redComponent.progress,
                            seekBar_greenComponent.progress,
                            seekBar_blueComponent.progress
                        ))
                        initializeDetailsCard()
                        close()
                    }
                    dismiss()
                }
                show()
            }
        }
        /*
        * Gradient End Color: END
        * */
    }
}

private fun MainActivity.raiseLongToast(message: String) {
    /**
    * Extension function on MainActivity to show LENGTH_LONG toast
    * */
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

private fun MainActivity.raiseShortToast(message: String) {
    /**
     * Extension function on MainActivity to show LENGTH_SHORT toast
     * */
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
