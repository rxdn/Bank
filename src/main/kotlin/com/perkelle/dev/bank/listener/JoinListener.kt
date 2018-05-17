package com.perkelle.dev.bank.listener

import com.perkelle.dev.bank.backend.getBackendProvider
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class JoinListener: Listener {

    @EventHandler fun onJoin(e: PlayerJoinEvent) {
        val p = e.player
        getBackendProvider().setUUID(p.name, p.uniqueId)
    }
}