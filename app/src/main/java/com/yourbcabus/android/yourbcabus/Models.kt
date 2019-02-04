package com.yourbcabus.android.yourbcabus

import java.util.Date

data class Bus(
        val _id: String,
        val school_id: String,
        val available: Boolean,
        val name: String?,
        val locations: Array<String>?,
        val invalidate_time: Date?
        )