package me.schooltests.stroulette.util

import java.util.*


class WeightedRandom<T : Any?>(map: Map<T, Double>) {
    private inner class Entry {
        var accumulatedWeight = 0.0
        var obj: T? = null
    }

    private val entries: MutableList<Entry> = ArrayList<Entry>()
    private var accumulatedWeight = 0.0
    private val rand = Random()
    fun addEntry(obj: T, weight: Double) {
        accumulatedWeight += weight
        val e: Entry = Entry()
        e.obj = obj
        e.accumulatedWeight = accumulatedWeight
        entries.add(e)
    }

    //should only happen when there are no entries
    val random: T?
        get() {
            val r = rand.nextDouble() * accumulatedWeight
            for (entry in entries) {
                if (entry.accumulatedWeight >= r) {
                    return entry.obj
                }
            }
            return null //should only happen when there are no entries
        }

    init {
        for (obj in map.entries) {
            addEntry(obj.key, obj.value)
        }
    }
}