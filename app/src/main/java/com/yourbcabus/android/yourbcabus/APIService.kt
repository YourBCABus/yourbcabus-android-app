package com.yourbcabus.android.yourbcabus

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

abstract class APIService(url: URL) {
    val url = url
    private val klaxon = Klaxon()

    private var busList = listOf<Bus>()
    private var busMap = mapOf<String, Int>()

    protected abstract fun fetchURL(url: String, handler: FetchURLHandler, errorHandler: FetchErrorHandler)

    private inline fun <reified Resource> fetchResource(path: String, crossinline handler: FetchResourceHandler<Resource>, noinline errorHandler: FetchErrorHandler) {
        fetchURL(URL(url, path).toString(), {
            handler(klaxon.parse<Resource>(it)!!)
        }, errorHandler)
    }

    private fun setBuses(list: List<Bus>) {
        busList = list

        val map = HashMap<String, Int>()
        busList.forEachIndexed { index, bus ->
            map[bus._id] = index
        }
        busMap = map.toMap()
    }

    fun reloadBuses(school: String) {
        fetchResource<List<Bus>>("/schools/$school/buses", { setBuses(it) }, {})
    }

    fun reloadBus(school: String, bus: String) {
        fetchResource<Bus>("/schools/$school/buses/$bus", {
            busList = busList.toMutableList().apply {
                if (busMap[bus] == null) {
                    busMap = busMap.toMutableMap().apply { set(bus, busList.size) }.toMap()
                }

                set(busMap.getValue(bus), it)
            }.toList()
        }, {})
    }

    fun getBuses(): List<Bus> {
        return busList
    }

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
        @JvmStatic val static = AndroidAPIService(URL("https://yourbcabus.com"))
    }
}