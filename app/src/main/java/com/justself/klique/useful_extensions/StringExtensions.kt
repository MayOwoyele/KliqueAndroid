package com.justself.klique.useful_extensions

fun String.initials(): String {
    val parts = this.trim().split(" ").filter { it.isNotEmpty() }
    return when {
        parts.size > 1 -> {
            (parts[0][0].toString() + parts[1][0]).uppercase()
        }
        parts.size == 1 -> {
            parts[0].take(2).uppercase()
        }
        else -> {
            "?"
        }
    }
}