package com.yourbcabus.android.yourbcabus

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import java.lang.Exception
import java.util.*

@Target(AnnotationTarget.FIELD)
annotation class KlaxonDate {
    class InvalidDateException: Exception()

    companion object : Converter {
        override fun canConvert(cls: Class<*>): Boolean = cls == Date::class.java

        private fun parseTimezone(str: String): TimeZone = when (str) {
            "Z" -> TimeZone.getTimeZone("UTC")
            else -> TimeZone.getTimeZone(str)
        }

        override fun fromJson(jv: JsonValue): Any? {
            try {
                val str = jv.string!!.replace("-", "").replace(":", "").replace(".", "")

                val year = str.substring(0..3).toInt()
                val month = str.substring(4..5).toInt()
                val day = str.substring(6..7).toInt()

                var hour = 0
                var minute = 0
                var second = 0
                var millisecond = 0
                var tz: TimeZone = TimeZone.getTimeZone("UTC")

                if (str.length > 8) {
                    hour = str.substring(9..10).toInt()
                    minute = str.substring(11..12).toInt()
                    second = str.substring(13..14).toInt()

                    if (str.length > 15) {
                        if (str[15].isDigit()) {
                            millisecond = str.substring(15..17).toInt()
                            if (str.length > 18) {
                                tz = parseTimezone(str.substring(18))
                            }
                        } else {
                            tz = parseTimezone(str.substring(15))
                        }
                    }
                }

                val calendar: Calendar = GregorianCalendar()
                calendar.timeZone = tz
                calendar.set(year, month - 1, day, hour, minute, second)
                calendar.set(Calendar.MILLISECOND, millisecond)

                return calendar.time
            } catch (e: Exception) {
                throw InvalidDateException()
            }
        }

        override fun toJson(value: Any): String {
            TODO()
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
        val name: String? = null,
        val available: Boolean,
        val locations: Array<String>? = null,
        val boarding: Int? = null,
        @KlaxonDate val invalidate_time: Date? = null
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