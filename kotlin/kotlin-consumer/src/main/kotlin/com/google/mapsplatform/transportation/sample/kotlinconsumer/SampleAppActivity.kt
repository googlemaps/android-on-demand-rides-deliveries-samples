/* Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("Deprecation")

package com.google.mapsplatform.transportation.sample.kotlinconsumer

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.tasks.Task
import com.google.android.libraries.mapsplatform.transportation.consumer.ConsumerApi
import com.google.android.libraries.mapsplatform.transportation.consumer.managers.TripModel
import com.google.android.libraries.mapsplatform.transportation.consumer.managers.TripModelManager
import com.google.android.libraries.mapsplatform.transportation.consumer.model.TerminalLocation
import com.google.android.libraries.mapsplatform.transportation.consumer.model.Trip
import com.google.android.libraries.mapsplatform.transportation.consumer.model.TripInfo
import com.google.android.libraries.mapsplatform.transportation.consumer.model.TripWaypoint
import com.google.android.libraries.mapsplatform.transportation.consumer.sessions.JourneySharingSession
import com.google.android.libraries.mapsplatform.transportation.consumer.view.ConsumerController
import com.google.android.libraries.mapsplatform.transportation.consumer.view.ConsumerGoogleMap
import com.google.android.libraries.mapsplatform.transportation.consumer.view.ConsumerGoogleMap.ConsumerMapReadyCallback
import com.google.android.libraries.mapsplatform.transportation.consumer.view.ConsumerMapView
import com.google.android.material.snackbar.Snackbar
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.ProviderUtils
import com.google.mapsplatform.transportation.sample.kotlinconsumer.state.AppStates
import java.util.Date

/** Main activity for the sample application. */
class SampleAppActivity : AppCompatActivity(), ConsumerViewModel.JourneySharingListener {
  // The current journey sharing trip status.
  private lateinit var tripStatusView: TextView

  // Displays the users trip id.
  private lateinit var tripIdView: TextView

  // Displays the users vehicle id.
  private lateinit var vehicleIdView: TextView

  // Indicates the Eta to the next waypoint in minutes.
  private lateinit var etaView: TextView

  // Indicates the remaining distance to the next waypoint.
  private lateinit var remainingDistanceView: TextView

  // The ridesharing map.
  private lateinit var consumerMapView: ConsumerMapView

  // Multipurpose button depending on the app state (could be for selecting, drop-off, pickup, or
  // requesting trip)
  private lateinit var actionButton: Button

  // Button that adds a stop in between pickup/drop-off. Only visible when selecting drop-off.
  private lateinit var addStopButton: Button

  // Dropoff pin in the center of the map.
  private lateinit var dropoffPin: View

  // Pickup pin in the center of the map.
  private lateinit var pickupPin: View

  // The reference to the map that is being displayed.
  private var googleMap: ConsumerGoogleMap? = null

  // The last location of the device.
  private var lastLocation: LatLng? = null

  // Marker representing the pickup location.
  private var pickupMarker: Marker? = null

  // Marker representing the dropoff location.
  private var dropoffMarker: Marker? = null

  // Array of markers representing intermediate stops during trip preview.
  private val intermediateStopsMarkers: MutableList<Marker> = mutableListOf()

  // Array of markers representing stops that driver has to make before starting the current trip.
  private val previousTripStopsMarkers: MutableList<Marker> = mutableListOf()

  // ViewModel for the consumer sample app.
  private lateinit var consumerViewModel: ConsumerViewModel
  private var tripPreviewPolyline: Polyline? = null
  private lateinit var tripModelManager: TripModelManager
  private lateinit var consumerController: ConsumerController

  // Session monitoring the current active trip.
  private var journeySharingSession: JourneySharingSession? = null
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)
    initViews()

    // Get the ViewModel.
    consumerViewModel = ViewModelProviders.of(this).get(ConsumerViewModel::class.java)
    consumerViewModel.setJourneySharingListener(this)
    initializeSdk()
    Log.i(TAG, "Consumer SDK version: " + ConsumerApi.getConsumerSDKVersion())
  }

  /**
   * Start the asynchronous call to get the map api and subsequently the consumer controller. The
   * ConsumerGoogleMap is returned, is used to access the ConsumerController. Observers are created
   * that update the UI based on observed trip data.
   */
  private fun initializeSdk() {
    showStartupLocation()
    consumerMapView.getConsumerGoogleMapAsync(
      object : ConsumerMapReadyCallback() {
        override fun onConsumerMapReady(consumerGoogleMap: ConsumerGoogleMap) {
          // Safe to do so as controller will only be nullified during consumerMap's onDestroy()
          consumerController = consumerGoogleMap.consumerController!!
          val consumerApiTask =
            ConsumerApi.initialize(
              this@SampleAppActivity,
              ProviderUtils.getProviderId(this@SampleAppActivity),
              TripAuthTokenFactory(application)
            )
          consumerApiTask?.addOnSuccessListener { consumerApi: ConsumerApi ->
            tripModelManager = consumerApi.tripModelManager!!
          }
          consumerApiTask?.addOnFailureListener { task: Exception ->
            Log.e(
              TAG,
              """
               ConsumerApi Initialization Error:
               ${task.message}
               """.trimIndent()
            )
          }
          ConsumerMarkerUtils.setCustomMarkers(consumerController, this@SampleAppActivity)
          setupViewBindings()
          googleMap = consumerGoogleMap
          centerCameraToLastLocation()
          setupMapListener()
        }
      },
      /* fragmentActivity= */ this,
      /* googleMapOptions= */ null
    )
  }

  override fun startJourneySharing(
    tripData: com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.model.TripData
  ): TripModel {
    val trip = tripModelManager.getTripModel(tripData.tripName)
    journeySharingSession =
      JourneySharingSession.createInstance(trip)?.also { consumerController.showSession(it) }
    return trip
  }

  override fun stopJourneySharing() {
    if (journeySharingSession != null) {
      journeySharingSession!!.stop()
      journeySharingSession = null
    }
    consumerController.hideAllSessions()
  }

  /** Center the camera to the last location of the device. */
  private fun centerCameraToLastLocation() {
    lastLocation?.also {
      googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(it, DEFAULT_ZOOM.toFloat()))
    }
  }

  override fun updateCurrentLocation(latLang: LatLng) {
    lastLocation = latLang
  }

  // Permissions are managed by the 'SplashScreenActivity'
  @SuppressLint("MissingPermission")
  private fun showStartupLocation() {
    val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    fusedLocationProviderClient.lastLocation.addOnCompleteListener { task: Task<Location?> ->
      lastLocation =
        if (task.isSuccessful && task.result != null) {
          val location = task.result
          LatLng(location!!.latitude, location.longitude)
        } else {
          DEFAULT_MAP_LOCATION
        }
      googleMap?.also { centerCameraToLastLocation() }
    }
  }

  /** Setting up map idle listeners to select pickup and dropoff locations. */
  private fun setupMapListener() {
    googleMap?.setOnCameraIdleListener(
      GoogleMap.OnCameraIdleListener {
        val cameraPosition = googleMap?.cameraPosition ?: return@OnCameraIdleListener
        @AppStates val state = consumerViewModel.getAppState().value ?: return@OnCameraIdleListener
        if (state != AppStates.SELECTING_DROPOFF && state != AppStates.SELECTING_PICKUP) {
          return@OnCameraIdleListener
        }
        val cameraLocation = cameraPosition.target
        val terminalLocation = TerminalLocation.create(cameraLocation)
        consumerViewModel.updateLocationPointForState(cameraLocation)
        updateMarkerBasedOnState(terminalLocation)
      }
    )
  }

  /**
   * Updates the current marker (depending on app state) to the given location. Ex: when app state
   * is 'SELECTING_PICKUP', it updates the pickupMarker object.
   */
  private fun updateMarkerBasedOnState(location: TerminalLocation) {
    @AppStates val state = consumerViewModel.getAppState().value ?: return
    if (state == AppStates.SELECTING_PICKUP) {
      if (pickupMarker == null) {
        pickupMarker =
          googleMap?.addMarker(
            ConsumerMarkerUtils.getConsumerMarkerOptions(this, ConsumerMarkerType.PICKUP_POINT)
              .position(location.latLng)
          )
      }
      pickupMarker?.apply { position = location.latLng }
    } else if (state == AppStates.SELECTING_DROPOFF) {
      if (dropoffMarker == null) {
        dropoffMarker =
          googleMap?.addMarker(
            ConsumerMarkerUtils.getConsumerMarkerOptions(this, ConsumerMarkerType.DROPOFF_POINT)
              .position(location.latLng)
          )
      }
      dropoffMarker?.apply { position = location.latLng }
    }
  }

  /**
   * When first selecting pickup, the idle handler on map is not initiated since the map was already
   * idle when selecting initial pickup. Select the current location as pickup initially.
   */
  private fun resetPickupMarkerAndLocation() {
    val cameraLocation = googleMap?.cameraPosition?.target ?: return
    consumerViewModel.setPickupLocation(cameraLocation)
    updateMarkerBasedOnState(TerminalLocation.builder(cameraLocation).build())
  }

  /**
   * When first selecting pickup, the idle handler on map is not initiated since the map was already
   * idle when selecting initial pickup. Select the current location as pickup initially.
   */
  private fun resetDropoffMarkerAndLocation() {
    val cameraLocation = googleMap?.cameraPosition?.target ?: return
    consumerViewModel.setDropoffLocation(cameraLocation)
    updateMarkerBasedOnState(TerminalLocation.create(cameraLocation))
  }

  /** Display the trip status based on the observed trip status. */
  private fun displayTripStatus(status: Int) {
    when (status) {
      Trip.TripStatus.NEW -> {
        if (consumerViewModel.isTripMatched &&
            consumerViewModel.getTripInfo().value?.nextWaypoint != null
        ) {
          val titleId = // Dropping someone else as part of a B2B trip.
            if (consumerViewModel.isDriverCompletingAnotherTrip)
              R.string.state_completing_another_trip
            else R.string.state_enroute_to_pickup
          setTripStatusTitle(titleId)
        } else {
          setTripStatusTitle(R.string.state_new)
        }
        actionButton.visibility = View.INVISIBLE
      }
      Trip.TripStatus.ENROUTE_TO_PICKUP -> {
        removeAllMarkers()
        setTripStatusTitle(R.string.state_enroute_to_pickup)
      }
      Trip.TripStatus.ARRIVED_AT_PICKUP -> setTripStatusTitle(R.string.state_arrived_at_pickup)
      Trip.TripStatus.ENROUTE_TO_DROPOFF -> setTripStatusTitle(R.string.state_enroute_to_dropoff)
      Trip.TripStatus.ARRIVED_AT_INTERMEDIATE_DESTINATION ->
        setTripStatusTitle(R.string.state_arrived_at_intermediate_destination)
      Trip.TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION ->
        setTripStatusTitle(R.string.state_enroute_to_intermediate_destination)
      Trip.TripStatus.COMPLETE, Trip.TripStatus.CANCELED -> {
        setTripStatusTitle(R.string.state_end_of_trip)
        hideTripData()
        removeAllMarkers()
      }
      else -> {}
    }
  }

  /**
   * This draws markers that need to be displayed *only* during 'JourneySharing`. Example: Waypoints
   * belonging to a previous trip are not displayed by ConsumerSDK, so we manually render them.
   */
  private fun drawJourneySharingStateMarkers(previousTripWaypoints: List<TripWaypoint>?) {
    var addedMarker: Marker?
    previousTripWaypoints?.forEach { waypoint ->
      addedMarker =
        googleMap?.addMarker(
          ConsumerMarkerUtils.getConsumerMarkerOptions(
              this@SampleAppActivity,
              ConsumerMarkerType.PREVIOUS_TRIP_PENDING_POINT
            )
            .position(waypoint.terminalLocation.latLng)
        )
      addedMarker?.also { previousTripStopsMarkers.add(it) }
    }
  }

  /** Clears the list of markers drawn in drawJourneySharingStateMarkers. */
  private fun clearJourneySharingStateMarkers() {
    for (marker in previousTripStopsMarkers) {
      marker.remove()
    }
    previousTripStopsMarkers.clear()
  }

  /** Display the reported map state and show trip data when in journey sharing state. */
  private fun displayAppState(state: Int) {
    when (state) {
      AppStates.SELECTING_PICKUP -> {
        setTripStatusTitle(R.string.state_select_pickup)
        centerCameraToLastLocation()
        pickupPin.visibility = View.VISIBLE
        resetPickupMarkerAndLocation()
      }
      AppStates.SELECTING_DROPOFF -> {
        setTripStatusTitle(R.string.state_select_dropoff)
        pickupPin.visibility = View.INVISIBLE
        dropoffPin.visibility = View.VISIBLE
        addStopButton.visibility = View.VISIBLE
      }
      AppStates.CONFIRMING_TRIP -> {
        dropoffPin.visibility = View.INVISIBLE
        tripStatusView.visibility = View.INVISIBLE
        addStopButton.visibility = View.GONE
      }
      AppStates.JOURNEY_SHARING -> {
        clearTripPreviewPolyline()
        setTripStatusTitle(R.string.state_enroute_to_pickup)
      }
      AppStates.INITIALIZED -> {
        tripStatusView.visibility = View.INVISIBLE
        resetActionButton()
        removeAllMarkers()
        hideTripData()
      }
      else -> hideTripData()
    }
  }

  /**
   * Callback method fired with the 'Add stop' button (+) has been pressed/touched. It grabs the
   * location of the 'DropoffLocation' container, removes the current 'dropoffMarker' and replaces
   * it with a 'intermediateDestinationMarker'.
   */
  private fun onAddStopButtonTapped() {
    val marker = dropoffMarker ?: return
    consumerViewModel.addIntermediateDestination()

    // Create an intermediate destination in place of the dropoffMarker.
    val intermediateStopMarker =
      googleMap?.addMarker(
        ConsumerMarkerUtils.getConsumerMarkerOptions(
            this,
            ConsumerMarkerType.INTERMEDIATE_DESTINATION_POINT
          )
          .position(marker.position)
      )
    intermediateStopsMarkers.add(intermediateStopMarker!!)
    marker.remove()
    dropoffMarker = null
    resetDropoffMarkerAndLocation()
  }

  /** Remove markers on the map used for preview. (pickup, dropoff, intermediate destinations) . */
  private fun removeAllMarkers() {
    if (dropoffMarker != null) {
      dropoffMarker!!.remove()
      dropoffMarker = null
    }
    if (pickupMarker != null) {
      pickupMarker!!.remove()
      pickupMarker = null
    }
    intermediateStopsMarkers.forEach { it.remove() }
    intermediateStopsMarkers.clear()
  }

  /** Set button to be initial state. */
  private fun resetActionButton() {
    actionButton.setText(R.string.request_button_label)
    actionButton.visibility = View.VISIBLE
    val roundedButton = actionButton.background
    DrawableCompat.setTint(roundedButton, ContextCompat.getColor(this, R.color.actionable))
  }

  private fun hideTripData() {
    tripIdView.visibility = View.INVISIBLE
    vehicleIdView.visibility = View.INVISIBLE
    etaView.visibility = View.INVISIBLE
    remainingDistanceView.visibility = View.INVISIBLE
  }

  /** Sets the UI state based on the trip state. */
  private fun setTripStatusTitle(@StringRes resourceId: Int) {
    tripStatusView.setText(resourceId)
    tripStatusView.visibility = View.VISIBLE
  }

  /** Updates the displayed trip Id. */
  private fun displayTripId(tripId: String?) {
    if (tripId == null) {
      tripIdView.text = ""
      return
    }
    tripIdView.text = resources.getString(R.string.trip_id_label, tripId)
    tripIdView.visibility = View.VISIBLE
  }

  private fun displayTripInfo(tripInfo: TripInfo?) {
    if (tripInfo == null || tripInfo.vehicleId == null) {
      vehicleIdView.text = ""
      return
    }
    vehicleIdView.text = resources.getString(R.string.vehicle_id_label, tripInfo.vehicleId)
    displayTripStatus(tripInfo.tripStatus)
    val visibility =
      if (tripInfo.tripStatus == Trip.TripStatus.COMPLETE ||
          tripInfo.tripStatus == Trip.TripStatus.CANCELED
      )
        View.INVISIBLE
      else View.VISIBLE
    vehicleIdView.visibility = visibility
  }

  private fun onActionButtonTapped() {
    when (consumerViewModel.getAppState().value) {
      AppStates.INITIALIZED -> {
        centerCameraToLastLocation()
        actionButton.setText(R.string.pickup_label)
        consumerViewModel.setState(AppStates.SELECTING_PICKUP)
      }
      AppStates.UNINITIALIZED -> {
        actionButton.setText(R.string.pickup_label)
        consumerViewModel.setState(AppStates.SELECTING_PICKUP)
      }
      AppStates.SELECTING_PICKUP -> {
        actionButton.setText(R.string.dropoff_label)
        consumerViewModel.setState(AppStates.SELECTING_DROPOFF)
      }
      AppStates.SELECTING_DROPOFF -> {
        actionButton.setText(R.string.confirm_trip_label)
        consumerViewModel.setState(AppStates.CONFIRMING_TRIP)
        drawTripPreviewPolyline()
        centerCameraForTripPreview()
      }
      AppStates.CONFIRMING_TRIP -> // Create trip
      consumerViewModel.startSingleExclusiveTrip()
    }
  }

  /** Display the minutes remaining to the next waypoint. */
  private fun displayEta(etaValue: Long?) {
    if (etaValue == null) {
      etaView.text = ""
      return
    }
    val current = Date()
    val diff = etaValue - current.time
    val minutes = (diff / MINUTE_IN_MILLIS).toDouble().toInt()
    etaView.visibility = View.VISIBLE
    if (minutes < 1) {
      etaView.text = resources.getString(R.string.eta_within_one_minute)
      return
    }
    etaView.text = resources.getQuantityString(R.plurals.eta_format_string, minutes, minutes)
  }

  /** Displays the remaining distance to the next waypoint. */
  private fun displayRemainingDistance(remainingDistanceMeters: Int?) {
    if (remainingDistanceMeters == null || remainingDistanceMeters <= 0) {
      remainingDistanceView.text = ""
      return
    }
    if (remainingDistanceMeters >= 1000) {
      // Display remaining distance in km.
      remainingDistanceView.text =
        getString(R.string.distance_format_string_km, remainingDistanceMeters / 1000.0)
    } else {
      // Display remaining distance in m.
      remainingDistanceView.text =
        resources.getString(R.string.distance_format_string_meters, remainingDistanceMeters)
    }
    remainingDistanceView.visibility = View.VISIBLE
  }

  private fun displayErrorMessage(@StringRes resourceId: Int) {
    Snackbar.make(tripStatusView, resourceId, Snackbar.LENGTH_SHORT).show()
  }

  private fun displayPreviousTripMarkers(waypoints: List<TripWaypoint>?) {
    clearJourneySharingStateMarkers()
    if (consumerViewModel.getAppState().value == AppStates.JOURNEY_SHARING) {
      drawJourneySharingStateMarkers(waypoints)
    }
  }

  /** Renders a polyline representing all the points contained for the given trip. */
  private fun drawTripPreviewPolyline() {
    val pickupLocation = consumerViewModel.getPickupLocation()
    val dropoffLocation = consumerViewModel.getDropoffLocation()
    val intermediateDestinations = consumerViewModel.getIntermediateDestinations()
    if (pickupLocation == null || dropoffLocation == null) {
      return
    }
    val polylineOptions =
      PolylineOptions()
        .width(8.0f)
        .color(TRIP_PREVIEW_POLYLINE_COLOR)
        .geodesic(true)
        .add(pickupLocation)
    intermediateDestinations?.forEach { polylineOptions.add(it) }
    polylineOptions.add(dropoffLocation)
    tripPreviewPolyline = googleMap?.addPolyline(polylineOptions)
  }

  /** Centers the camera within the bounds of the trip preview polyline. */
  private fun centerCameraForTripPreview() {
    val pickupLocation = consumerViewModel.getPickupLocation()
    val dropoffLocation = consumerViewModel.getDropoffLocation()
    val intermediateDestinations = consumerViewModel.getIntermediateDestinations()
    if (pickupLocation == null || dropoffLocation == null) {
      return
    }
    val boundsBuilder = LatLngBounds.builder()
    boundsBuilder.include(pickupLocation)
    boundsBuilder.include(dropoffLocation)
    intermediateDestinations?.forEach { boundsBuilder.include(it) }

    googleMap?.moveCamera(
      CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), TRIP_PREVIEW_CAMERA_PADDING)
    )
  }

  /** Removes the trip preview polyline after a trip has been confirmed/cancelled. */
  private fun clearTripPreviewPolyline() =
    tripPreviewPolyline?.apply { remove() }

  private fun initViews() {
    // Find the UI views for later updates.
    tripStatusView = findViewById(R.id.tripStatus)
    tripIdView = findViewById(R.id.tripId)
    vehicleIdView = findViewById(R.id.vehicleId)
    etaView = findViewById(R.id.eta)
    remainingDistanceView = findViewById(R.id.remainingDistance)
    consumerMapView = findViewById(R.id.consumer_map_view)
    pickupPin = findViewById(R.id.pickup_pin)
    dropoffPin = findViewById(R.id.dropoff_pin)
    actionButton = findViewById(R.id.actionButton)
    actionButton.setOnClickListener { onActionButtonTapped() }
    addStopButton = findViewById(R.id.addStopButton)
    addStopButton.setOnClickListener { onAddStopButtonTapped() }
    resetActionButton()
  }

  private fun setupViewBindings() {
    // Start observing trip data.
    consumerViewModel
      .getTripStatus()
      .observe(this@SampleAppActivity, { status: Int -> displayTripStatus(status) })
    consumerViewModel
      .getAppState()
      .observe(this@SampleAppActivity, { state: Int -> displayAppState(state) })
    consumerViewModel
      .getTripId()
      .observe(this@SampleAppActivity, { tripId: String? -> displayTripId(tripId) })
    consumerViewModel
      .getTripInfo()
      .observe(this@SampleAppActivity, { tripInfo: TripInfo? -> displayTripInfo(tripInfo) })
    consumerViewModel
      .getNextWaypointEta()
      .observe(this@SampleAppActivity, { etaValue: Long? -> displayEta(etaValue) })
    consumerViewModel
      .getRemainingDistanceMeters()
      .observe(
        this@SampleAppActivity,
        { remainingDistanceMeters: Int? -> displayRemainingDistance(remainingDistanceMeters) }
      )
    consumerViewModel.errorMessage.observe(
      this@SampleAppActivity,
      { resourceId: Int? -> resourceId?.also { displayErrorMessage(it) } }
    )
    consumerViewModel
      .getPreviousTripWaypoints()
      .observe(
        this@SampleAppActivity,
        { waypoints: List<TripWaypoint>? -> displayPreviousTripMarkers(waypoints) }
      )
  }

  override fun onDestroy() {
    super.onDestroy()
    consumerViewModel.unregisterTripCallback()
  }

  companion object {
    private const val TAG = "SampleAppActivity"
    private const val MINUTE_IN_MILLIS = (1000 * 60).toLong()

    /** Default zoom of initial map state. */
    private const val DEFAULT_ZOOM = 16

    /** Default Map location if failed to receive FLP location. Defaulted to Google MTV. */
    private val DEFAULT_MAP_LOCATION = LatLng(37.423061, -122.084051)

    // Default padding used when moving the camera within the bounds of the trip preview polyline.
    private const val TRIP_PREVIEW_CAMERA_PADDING = 48

    // Default color used for the trip preview polyline.
    private val TRIP_PREVIEW_POLYLINE_COLOR = Color.rgb(69, 151, 255)
  }
}
