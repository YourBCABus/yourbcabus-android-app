package com.yourbcabus.android.yourbcabus

import java.util.*

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
        val invalidate_time: Date?
)

data class Stop(
        val _id: String,
        val bus_id: String,
        val name: String?,
        val description: String?,
        val location: Coordinate,
        val order: Double?
)