package me.schooltests.stroulette.gui.animations

import me.schooltests.stroulette.RoulettePlugin
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

class DuplicateAnyGUI(private val player: Player, rolling: RouletteType) {
    companion object {
        const val mainSlot = 22

        private val inMenu = mutableMapOf<UUID, DuplicateAnyGUI>()
        fun getMenu(player: Player): DuplicateAnyGUI? = inMenu[player.uniqueId]

        val endRewardAnimationPhases = mapOf(
            1 to arrayListOf(21, 23),
            2 to arrayListOf(20, 24),
            3 to arrayListOf(19, 25),
            4 to arrayListOf(18, 26),
            5 to arrayListOf(12, 32),
            6 to arrayListOf(30, 14),
            7 to arrayListOf(1, 2, 3, 4, 5, 6, 7),
            8 to arrayListOf(37, 38, 39, 40, 41, 42, 43),
            9 to arrayListOf(1, 2, 3, 4, 5, 6, 7),
            10 to arrayListOf(37, 38, 39, 40, 41, 42, 43),
            11 to arrayListOf(1, 2, 3, 4, 5, 6, 7, 37, 38, 39, 40, 41, 42, 43)
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
                val gui = getMenu(event.player as Player)!!
                for (i in 1..Random().nextInt(9))
                    gui.roll(playSound = false)
                gui.forceEnd(true)
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
        for (i in 1..20) {
            var amount = random.nextInt(3) + 1
            val config = RoulettePlugin.instance!!.config

            if (config.getInt("jackpot.chance") > random.nextInt(100)) {
                amount = config.getInt("jackpot.amount")
            }

            if (rolling is KeyType) {
                rewardItems.add(Pair(rolling, amount))
            } else if (rolling is SpawnerType) {
                rewardItems.add(Pair(rolling, amount))
            }
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

                val type = rewardItems[0]
                val item = type.first.item(type.second)

                for (i in 0 until 45) {
                    inv.setItem(i, randomColor)
                }

                inv.setItem(mainSlot, item)
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

    private fun roll(playSound: Boolean = true) {
        // Pushes all elements forward by 1
        val itemAt = rewardItems[rewardItems.size - 1]
        rewardItems.removeAt(rewardItems.size - 1)
        rewardItems.add(0, itemAt)

        // Refills the gui with new random colors
        for (i in 0 until 45) {
            inv.setItem(i, randomColor)
        }

        inv.setItem(mainSlot, rewardItems[0].first.item(rewardItems[0].second))

        // Play sound
        if (playSound)
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
            val type = rewardItems[0]
            handler.invoke(type.first, type.second)
            receivedReward = true
        }

        task?.cancel()
        phaseTask?.cancel()
    }
}