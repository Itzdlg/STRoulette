package me.schooltests.stroulette.util

import me.schooltests.stroulette.RoulettePlugin
import me.schooltests.stroulette.chance.ChanceManager
import me.schooltests.stroulette.chance.SpawnerType
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.block.CreatureSpawner
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta

object SpawnerUtil {
    fun getSpawners(player: Player, spawner: SpawnerType): Int {
        var amount = 0
        for (content in player.inventory.contents) {
            if (isValid(content) && getSpawner(content).typeId == spawner.entityId) {
                amount += content.amount
            }
        }
        return amount
    }

    fun removeSpawners(player: Player, spawner: SpawnerType, amount: Int): Boolean {
        var left = 0
        for (i in player.inventory.contents.indices) {
            if (left == amount) return true
            val itemStack = player.inventory.contents[i]
            if (!isValid(itemStack) || getSpawner(itemStack).typeId != spawner.entityId) continue
            if (left + itemStack.amount > amount) {
                var value = 0
                for (a in 0 until itemStack.amount) {
                    value++
                    left++
                    if (left == amount) break
                }
                itemStack.amount = itemStack.amount - value
                player.inventory.setItem(i, itemStack)
                return true
            } else {
                player.inventory.clear(i)
                left += itemStack.amount
            }
        }
        return false
    }

    private fun getSpawner(itemStack: ItemStack): EntityType {
        if (itemStack.type != Material.MOB_SPAWNER) return EntityType.UNKNOWN
        val name = itemStack.itemMeta?.displayName ?: return EntityType.UNKNOWN
        if (!name.contains("Spawner")) return EntityType.UNKNOWN

        val type: String = ChanceManager.spawnerChances.keys.filter {
            val type = SpawnerType(it)
            return@filter type.displayName == ChatColor.stripColor(name.substringBeforeLast("Spawner").trim())
        }.firstOrNull() ?: return EntityType.UNKNOWN

        val id = SpawnerType(type).entityId
        val entityType = EntityType.fromId(id.toInt())
        return entityType ?: EntityType.UNKNOWN
    }

    private fun isValid(item: ItemStack?): Boolean {
        return item != null && item.type != Material.AIR
    }
}
