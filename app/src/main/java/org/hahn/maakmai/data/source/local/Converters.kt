package org.hahn.maakmai.data.source.local

import androidx.room.TypeConverter
import java.util.UUID

class Converters {
    @TypeConverter
    fun fromUUID(uuid: UUID): String {
        return uuid.toString()
    }

    @TypeConverter
    fun toUUID(uuidString: String): UUID {
        return UUID.fromString(uuidString)
    }

    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return list.joinToString(",")
    }

    @TypeConverter
    fun toStringList(string: String): List<String> {
        return if (string.isEmpty()) emptyList() else string.split(",")
    }
}