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

abstract class APIService(val url: URL, val schoolId: String): EventEmitter {
    @Deprecated("Use APIService.BUSES_CHANGED_EVENT instead.") val BUSES_CHANGED_EVENT get() = APIService.BUSES_CHANGED_EVENT

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

    companion object {
        val BUSES_CHANGED_EVENT = "busesChanged"
    }
}

class AndroidAPIService(url: URL, schoolId: String, requestQueue: RequestQueue = RequestQueue(NoCache(), BasicNetwork(HurlStack()))): APIService(url, schoolId) {
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
        @Deprecated(message = "Use standardForSchool(String) instead.") @JvmStatic val standard get() = standardForSchool("5bca51e785aa2627e14db459")

        private val standards: MutableMap<String, AndroidAPIService> = HashMap()

        private fun createAPIServiceForSchool(school: String): AndroidAPIService {
            return AndroidAPIService(URL("https://db.yourbcabus.com"), school)
        }

        @JvmStatic fun standardForSchool(school: String): AndroidAPIService {
            if (!standards.containsKey(school)) {
                standards[school] = createAPIServiceForSchool(school)
            }

            return standards.getValue(school)
        }
    }
}