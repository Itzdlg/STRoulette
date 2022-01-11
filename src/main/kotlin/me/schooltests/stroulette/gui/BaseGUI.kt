package me.schooltests.stroulette.gui

import me.schooltests.stroulette.util.durability
import me.schooltests.stroulette.util.enchantment
import me.schooltests.stroulette.util.flag
import me.schooltests.stroulette.util.name
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import java.util.*

open class BaseGUI(protected val inventory: Inventory, protected val owner: Player) {
    companion object {
        private val inMenu: MutableMap<UUID, BaseGUI> = mutableMapOf()
    }

    object InventoryListener : Listener {
        @EventHandler
        fun onClick(event: InventoryClickEvent) {
            val menu = inMenu[event.whoClicked.uniqueId] ?: return
            val clickEvent = menu.clickEvents[event.rawSlot] ?: return

            event.isCancelled = true // Make the event cancelled by default
            clickEvent.invoke(event)
        }

        @EventHandler
        fun onClose(event: InventoryCloseEvent) {
            inMenu.remove(event.player.uniqueId)
        }
    }

    protected val clickEvents: MutableMap<Int, (InventoryClickEvent) -> (Unit)> = mutableMapOf()
    open fun fill(
        i1: ItemStack = ItemStack(Material.STAINED_GLASS_PANE)
            .name("&c")
            .durability(14)
            .enchantment(Enchantment.DURABILITY, 1)
            .flag(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS
        ), i2: ItemStack = ItemStack(Material.STAINED_GLASS_PANE)
            .name("&c")
            .durability(4)
            .enchantment(Enchantment.DURABILITY, 1)
            .flag(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)) {
        for (i in 0 until inventory.size step 2) {
            setItem(i, i1)

            if (i + 1 >= inventory.size) break
            setItem(i + 1, i2)
        }
    }

    open fun setItem(slot: Int, item: ItemStack, event: (InventoryClickEvent) -> (Unit) = {it.isCancelled = true}) {
        inventory.setItem(slot, item)
        clickEvents[slot] = event
    }

    open fun open() {
        owner.closeInventory()
        owner.openInventory(inventory)
        inMenu[owner.uniqueId] = this
    }
}