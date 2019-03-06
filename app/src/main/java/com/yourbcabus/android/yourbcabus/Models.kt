package com.yourbcabus.android.yourbcabus

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.*

@Target(AnnotationTarget.FIELD)
annotation class KlaxonDate {
    companion object : Converter {
        private val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")

        override fun canConvert(cls: Class<*>): Boolean = cls == Date::class.java

        override fun fromJson(jv: JsonValue): Any? {
            return formatter.parse(jv.string)
        }

        override fun toJson(value: Any): String {
            return formatter.format(value)
        }
    }
}

data class Coordinate(
        val latitude: Double,
        val longitude: Double
)

data class School(
        val _id: String,
        val name: String?,
        val location: Coordinate?
)

data class Bus(
        val _id: String,
        val school_id: String,
        val name: String?,
        val available: Boolean,
        val locations: Array<String>?,
        val boarding: Int,
        @KlaxonDate val invalidate_time: Date?
) {
    fun isValidated(asOf: Date): Boolean {
        return invalidate_time == null || asOf < invalidate_time
    }

    fun getLocation(asOf: Date? = null): String? {
        return if (asOf == null || isValidated(asOf)) locations?.firstOrNull() else null
    }
}

data class Stop(
        val _id: String,
        val bus_id: String,
        val name: String? = null,
        val description: String? = null,
        val location: Coordinate,
        val order: Double?,
        @KlaxonDate val arrival_time: Date
)