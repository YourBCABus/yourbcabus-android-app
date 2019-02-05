package com.yourbcabus.android.yourbcabus

import android.app.Application
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.NoCache
import com.android.volley.toolbox.StringRequest
import com.beust.klaxon.Klaxon
import java.net.URL

enum class FetchError {
    OTHER
}

typealias FetchURLHandler = (String) -> Unit
typealias FetchErrorHandler = (FetchError) -> Unit

private typealias FetchResourceHandler<Resource> = (Resource) -> Unit

abstract class APIService(url: URL): EventEmitter {
    val BUSES_CHANGED_EVENT = "busesChanged"

    val url = url
    private val klaxon = Klaxon().fieldConverter(KlaxonDate::class, KlaxonDate)

    private var busList = listOf<Bus>()
    private var busMap = mapOf<String, Int>()

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

    fun reloadBuses(school: String) {
        fetchResourceArray<Bus>("/schools/$school/buses", { setBuses(it) }, {})
    }

    fun reloadBus(school: String, bus: String) {
        fetchResource<Bus>("/schools/$school/buses/$bus", {
            busList = busList.toMutableList().apply {
                if (busMap[bus] == null) {
                    busMap = busMap.toMutableMap().apply { set(bus, size) }.toMap()
                }

                set(busMap.getValue(bus), it)
            }.toList()
            emit(BUSES_CHANGED_EVENT, null)
        }, {})
    }

    val buses get() = busList

    fun getBus(_id: String): Bus? {
        return busMap[_id]?.let { busList[it] }
    }
}

class AndroidAPIService(url: URL, requestQueue: RequestQueue = RequestQueue(NoCache(), BasicNetwork(HurlStack()))): APIService(url) {
    private val requestQueue = requestQueue.apply {
        start()
    }

    override fun fetchURL(url: String, handler: FetchURLHandler, errorHandler: FetchErrorHandler) {
        val request = StringRequest(Request.Method.GET, url, Response.Listener<String> {
            handler(it)
        }, Response.ErrorListener {
            errorHandler(FetchError.OTHER)
        })

        requestQueue.add(request)
    }

    companion object {
        @JvmStatic val standard = AndroidAPIService(URL("https://db.yourbcabus.com"))
    }
}