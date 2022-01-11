package me.schooltests.stroulette.util

import org.bukkit.ChatColor
import org.bukkit.command.CommandSender

fun CommandSender.send(message: String) {
    this.sendMessage(ChatColor.translateAlternateColorCodes('&', message))
}