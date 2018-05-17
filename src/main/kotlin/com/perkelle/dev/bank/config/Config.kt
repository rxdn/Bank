package com.perkelle.dev.bank.config

import com.perkelle.dev.bank.Bank
import com.perkelle.dev.bank.backend.BackendType
import org.bukkit.ChatColor

fun getConfig() = Bank.instance.config

@FileName("config.yml")
class Config: YMLConfig() {

    fun getStorageType() = BackendType.values().firstOrNull { it.simpleName == config.getString("storage.type") }

    fun getDatabaseDetails() = DatabaseDetails(
            config.getString("storage.mysql.host"),
            config.getInt("storage.mysql.port"),
            config.getString("storage.mysql.username"),
            config.getString("storage.mysql.password"),
            config.getString("storage.mysql.database"),
            config.getString("storage.mysql.table-prefix")
    )

    fun getMessage(type: MessageType) = ChatColor.translateAlternateColorCodes('&', getConfigValue<String>("lang.${MessageType.PREFIX.configName}") + getConfigValue<String>("lang.${type.configName}"))

    data class DatabaseDetails(val host: String, val port: Int, val username: String, val password: String, val database: String, val tablePrefix: String)
}