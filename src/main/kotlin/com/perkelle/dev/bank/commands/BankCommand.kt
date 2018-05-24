package com.perkelle.dev.bank.commands

import com.perkelle.dev.bank.backend.getBackendProvider
import com.perkelle.dev.bank.config.MessageType
import com.perkelle.dev.bank.config.getConfig
import com.perkelle.dev.bank.utils.addBalance
import com.perkelle.dev.bank.utils.getBalance
import com.perkelle.dev.bank.utils.removeBalance
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
                        p.sendMessage(getConfig().getMessage(MessageType.OWN_BALANCE).replace("%amount", balance.toString()))
                    }
                } else {
                    if(!p!!.hasPermission("bank.admin")) {
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
                            p.sendMessage(getConfig().getMessage(MessageType.OTHER_BALANCE).replace("%name", args[0]).replace("%amount", balance.toString()))
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

                        val amount = args[1].toDoubleOrNull()
                        if(amount == null) {
                            sender.sendMessage(getConfig().getMessage(MessageType.SPECIFY_AMOUNT))
                            return@getUUID
                        }

                        val target = Bukkit.getOfflinePlayer(uuid)
                        val balance = target.getBalance()

                        if(amount > balance) {
                            sender.sendMessage(getConfig().getMessage(MessageType.TOO_POOR_OTHER).replace("%name", args[0]))
                            return@getUUID
                        }

                        if(target.removeBalance(amount)) {
                            getBackendProvider().deposit(uuid, amount)
                            sender.sendMessage(getConfig().getMessage(MessageType.DEPOSIT_OTHER).replace("%name", args[0]).replace("%amount", amount.toString()))
                        } else {
                            sender.sendMessage(getConfig().getMessage(MessageType.ERROR))
                        }
                    }
                } else {
                    val amount = args[0].toDoubleOrNull()
                    if(amount == null) {
                        sender.sendMessage(getConfig().getMessage(MessageType.SPECIFY_AMOUNT))
                        return@subCommand
                    }

                    val balance = p!!.getBalance()
                    if(amount > balance) {
                        sender.sendMessage(getConfig().getMessage(MessageType.TOO_POOR))
                        return@subCommand
                    }

                    if(p.removeBalance(amount)) {
                        getBackendProvider().deposit(p.uniqueId, amount)
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

                    println(args.joinToString(" "))
                    val targetName = args[0].toLowerCase()
                    getBackendProvider().getUUID(targetName) { uuid ->
                        if(uuid == null) {
                            p.sendMessage(getConfig().getMessage(MessageType.NEVER_JOINED))
                            return@getUUID
                        }

                        val amount = args[1].toDoubleOrNull()
                        if(amount == null) {
                            sender.sendMessage(getConfig().getMessage(MessageType.SPECIFY_AMOUNT))
                            return@getUUID
                        }

                        val target = Bukkit.getOfflinePlayer(uuid)

                        getBackendProvider().getBalance(uuid) { balance ->
                            if(amount > balance) {
                                sender.sendMessage(getConfig().getMessage(MessageType.TOO_POOR_OTHER).replace("%name", args[0]))
                                return@getBalance
                            }

                            if(target.addBalance(amount)) {
                                getBackendProvider().withdraw(uuid, amount)
                                sender.sendMessage(getConfig().getMessage(MessageType.WITHDRAW_OTHER).replace("%name", args[0]).replace("%amount", amount.toString()))
                            } else {
                                sender.sendMessage(getConfig().getMessage(MessageType.ERROR))
                            }
                        }
                    }
                } else {
                    val amount = args[0].toDoubleOrNull()
                    if(amount == null) {
                        sender.sendMessage(getConfig().getMessage(MessageType.SPECIFY_AMOUNT))
                        return@subCommand
                    }

                    getBackendProvider().getBalance(p!!.uniqueId) { balance ->
                        if(amount > balance) {
                            sender.sendMessage(getConfig().getMessage(MessageType.TOO_POOR))
                            return@getBalance
                        }

                        if(p.addBalance(amount)) {
                            getBackendProvider().withdraw(p.uniqueId, amount)
                            sender.sendMessage(getConfig().getMessage(MessageType.WITHDRAW_OWN).replace("%amount", amount.toString()))
                        } else {
                            sender.sendMessage(getConfig().getMessage(MessageType.ERROR))
                        }
                    }
                }
            }

            subCommand("set", permission = "bank.admin") {
                if(args.isEmpty()) {
                    sender.sendMessage(getConfig().getMessage(MessageType.SPECIFY_PLAYER))
                    return@subCommand
                } else if(args.size == 1) {
                    sender.sendMessage(getConfig().getMessage(MessageType.SPECIFY_AMOUNT))
                    return@subCommand
                }

                val targetName = args[0].toLowerCase()
                getBackendProvider().getUUID(targetName) { uuid ->
                    if (uuid == null) {
                        sender.sendMessage(getConfig().getMessage(MessageType.NEVER_JOINED))
                        return@getUUID
                    }

                    val amount = args[1].toDoubleOrNull()
                    if (amount == null) {
                        sender.sendMessage(getConfig().getMessage(MessageType.SPECIFY_AMOUNT))
                        return@getUUID
                    }

                    getBackendProvider().setAmount(uuid, amount)

                    sender.sendMessage(getConfig().getMessage(MessageType.UPDATED_OTHER_BALANCE)
                            .replace("%name", targetName)
                            .replace("%amount", amount.toString()))
                    Bukkit.getPlayer(uuid)?.sendMessage(getConfig().getMessage(MessageType.UPDATED_BALANCE)
                            .replace("%amount", amount.toString()))
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