package com.yourbcabus.android.yourbcabus

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.support.design.widget.Snackbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.yourbcabus.android.yourbcabus.dummy.DummyContent
import kotlinx.android.synthetic.main.activity_bus_list.*
import kotlinx.android.synthetic.main.bus_list_content.view.*
import kotlinx.android.synthetic.main.bus_list.*

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

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private var twoPane: Boolean = false

    private val busObserver: Observer = {
        bus_list.adapter?.notifyDataSetChanged()
    }

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
    }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        recyclerView.adapter = SimpleBusRecyclerViewAdapter(this, AndroidAPIService.standard, twoPane)
    }

    class SimpleBusRecyclerViewAdapter(private val parentActivity: BusListActivity, private val api: APIService, private val twoPane: Boolean) : RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleItemRecyclerViewAdapter.ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.bus_list_content, parent, false)
            return SimpleItemRecyclerViewAdapter.ViewHolder(view)
        }

        override fun onBindViewHolder(holder: SimpleItemRecyclerViewAdapter.ViewHolder, position: Int) {
            val item = api.buses.sortedBy { it.name }[position]
            holder.busNameView.text = item.name
            holder.busLocationView.text = item.locations?.firstOrNull()?.substring(0, 2)
            holder.busLocationView.text = if (holder.busLocationView.text != "") holder.busLocationView.text else "NA"

            with(holder.itemView) {
                tag = item
            }
        }

        override fun getItemCount() = api.buses.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val busNameView: TextView = view.bus_name
            val busLocationView: TextView = view.bus_location
        }
    }

    class SimpleItemRecyclerViewAdapter(private val parentActivity: BusListActivity,
                                        private val values: List<DummyContent.DummyItem>,
                                        private val twoPane: Boolean) :
            RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        private val onClickListener: View.OnClickListener

        init {
            onClickListener = View.OnClickListener { v ->
                val item = v.tag as DummyContent.DummyItem
                if (twoPane) {
                    val fragment = BusDetailFragment().apply {
                        arguments = Bundle().apply {
                            putString(BusDetailFragment.ARG_ITEM_ID, item.id)
                        }
                    }
                    parentActivity.supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.bus_detail_container, fragment)
                            .commit()
                } else {
                    val intent = Intent(v.context, BusDetailActivity::class.java).apply {
                        putExtra(BusDetailFragment.ARG_ITEM_ID, item.id)
                    }
                    v.context.startActivity(intent)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.bus_list_content, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.busNameView.text = item.content
            holder.busLocationView.text = item.content

            with(holder.itemView) {
                tag = item
                setOnClickListener(onClickListener)
            }
        }

        override fun getItemCount() = values.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val busNameView: TextView = view.bus_name
            val busLocationView: TextView = view.bus_location
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AndroidAPIService.standard.off(AndroidAPIService.standard.BUSES_CHANGED_EVENT, busObserver)
    }
}
