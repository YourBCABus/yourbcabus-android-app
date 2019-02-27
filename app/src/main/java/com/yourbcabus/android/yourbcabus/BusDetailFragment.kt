package com.yourbcabus.android.yourbcabus

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_bus_detail.*
import kotlinx.android.synthetic.main.bus_detail.*
import kotlinx.android.synthetic.main.bus_detail_content.view.*
import java.sql.Time
import java.text.SimpleDateFormat
import java.util.*

/**
 * A fragment representing a single Bus detail screen.
 * This fragment is either contained in a [BusListActivity]
 * in two-pane mode (on tablets) or a [BusDetailActivity]
 * on handsets.
 */
class BusDetailFragment : Fragment() {

    private var argBusId: String = ""
    private var item: Bus? = null
    private var stops = listOf<Stop>()
    private var dateFormat = SimpleDateFormat("h:mm a")

    val schoolId = "5bca51e785aa2627e14db459"
    val apiService: APIService = AndroidAPIService.standardForSchool(schoolId)

    private lateinit var busDetail: RecyclerView

    private val stopObserver: Observer = {
        stops = AndroidAPIService.standardForSchool(schoolId).getStops(argBusId)
        busDetail.adapter?.notifyDataSetChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            if (it.containsKey(ARG_BUS_ID)) {
                argBusId = it.getString(ARG_BUS_ID)?:""
                item = AndroidAPIService.standardForSchool(schoolId).getBus(argBusId)
                activity?.toolbar_title?.text = item?.name
                stops = AndroidAPIService.standardForSchool(schoolId).getStops(argBusId)
            }
        }

        AndroidAPIService.standardForSchool(schoolId).on(APIService.STOPS_CHANGED_EVENT_FOR(argBusId), stopObserver)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.bus_detail, container, false)

        busDetail = rootView as RecyclerView
        setupRecyclerView(busDetail)

        AndroidAPIService.standardForSchool(schoolId).reloadStops(argBusId)
        return rootView
    }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        recyclerView.adapter = SimpleStopRecyclerViewAdapter(this)
    }

    inner class SimpleStopRecyclerViewAdapter(private val parentActivity: BusDetailFragment) : RecyclerView.Adapter<SimpleStopRecyclerViewAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.bus_detail_content, parent, false)

            val holder = ViewHolder(view)

            return holder
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = stops[position]
            holder.busStopView.text = item.name
            holder.busStopTime.text = dateFormat.format(item.arrival_time)
        }

        override fun getItemCount() = stops.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val busStopView: TextView = view.bus_detail_stop
            val busStopTime: TextView = view.bus_detail_time
        }
    }

    companion object {
        /**
         * The fragment argument representing the item ID that this fragment
         * represents.
         */
        const val ARG_BUS_ID = "bus_id"
    }
}
