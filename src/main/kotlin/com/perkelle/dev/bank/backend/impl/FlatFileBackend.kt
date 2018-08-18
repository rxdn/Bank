package com.perkelle.dev.bank.backend.impl

import com.perkelle.dev.bank.Bank
import com.perkelle.dev.bank.backend.StoreBackend
import com.perkelle.dev.bank.config.FileName
import com.perkelle.dev.bank.config.YMLConfig
import com.perkelle.dev.bank.config.getConfig
import com.perkelle.dev.bank.utils.Callback
import com.perkelle.dev.bank.utils.getBalance
import com.perkelle.dev.bank.utils.onComplete
import kotlinx.coroutines.experimental.async
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import java.util.*
import kotlin.collections.ArrayList

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
        dataFile.save()
    }

    override fun withdraw(p: UUID, amount: Double) {
        val current = data.getDouble("balances.$p", 0.0)
        val updated = current - amount

        data.set("balances.$p", updated)
        dataFile.save()
    }

    override fun setAmount(p: UUID, amount: Double) {
        data.set("balances.$p", amount)
        dataFile.save()
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
        data.getConfigurationSection("uuid")?.getKeys(false)?.map { it to data.getString("uuid.$it") }?.filter { it.second != null }?.filter { it.second == uuid.toString() }?.forEach {
            if(it.first != name) {
                data.set("uuid.${it.first}", null)
            }
        }

        data.set("uuid.${name.toLowerCase()}", uuid.toString())
    }

    override fun getTop10(callback: Callback<List<Pair<String, Double>>>) {
        async {
            val all = data.getConfigurationSection("uuid").getKeys(false)
                    .map { it to UUID.fromString(data.getString("uuid.$it")) }
                    .mapNotNull {  (name, uuid) ->
                        val p = Bukkit.getOfflinePlayer(uuid)
                        if(p == null || p.name == null) return@mapNotNull null

                        val ecoBalance = Bukkit.getOfflinePlayer(uuid)?.getBalance() ?: 0.0
                        name to ecoBalance + dataFile.getConfigValue("balances.$uuid", 0.0) }
                    .sortedByDescending { it.second }

            val top10 by lazy {
                if(all.size > 10) all.subList(0, 10)
                else all
            }

            top10.toList()
        }.onComplete(callback)
    }

    override fun shutdown() {
        dataFile.save()
    }

    fun getPlayers() = data.getConfigurationSection("uuid").getKeys(false).map { it to UUID.fromString(data.getString("uuid.$it")) }.toMap()

    @FileName("data.yml")
    class DataFile: YMLConfig(Bank.instance.dataFolder)
}