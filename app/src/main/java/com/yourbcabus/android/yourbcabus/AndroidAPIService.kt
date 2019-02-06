package com.yourbcabus.android.yourbcabus

import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.NoCache
import com.android.volley.toolbox.StringRequest
import java.net.URL

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