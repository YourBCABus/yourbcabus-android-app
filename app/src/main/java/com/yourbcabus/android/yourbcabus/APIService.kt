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

typealias FetchURLHandler = (String) -> Unit

abstract class APIService(url: URL) {
    val url = url
    val klaxon = Klaxon()

    @Throws protected abstract fun fetchURL(url: String, handler: FetchURLHandler)
}

class AndroidAPIService(url: URL, requestQueue: RequestQueue = RequestQueue(NoCache(), BasicNetwork(HurlStack()))): APIService(url) {
    private val requestQueue = requestQueue.apply {
        start()
    }

    @Throws override fun fetchURL(url: String, handler: FetchURLHandler) {
        val request = StringRequest(Request.Method.GET, url, Response.Listener<String> {
            handler(it)
        }, Response.ErrorListener {})

        requestQueue.add(request)
    }

    companion object {
        @JvmStatic val static = AndroidAPIService(URL("https://yourbcabus.com"))
    }
}