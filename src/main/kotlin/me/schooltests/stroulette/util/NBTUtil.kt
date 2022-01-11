package me.schooltests.stroulette.util

import de.tr7zw.changeme.nbtapi.NBTItem
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.collections.HashMap

/**
 * A kotlin utility with extension functions
 * for org.bukkit.inventory.ItemStack
 * that allows for easily modifying the NBT
 * of an ItemStack.
 *
 * @author Ghqst
 */
fun ItemStack.setNbtTags(vararg pairs: Pair<String, Any?>): ItemStack {
    val tags = hashMapOf(*pairs)
    val nbtItem = NBTItem(this)
    tags.forEach { entry ->
        entry.value?.let {
            when (it) {
                // Numbers
                is Int -> nbtItem.setInteger(entry.key, it)
                is Long -> nbtItem.setLong(entry.key, it)
                is Short -> nbtItem.setShort(entry.key, it)
                is Double -> nbtItem.setDouble(entry.key, it)
                is Float -> nbtItem.setFloat(entry.key, it)
                is IntArray -> nbtItem.setIntArray(entry.key, it)
                // Bytes
                is Byte -> nbtItem.setByte(entry.key, it)
                is ByteArray -> nbtItem.setByteArray(entry.key, it)
                // Other Types
                is String -> nbtItem.setString(entry.key, it)
                is Boolean -> nbtItem.setBoolean(entry.key, it)
                // Useful Objects
                is ItemStack -> nbtItem.setItemStack(entry.key, it)
                is UUID -> nbtItem.setUUID(entry.key, it)
                // Leftovers
                else -> nbtItem.setObject(entry.key, it)
            }
        }

    }
    return nbtItem.item
}

fun ItemStack.getNbtTags(vararg pairs: Pair<String, NbtTypes>): HashMap<String, Any?> {
    val nbtItem = NBTItem(this)
    val output = hashMapOf<String, Any?>()
    pairs.forEach {
        val value: Any? = when (it.second) {
            NbtTypes.INT -> nbtItem.getInteger(it.first)
            NbtTypes.LONG -> nbtItem.getLong(it.first)
            NbtTypes.SHORT -> nbtItem.getShort(it.first)
            NbtTypes.DOUBLE -> nbtItem.getDouble(it.first)
            NbtTypes.FLOAT -> nbtItem.getFloat(it.first)
            NbtTypes.INTARRAY -> nbtItem.getIntArray(it.first)
            // Bytes
            NbtTypes.BYTE -> nbtItem.getByte(it.first)
            NbtTypes.BYTEARRAY -> nbtItem.getByteArray(it.first)
            // Other Types
            NbtTypes.STRING -> nbtItem.getString(it.first)
            NbtTypes.BOOLEAN -> nbtItem.getBoolean(it.first)
            // Useful Objects
            NbtTypes.ITEMSTACK -> nbtItem.getItemStack(it.first)
            NbtTypes.UUID -> nbtItem.getUUID(it.first)
        }
        output[it.first] = value
    }
    return output
}

enum class NbtTypes {
    INT,
    LONG,
    SHORT,
    DOUBLE,
    FLOAT,
    INTARRAY,
    BYTE,
    BYTEARRAY,
    STRING,
    BOOLEAN,
    ITEMSTACK,
    UUID
}