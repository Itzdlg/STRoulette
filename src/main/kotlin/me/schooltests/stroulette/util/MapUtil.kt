package me.schooltests.stroulette.util

fun <K, V : Comparable<V>?> Map<K, V>.sortByValue(): Map<K, V> {
    val list: List<Map.Entry<K, V>> = ArrayList(this.entries)
    list.sortedWith(java.util.Map.Entry.comparingByValue())
    val result: MutableMap<K, V> = LinkedHashMap()
    for ((key, value) in list) {
        result[key] = value
    }

    return result
}