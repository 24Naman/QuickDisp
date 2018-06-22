package com.naman.quickdisp

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Menu
import android.view.MenuItem
import android.widget.SeekBar
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.color_picker_dialog.*

class MainActivity : Activity() {

    private lateinit var quickSQL: QuickSQL
    private lateinit var data: QuickSQLData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
                    this.setTitle("Reset App Settings")
                    this.setMessage("This will change the preferences and color to the default " +
                            "setting permanently.")
                    this.setIcon(
                        resources.getDrawable(
                            R.drawable.ic_reset_settings,
                            context.theme
                        )
                    )
                    val createdDialog = create()
                    this.setPositiveButton(R.string.dialog_yes) { _: DialogInterface, _: Int ->
                        com.naman.quickdisp.QuickSQL(this@MainActivity).apply {
                            resetData()
                            close()
                        }
                        initialize()
                    }
                    this.setNegativeButton(R.string.dialog_no) { _: DialogInterface, _: Int ->
                        createdDialog.dismiss()
                    }
                }.show()
            }
        }
        return true
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
                        switch_showUsername.isChecked = true
                    }
                    else -> {
                        Toast.makeText(
                            this,
                            "${permissions?.get(0)} Not Granted",
                            Toast.LENGTH_LONG
                        ).show()
                        switch_showUsername.isChecked = false
                    }
                }
            }
        }
    }

    private fun initializeSwitches() {
        quickSQL = QuickSQL(this)
        data = quickSQL.getData()
        // setting the state of switches
        data.apply {
            switch_showUsername.isChecked = showUserNameOnDialog
            switch_showDeviceName.isChecked = showDeviceModelNumberOnDialog
            switch_autoClose.isChecked = autoCloseDialog
        }
    }

    private fun initialize() {
        initializeSwitches()

        // start color gradient
        val startColorHex = "#${data.startColor}"
        textView_startColorHex.text = startColorHex
        // converting color from hex code to RGB format
        val colorStart = Integer.parseInt(data.endColor, 16)
        val startColorRed = colorStart shr 16 and 0xFF
        val startColorGreen = colorStart shr 8 and 0xFF
        val startColorBlue = colorStart and 0xFF

        imageView_startColor.setBackgroundColor(
            Color.rgb(
                startColorRed,
                startColorGreen,
                startColorBlue
            ))

        // end color gradient
        val endColorHex = "#${data.endColor}"
        textView_endColorHex.text = endColorHex
        val colorEnd = Integer.parseInt(data.endColor, 16)
        // converting color from hex code to RGB format
        val endColorRed = colorEnd shr 16 and 0xFF
        val endColorGreen = colorEnd shr 8 and 0xFF
        val endColorBlue = colorEnd and 0xFF

        imageView_endColor.setBackgroundColor(
            Color.rgb(
                endColorRed,
                endColorGreen,
                endColorBlue
            )
        )
    }

    private fun bindListeners() {
        switch_autoClose.setOnClickListener {
            QuickSQL(this).apply {
                updateAutoClose(switch_autoClose.isChecked)
                close()
            }
            Toast.makeText(
                this,
                when (switch_autoClose.isChecked) {
                    true -> "Dialog will be closed on selecting any option"
                    else -> "Username will not be closed until user select the close button"
                },
                Toast.LENGTH_SHORT
            ).show()
        }

        switch_showDeviceName.setOnCheckedChangeListener { _, boolean ->
            QuickSQL(this).apply {
                updateShowDeviceName(switch_showDeviceName.isChecked)
                close()
            }
            textView_deviceName.text = when (boolean) {
                true -> "${android.os.Build.BRAND} ${android.os.Build.MODEL}"
                else -> resources.getString(R.string.device_name)
            }
            Toast.makeText(
                this,
                when (switch_showDeviceName.isChecked) {
                    true -> "Device Name will be shown"
                    else -> "Device Name will not be shown"
                },
                Toast.LENGTH_SHORT
            ).show()
        }

        switch_showUsername.setOnCheckedChangeListener { _, boolean ->
            QuickSQL(this).apply {
                updateShowUsername(boolean)
                close()
            }
            Toast.makeText(
                this,
                when (boolean) {
                    true -> "Username will be shown"
                    else -> "Username will not be shown"
                },
                Toast.LENGTH_SHORT
            ).show()

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
                                    it.moveToFirst()
                                    username = it.getString(it.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME))
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
                                                requestPermissions(
                                                    arrayOf(Manifest.permission.READ_CONTACTS),
                                                    1234
                                                )
                                        }
                                        this.setNegativeButton(R.string.dialog_no) { _: DialogInterface, _: Int ->
                                                this@MainActivity.switch_showUsername.isChecked = false
                                        }
                                        create()
                                    }.show()
                                }
                            }
                        }
                    }
                    username      // Show Empty String when permission to contacts is not given
                }
                false -> {
                    username      // Show Empty String when switch is unchecked
                }
            }
        }

        button_changeStartColor.setOnClickListener {
            with(Dialog(this)) {
                this.setContentView(R.layout.color_picker_dialog)

                data = quickSQL.getData()

                seekBar_redComponent.progress = Integer.parseInt(data.startColor) shr 16 and 0xFF
                seekBar_greenComponent.progress = Integer.parseInt(data.startColor) shr 8 and 0xFF
                seekBar_blueComponent.progress = Integer.parseInt(data.startColor) and 0xFF

                cardView_finalColor.setCardBackgroundColor(Color.rgb(
                    seekBar_redComponent.progress,
                    seekBar_greenComponent.progress,
                    seekBar_blueComponent.progress
                ))

                seekBar_redComponent.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }
                })

                seekBar_greenComponent.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }
                })

                seekBar_blueComponent.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }
                })

                button_colorPickerCancel.setOnClickListener {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                button_colorPickerOk.setOnClickListener {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }
            }
        }

        button_changeEndColor.setOnClickListener {
            with(Dialog(this)) {
                this.setContentView(R.layout.color_picker_dialog)

                data = quickSQL.getData()

                seekBar_redComponent.progress = Integer.parseInt(data.endColor) shr 16 and 0xFF
                seekBar_greenComponent.progress = Integer.parseInt(data.endColor) shr 8 and 0xFF
                seekBar_blueComponent.progress = Integer.parseInt(data.endColor) and 0xFF

                cardView_finalColor.setCardBackgroundColor(Color.rgb(
                    seekBar_redComponent.progress,
                    seekBar_greenComponent.progress,
                    seekBar_blueComponent.progress
                ))

                seekBar_redComponent.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }
                })

                seekBar_greenComponent.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }
                })

                seekBar_blueComponent.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }
                })

                button_colorPickerCancel.setOnClickListener {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                button_colorPickerOk.setOnClickListener {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }
            }
        }
    }
}
