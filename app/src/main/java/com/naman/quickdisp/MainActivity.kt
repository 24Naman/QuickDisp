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
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
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

        quickSQL = QuickSQL(this).apply {
            data = getData()
        }

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
        QuickSQL(this).apply {
            // applying the state of switches from the database
            getData().apply {
                switch_showUsername.isChecked = showUserNameOnDialog
                switch_showDeviceName.isChecked = showDeviceModelNumberOnDialog
                switch_autoClose.isChecked = autoCloseDialog
            }
            close()
        }
    }

    private fun initializeDetailsCard() {
        /*
        * initialize the details on the title CardView
        * */

        QuickSQL(this).apply {
            getData().apply {
                main_layout.background = android.graphics.drawable.ColorDrawable().let {
                    it.color = this.bgColorRgb
                    it
                }

                // start gradient color
                textView_startColorHex.text = startColorHex
                imageView_startColor.setBackgroundColor(startColorRgb)

                // end gradient color
                textView_endColorHex.text = endColorHex
                imageView_endColor.setBackgroundColor(endColorRgb)

                // background color
                textView_bgColorHex.text = bgColorHex
                imageView_bgColor.setBackgroundColor(bgColorRgb)

                with (cardView_details) {
                    background = android.graphics.drawable.GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        intArrayOf(startColorRgb, endColorRgb)
                    ).apply {
                        this.cornerRadius = 20F
                    }
                    cardElevation = 20F
                    this.radius = 20F
                    setContentPadding(5, 5, 5, 5)
                }
            }
            close()
        }
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
                    else -> "Dialog will not be closed until user select the close button"
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
                showUsername = switch_showUsername.isChecked
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
                                val queryCursor = query(
                                    ContactsContract.Profile.CONTENT_URI,
                                    null,
                                    null,
                                    null,
                                    null
                                )
                                queryCursor.apply {
                                    this?.moveToFirst()
                                    username = try {
                                        this?.getString(this.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME)) ?: "My"
                                    } catch (e: CursorIndexOutOfBoundsException) {
                                        raiseLongToast("Username not available")
                                        "My"
                                    }
                                }
                                queryCursor?.close()
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
            val color = QuickSQL(this).run {
                val temp = getData().startColor
                close()
                temp
            }

            with(ColorDialog(this, "Select Gradient Start Color", color)) {
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
        * Gradient End Color: START
        * */
        button_changeEndColor.setOnClickListener {
            /*
            * updating gradient end color
            * */
            val color = QuickSQL(this).run {
                val temp = getData().endColor
                close()
                temp
            }

            with(ColorDialog(this, "Select Gradient End Color", color)) {
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
        * Dialog Background Color: START
        * */
        button_changeBgColor.setOnClickListener {
            /*
            * updating gradient end color
            * */
            val color = QuickSQL(this).run {
                val temp = getData().bgColor
                close()
                temp
            }

            with(ColorDialog(this, "Select Gradient End Color", color)) {
                button_colorPickerCancel.setOnClickListener { _ ->
                    dismiss()
                }

                button_colorPickerOk.setOnClickListener { _ ->
                    with(QuickSQL(this@MainActivity)) {
                        dialogBgColor = textView_hexCode.text as String
                        val hexCode = "#${textView_hexCode.text.subSequence(1, 7).toString().toUpperCase()}"
                        this@MainActivity.textView_bgColorHex.text = hexCode
                        this@MainActivity.imageView_bgColor.setBackgroundColor(Color.rgb(
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
    }

    private class ColorDialog(context: Activity, title: String, color: String) : Dialog(context) {
        init {
            setContentView(R.layout.color_picker_dialog)
            setTitle(title)

            // converting HexCode to RGB value
            seekBar_redComponent.progress = Integer.parseInt(color, 16) shr 16 and 0xFF
            seekBar_greenComponent.progress = Integer.parseInt(color, 16) shr 8 and 0xFF
            seekBar_blueComponent.progress = Integer.parseInt(color, 16) and 0xFF

            cardView_finalColor.setCardBackgroundColor(Color.rgb(
                seekBar_redComponent.progress,
                seekBar_greenComponent.progress,
                seekBar_blueComponent.progress
            ))

            // setting the initial value of color components in the TextViews
            textView_redComponent.text = String.format("#%02X", seekBar_redComponent.progress)
            textView_greenComponent.text = String.format("#%02X", seekBar_greenComponent.progress)
            textView_blueComponent.text = String.format("#%02X", seekBar_blueComponent.progress)

            textView_hexCode.text = String.format("#%02X%02X%02X", seekBar_redComponent.progress,
                seekBar_greenComponent.progress,seekBar_blueComponent.progress
            )

            seekBar_redComponent.setOnSeekBarChangeListener(ColorSeekBarChangeListener(
                imageView_redComponent,
                textView_redComponent,
                seekBar_redComponent
            ))

            seekBar_greenComponent.setOnSeekBarChangeListener(ColorSeekBarChangeListener(
                imageView_greenComponent,
                textView_greenComponent,
                seekBar_greenComponent
            ))

            seekBar_blueComponent.setOnSeekBarChangeListener(ColorSeekBarChangeListener(
                imageView_blueComponent,
                textView_blueComponent,
                seekBar_blueComponent
            ))
        }

        private inner class ColorSeekBarChangeListener(
            val imageView: ImageView,
            val textView: TextView,
            val seekBar: SeekBar
        ) : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                imageView.setBackgroundColor(Color.rgb(
                    seekBar.progress, 0,0
                ))

                cardView_finalColor.setCardBackgroundColor(Color.rgb(
                    seekBar_redComponent.progress,
                    seekBar_greenComponent.progress,
                    seekBar_blueComponent.progress
                ))
                textView.text = String.format(
                    "#%02X",
                    seekBar.progress
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
                textView.text = String.format(
                    "#%02X",
                    seekBar.progress
                )
                textView_hexCode.text = String.format(
                    "#%02X%02X%02X",
                    seekBar_redComponent.progress,
                    seekBar_greenComponent.progress,
                    seekBar_blueComponent.progress
                )
            }
        }
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