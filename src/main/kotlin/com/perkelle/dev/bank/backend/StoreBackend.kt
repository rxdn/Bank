package com.perkelle.dev.bank.backend

import com.perkelle.dev.bank.Bank
import com.perkelle.dev.bank.utils.Callback
import org.bukkit.entity.Player
import java.util.*

fun getBackendProvider() = Bank.instance.backendProvider

interface StoreBackend {

    fun setup(): Boolean

    fun deposit(p: UUID, amount: Double)

    fun withdraw(p: UUID, amount: Double)

    fun setAmount(p: UUID, amount: Double)

    fun getBalance(p: UUID, callback: Callback<Double>)

    fun getUUID(name: String, callback: Callback<UUID?>)

    fun setUUID(name: String, uuid: UUID)

    fun getTop10(callback: Callback<TreeMap<String, Double>>)

    fun shutdown()
}