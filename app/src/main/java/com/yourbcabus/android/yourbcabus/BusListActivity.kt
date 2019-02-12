package com.yourbcabus.android.yourbcabus

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import kotlinx.android.synthetic.main.activity_bus_list.*
import kotlinx.android.synthetic.main.bus_list_content.view.*
import kotlinx.android.synthetic.main.bus_list.*
import android.support.v4.widget.SwipeRefreshLayout
import android.widget.CheckBox
import android.widget.RelativeLayout
import java.util.*


/**
 * An activity representing a list of Pings. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [BusDetailActivity] representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
class BusListActivity : AppCompatActivity() {

    val schoolId = "5bca51e785aa2627e14db459"
    val apiService: APIService = AndroidAPIService.standardForSchool(schoolId)

    private var preferences: SharedPreferences? = null
    private val savedBuses get() = preferences?.getStringSet(SAVED_BUSES_PREFERENCE_NAME, null) ?: setOf()

    private var date = Date()

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private var twoPane: Boolean = false

    private val busObserver: Observer = {
        val adapter = bus_list.adapter as? SimpleBusRecyclerViewAdapter
        if (adapter != null) {
            date = Date()
            adapter.buses = apiService.buses.sortedBy { it.name }
            adapter.notifyDataSetChanged()
        }
    }

    private var swipeRefreshLayout: SwipeRefreshLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bus_list)

        setSupportActionBar(toolbar)
        toolbar.title = title

        if (bus_detail_container != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            twoPane = true
        }

        setupRecyclerView(bus_list)

        AndroidAPIService.standard.on(AndroidAPIService.standard.BUSES_CHANGED_EVENT, busObserver)
        AndroidAPIService.standard.reloadBuses(schoolId)

        swipeRefreshLayout = findViewById(R.id.swiperefresh)

        swipeRefreshLayout?.setOnRefreshListener(
                SwipeRefreshLayout.OnRefreshListener {
                    AndroidAPIService.standardForSchool(schoolId).reloadBuses {}
                    swipeRefreshLayout?.isRefreshing = false
                }
        )

        preferences = PreferenceManager.getDefaultSharedPreferences(this)
    }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        recyclerView.adapter = SimpleBusRecyclerViewAdapter(this, twoPane)
    }

    inner class SimpleBusRecyclerViewAdapter(private val parentActivity: BusListActivity, private val twoPane: Boolean) : RecyclerView.Adapter<SimpleBusRecyclerViewAdapter.ViewHolder>() {
        var buses = listOf<Bus>()
        private val saved get() = buses.filter { savedBuses.contains(it._id) }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.bus_list_content, parent, false)

            val holder = ViewHolder(view)

            holder.savedCheckbox.setOnClickListener {
                val id = holder.bus?._id
                if (id != null) {
                    val editor = preferences?.edit()
                    if (editor != null) {
                        editor.putStringSet(SAVED_BUSES_PREFERENCE_NAME, savedBuses.toMutableSet().apply {
                            if (holder.savedCheckbox.isChecked) {
                                add(id)
                            } else {
                                remove(id)
                            }
                        })
                        editor.apply()
                        notifyDataSetChanged()
                    }
                }
            }

            return holder
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = if (position >= saved.size) buses[position - saved.size] else saved[position]
            holder.busNameView.text = item.name

            val parent = holder.busLocationView.parent as RelativeLayout

            val location = item.getLocation(date)

            if (location == null) {
                holder.busLocationView.text = "?"
                parent.background = resources.getDrawable(R.drawable.bg_list_item)
                holder.busLocationView.setTextColor(resources.getColor(R.color.colorPrimary))
            } else {
                holder.busLocationView.text = location
                parent.background = resources.getDrawable(R.drawable.bg_list_item_arrived)
                holder.busLocationView.setTextColor(resources.getColor(R.color.white))
            }

            holder.savedCheckbox.isChecked = savedBuses.contains(item._id)

            with(holder.itemView) {
                tag = item
            }

            holder.divider.visibility = if (position == saved.size) View.VISIBLE else View.INVISIBLE
        }

        override fun getItemCount() = saved.size + buses.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val busNameView: TextView = view.bus_name
            val busLocationView: TextView = view.bus_location
            val savedCheckbox: CheckBox = view.bus_saved
            val divider: View = view.divider

            val bus get() = itemView.tag as? Bus
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AndroidAPIService.standard.off(AndroidAPIService.standard.BUSES_CHANGED_EVENT, busObserver)
    }
}
