package com.perkelle.dev.bank

import com.perkelle.dev.bank.backend.BackendType
import com.perkelle.dev.bank.backend.StoreBackend
import com.perkelle.dev.bank.backend.getBackendProvider
import com.perkelle.dev.bank.backend.impl.DatabaseBackend
import com.perkelle.dev.bank.backend.impl.FlatFileBackend
import com.perkelle.dev.bank.commands.BankCommand
import com.perkelle.dev.bank.config.Config
import com.perkelle.dev.bank.listener.JoinListener
import net.milkbowl.vault.economy.Economy
import org.bukkit.plugin.java.JavaPlugin

class Bank: JavaPlugin() {

    companion object {
        lateinit var instance: Bank
    }

    init {
        instance = this
    }

    lateinit var backendProvider: StoreBackend
    lateinit var config: Config
    var economy: Economy? = null

    override fun onEnable() {
        config = Config()
        config.load()

        backendProvider = when {
            config.getStorageType() == BackendType.FLATFILE -> FlatFileBackend()
            config.getStorageType() == BackendType.FLATFILE -> DatabaseBackend()
            else -> {
                logger.severe("Invalid backend type (should be 'file' or 'mysql'). Defaulting to file.")
                FlatFileBackend()
            }
        }

        if(!backendProvider.setup()) {
            logger.severe("Failed to connect to backend. Shutting down.")
            server.pluginManager.disablePlugin(this)
            return
        }

        setupEconomy()
        if(economy == null) {
            logger.severe("Failed to load Vault economy, disabling")
            server.pluginManager.disablePlugin(this)
            return
        }

        server.pluginManager.registerEvents(JoinListener(), this)
        BankCommand().register()
    }

    override fun onDisable() {
        getBackendProvider().shutdown()
    }

    private fun setupEconomy(): Boolean {
        if(server.pluginManager.getPlugin("Vault") == null) {
            logger.severe("Vault is not installed! Disabling...")
            server.pluginManager.disablePlugin(this)
            return false
        }

        val rsp = server.servicesManager.getRegistration(Economy::class.java) ?: return false
        economy = rsp.provider

        return economy != null
    }
}