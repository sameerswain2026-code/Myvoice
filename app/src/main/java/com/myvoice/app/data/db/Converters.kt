package com.myvoice.app.data.db

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class Converters {

    private val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(String.serializer())

    @TypeConverter
    fun fromTags(tags: List<String>): String = json.encodeToString(listSerializer, tags)

    @TypeConverter
    fun toTags(raw: String): List<String> = try {
        json.decodeFromString(listSerializer, raw)
    } catch (_: Exception) {
        emptyList()
    }
}
