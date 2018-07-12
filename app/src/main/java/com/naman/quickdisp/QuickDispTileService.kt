package com.naman.quickdisp

import android.app.AlertDialog
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.provider.ContactsContract
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.view.View
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

    private fun getScreenTimeoutDetails(): Triple<String, String, Int> {
        // timeout in seconds
        val timeout = Settings.System.getInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT) / 1000
        val finalTimeout = when (timeout) {
            15, 30 -> // seconds
                "$timeout Seconds"
            else -> {
                "${timeout/60} " + when (timeout/60) {
                    1 -> "Minute"
                    else -> "Minutes"
                }
            }
        }

        val brightnessMode = when (Settings.System.getInt(
            contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE
        )) {
            Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC -> "A"
            else -> "M"
        }

        val imageResource = when (brightnessMode) {
            "M" -> when (timeout){
                15 -> R.drawable.manual_fifteen_seconds
                30 -> R.drawable.manual_thirty_seconds
                60 -> R.drawable.manual_one_minute
                120 -> R.drawable.manual_two_minutes
                300 -> R.drawable.manual_five_minutes
                600 -> R.drawable.manual_ten_minutes
                1000 -> R.drawable.manual_five_minutes
                else -> R.drawable.default_icon
            }
            "A" -> when (timeout){
                15 -> R.drawable.auto_fifteen_seconds
                30 -> R.drawable.auto_thirty_seconds
                60 -> R.drawable.auto_one_minute
                120 -> R.drawable.auto_two_minutes
                300 -> R.drawable.auto_five_minutes
                600 -> R.drawable.auto_ten_minutes
                1000 -> R.drawable.auto_thirty_minutes
                else -> R.drawable.default_icon
            }
            else -> R.drawable.default_icon
        }
        return Triple(finalTimeout, brightnessMode, imageResource)
    }

    override fun onStartListening() {
        super.onStartListening()

        val screenTimeoutDetails = getScreenTimeoutDetails()
        quickSQLData = QuickSQL(this).getData()

        with (qsTile) {
            label = "${screenTimeoutDetails.first} | ${screenTimeoutDetails.second}"
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
                    this.query(
                        ContactsContract.Profile.CONTENT_URI,
                        null,
                        null,
                        null,
                        null
                    ).let {
                        it.moveToFirst()
                        it.getString(it.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME))
                    }
                }
                else -> {
                    resources.getString(R.string.username)
                }
            }

            titleBackground.textView_deviceName.text = when (quickSQLData.showDeviceModelNumberOnDialog) {
                true -> "${android.os.Build.BRAND} ${android.os.Build.MODEL}"
                else -> resources.getString(R.string.device_name)
            }
        }

        val generatedDialog = AlertDialog.Builder(this).apply {
            this.setCustomTitle(titleBackground)
        }.create()

        // Displaying the generated dialog
        showDialog(generatedDialog)
    }
}
