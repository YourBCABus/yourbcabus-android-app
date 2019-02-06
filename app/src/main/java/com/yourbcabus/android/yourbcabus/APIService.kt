package com.yourbcabus.android.yourbcabus

import com.beust.klaxon.Klaxon
import java.net.URL
import kotlin.properties.Delegates

enum class FetchError {
    OTHER
}

typealias FetchURLHandler = (String) -> Unit
typealias FetchErrorHandler = (FetchError) -> Unit

private typealias FetchResourceHandler<Resource> = (Resource) -> Unit

private class StopManager {
    var list = listOf<Stop>()
    var map = mapOf<String, Int>()
}

abstract class APIService(val url: URL, val schoolId: String): EventEmitter {
    @Deprecated("Use APIService.BUSES_CHANGED_EVENT instead.") val BUSES_CHANGED_EVENT get() = APIService.BUSES_CHANGED_EVENT

    private val klaxon = Klaxon().fieldConverter(KlaxonDate::class, KlaxonDate)

    private var _school by Delegates.observable<School?>(null) { _, _, _ ->
        emit(SCHOOL_CHANGED_EVENT, null)
    }
    val school get() = _school

    private var busList = listOf<Bus>()
    private var busMap = mapOf<String, Int>()

    private var stops: MutableMap<String, StopManager> = HashMap()

    override val observers = HashMap<String, MutableList<Observer>>()
    override val onceObservers = HashMap<String, MutableList<Observer>>()

    protected abstract fun fetchURL(url: String, handler: FetchURLHandler, errorHandler: FetchErrorHandler)

    private fun transformURL(path: String) = URL(url, path)

    private inline fun <reified Resource> fetchResource(path: String, crossinline handler: FetchResourceHandler<Resource>, noinline errorHandler: FetchErrorHandler) {
        fetchURL(transformURL(path).toString(), {
            handler(klaxon.parse<Resource>(it)!!)
        }, errorHandler)
    }

    private inline fun <reified Resource> fetchResourceArray(path: String, crossinline handler: FetchResourceHandler<List<Resource>>, noinline errorHandler: FetchErrorHandler) {
        fetchURL(transformURL(path).toString(), {
            handler(klaxon.parseArray(it)!!)
        }, errorHandler)
    }

    private fun setBuses(list: List<Bus>) {
        busList = list

        val map = HashMap<String, Int>()
        busList.forEachIndexed { index, bus ->
            map[bus._id] = index
        }
        busMap = map.toMap()

        emit(BUSES_CHANGED_EVENT, null)
    }

    fun reloadSchool() {
        fetchResource<School>("/schools/$schoolId", {
            _school = it
        }, {})
    }

    fun reloadBuses() {
        fetchResourceArray<Bus>("/schools/$schoolId/buses", { setBuses(it) }, {})
    }

    @Deprecated(message = "Use reloadBuses() instead.") fun reloadBuses(school: String) {
        reloadBuses()
    }

    fun reloadBus(bus: String) {
        fetchResource<Bus>("/schools/$schoolId/buses/$bus", {
            busList = busList.toMutableList().apply {
                if (busMap[bus] == null) {
                    busMap = busMap.toMutableMap().apply { set(bus, size) }.toMap()
                }

                set(busMap.getValue(bus), it)
            }.toList()
            emit(BUSES_CHANGED_EVENT, null)
        }, {})
    }

    @Deprecated(message = "Use reloadBus(String) instead.") fun reloadBus(school: String, bus: String) {
        reloadBus(bus)
    }

    val buses get() = busList

    fun getBus(_id: String): Bus? {
        return busMap[_id]?.let { busList[it] }
    }

    fun getStops(bus: String): List<Stop> {
        return stops[bus]?.list ?: listOf()
    }

    fun getStop(bus: String, stop: String): Stop? {
        return stops[bus]?.map?.get(stop)?.let { stops[bus]!!.list[it] }
    }

    companion object {
        @JvmStatic val SCHOOL_CHANGED_EVENT = "schoolChanged"
        val BUSES_CHANGED_EVENT = "busesChanged"
        val STOPS_CHANGED_EVENT = "stopsChanged"
        @JvmStatic fun STOPS_CHANGED_EVENT_FOR(bus: String): String {
            return "$STOPS_CHANGED_EVENT.$bus"
        }
    }
}