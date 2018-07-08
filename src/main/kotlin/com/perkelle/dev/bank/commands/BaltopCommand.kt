package com.perkelle.dev.bank.commands

import com.perkelle.dev.bank.backend.getBackendProvider
import com.perkelle.dev.bank.config.MessageType
import com.perkelle.dev.bank.config.getConfig
import java.text.DecimalFormat

class BaltopCommand: ICommand {

    private val df = DecimalFormat("###,###,###,###,###.##")

    override fun register() {
        command("baltop") {
            root(permission = "bank.player", register = getConfig().overrideBaltop()) {
                sender.sendMessage(getConfig().getMessage(MessageType.BALTOP_FIRST))

                getBackendProvider().getTop10 {
                    var index = 1 //Beautify for players

                    it.toMap().forEach { name, balance ->
                        sender.sendMessage(getConfig().getMessage(MessageType.BALTOP_LINE).replace("%number", index.toString()).replace("%name", name).replace("%amount", df.format(balance)))
                        index += 1
                    }
                }
            }
        }
    }
}