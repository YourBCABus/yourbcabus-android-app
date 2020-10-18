package com.yourbcabus.android.yourbcabus

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.opengl.Visibility
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView

import kotlinx.android.synthetic.main.activity_bus_list.*
import kotlinx.android.synthetic.main.bus_list_content.view.*
import kotlinx.android.synthetic.main.bus_list.*
import android.support.v4.widget.SwipeRefreshLayout
import android.view.*
import com.google.firebase.messaging.FirebaseMessaging
import java.util.*
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.widget.*
import java.text.SimpleDateFormat


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
    private val savedBuses
        get() = preferences?.getStringSet(SAVED_BUSES_PREFERENCE_NAME, null) ?: setOf()

    private var date = Date()
    private var timer = Timer()

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

        noInternet.visibility = View.GONE

        AndroidAPIService.standard.on(AndroidAPIService.standard.BUSES_CHANGED_EVENT, busObserver)
        AndroidAPIService.standardForSchool(schoolId).reloadBuses {
            loadingPanel.visibility = View.GONE
            noInternet.visibility = View.VISIBLE
            bus_list.visibility = View.INVISIBLE
        }

        swipeRefreshLayout = findViewById(R.id.swiperefresh)

        swipeRefreshLayout?.setOnRefreshListener(
                SwipeRefreshLayout.OnRefreshListener {
                    AndroidAPIService.standardForSchool(schoolId).reloadBuses { onReloadBuses(it) }
                }
        )

        preferences = PreferenceManager.getDefaultSharedPreferences(this)

        window.navigationBarColor = resources.getColor(R.color.navigationBarDefaultColor)
    }

    override fun onResume() {
        super.onResume()
        timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                AndroidAPIService.standardForSchool(schoolId).reloadBuses { onReloadBuses(it, true) }
            }
        }, 0, 10000)
    }

    override fun onPause() {
        super.onPause()
        timer.cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.action_menu_refresh -> AndroidAPIService.standardForSchool(schoolId).reloadBuses { onReloadBuses(it) }
        }

        return super.onOptionsItemSelected(item)
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

            holder?.busView?.setOnClickListener { v ->
                val item = v.tag as Bus
                if (twoPane) {
                    val fragment = BusDetailFragment().apply {
                        arguments = Bundle().apply {
                            putString(BusDetailFragment.ARG_BUS_ID, item._id)
                        }
                    }
                    parentActivity.supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.bus_detail_container, fragment)
                            .commit()
                } else {
                    val intent = Intent(v.context, BusDetailActivity::class.java).apply {
                        putExtra(BusDetailFragment.ARG_BUS_ID, item._id)
                    }
                    v.context.startActivity(intent)
                }
            }

            holder.savedCheckbox.setOnClickListener {
                val id = holder.bus?._id
                if (id != null) {
                    val editor = preferences?.edit()
                    val notificationsEnabled = preferences?.getBoolean(NOTIFICATIONS_BUS_ARRIVAL_PREFERENCE_NAME, false) == true
                    if (editor != null) {
                        editor.putStringSet(SAVED_BUSES_PREFERENCE_NAME, savedBuses.toMutableSet().apply {
                            if (holder.savedCheckbox.isChecked) {
                                add(id)
                                if (notificationsEnabled) {
                                    FirebaseMessaging.getInstance().subscribeToTopic("school.$schoolId.bus.$id")
                                }
                            } else {
                                remove(id)
                                if (notificationsEnabled) {
                                    FirebaseMessaging.getInstance().unsubscribeFromTopic("school.$schoolId.bus.$id")
                                }
                            }
                        })
                        editor.apply()
                        notifyDataSetChanged()
                    }

                    val didAskUser = preferences?.getBoolean(DID_ASK_TO_ENABLE_NOTIFICATIONS_PREFERENCE_NAME, false) == true
                    if (!didAskUser) {
                        val editor = preferences?.edit()
                        if (editor != null) {
                            val inflater = LayoutInflater.from(applicationContext).inflate(R.layout.alertdialog, null)
                            val builder = AlertDialog.Builder(this.parentActivity, R.style.AlertDialogTheme)

                            builder.setView(inflater)
                            builder.setCancelable(false)
                            val alertDialog = builder.create()
                            alertDialog.show()
                            val height = resources.displayMetrics.heightPixels
                            val width = resources.displayMetrics.widthPixels
                            alertDialog?.window?.setLayout(width, height)
                            window.navigationBarColor = resources.getColor(R.color.colorPrimaryLight)
                            window.statusBarColor = resources.getColor(R.color.colorPrimaryLight)
                            editor.putBoolean(DID_ASK_TO_ENABLE_NOTIFICATIONS_PREFERENCE_NAME, true)

                            val positiveButton = inflater.findViewById(R.id.alertDesignPositive) as Button
                            positiveButton.setOnClickListener {
                                try {
                                    editor.putBoolean(NOTIFICATIONS_BUS_ARRIVAL_PREFERENCE_NAME, true)
                                    editor.apply()
                                    if (holder.savedCheckbox.isChecked) {
                                        FirebaseMessaging.getInstance().subscribeToTopic("school.$schoolId.bus.$id")
                                    } else {
                                        FirebaseMessaging.getInstance().unsubscribeFromTopic("school.$schoolId.bus.$id")
                                    }
                                    alertDialog.dismiss()
                                    window.navigationBarColor = resources.getColor(R.color.navigationBarDefaultColor)
                                    window.statusBarColor = resources.getColor(R.color.colorPrimaryDark)
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                }
                            }

                            val negativeButton = inflater.findViewById(R.id.alertDesignNegative) as Button
                            negativeButton.setOnClickListener {
                                try {
                                    editor.putBoolean(NOTIFICATIONS_BUS_ARRIVAL_PREFERENCE_NAME, false)
                                    editor.apply()
                                    alertDialog.dismiss()
                                    window.navigationBarColor = resources.getColor(R.color.navigationBarDefaultColor)
                                    window.statusBarColor = resources.getColor(R.color.colorPrimaryDark)
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                }
                            }
                        }
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

            var indicatorColor: Int
            var detailString: String

            if (item.boarding == null) {
                indicatorColor = R.color.indicatorNArrive
                detailString = "Not at BCA"
            } else {
                when {
                    item.boarding < 150 -> {
                        indicatorColor = R.color.indicator1
                        detailString = "Expected Early"
                    }
                    item.boarding <= 600 -> {
                        indicatorColor = R.color.indicator2
                        detailString = "Expected On Time"
                    }
                    item.boarding <= 900 -> {
                        indicatorColor = R.color.indicator3
                        detailString = "Expected Slightly Late"
                    }
                    item.boarding < 1200 -> {
                        indicatorColor = R.color.indicator4
                        detailString = "Expected Late"
                    }
                    else -> {
                        indicatorColor = R.color.indicator5
                        detailString = "Expected Very Late"
                    }
                }
            }

            if (location == null) {
                holder.busLocationView.text = "?"
                parent.background = resources.getDrawable(R.drawable.bg_list_item)
                holder.busLocationView.setTextColor(resources.getColor(R.color.colorPrimary))
                if (!item.available) {
                    indicatorColor = R.color.indicatorNAvailable
                    detailString = "Not running"
                }
            } else {
                holder.busLocationView.text = location
                parent.background = resources.getDrawable(R.drawable.bg_list_item_arrived)
                holder.busLocationView.setTextColor(resources.getColor(R.color.white))
                indicatorColor = R.color.indicatorArrive
                detailString = "Arrived at BCA"
            }

            if (item.departure != null) {
                val cal = GregorianCalendar()
                cal.set(Calendar.HOUR_OF_DAY, item.departure / 60)
                cal.set(Calendar.MINUTE, item.departure % 60)
                cal.set(Calendar.SECOND, 0)

                val departureString = SimpleDateFormat("h:mm", Locale.US).format(cal.time)

                detailString = if (Date() <= cal.time)
                    "Departs at $departureString"
                else
                    "Departed at $departureString"
            }

            holder.busBoardingView.background.setTint(resources.getColor(indicatorColor))
            holder.busDetailView.text = detailString

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
            val busDetailView: TextView = view.bus_details
            val savedCheckbox: CheckBox = view.bus_saved
            val divider: View = view.divider
            val busView: View = view.bus_view
            val busBoardingView: View = view.bus_boarding_indicator

            val bus get() = itemView.tag as? Bus
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AndroidAPIService.standard.off(AndroidAPIService.standard.BUSES_CHANGED_EVENT, busObserver)
    }

    private fun onReloadBuses(it: Boolean) {
        onReloadBuses(it, false)
    }

    private fun onReloadBuses(it: Boolean, timer: Boolean) {
        swipeRefreshLayout?.isRefreshing = false
        if (it) {
            noInternet.visibility = View.GONE
            bus_list.visibility = View.VISIBLE
        } else {
            if (!timer) Toast.makeText(this, "There is no Internet connection.", Toast.LENGTH_LONG).show()
        }
    }
}
