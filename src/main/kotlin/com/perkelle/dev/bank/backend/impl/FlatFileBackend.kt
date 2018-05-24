package com.perkelle.dev.bank.backend.impl

import com.perkelle.dev.bank.Bank
import com.perkelle.dev.bank.backend.StoreBackend
import com.perkelle.dev.bank.config.FileName
import com.perkelle.dev.bank.config.YMLConfig
import com.perkelle.dev.bank.config.getConfig
import com.perkelle.dev.bank.utils.Callback
import com.perkelle.dev.bank.utils.getBalance
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import java.util.*

class FlatFileBackend: StoreBackend {

    private lateinit var dataFile: DataFile
    private lateinit var data: YamlConfiguration

    override fun setup(): Boolean {
        return try {
            dataFile = DataFile()
            dataFile.load()

            data = dataFile.config
            true
        } catch(ex: Exception) {
            ex.printStackTrace()
            false
        }
    }

    override fun deposit(p: UUID, amount: Double) {
        val current = data.getDouble("balances.$p", 0.0)
        val updated = current + amount

        data.set("balances.$p", updated)
    }

    override fun withdraw(p: UUID, amount: Double) {
        val current = data.getDouble("balances.$p", 0.0)
        val updated = current - amount

        data.set("balances.$p", updated)
    }

    override fun setAmount(p: UUID, amount: Double) {
        data.set("balances.$p", amount)
    }

    override fun getBalance(p: UUID, callback: Callback<Double>) {
        val amount = data.getDouble("balances.$p", 0.0)
        callback(amount)
    }

    override fun getUUID(name: String, callback: Callback<UUID?>) {
        val uuidStr = dataFile.getConfigValue<String>("uuid.${name.toLowerCase()}")

        if(uuidStr == null) callback(null)
        else callback(UUID.fromString(uuidStr))
    }

    private fun getName(uuid: String) = data.getConfigurationSection("uuid").getKeys(false).first { data.getString("uuid.$it") == uuid }

    override fun setUUID(name: String, uuid: UUID) {
        data.set("uuid.${name.toLowerCase()}", uuid.toString())
    }

    override fun getTop10(callback: Callback<TreeMap<String, Double>>) {
        val map = TreeMap<String, Double>()

        val all = data.getConfigurationSection("balances").getKeys(false)
                .map { getName(it) to data.getDouble("balances.$it", 0.0) + Bukkit.getOfflinePlayer(UUID.fromString(it)).getBalance() }
                .sortedByDescending { it.second }

        val top10 by lazy {
            if(all.size > 10) all.subList(0, 9)
            else all.subList(0, all.size)
        }

        map.putAll(top10.toMap())
        callback(map)
    }

    override fun shutdown() {
        dataFile.save()
    }

    @FileName("data.yml")
    class DataFile: YMLConfig(Bank.instance.dataFolder)
}