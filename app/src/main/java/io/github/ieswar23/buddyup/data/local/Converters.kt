package io.github.ieswar23.buddyup.data.local

import androidx.room.TypeConverter

/** Stores small string lists (interests, tags) as a single delimited column. */
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(SEPARATOR)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(SEPARATOR)

    private companion object {
        const val SEPARATOR = "|"
    }
}
