package me.schooltests.stroulette.chance

import me.schooltests.stroulette.prefix
import me.schooltests.stroulette.util.*

import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException

object ChanceManager {
    var keyChances = mapOf<String, WeightedRandom<KeyType>>()
    var spawnerChances = mapOf<String, WeightedRandom<SpawnerType>>()

    private val defaultKey
        get() = KeyType(keyChances.keys.first())

    private val defaultSpawner
        get() = SpawnerType(spawnerChances.keys.first())

    fun randomKey(type: String): KeyType {
        return keyChances[type]?.random ?: defaultKey
    }

    fun randomSpawner(type: String): SpawnerType {
        return spawnerChances[type]?.random ?: defaultSpawner
    }

    fun <T> readChances(file: File, toWeightedRandom: (Map<String, Double>) -> WeightedRandom<T>): Map<String, WeightedRandom<T>> {
        try {
            val config = YamlConfiguration.loadConfiguration(file)
            val defaultsSection = config.getConfigurationSection("defaults")

            val defaults = mutableMapOf<String, Double>()
            for (key in defaultsSection.getKeys(false)) {
                val value = defaultsSection.getDouble(key)
                defaults[key] = value
            }

            val parents = config.getConfigurationSection("").getKeys(false)
            parents.remove("defaults")

            val finalMap = mutableMapOf<String, WeightedRandom<T>>()
            for (rolled in parents) {
                val map = mutableMapOf<String, Double>()
                for (d in defaults.entries) {
                    map[d.key] = d.value
                }

                val children = config.getConfigurationSection(rolled).getKeys(false)
                for (child in children) {
                    val value = config.getConfigurationSection(rolled).getDouble(child)
                    map[child] = value
                }

                finalMap[rolled] = toWeightedRandom.invoke(map)
            }

            return finalMap
        } catch (e: IOException) {
            Bukkit.getConsoleSender().send("$prefix &cUnable to read key chance file!")
            Bukkit.shutdown()
        }

        return mutableMapOf()
    }
}