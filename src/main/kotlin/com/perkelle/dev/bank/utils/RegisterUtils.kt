package com.perkelle.dev.bank.utils

import com.perkelle.dev.bank.Bank
import com.perkelle.dev.bank.utils.RegisterUtils.Companion.cmdMap
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.SimpleCommandMap
import org.bukkit.plugin.java.JavaPlugin

fun Command.register() = getCommandMap().register(name, this)

private fun getCommandMap() : SimpleCommandMap {
    return if(cmdMap != null) cmdMap!!
    else {
        val map = JavaPlugin.getPlugin(Bank::class.java).server.javaClass.getDeclaredField("commandMap")
        map.isAccessible = true
        cmdMap = map.get(Bukkit.getServer()) as SimpleCommandMap
        cmdMap!!
    }
}

class RegisterUtils {
    companion object {
        var cmdMap: SimpleCommandMap? = null
    }
}