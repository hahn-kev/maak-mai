package org.hahn.maakmai.data.source.local

import androidx.room.TypeConverter
import java.util.Base64
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

//    @TypeConverter
//    fun fromByteArray(byteArray: ByteArray): String {
//        return Base64.getEncoder().encodeToString(byteArray)
//    }
//
//    @TypeConverter
//    fun toByteArray(base64String: String): ByteArray {
//        return Base64.getDecoder().decode(base64String)
//    }
}
