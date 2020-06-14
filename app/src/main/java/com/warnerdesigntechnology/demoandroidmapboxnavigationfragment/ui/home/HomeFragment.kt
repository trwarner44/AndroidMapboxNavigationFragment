package com.warnerdesigntechnology.demoandroidmapboxnavigationfragment.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.warnerdesigntechnology.demoandroidmapboxnavigationfragment.R
import kotlinx.android.synthetic.main.navigation_view_fragment_layout.view.*
import retrofit2.Call
import retrofit2.Response
import com.warnerdesigntechnology.demoandroidmapboxnavigationfragment.ui.home.mapboxAccessToken

class HomeFragment: Fragment(),
    OnNavigationReadyCallback, NavigationListener, ProgressChangeListener {

    private var navigationView: com.mapbox.services.android.navigation.ui.v5.NavigationView? = null
    private var directionsRoute: DirectionsRoute? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Mapbox.getInstance(requireContext(), mapboxAccessToken)

        return inflater.inflate(R.layout.navigation_view_fragment_layout, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        updateNightMode()

        navigationView = view.findViewById(R.id.navigation_view_fragment)
        navigationView!!.onCreate(savedInstanceState)
        navigationView!!.initialize(this)
    }

    override fun onStart() {
        super.onStart()
        navigationView!!.onStart()
    }

    override fun onResume() {
        super.onResume()
        navigationView!!.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        println("on save instance state")
        navigationView!!.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            navigationView!!.onRestoreInstanceState(savedInstanceState)
        }
    }

    override fun onPause() {
        super.onPause()
        navigationView!!.onPause()
    }

    override fun onStop() {
        super.onStop()
        navigationView!!.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        navigationView!!.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        navigationView!!.onDestroy()
    }

    override fun onNavigationReady(isRunning: Boolean) {
        val origin: Point = Point.fromLngLat(
            ORIGIN_LONGITUDE,
            ORIGIN_LATITUDE
        )
        val destination: Point = Point.fromLngLat(
            DESTINATION_LONGITUDE,
            DESTINATION_LATITUDE
        )
        fetchRoute(origin, destination)
    }

    override fun onCancelNavigation() {
        navigationView!!.stopNavigation()
        stopNavigation()
    }

    override fun onNavigationFinished() {
// no-op
    }

    override fun onNavigationRunning() {
// no-op
    }

    override fun onProgressChange(
        location: Location?,
        routeProgress: RouteProgress
    ) {
        val isInTunnel: Boolean = routeProgress.inTunnel()
        val wasInTunnel = wasInTunnel()
        if (isInTunnel) {
            if (!wasInTunnel) {
                updateWasInTunnel(true)
                updateCurrentNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        } else {
            if (wasInTunnel) {
                updateWasInTunnel(false)
                updateCurrentNightMode(AppCompatDelegate.MODE_NIGHT_AUTO)
            }
        }
    }

    @SuppressLint("WrongConstant")
    private fun updateNightMode() {
        if (wasNavigationStopped()) {
            updateWasNavigationStopped(false)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO)
            requireActivity().recreate()
        }
    }

    private fun fetchRoute(origin: Point, destination: Point) {
        NavigationRoute.builder(context)
            .accessToken(mapboxAccessToken)
            .origin(origin)
            .destination(destination)
            .build()
            .getRoute(object : SimplifiedCallback() {
                override fun onResponse(
                    call: Call<DirectionsResponse?>?,
                    response: Response<DirectionsResponse?>
                ) {
                    directionsRoute = response.body()!!.routes().get(0)
                    startNavigation()
                }
            })

    }

    private fun startNavigation() {
        if (directionsRoute == null) {
            return
        }
        val options: NavigationViewOptions = NavigationViewOptions.builder()
            .directionsRoute(directionsRoute)
            .shouldSimulateRoute(true)
            .navigationListener(this@HomeFragment)
            .progressChangeListener(this)
            .build()
        navigationView!!.startNavigation(options)
    }

    private fun stopNavigation() {
        val activity = activity
        if (activity != null && activity is FragmentNavigationActivity) {
            val fragmentNavigationActivity: FragmentNavigationActivity =
                activity as FragmentNavigationActivity
            fragmentNavigationActivity.showPlaceholderFragment()
            fragmentNavigationActivity.showNavigationFab()
            updateWasNavigationStopped(true)
            updateWasInTunnel(false)
        }
    }

    private fun wasInTunnel(): Boolean {
        val context: Context? = activity
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getBoolean(requireContext().getString(R.string.was_in_tunnel), false)
    }

    private fun updateWasInTunnel(wasInTunnel: Boolean) {
        val context: Context? = activity
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = preferences.edit()
        editor.putBoolean(requireContext().getString(R.string.was_in_tunnel), wasInTunnel)
        editor.apply()
    }

    private fun updateCurrentNightMode(nightMode: Int) {
        AppCompatDelegate.setDefaultNightMode(nightMode)
        requireActivity().recreate()
    }

    private fun wasNavigationStopped(): Boolean {
        val context: Context? = activity
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getBoolean(getString(R.string.was_navigation_stopped), false)
    }

    fun updateWasNavigationStopped(wasNavigationStopped: Boolean) {
        val context: Context? = activity
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = preferences.edit()
        editor.putBoolean(getString(R.string.was_navigation_stopped), wasNavigationStopped)
        editor.apply()
    }

    companion object {
        private const val ORIGIN_LONGITUDE = -3.714873
        private const val ORIGIN_LATITUDE = 40.397389
        private const val DESTINATION_LONGITUDE = -3.712331
        private const val DESTINATION_LATITUDE = 40.401686
    }
}
