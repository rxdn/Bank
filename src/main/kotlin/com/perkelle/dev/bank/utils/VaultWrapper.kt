package com.perkelle.dev.bank.utils

import com.perkelle.dev.bank.Bank
import org.bukkit.OfflinePlayer

fun OfflinePlayer.addBalance(amount: Double) = getEcon().depositPlayer(this, amount).transactionSuccess()
fun OfflinePlayer.removeBalance(amount: Double) = getEcon().withdrawPlayer(this, amount).transactionSuccess()
fun OfflinePlayer.getBalance() = getEcon().getBalance(this)

fun getEcon() = Bank.instance.economy!!