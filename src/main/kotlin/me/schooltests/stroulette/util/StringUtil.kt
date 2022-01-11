package me.schooltests.stroulette.util

fun String.capitalize(): String {
    val words: List<String> = this.split("\\s")
    var capitalizeWord = ""
    for (w in words) {
        val first = w.substring(0, 1)
        val afterfirst = w.substring(1)
        capitalizeWord += first.toUpperCase() + afterfirst + " "
    }
    return capitalizeWord.trim { it <= ' ' }
}