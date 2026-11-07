package com.example.naviglas.service

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat

class NaviGlasTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        val isActive = (tile.state == Tile.STATE_ACTIVE)

        val serviceIntent = Intent(this, VoiceAssistantService::class.java)
        if (isActive) {
            serviceIntent.action = VoiceAssistantService.ACTION_STOP_SERVICE
            try {
                stopService(serviceIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            tile.state = Tile.STATE_INACTIVE
        } else {
            serviceIntent.action = VoiceAssistantService.ACTION_START_SERVICE
            try {
                ContextCompat.startForegroundService(this, serviceIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            tile.state = Tile.STATE_ACTIVE
        }
        tile.updateTile()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        tile.label = "NaviGlas Vožnja"
        tile.state = Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
