package com.justself.klique
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Emoji(
    val emoji: String,
    val description: String,
    val category: String,
    val aliases: List<String>,
    val tags: List<String>,
    val unicode_version: String,
    val ios_version: String
)

fun loadEmojis(context: Context): List<Emoji> {
    val json = context.assets.open("emojis.json").bufferedReader().use { it.readText() }
    val emojiType = object : TypeToken<List<Emoji>>() {}.type
    return Gson().fromJson(json, emojiType)
}
