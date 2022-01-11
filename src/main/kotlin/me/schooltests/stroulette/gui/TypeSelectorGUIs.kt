package me.schooltests.stroulette.gui

import me.schooltests.stroulette.RoulettePlugin
import me.schooltests.stroulette.chance.KeyType
import me.schooltests.stroulette.chance.RouletteType
import me.schooltests.stroulette.chance.SpawnerType
import me.schooltests.stroulette.gui.BaseGUI
import me.schooltests.stroulette.prefix
import me.schooltests.stroulette.util.capitalize
import me.schooltests.stroulette.util.lore
import me.schooltests.stroulette.util.name
import me.schooltests.stroulette.util.send
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.lang.IllegalArgumentException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class MainRouletteGUI(player: Player) : BaseGUI(
    Bukkit.createInventory(null, 3*9, ChatColor.translateAlternateColorCodes('&', "&e&lRoulette Selector")),
    player
) {
    init {
        fill()

        setItem(12, ItemStack(Material.MOB_SPAWNER)
            .name("&6&lSpawner")
            .lore("&7Click to view spawner")
            .lore("&7roulette options.")) {
            RouletteOptionsGUI(
                player,
                "spawner",
                Material.MOB_SPAWNER,
                execute = { SelectorGUI(player, it, "spawner").open() }
            ).open()
        }

        setItem(14, ItemStack(Material.TRIPWIRE_HOOK)
            .name("&6&lKey")
            .lore("&7Click to view key")
            .lore("&7roulette options.")) {
            RouletteOptionsGUI(
                player,
                "key",
                Material.TRIPWIRE_HOOK,
                execute = { SelectorGUI(player, it, "key").open() }
            ).open()
        }
    }
}

class RouletteOptionsGUI(player: Player, type: String, material: Material, execute: (String) -> (Unit)) : BaseGUI(
    Bukkit.createInventory(null, 3*9, ChatColor.translateAlternateColorCodes('&', "&e&l${type.capitalize()} Roulette")),
    player
) {
    init {
        fill()

        setItem(12, ItemStack(material)
            .name("&6&lReroll ${type.capitalize()}")
            .lore("&7Try your luck at getting")
            .lore("&7a different type of ${type}.")
            .lore("")
            .lore("&7Price: &6$${RoulettePlugin.instance!!.config.getInt("prices.reroll-${type}")}")
        ) {
            execute.invoke("reroll")
        }

        setItem(14, ItemStack(Material.GOLD_INGOT)
            .name("&6&lTry Your Luck")
            .lore("&7Try increasing the number")
            .lore("&7of ${type}s you have!")
            .lore("")
            .lore("&7Price: &6$${RoulettePlugin.instance!!.config.getInt("prices.reroll-${type}")} &7& &61x ${type.capitalize()} Ticket")
            .lore("&c&lDISABLED")
        ) {
            execute.invoke("dupe")
        }
    }
}

class SelectorGUI(player: Player, private val action: String, private val abstractType: String) : BaseGUI(
    Bukkit.createInventory(null, 4*9, ChatColor.translateAlternateColorCodes('&', "&e&l${abstractType.capitalize()} Selection")),
    player
) {
    companion object {
        val MULTI_ROLL_TIPS_GIVEN = mutableMapOf<UUID, Instant>()
    }

    init {
        fill()
        for (i in 10..16) setItem(i, ItemStack(Material.AIR))
        for (i in 19..25) setItem(i, ItemStack(Material.AIR))

        val orderedList = RoulettePlugin.instance!!.orderConfig.getStringList(abstractType + "s")
        for ((slot, type) in orderedList.withIndex()) {
            var slotOffset = 0
            if (slot >= 7) slotOffset = 2

            val rollableType = when (abstractType) {
                "key" -> KeyType(type)
                "spawner" -> SpawnerType(type)
                else -> throw IllegalArgumentException("Unknown abstract type: $abstractType")
            }

            setItem(slot + 10 + slotOffset, rollableType.item(1)
                .lore("&7Click to roll a")
                .lore("&e${rollableType.displayName.capitalize()} &7${abstractType}!")
            ) {
                player.closeInventory()
                Bukkit.dispatchCommand(player, "roulette ${action}${abstractType}s ${rollableType.type} 1")

                val instant = MULTI_ROLL_TIPS_GIVEN[player.uniqueId] ?: Instant.now()
                if (instant.toEpochMilli() <= Instant.now().toEpochMilli()) {
                    player.send("$prefix Did you know that you can roll multiple keys or spawners with the commands? Try /roulette help")
                    MULTI_ROLL_TIPS_GIVEN[player.uniqueId] = Instant.now().plus(3, ChronoUnit.HOURS)
                }
            }
        }
    }
}