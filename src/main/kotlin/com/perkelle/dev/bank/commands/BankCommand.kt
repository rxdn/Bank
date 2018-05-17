package com.perkelle.dev.bank.commands

import com.perkelle.dev.bank.backend.getBackendProvider
import com.perkelle.dev.bank.config.MessageType
import com.perkelle.dev.bank.config.getConfig
import com.perkelle.dev.bank.utils.addBalance
import com.perkelle.dev.bank.utils.getBalance
import com.perkelle.dev.bank.utils.removeBalance
import com.perkelle.dev.bank.utils.roundDown
import org.bukkit.Bukkit

class BankCommand: ICommand {

    override fun register() {
        command("bank") {
            root(false, "bank.player") {
                sender.sendMessage(getConfig().getMessage(MessageType.HELP))
            }

            subCommand("balance", true, "bank.player", arrayOf("bal")) {
                if(args.isEmpty()) {
                    getBackendProvider().getBalance(p!!.uniqueId) { balance ->
                        p.sendMessage(getConfig().getMessage(MessageType.OWN_BALANCE).replace("%amount", balance.roundDown().toString()))
                    }
                } else {
                    if(p!!.hasPermission("bank.admin")) {
                        p.sendMessage(getConfig().getMessage(MessageType.NO_PERMISSION))
                        return@subCommand
                    }

                    val name = args[0].toLowerCase()

                    getBackendProvider().getUUID(name) { uuid ->
                        if(uuid == null) {
                            p.sendMessage(getConfig().getMessage(MessageType.NEVER_JOINED))
                            return@getUUID
                        }

                        getBackendProvider().getBalance(uuid) { balance ->
                            p.sendMessage(getConfig().getMessage(MessageType.OTHER_BALANCE).replace("%name", args[0]).replace("%amount", balance.roundDown().toString()))
                        }
                    }
                }
            }

            subCommand("deposit", true, "bank.player") {
                if(args.isEmpty()) {
                    sender.sendMessage(getConfig().getMessage(MessageType.SPECIFY_AMOUNT))
                } else if(args.size == 2) {
                    if(!p!!.hasPermission("bank.admin")) {
                        p.sendMessage(getConfig().getMessage(MessageType.NO_PERMISSION))
                        return@subCommand
                    }

                    val targetName = args[0].toLowerCase()
                    getBackendProvider().getUUID(targetName) { uuid ->
                        if(uuid == null) {
                            p.sendMessage(getConfig().getMessage(MessageType.NEVER_JOINED))
                            return@getUUID
                        }

                        val amount = args[1].toIntOrNull()
                        if(amount == null) {
                            sender.sendMessage(getConfig().getMessage(MessageType.SPECIFY_AMOUNT))
                            return@getUUID
                        }

                        val target = Bukkit.getOfflinePlayer(uuid)
                        val balance = target.getBalance()

                        if(amount > balance) {
                            sender.sendMessage(getConfig().getMessage(MessageType.TOO_POOR_OTHER))
                            return@getUUID
                        }

                        if(target.removeBalance(amount.toDouble())) {
                            getBackendProvider().deposit(uuid, amount.toDouble())
                            sender.sendMessage(getConfig().getMessage(MessageType.DEPOSIT_OTHER).replace("%name", args[0]).replace("%amount", amount.toString()))
                        } else {
                            sender.sendMessage(getConfig().getMessage(MessageType.ERROR))
                        }
                    }
                } else {
                    val amount = args[0].toIntOrNull()
                    if(amount == null) {
                        sender.sendMessage(getConfig().getMessage(MessageType.SPECIFY_AMOUNT))
                        return@subCommand
                    }

                    val balance = p!!.getBalance()
                    if(amount > balance) {
                        sender.sendMessage(getConfig().getMessage(MessageType.TOO_POOR))
                        return@subCommand
                    }

                    if(p.removeBalance(amount.toDouble())) {
                        getBackendProvider().deposit(p.uniqueId, amount.toDouble())
                        sender.sendMessage(getConfig().getMessage(MessageType.DEPOSIT_OWN).replace("%amount", amount.toString()))
                    } else {
                        sender.sendMessage(getConfig().getMessage(MessageType.ERROR))
                    }
                }
            }

            subCommand("withdraw", true, "bank.player") {
                if(args.isEmpty()) {
                    sender.sendMessage(getConfig().getMessage(MessageType.SPECIFY_AMOUNT))
                } else if(args.size == 2) {
                    if(!p!!.hasPermission("bank.admin")) {
                        p.sendMessage(getConfig().getMessage(MessageType.NO_PERMISSION))
                        return@subCommand
                    }

                    val targetName = args[0].toLowerCase()
                    getBackendProvider().getUUID(targetName) { uuid ->
                        if(uuid == null) {
                            p.sendMessage(getConfig().getMessage(MessageType.NEVER_JOINED))
                            return@getUUID
                        }

                        val amount = args[1].toIntOrNull()
                        if(amount == null) {
                            sender.sendMessage(getConfig().getMessage(MessageType.SPECIFY_AMOUNT))
                            return@getUUID
                        }

                        val target = Bukkit.getOfflinePlayer(uuid)

                        getBackendProvider().getBalance(p.uniqueId) { balance ->
                            if(amount > balance) {
                                sender.sendMessage(getConfig().getMessage(MessageType.TOO_POOR_OTHER))
                                return@getBalance
                            }

                            if(target.addBalance(amount.toDouble())) {
                                getBackendProvider().deposit(uuid, amount.toDouble())
                                sender.sendMessage(getConfig().getMessage(MessageType.WITHDRAW_OWN).replace("%name", args[0]).replace("%amount", amount.toString()))
                            } else {
                                sender.sendMessage(getConfig().getMessage(MessageType.ERROR))
                            }
                        }
                    }
                } else {
                    val amount = args[0].toIntOrNull()
                    if(amount == null) {
                        sender.sendMessage(getConfig().getMessage(MessageType.SPECIFY_AMOUNT))
                        return@subCommand
                    }

                    getBackendProvider().getBalance(p!!.uniqueId) { balance ->
                        if(amount > balance) {
                            sender.sendMessage(getConfig().getMessage(MessageType.TOO_POOR))
                            return@getBalance
                        }

                        if(p.addBalance(amount.toDouble())) {
                            getBackendProvider().withdraw(p.uniqueId, amount.toDouble())
                            sender.sendMessage(getConfig().getMessage(MessageType.WITHDRAW_OWN).replace("%amount", amount.toString()))
                        } else {
                            sender.sendMessage(getConfig().getMessage(MessageType.ERROR))
                        }
                    }
                }
            }
        }

        command("bb") {
            root {
                Bukkit.getServer().dispatchCommand(sender, "bank balance ${args.joinToString(" ")}")
            }
        }

        command("bd") {
            root {
                Bukkit.getServer().dispatchCommand(sender, "bank deposit ${args.joinToString(" ")}")
            }
        }

        command("bw") {
            root {
                Bukkit.getServer().dispatchCommand(sender, "bank withdraw ${args.joinToString(" ")}")
            }
        }
    }
}