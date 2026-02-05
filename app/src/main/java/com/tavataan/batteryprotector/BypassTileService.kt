package com.tavataan.bypass

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BypassTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        
        qsTile.state = Tile.STATE_UNAVAILABLE
        qsTile.updateTile()

        serviceScope.launch {
            val isCurrentlyActive = RootHandler.isBypassActive()
            val newState = !isCurrentlyActive
            
            val success = RootHandler.setBypass(newState)

            withContext(Dispatchers.Main) {
                if (success) {
                    updateTileVisuals(newState)
                    // Mensajes traducidos
                    val mensaje = if (newState) getString(R.string.bypass_activated) else getString(R.string.bypass_deactivated)
                    Toast.makeText(applicationContext, mensaje, Toast.LENGTH_SHORT).show()
                } else {
                    updateTileVisuals(isCurrentlyActive)
                    // Error traducido
                    Toast.makeText(applicationContext, getString(R.string.tile_error_root), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateTileState() {
        serviceScope.launch {
            val isActive = RootHandler.isBypassActive()
            withContext(Dispatchers.Main) {
                updateTileVisuals(isActive)
            }
        }
    }

    private fun updateTileVisuals(isActive: Boolean) {
        val tile = qsTile ?: return
        
        if (isActive) {
            tile.state = Tile.STATE_ACTIVE
            // Etiqueta traducida
            tile.label = getString(R.string.bypass_on)
            tile.icon = android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_stat_bateria)
        } else {
            tile.state = Tile.STATE_INACTIVE
            // Etiqueta traducida
            tile.label = getString(R.string.bypass_off)
            tile.icon = android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_stat_bateria)
        }
        tile.updateTile()
    }
}