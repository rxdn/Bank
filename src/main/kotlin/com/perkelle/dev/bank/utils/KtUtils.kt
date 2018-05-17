package com.perkelle.dev.bank.utils

import com.perkelle.dev.bank.Bank
import kotlinx.coroutines.experimental.Deferred
import org.bukkit.Bukkit

fun<T> Deferred<T>.onComplete(block: (T) -> Unit) = invokeOnCompletion { block(getCompleted()) }

fun Double.roundDown() = Math.floor(this).toInt()

fun sync(block: () -> Unit) = Bukkit.getScheduler().runTask(Bank.instance, block)