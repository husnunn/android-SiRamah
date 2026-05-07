package com.gridy.rohmahapp.api.moshi

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import kotlin.annotation.AnnotationRetention
import kotlin.annotation.MustBeDocumented
import kotlin.annotation.Retention

/**
 * Field API kadang string, kadang objek (mis. `role`, `school`, `message` nested), kadang angka.
 * Adapter ini mengembalikan [String] atau null agar tidak error "expected STRING but was BEGIN_OBJECT".
 */
@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
@MustBeDocumented
annotation class FlexibleString

class FlexibleStringJsonAdapter {
    @FlexibleString
    @FromJson
    fun fromJson(reader: JsonReader): String? {
        return when (reader.peek()) {
            JsonReader.Token.STRING -> reader.nextString()
            JsonReader.Token.NUMBER -> {
                val v = reader.nextDouble()
                if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
            }
            JsonReader.Token.BOOLEAN -> reader.nextBoolean().toString()
            JsonReader.Token.NULL -> reader.nextNull()
            JsonReader.Token.BEGIN_OBJECT -> readObjectAsString(reader)
            JsonReader.Token.BEGIN_ARRAY -> {
                reader.beginArray()
                while (reader.hasNext()) reader.skipValue()
                reader.endArray()
                null
            }
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    /**
     * Ambil string pertama dari kunci umum Laravel / REST; jika tidak ada, fallback ke `id` numerik.
     */
    private fun readObjectAsString(reader: JsonReader): String? {
        reader.beginObject()
        var result: String? = null
        var numericId: String? = null
        while (reader.hasNext()) {
            when (val name = reader.nextName()) {
                "name", "title", "label", "slug", "value", "text", "code",
                "username", "email", "url", "address", "profile_photo_url", "photo_url",
                -> {
                    when (reader.peek()) {
                        JsonReader.Token.STRING -> {
                            val s = reader.nextString()
                            if (result == null) result = s
                        }
                        JsonReader.Token.NUMBER -> {
                            val s = reader.nextLong().toString()
                            if (result == null) result = s
                        }
                        else -> reader.skipValue()
                    }
                }
                "id" -> {
                    when (reader.peek()) {
                        JsonReader.Token.NUMBER -> numericId = reader.nextLong().toString()
                        JsonReader.Token.STRING -> numericId = reader.nextString()
                        else -> reader.skipValue()
                    }
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return result ?: numericId
    }

    @FlexibleString
    @ToJson
    fun toJson(writer: JsonWriter, @FlexibleString value: String?) {
        if (value == null) writer.nullValue() else writer.value(value)
    }
}
