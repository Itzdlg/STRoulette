package me.schooltests.stroulette

import cloud.commandframework.CommandManager
import cloud.commandframework.bukkit.BukkitCommandManager
import cloud.commandframework.execution.CommandExecutionCoordinator
import io.leangen.geantyref.TypeToken
import me.schooltests.stroulette.chance.ChanceManager
import me.schooltests.stroulette.chance.KeyType
import me.schooltests.stroulette.chance.SpawnerType
import me.schooltests.stroulette.commands.KeyTypeArgument
import me.schooltests.stroulette.commands.SpawnerTypeArgument
import me.schooltests.stroulette.commands.UserCommands
import me.schooltests.stroulette.gui.BaseGUI
import me.schooltests.stroulette.gui.animations.DuplicateAnyGUI
import me.schooltests.stroulette.gui.animations.RerollAnyGUI
import me.schooltests.stroulette.util.WeightedRandom
import me.schooltests.stroulette.util.send
import me.ztowne13.customcrates.SpecializedCrates
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.function.Function

val prefix = ChatColor.translateAlternateColorCodes('&', "&8[&e&lChaos&6&lRoulette&8]&7")
class RoulettePlugin : JavaPlugin() {
    companion object {
        var instance: RoulettePlugin? = null
        private set

        var scrates: SpecializedCrates? = null
        private set

        var economy: Economy? = null
        private set
    }

    private var manager: CommandManager<CommandSender>? = null

    var config = YamlConfiguration()
    private set

    var orderConfig = YamlConfiguration()
    private set

    override fun onEnable() {
        instance = this
        scrates = getPlugin(SpecializedCrates::class.java)
        if (server.pluginManager.getPlugin("Vault") == null) {
            Bukkit.shutdown()
            Bukkit.getConsoleSender().send("$prefix &cUnable to load Vault for economy features. Shutting down.")
            return
        }

        val rsp = server.servicesManager.getRegistration(Economy::class.java)

        if (rsp == null) {
            Bukkit.shutdown()
            Bukkit.getConsoleSender().send("$prefix &cUnable to load Vault for economy features. Shutting down.")
            return
        }

        economy = rsp.provider

        saveResource("config.yml", false)
        val configFile = File(dataFolder, "config.yml")
        config = YamlConfiguration.loadConfiguration(configFile)

        saveResource("order.yml", false)
        val orderFile = File(dataFolder, "order.yml")
        orderConfig = YamlConfiguration.loadConfiguration(orderFile)

        readChances()
        registerCommands()
        registerListeners()
    }

    override fun onDisable() {
        for (player in Bukkit.getOnlinePlayers()) {
            RerollAnyGUI.getMenu(player)?.forceEnd(false)
        }
    }

    private fun readChances() {
        saveResource("keys.yml", false)
        saveResource("spawners.yml", false)

        ChanceManager.keyChances = ChanceManager.readChances(
            File(dataFolder, "keys.yml")
        ) {
            val map = mutableMapOf<KeyType, Double>()
            for (entry in it.entries) {
                map[KeyType(entry.key)] = entry.value
            }

            WeightedRandom(map)
        }

        ChanceManager.spawnerChances = ChanceManager.readChances(
            File(dataFolder, "spawners.yml")
        ) {
            val map = mutableMapOf<SpawnerType, Double>()
            for (entry in it.entries) {
                map[SpawnerType(entry.key)] = entry.value
            }

            WeightedRandom(map)
        }
    }

    private fun registerCommands() {
        manager = BukkitCommandManager(
            this,
            CommandExecutionCoordinator.SimpleCoordinator.simpleCoordinator<CommandSender>(),
            Function.identity(),
            Function.identity()
        )

        manager?.parserRegistry?.registerParserSupplier(TypeToken.get(KeyType::class.java)) { KeyTypeArgument.KeyTypeParser() }
        manager?.parserRegistry?.registerParserSupplier(TypeToken.get(SpawnerType::class.java)) { SpawnerTypeArgument.SpawnerTypeParser() }
        UserCommands.registerCommands(manager!!)
    }

    private fun registerListeners() {
        Bukkit.getPluginManager().registerEvents(RerollAnyGUI.InventoryListener, this)
        Bukkit.getPluginManager().registerEvents(DuplicateAnyGUI.InventoryListener, this)
        Bukkit.getPluginManager().registerEvents(BaseGUI.InventoryListener, this)
    }
}