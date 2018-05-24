package com.perkelle.dev.bank.commands

import com.perkelle.dev.bank.backend.getBackendProvider
import com.perkelle.dev.bank.config.MessageType
import com.perkelle.dev.bank.config.getConfig

class BaltopCommand: ICommand {

    override fun register() {
        command("baltop") {
            root(permission = "bank.player", register = getConfig().overrideBaltop()) {
                getBackendProvider().getTop10 {
                    sender.sendMessage(getConfig().getMessage(MessageType.BALTOP_FIRST))

                    var index = 1 //Beautify for players
                    it.forEach { name, balance ->
                        sender.sendMessage(getConfig().getMessage(MessageType.BALTOP_LINE).replace("%number", index.toString()).replace("%name", name).replace("%amount", balance.toString()))
                        index += 1
                    }
                }
            }
        }
    }
}