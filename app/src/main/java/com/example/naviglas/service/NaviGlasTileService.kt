package com.example.naviglas.service
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.naviglas.MainActivity
class NaviGlasTileService : TileService() {
    override fun onStartListening() {
        qsTile?.apply { label = "NaviGlas"; state = Tile.STATE_INACTIVE; updateTile() }
    }
    override fun onClick() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("EXTRA_TRIGGER_MIC", true)
        if (Build.VERSION.SDK_INT >= 34) startActivityAndCollapse(PendingIntent.getActivity(this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        else { @Suppress("DEPRECATION") startActivityAndCollapse(intent) }
    }
}

