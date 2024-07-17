package com.justself.klique.useful_extensions

fun String.capitalizeWords(): String{
    return split(" ").joinToString(" ") { it.capitalize() }
}

fun String.initials(): String {
    val parts = this.split(" ")

    val initials = if (parts.size > 1) {
        parts[0].substring(0, 1) + parts[1].substring(0, 1)
    } else {
        this.substring(0, 2)
    }
    return initials.uppercase()
}

