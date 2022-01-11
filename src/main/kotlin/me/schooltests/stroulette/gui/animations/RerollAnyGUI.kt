package me.schooltests.stroulette.gui.animations

import me.schooltests.stroulette.RoulettePlugin
import me.schooltests.stroulette.chance.ChanceManager
import me.schooltests.stroulette.chance.KeyType
import me.schooltests.stroulette.chance.RouletteType
import me.schooltests.stroulette.chance.SpawnerType
import me.schooltests.stroulette.prefix
import me.schooltests.stroulette.util.durability
import me.schooltests.stroulette.util.flag
import me.schooltests.stroulette.util.name
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class RerollAnyGUI(private val player: Player, rolling: RouletteType) {
    companion object {
        const val chooseFromSlot = 13
        const val endRewardSlot = 22

        private val inMenu = mutableMapOf<UUID, RerollAnyGUI>()
        fun getMenu(player: Player): RerollAnyGUI? = inMenu[player.uniqueId]

        val rewardSlots = arrayListOf(12, 13, 14, 23, 32, 31, 30, 21)
        val endRewardAnimationPhases = mapOf(
            1 to arrayListOf(12, 13, 14, 23, 32, 31, 30, 21),
            2 to arrayListOf(2, 3, 4, 5, 6, 15, 24, 33, 42, 41, 40, 39, 38, 29, 20, 11),
            3 to arrayListOf(10, 19, 28, 16, 25, 34),
            4 to arrayListOf(18, 26)
        )

        val randomColor: ItemStack
        get() = ItemStack(Material.STAINED_GLASS_PANE)
                .durability(listOf(4, 1, 5, 6, 3, 2).random())
                .name("&c")
                .flag(ItemFlag.HIDE_ATTRIBUTES)
    }

    object InventoryListener : Listener {
        @EventHandler
        fun inventoryClick(event: InventoryClickEvent) {
            if (event.whoClicked is Player && getMenu(event.whoClicked as Player) != null) {
                event.isCancelled = true
            }
        }

        @EventHandler
        fun inventoryClose(event: InventoryCloseEvent) {
            if (event.player is Player && getMenu(event.player as Player) != null) {
                getMenu(event.player as Player)!!.forceEnd(true)
                inMenu.remove(event.player.uniqueId)
            }
        }
    }
    private val inv: Inventory = Bukkit.createInventory(player, 5 * 9, prefix)
    private var rewardItems = mutableListOf<Pair<RouletteType, Int>>()

    private var task: BukkitTask? = null
    private var phaseTask: BukkitTask? = null

    private var handler: (RouletteType, Int) -> Unit = { type, amount -> type.give(player, amount, true) }
    private var receivedReward = false

    private val atomic = AtomicInteger(0)

    init {
        inMenu[player.uniqueId] = this

        for (i in 0 until 45) {
            inv.setItem(i, randomColor)
        }

        val random = Random()
        for (item in rewardSlots.withIndex()) {
            var amount = 1
            val config = RoulettePlugin.instance!!.config

            if (config.getInt("jackpot.chance") > random.nextInt(100)) {
                amount = config.getInt("jackpot.amount")
            }

            if (rolling is KeyType) {
                rewardItems.add(item.index, Pair(ChanceManager.randomKey(rolling.type), amount))
            } else if (rolling is SpawnerType) {
                rewardItems.add(item.index, Pair(ChanceManager.randomSpawner(rolling.type), amount))
            }

            inv.setItem(item.value, rewardItems[item.index].first.item(amount))
        }

        player.openInventory(inv)
        beginRolling()
    }

    private fun beginRolling() {
        atomic.set(0)

        task = Bukkit.getScheduler().runTaskTimer(RoulettePlugin.instance, {
            if (atomic.addAndGet(1) > 10) {
                beginEndRoll(1)
                return@runTaskTimer
            }

            roll()
        }, 0L, 5L)
    }

    private fun beginEndRoll(phase: Int) {
        task?.cancel()
        phaseTask?.cancel()
        atomic.set(0)

        task = when (phase) {
            1 -> Bukkit.getScheduler().runTaskTimer(RoulettePlugin.instance, {
                if (atomic.addAndGet(1) > 7) {
                    beginEndRoll(2)
                    return@runTaskTimer
                }

                roll()
            }, 0L, 10L)
            2 -> Bukkit.getScheduler().runTaskTimer(RoulettePlugin.instance, {
                if (atomic.addAndGet(1) > 3) {
                    beginEndRoll(3)
                    return@runTaskTimer
                }

                roll()
            }, 0L, 15L)
            3 -> Bukkit.getScheduler().runTask(RoulettePlugin.instance) {
                fun endPhaseTask() {
                    phaseTask?.cancel()
                }

                val indexOfSlot = rewardSlots.indexOf(chooseFromSlot)
                val type = rewardItems[indexOfSlot]
                val item = type.first.item(type.second)

                for (i in 0 until 45) {
                    inv.setItem(i, randomColor)
                }

                inv.setItem(endRewardSlot, item)
                handler.invoke(type.first, type.second)
                receivedReward = true

                phaseTask = Bukkit.getScheduler().runTaskTimer(RoulettePlugin.instance, {
                    if (atomic.getAndAdd(1) >= endRewardAnimationPhases.size) {
                        endPhaseTask()
                        return@runTaskTimer
                    }

                    val animations = endRewardAnimationPhases[atomic.get()]
                    val prevAnimations = try {
                        endRewardAnimationPhases[atomic.get() - 1]
                    } catch (e: ArrayIndexOutOfBoundsException) { listOf() }

                    for (i in prevAnimations ?: listOf()) {
                        inv.setItem(i, randomColor)
                    }

                    for (i in animations ?: listOf()) {
                        inv.setItem(i, ItemStack(Material.STAINED_GLASS_PANE).durability(10).name("&c").flag(ItemFlag.HIDE_ATTRIBUTES))
                    }
                }, 0L, 5L)
            }
            else -> null
        }
    }

    private fun roll() {
        // Pushes all elements forward by 1
        val itemAt = rewardItems[rewardItems.size - 1]
        rewardItems.removeAt(rewardItems.size - 1)
        rewardItems.add(0, itemAt)

        // Refills the gui with new random colors
        for (i in 0 until 45) {
            inv.setItem(i, randomColor)
        }

        // Sets the scrolling wheel to have the proper items
        for (item in rewardSlots.withIndex()) {
            val reward = rewardItems[item.index]
            inv.setItem(item.value, reward.first.item(reward.second))
        }

        // Play sound
        player.playSound(player.location, Sound.NOTE_PLING, 1F, 1F)
    }

    fun handler(handler: (RouletteType, Int) -> Unit) {
        this.handler = handler
    }

    fun forceEnd(alreadyClosed: Boolean) {
        if (!alreadyClosed) {
            if (!receivedReward) {
                beginEndRoll(3)
                phaseTask?.cancel()
            }

            task?.cancel()
            player.closeInventory()

            return
        }

        if (!receivedReward) {
            val indexOfSlot = rewardSlots.indexOf(chooseFromSlot)
            val type = rewardItems[indexOfSlot]
            handler.invoke(type.first, type.second)
            receivedReward = true
        }

        task?.cancel()
        phaseTask?.cancel()
    }
}