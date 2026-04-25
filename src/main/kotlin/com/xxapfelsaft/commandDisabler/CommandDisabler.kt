package com.xxapfelsaft.commandDisabler

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.plugin.java.JavaPlugin

class CommandDisabler : JavaPlugin(), Listener, CommandExecutor {

    override fun onEnable() {
        saveDefaultConfig()
        server.pluginManager.registerEvents(this, this)
        getCommand("commanddisabler")?.setExecutor(this)
        logger.info("CommandDisabler enabled!")
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) return false

        val miniMessage = MiniMessage.miniMessage()

        when (args[0].lowercase()) {
            "reload" -> {
                if (!sender.hasPermission("commanddisabler.reload")) {
                    sender.sendMessage(miniMessage.deserialize("<red>No permission!</red>"))
                    return true
                }
                reloadConfig()
                sender.sendMessage(miniMessage.deserialize("<green>Configuration reloaded!</green>"))
                return true
            }
            "add" -> {
                if (!sender.hasPermission("commanddisabler.add")) {
                    sender.sendMessage(miniMessage.deserialize("<red>No permission!</red>"))
                    return true
                }
                if (args.size < 3) {
                    sender.sendMessage(miniMessage.deserialize("<red>Usage: /commanddisabler add <command> <permission></red>"))
                    return true
                }
                val cmdToAdd = args[1].lowercase().removePrefix("/")
                val permission = args[2]

                config.set("disabled-commands.$cmdToAdd", permission)
                saveConfig()
                sender.sendMessage(miniMessage.deserialize("<green>Command '<white>$cmdToAdd</white>' added with permission '<white>$permission</white>'.</green>"))
                return true
            }
        }
        return false
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onCommandPreprocess(event: PlayerCommandPreprocessEvent) {
        val player = event.player
        if (player.isOp) return

        val fullMessage = event.message.lowercase().substring(1) // remove /
        val commandPart = fullMessage.split(" ")[0]
        
        // Handle namespaced commands like /minecraft:tp
        val command = if (commandPart.contains(":")) {
            commandPart.split(":")[1]
        } else {
            commandPart
        }

        val disabledCommands = config.getConfigurationSection("disabled-commands") ?: return
        
        if (disabledCommands.contains(command)) {
            val bypassPermission = disabledCommands.getString(command)
            
            if (bypassPermission != null && !player.hasPermission(bypassPermission)) {
                event.isCancelled = true
                val noPermissionMessage = config.getString("no-permission-message") ?: "<red>You do not have permission!</red>"
                player.sendMessage(MiniMessage.miniMessage().deserialize(noPermissionMessage))
            }
        }
    }
}
