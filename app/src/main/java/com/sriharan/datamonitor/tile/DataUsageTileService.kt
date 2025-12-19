package com.sriharan.datamonitor.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log

class DataUsageTileService : TileService() {

    override fun onTitleChanged(requestCode: Int) {
        super.onTitleChanged(requestCode)
        // Update tile state
    }

    override fun onStartListening() {
        super.onStartListening()
        // Update tile when it becomes visible
        val tile = qsTile
        tile.state = Tile.STATE_ACTIVE
        tile.label = "Data: --" // Placeholder
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        Log.d("DataUsageTile", "Tile clicked")
        // TODO: Handle click (expand or open app)
    }
}
