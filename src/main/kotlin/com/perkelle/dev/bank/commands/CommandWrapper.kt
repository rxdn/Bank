package com.perkelle.dev.bank.commands

import com.perkelle.dev.bank.config.MessageType
import com.perkelle.dev.bank.config.getConfig
import com.perkelle.dev.bank.utils.register
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

inline fun command(name: String, block: CommandBuilder.() -> Unit) = CommandBuilder(name).also(block)

class CommandBuilder(val cmdName: String): Command(cmdName) {

    private lateinit var rootCommand: Pair<StoredCommand, CommandContext.() -> Unit>
    val subcommands = mutableMapOf<StoredCommand, CommandContext.() -> Unit>()

    override fun execute(sender: CommandSender, label: String, args: Array<String>): Boolean {
        if(args.isEmpty() || subcommands.keys.none { it.name.equals(args[0], true) }) {
            val rootContext = rootCommand.first
            val permission = rootContext.permission
            val playerOnly = rootContext.playerOnly

            if(permission.isNotEmpty() && !sender.hasPermission(permission)) {
                sender.sendMessage(getConfig().getMessage(MessageType.NO_PERMISSION))
                return true
            }

            if(rootContext.playerOnly && sender !is Player) {
                sender.sendMessage(getConfig().getMessage(MessageType.PLAYER_ONLY))
                return true
            }

            val player = when(playerOnly) {
                true -> sender as Player
                false -> null
            }

            rootCommand.second(CommandContext(sender, label, args, player))
        } else {
            val subCommand = args[0].toLowerCase()
            val subArgs = args.toList().subList(1, args.size).toTypedArray()

            val data = subcommands.keys.first { subCommand.equals(it.name, true) }
            val context = subcommands[data]
            val playerOnly = data.playerOnly

            if(data.permission.isNotEmpty() && !sender.hasPermission(data.permission)) {
                sender.sendMessage(getConfig().getMessage(MessageType.NO_PERMISSION))
                return true
            }

            if(data.playerOnly && sender !is Player) {
                sender.sendMessage(getConfig().getMessage(MessageType.NO_PERMISSION))
                return true
            }

            val player = when(playerOnly) {
                true -> sender as Player
                false -> null
            }

            context?.invoke(CommandContext(sender, subCommand, subArgs, player))
        }
        return true
    }

    fun root(playerOnly: Boolean = false, permission: String = "", register: Boolean = true, method: CommandContext.() -> Unit) {
        rootCommand = StoredCommand(cmdName, playerOnly, permission) to method
        if(register) register()
    }

    fun subCommand(argument: String, playerOnly: Boolean = false, permission: String = "", subAliases: Array<String> = arrayOf(), method: CommandContext.() -> Unit) {
        subcommands[StoredCommand(argument, playerOnly, permission)] = method
        subAliases.forEach { subcommands[StoredCommand(it, playerOnly, permission)] = method }
    }

    fun aliases(vararg aliases: String) {
        this.aliases = aliases.toMutableList()
    }

    fun usage(usage: String) {
        this.usage = usage
    }

    data class StoredCommand(val name: String, val playerOnly: Boolean = false, val permission: String = "")
}

interface ICommand {

    fun register()
}

data class CommandContext(val sender: CommandSender, val command: String, val args: Array<String>, val p: Player?)
