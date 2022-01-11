package me.schooltests.stroulette.chance

import me.schooltests.stroulette.RoulettePlugin
import me.schooltests.stroulette.util.*
import me.schooltests.stroulette.prefix
import me.ztowne13.customcrates.SpecializedCrates
import me.ztowne13.customcrates.crates.Crate
import me.ztowne13.customcrates.crates.CrateSettings
import me.ztowne13.customcrates.players.PlayerManager

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

private val scrates: SpecializedCrates
get() = RoulettePlugin.scrates!!

abstract class RouletteType(val abstractType: String, val type: String) {
    var displayName = type
    protected set

    abstract fun give(player: Player, amount: Int, message: Boolean = true)
    abstract fun take(player: Player, amount: Int, message: Boolean = true): Boolean

    abstract fun numberOf(player: Player): Int
    abstract fun item(amount: Int): ItemStack
}

class KeyType(type: String): RouletteType("key", type) {
    override fun give(player: Player, amount: Int, message: Boolean) {
        Bukkit.dispatchCommand(
            Bukkit.getConsoleSender(),
            "scrates givekey $type ${player.name} $amount -v"
        )

        if (message) {
            player.send("$prefix You won &6${amount}x ${type.capitalize()} key(s)&7!")
        }
    }

    override fun item(amount: Int): ItemStack {
        return ItemStack(Material.TRIPWIRE_HOOK)
            .name("&6${amount}x ${type.capitalize()} key(s)")
            .amount(1)
            .enchantment(Enchantment.DURABILITY)
            .flag(ItemFlag.HIDE_ENCHANTS)
    }

    override fun take(player: Player, amount: Int, message: Boolean): Boolean {
        val crate = Crate.getCrate(scrates, type)
        val crateSettings: CrateSettings = crate.settings
        for (i in 1..amount) {
            crateSettings.keyItemHandler.takeKeyFromPlayer(player, false)
        }

        if (message) {
            player.send("$prefix Taken &6${amount}x $displayName key(s) from you!")
        }

        return true
    }

    override fun numberOf(player: Player): Int {
        val pdm = PlayerManager.get(scrates, player).pdm
        val crate = Crate.getCrate(scrates, type)

        return pdm.getVCCrateData(crate).keys
    }
}

class SpawnerType(type: String): RouletteType("spawner", type) {
    val silkSpawnersType: String = when (type) {
        "PIGMAN" -> "zombiepigman"
        "IRON_GOLEM" -> "villagergolem"
        else -> type.toLowerCase()
    }

    val entityId: Short = when (type) {
        "CREEPER" -> 50
        "SKELETON" -> 51
        "ZOMBIE" -> 54
        "PIGMAN" -> 57
        "SILVERFISH" -> 60
        "BLAZE" -> 61
        "WITCH" -> 66
        "ENDERMITE" -> 67
        "PIG" -> 90
        "SHEEP" -> 91
        "COW" -> 92
        "CHICKEN" -> 93
        "IRON_GOLEM" -> 99
        "HORSE" -> 100
        "RABBIT" -> 101
        "VILLAGER" -> 120
        else -> 0
    }

    init {
        displayName = when (type) {
            "PIGMAN" -> "Zombie Pigman"
            "IRON_GOLEM" -> "Iron Golem"
            else -> type.toLowerCase().capitalize()
        }
    }

    override fun give(player: Player, amount: Int, message: Boolean) {
        Bukkit.dispatchCommand(
            Bukkit.getConsoleSender(),
            "silkspawners give ${player.name} $silkSpawnersType $amount"
        )

        if (message) {
            player.send("$prefix You won &6${amount}x $displayName spawner(s)&7!")
        }
    }

    override fun item(amount: Int): ItemStack {
        return ItemStack(Material.MOB_SPAWNER)
            .name("&6${amount}x $displayName spawner(s)")
            .amount(1)
            .enchantment(Enchantment.DURABILITY)
            .flag(ItemFlag.HIDE_ENCHANTS)
    }

    override fun take(player: Player, amount: Int, message: Boolean): Boolean {
        if (message) {
            player.send("$prefix Taken &6${amount}x $displayName spawner(s) from you!")
        }

        return SpawnerUtil.removeSpawners(player, this, amount)
    }

    override fun numberOf(player: Player): Int {
        return SpawnerUtil.getSpawners(player, this)
    }
}