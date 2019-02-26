package com.yourbcabus.android.yourbcabus

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.yourbcabus.android.yourbcabus.dummy.DummyContent
import kotlinx.android.synthetic.main.activity_bus_detail.*
import kotlinx.android.synthetic.main.bus_detail.view.*

/**
 * A fragment representing a single Bus detail screen.
 * This fragment is either contained in a [BusListActivity]
 * in two-pane mode (on tablets) or a [BusDetailActivity]
 * on handsets.
 */
class BusDetailFragment : Fragment() {

    private var item: Bus? = null

    val schoolId = "5bca51e785aa2627e14db459"
    val apiService: APIService = AndroidAPIService.standardForSchool(schoolId)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            if (it.containsKey(ARG_BUS_ID)) {
                item = AndroidAPIService.standardForSchool(schoolId).getBus(it.getString(ARG_BUS_ID)?:"")
                activity?.toolbar_layout?.title = item?.name

            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.bus_detail, container, false)

        // Show the dummy content as text in a TextView.
        item.let {
            rootView.bus_detail.text = item?.name
        }

        return rootView
    }

    companion object {
        /**
         * The fragment argument representing the item ID that this fragment
         * represents.
         */
        const val ARG_BUS_ID = "bus_id"
    }
}
