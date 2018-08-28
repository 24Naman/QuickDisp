package com.naman.quickdisp

import android.app.AlertDialog
import android.database.CursorIndexOutOfBoundsException
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.provider.ContactsContract
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.view.View
import kotlinx.android.synthetic.main.quick_display_contents.view.*
import kotlinx.android.synthetic.main.quick_display_title.view.*


/**
OnTileAdded: When the user adds the Tile in the Quick Settings
OnTileRemoved: When the user removes the Tile from the Quick Settings
OnTileClick: When the user clicks the Tile
OnStartListening: When the Tile becomes visible (this happens when you have already added the tile and you open the Quick Settings)
OnStopListening: When the Tile is no longer visible (when you close the Quick Settings)
 */

class QuickDispTileService : TileService() {
    private lateinit var quickSQLData: QuickSQLData

    override fun onStartListening() {
        super.onStartListening()

        val screenTimeoutDetails = getScreenTimeoutDetails()
        quickSQLData = QuickSQL(this).getData()
        val timeout = screenTimeoutDetails.first

        val messagePart = when (timeout) {
            15, 30 -> // seconds
                "$timeout Seconds"
            else -> {
                "${timeout/60} " + when (timeout/60) {
                    1 -> "Minute"
                    else -> "Minutes"
                }
            }
        }

        with (qsTile) {
            label = "$messagePart | ${screenTimeoutDetails.second}"
            icon = Icon.createWithResource(applicationContext, screenTimeoutDetails.third)
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()

        val titleBackground = View.inflate(
            this@QuickDispTileService,
            R.layout.quick_display_title,
            null
        )

        with(titleBackground.cardView_details) {
            // custom gradient colors
            background = android.graphics.drawable.GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    quickSQLData.startColorRgb,
                    quickSQLData.endColorRgb
                )
            ).apply {
                this.cornerRadius = 20F
            }
            cardElevation = 20F
            this.radius = 20F
            setContentPadding(5, 5, 5, 5)

            titleBackground.textView_userName.text = when (quickSQLData.showUserNameOnDialog) {
                true -> with(contentResolver) {
                    try {
                        this.query(
                            ContactsContract.Profile.CONTENT_URI,
                            null,
                            null,
                            null,
                            null
                        ).let {
                            it?.moveToFirst()
                            "${it?.getString(it.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME))}'s"
                        }
                    } catch (e: CursorIndexOutOfBoundsException) {
                        resources.getString(R.string.username)
                    }
                }
                else -> {
                    resources.getString(R.string.username)
                }
            }

            titleBackground.textView_deviceName.text = when (quickSQLData.showDeviceModelNumberOnDialog) {
                true -> when (android.os.Build.MODEL.startsWith(android.os.Build.BRAND)) {
                    true -> android.os.Build.MODEL
                    else -> "${android.os.Build.BRAND} ${android.os.Build.MODEL}"
                }
                else -> resources.getString(R.string.device_name)
            }
        }

        val dialogBackground = View.inflate(
            this@QuickDispTileService,
            R.layout.quick_display_contents,
            null
        )

        with(dialogBackground.cardView_dialogIcon) {
            // custom gradient colors
            background = android.graphics.drawable.GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    quickSQLData.endColorRgb,
                    quickSQLData.startColorRgb
                )
            ).apply {
                cornerRadius = 20F
            }
            cardElevation = 20F
            radius = 20F
            setContentPadding(5, 5, 5, 5)
        }

        with(dialogBackground.cardView_dialogButtons) {
            // custom gradient colors
            background = android.graphics.drawable.GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    quickSQLData.startColorRgb,
                    quickSQLData.endColorRgb
                )
            ).apply {
                this.cornerRadius = 20F
            }
            cardElevation = 20F
            this.radius = 20F
            setContentPadding(5, 5, 5, 5)
        }

        class ScreenTimeoutListener : View.OnClickListener {
            override fun onClick(p0: View?) {
                Settings.System.putInt(
                    contentResolver,
                    Settings.System.SCREEN_OFF_TIMEOUT,
                    dialogBackground.let {
                        when (p0) {
                            it.imageButton_fifteenSeconds -> 15000    // 15 Seconds
                            it.imageButton_thirtySeconds -> 30000  // 30 Seconds
                            it.imageButton_oneMinute -> 60000 // 1 Minute
                            it.imageButton_twoMinutes -> 120000   // 2 Minutes
                            it.imageButton_fiveMinutes -> 300000   // 5 Minutes
                            it.imageButton_tenMinutes -> 600000   // 10 Minutes
                            it.imageButton_thirtyMinutes -> 1800000   // 30 Minutes
                            else -> 30000  // 30 Seconds
                        }
                    }
                )
                when (quickSQLData.autoCloseDialog) {
                    true -> {
                        dialogBackground.imageButton_cancel.performClick()
                        return
                    }
                }
                initDialog(dialogBackground)
            }
        }

        // Displaying the generated dialog
        showDialog(
            with(AlertDialog.Builder(this)) {
                setCustomTitle(titleBackground)
                initDialog(dialogBackground)
                setView(dialogBackground)

                var createdDialog: AlertDialog? = null

                dialogBackground.run {
                    imageButton_thirtyMinutes.setOnClickListener(ScreenTimeoutListener())
                    imageButton_fifteenSeconds.setOnClickListener(ScreenTimeoutListener())
                    imageButton_thirtySeconds.setOnClickListener(ScreenTimeoutListener())
                    imageButton_oneMinute.setOnClickListener(ScreenTimeoutListener())
                    imageButton_twoMinutes.setOnClickListener(ScreenTimeoutListener())
                    imageButton_fiveMinutes.setOnClickListener(ScreenTimeoutListener())
                    imageButton_tenMinutes.setOnClickListener(ScreenTimeoutListener())

                    switch_autoBright.setOnCheckedChangeListener { _, b ->
                        Settings.System.putInt(
                            contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS_MODE,
                            when (b) {
                                true -> Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                                else -> Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                            }
                        )
                        initDialog(this)
                    }

                    createdDialog = create()

                    imageButton_cancel.setOnClickListener {
                        (createdDialog as AlertDialog).dismiss()
                    }
                }

                createdDialog
            }
        )
    }

    private fun getScreenTimeoutDetails(): Triple<Int, String, Int> {
        // timeout in seconds
        val timeout = Settings.System.getInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT) / 1000

        val brightnessMode = when (Settings.System.getInt(
            contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE
        )) {
            Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC -> "A"
            else -> "M"
        }

        val imageResource = when (brightnessMode) {
            "M" -> when (timeout) {
                15 -> R.drawable.manual_fifteen_seconds
                30 -> R.drawable.manual_thirty_seconds
                60 -> R.drawable.manual_one_minute
                120 -> R.drawable.manual_two_minutes
                300 -> R.drawable.manual_five_minutes
                600 -> R.drawable.manual_ten_minutes
                1800 -> R.drawable.manual_thirty_minutes
                else -> R.drawable.default_icon
            }
            "A" -> when (timeout) {
                15 -> R.drawable.auto_fifteen_seconds
                30 -> R.drawable.auto_thirty_seconds
                60 -> R.drawable.auto_one_minute
                120 -> R.drawable.auto_two_minutes
                300 -> R.drawable.auto_five_minutes
                600 -> R.drawable.auto_ten_minutes
                1800 -> R.drawable.auto_thirty_minutes
                else -> R.drawable.default_icon
            }
            else -> R.drawable.default_icon
        }
        return Triple(timeout, brightnessMode, imageResource)
    }


    private fun initDialog(dialogBackground: View) {
        val screenDetails = getScreenTimeoutDetails()

        // setting up the dialog
        when (screenDetails.second) {
            "A" -> {
                dialogBackground.apply {
                    imageButton_fifteenSeconds.setImageResource(R.drawable.auto_fifteen_seconds)
                    imageButton_thirtySeconds.setImageResource(R.drawable.auto_thirty_seconds)
                    imageButton_oneMinute.setImageResource(R.drawable.auto_one_minute)
                    imageButton_twoMinutes.setImageResource(R.drawable.auto_two_minutes)
                    imageButton_fiveMinutes.setImageResource(R.drawable.auto_five_minutes)
                    imageButton_tenMinutes.setImageResource(R.drawable.auto_ten_minutes)
                    imageButton_thirtyMinutes.setImageResource(R.drawable.auto_thirty_minutes)

                    switch_autoBright.isChecked = true

                    imageView_currentSetting.setImageResource(screenDetails.third)
                }
            }
            "M" -> {
                dialogBackground.apply {
                    imageButton_fifteenSeconds.setImageResource(R.drawable.manual_fifteen_seconds)
                    imageButton_thirtySeconds.setImageResource(R.drawable.manual_thirty_seconds)
                    imageButton_oneMinute.setImageResource(R.drawable.manual_one_minute)
                    imageButton_twoMinutes.setImageResource(R.drawable.manual_two_minutes)
                    imageButton_fiveMinutes.setImageResource(R.drawable.manual_five_minutes)
                    imageButton_tenMinutes.setImageResource(R.drawable.manual_ten_minutes)
                    imageButton_thirtyMinutes.setImageResource(R.drawable.manual_thirty_minutes)

                    switch_autoBright.isChecked = false

                    imageView_currentSetting.setImageResource(screenDetails.third)
                }
            }
        }
    }
}
