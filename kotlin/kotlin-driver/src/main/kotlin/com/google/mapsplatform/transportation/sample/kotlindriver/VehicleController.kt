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
package com.google.mapsplatform.transportation.sample.kotlindriver

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.DriverContext
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.DriverContext.DriverStatusListener.StatusCode
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.DriverContext.DriverStatusListener.StatusLevel
import com.google.android.libraries.mapsplatform.transportation.driver.api.ridesharing.RidesharingDriverApi
import com.google.android.libraries.mapsplatform.transportation.driver.api.ridesharing.vehiclereporter.RidesharingVehicleReporter
import com.google.android.libraries.mapsplatform.transportation.driver.api.ridesharing.vehiclereporter.RidesharingVehicleReporter.VehicleState
import com.google.android.libraries.navigation.ListenableResultFuture
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.Waypoint as NavigationWaypoint
import com.google.common.util.concurrent.MoreExecutors
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.ProviderUtils
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.request.VehicleSettings
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.response.VehicleModel
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.response.Waypoint
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.service.LocalProviderService
import com.google.mapsplatform.transportation.sample.kotlindriver.state.TripState
import com.google.mapsplatform.transportation.sample.kotlindriver.state.TripStatus
import com.google.mapsplatform.transportation.sample.kotlindriver.utils.TripUtils
import java.lang.ref.WeakReference
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** Controls vehicle related functionalities. */
@Suppress("UnstableApiUsage")
class VehicleController(
  private val navigator: Navigator,
  private val executor: ExecutorService,
  context: Context,
  private val coroutineScope: CoroutineScope,
  private val providerService: LocalProviderService,
  private val localSettings: LocalSettings,
) {
  private val authTokenFactory: TripAuthTokenFactory = TripAuthTokenFactory(providerService)
  private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)
  private val sequentialExecutor: Executor = MoreExecutors.newSequentialExecutor(executor)
  private val vehicleSimulator: VehicleSimulator =
    VehicleSimulator(navigator.simulator, localSettings)
  private var presenterRef = WeakReference<Presenter>(null)
  private lateinit var vehicleReporter: RidesharingVehicleReporter
  private var tripState: TripState? = null
  private var vehicleStateJob: Job? = null
  lateinit var vehicleSettings: VehicleSettings

  private var currentWaypoint: Waypoint? = null
  private var nextWaypoint: Waypoint? = null
  private var nextWaypointOfCurrentTrip: Waypoint? = null

  private var waypoints: List<Waypoint> = emptyList()
  private var matchedTripIds: List<String> = emptyList()

  private val tripStates: MutableMap<String, TripState> = mutableMapOf()

  fun onVehicleStateUpdate(vehicleModel: VehicleModel) {
    Log.i(TAG, "onVehicleStateUpdate")

    waypoints = vehicleModel.waypoints
    matchedTripIds = vehicleModel.currentTripsIds

    // Reset and recalculate waypoints every server state update.
    currentWaypoint = null
    nextWaypoint = null
    nextWaypointOfCurrentTrip = null

    if (!waypoints.isEmpty()) {
      var updatedNextWaypointOfCurrentTrip: Waypoint? = null

      waypoints.forEachIndexed { index, waypoint ->
        Log.i(TAG, "$waypoint.tripId $waypoint.waypointType")

        val isTripForWaypointAccepted = tripStates.containsKey(waypoint.tripId)

        if (!isTripForWaypointAccepted) {
          acceptTrip(waypoint)

          if (currentWaypoint == null) {
            setWaypointDestination(waypoint)
          }
        }

        /**
         * When 'Vehicle' state is updated from the 'Provider', the client needs to update the
         * current waypoint pointer so the UI can be refreshed accordingly. It also keeps track of
         * the second/next waypoint for convenience, this is utilized at the moment where the
         * current waypoint status changes to 'ARRIVED/COMPLETE'.
         */
        if (index == 0) {
          currentWaypoint = waypoint
        } else {
          if (index == 1) {
            nextWaypoint = waypoint
          }

          if (
            updatedNextWaypointOfCurrentTrip == null && currentWaypoint?.tripId == waypoint.tripId
          ) {
            updatedNextWaypointOfCurrentTrip = waypoint
          }
        }
      }

      nextWaypointOfCurrentTrip = updatedNextWaypointOfCurrentTrip
    } else {
      stopJourneySharing()
    }

    updateUiForWaypoint(currentWaypoint)
  }

  /**
   * Initializes the current vehicle and associated [RidesharingVehicleReporter].
   *
   * @param application Application instance.
   */
  suspend fun initVehicleAndReporter(application: Application) {
    val vehicleModel = providerService.registerVehicle(localSettings.getVehicleId())

    initializeApi(application, vehicleModel)
  }

  /**
   * Updates the stored vehicle settings. In case vehicle is updated, a new reporter has to be
   * instantiated.
   *
   * @param application Application instance.
   * @param vehicleSettings the updated vehicle settings.
   */
  suspend fun updateVehicleSettings(application: Application, vehicleSettings: VehicleSettings) {
    val vehicleModel = providerService.createOrUpdateVehicle(vehicleSettings)
    localSettings.saveVehicleId(extractVehicleId(vehicleModel.name))

    initializeApi(application, vehicleModel)
  }

  /**
   * Initialize vehicle and associated [RidesharingVehicleReporter].
   *
   * @param application Application instance.
   */
  @Suppress("UnstableApiUsage")
  fun initializeApi(application: Application, vehicleModel: VehicleModel) {
    // If there is a previous instance RidesharingDriverApi (we're updating vehicleId), we clear it
    // to get a fresh one for the new vehicle.
    if (RidesharingDriverApi.getInstance() != null) {
      RidesharingDriverApi.clearInstance()
    }

    vehicleReporter =
      RidesharingDriverApi.createInstance(
          DriverContext.builder(application)
            .setNavigator(navigator)
            .setProviderId(ProviderUtils.getProviderId(application))
            .setVehicleId(localSettings.getVehicleId())
            .setAuthTokenFactory(authTokenFactory)
            .setRoadSnappedLocationProvider(
              NavigationApi.getRoadSnappedLocationProvider(application)
            )
            .setDriverStatusListener(::logLocationUpdate)
            .build()
        )
        .ridesharingVehicleReporter

    vehicleSettings =
      vehicleModel.let {
        VehicleSettings(
          vehicleId = extractVehicleId(it.name)!!,
          backToBackEnabled = it.backToBackEnabled,
          supportedTripTypes = it.supportedTripTypes,
          maximumCapacity = it.maximumCapacity,
        )
      }

    setVehicleOnline()
    startVehiclePeriodicUpdate()
  }

  private fun acceptTrip(waypoint: Waypoint) =
    coroutineScope.launch {
      updateTripStatusInServer(TripUtils.getInitialTripState(waypoint.tripId))
    }

  /** Cleans up active resources prior to activity onDestroy, mainly to prevent memory leaks. */
  fun cleanUp() {
    stopVehiclePeriodicUpdate()

    /** Clear existing API instance once we know it won't be needed. */
    RidesharingDriverApi.clearInstance()
  }

  private fun startVehiclePeriodicUpdate() {
    stopVehiclePeriodicUpdate()

    vehicleStateJob =
      providerService
        .getVehicleModelFlow(localSettings.getVehicleId())
        .onEach(::onVehicleStateUpdate)
        .launchIn(coroutineScope)
  }

  private fun stopVehiclePeriodicUpdate() {
    coroutineScope.launch {
      vehicleStateJob?.cancelAndJoin()
      vehicleStateJob = null
    }
  }

  private fun setVehicleOnline() {
    vehicleReporter.setLocationReportingInterval(
      DEFAULT_LOCATION_UPDATE_INTERVAL_SECONDS,
      TimeUnit.SECONDS,
    )

    // enableLocationTracking() must be called before setting state to Online.
    vehicleReporter.enableLocationTracking()
    vehicleReporter.setVehicleState(VehicleState.ONLINE)
  }

  /** Updates [TripStatus] to the controller. */
  fun processNextState() {
    currentWaypoint?.let {
      // Use Guava's SequentialExecutor to change the internal state because it is
      // accessed in multiple threads.
      sequentialExecutor.execute {
        val previousTripState = tripStates[it.tripId] ?: return@execute

        val updatedTripState =
          TripUtils.getNextTripState(previousTripState, nextWaypointOfCurrentTrip)

        coroutineScope.launch {
          updateTripStatusInServer(updatedTripState)

          if (TripUtils.isTripStatusArrived(updatedTripState.tripStatus)) {
            advanceNextWaypointOnArrival()
          }
        }

        updateNavigationForWaypoints(updatedTripState)

        Log.i(
          TAG,
          "Previous trip status: $previousTripState.tripStatus " +
            "Current trip status: $updatedTripState.tripStatus",
        )
      }
    }
  }

  /** Updates the navigation status based on the given trip state. */
  private fun updateNavigationForWaypoints(tripState: TripState) {
    when (tripState.tripStatus) {
      TripStatus.ENROUTE_TO_PICKUP -> {
        currentWaypoint?.let {
          startJourneySharing()
          navigateToWaypoint(it)
        }
      }
      TripStatus.ARRIVED_AT_PICKUP,
      TripStatus.ARRIVED_AT_INTERMEDIATE_DESTINATION,
      TripStatus.COMPLETE -> nextWaypoint?.let { navigateToWaypoint(it) }
      else -> {}
    }
  }

  /** Advances the trip status of the next waypoint to enroute. */
  private suspend fun advanceNextWaypointOnArrival() {
    nextWaypoint?.let {
      val updatedStateForNextWaypoint =
        TripUtils.getEnrouteStateForWaypoint(tripStates[it.tripId] ?: return@let, it)

      updateTripStatusInServer(updatedStateForNextWaypoint)
    }
  }

  /** Updates the UI (via 'Presenter') based on the given waypoint. */
  private fun updateUiForWaypoint(waypoint: Waypoint?) {
    val presenter = presenterRef.get() ?: return

    mainExecutor.execute {
      if (waypoint == null) {
        presenter.showTripId(NO_TRIP_ID)
        presenter.showTripStatus(TripStatus.UNKNOWN_TRIP_STATUS)
        presenter.showMatchedTripIds(emptyList())
      } else {
        tripStates[waypoint.tripId]?.let {
          presenter.showTripId(it.tripId)
          presenter.showTripStatus(it.tripStatus)
          presenter.showMatchedTripIds(matchedTripIds)
        }
      }
    }
  }

  /** Updates the trip status in the server based on the given state. */
  private suspend fun updateTripStatusInServer(updatedState: TripState) {
    tripStates[updatedState.tripId] = updatedState

    if (updatedState.tripStatus == TripStatus.UNKNOWN_TRIP_STATUS) {
      return
    }

    providerService.updateTripStatus(updatedState)
  }

  /** Sets presenter to the controller so it can invokes UI related callbacks. */
  fun setPresenter(presenter: Presenter?) {
    presenterRef = WeakReference(presenter)
  }

  // Starts simulating journey sharing. Reduces the update interval.
  private fun startJourneySharing() {
    vehicleReporter.setLocationReportingInterval(
      JOURNEY_SHARING_LOCATION_UPDATE_INTERVAL_SECONDS,
      TimeUnit.SECONDS,
    )
  }

  // Stops journey sharing by turning off guidance and reducing the location reporting frequency.
  private fun stopJourneySharing() {
    vehicleReporter.setLocationReportingInterval(
      DEFAULT_LOCATION_UPDATE_INTERVAL_SECONDS,
      TimeUnit.SECONDS,
    )

    // The following two are the only required methods to clean up navigator state in between stops.
    navigator.stopGuidance()
    navigator.clearDestinations()
  }

  /** Sets the destination for the navigator to the waypoint provided. */
  private fun setWaypointDestination(waypoint: Waypoint) {
    val locationPoint = waypoint.location?.point ?: return

    val destinationWaypoint: NavigationWaypoint =
      NavigationWaypoint.builder()
        .setLatLng(locationPoint.latitude, locationPoint.longitude)
        .setTitle(waypoint.waypointType)
        .build()

    // If the loaded trip has a 'routeToken' generated it would be set to NavSDK here.
    navigator.setDestination(destinationWaypoint)
  }

  /**
   * Given the current waypoint, it determines what the next one will be, set the navigator
   * destination to it and kicks off navigation when ready.
   */
  private fun navigateToWaypoint(waypoint: Waypoint?) {
    if (waypoint == null) {
      navigator.stopGuidance()
      return
    }

    val locationPoint = waypoint.location?.point ?: return

    val pendingRoute =
      navigator.setDestination(
        NavigationWaypoint.builder()
          .setLatLng(locationPoint.latitude, locationPoint.longitude)
          .setTitle(waypoint.waypointType)
          .build()
      )

    pendingRoute.setOnResultListener(
      object : ListenableResultFuture.OnResultListener<Navigator.RouteStatus> {
        override fun onResult(code: Navigator.RouteStatus) {
          when (code) {
            Navigator.RouteStatus.OK -> {
              navigator.startGuidance()
              vehicleSimulator.start(SIMULATOR_SPEED_MULTIPLIER)
            }
            Navigator.RouteStatus.NO_ROUTE_FOUND,
            Navigator.RouteStatus.NETWORK_ERROR,
            Navigator.RouteStatus.ROUTE_CANCELED -> {
              Log.e(TAG, "Failed to set a route to next waypoint")
            }
            else -> {}
          }
        }
      }
    )
  }

  /**
   * Determines if the next waypoint of the current trip (if any) is an intermediate destination.
   */
  fun isNextCurrentTripWaypointIntermediate() =
    nextWaypointOfCurrentTrip?.waypointType == TripUtils.INTERMEDIATE_DESTINATION_WAYPOINT_TYPE

  companion object {
    // Controls the relative speed of the simulator.
    private const val SIMULATOR_SPEED_MULTIPLIER = 5.0f

    // Faster location update interval during journey sharing.
    private const val JOURNEY_SHARING_LOCATION_UPDATE_INTERVAL_SECONDS: Long = 5

    // Pattern of a FleetEngine full qualified a vehicle name.
    private val VEHICLE_NAME_FORMAT = Pattern.compile("providers/(.*)/vehicles/(.*)")
    const val NO_TRIP_ID = ""

    // Index of the vehicle ID on a group matcher.
    private const val VEHICLE_ID_INDEX = 2
    private const val ON_TRIP_FINISHED_DELAY_SECONDS = 5
    private const val TAG = "VehicleController"

    // Location update interval when the vehicle is waiting for a trip match.
    private const val DEFAULT_LOCATION_UPDATE_INTERVAL_SECONDS: Long = 10

    private fun logLocationUpdate(
      statusLevel: StatusLevel,
      statusCode: StatusCode,
      statusMsg: String,
      cause: Throwable?,
    ) {
      val message = "Location update: $statusLevel $statusCode: $statusMsg"

      if (statusLevel == StatusLevel.ERROR) {
        if (cause == null) {
          Log.e(TAG, message)
        } else {
          Log.e(TAG, message + " cause: ${cause?.message}")
        }
      } else {
        Log.i(TAG, message)
      }
    }

    private fun extractVehicleId(vehicleName: String): String? {
      val matches = VEHICLE_NAME_FORMAT.matcher(vehicleName)
      return if (matches.matches()) {
        matches.group(VEHICLE_ID_INDEX)
      } else vehicleName
    }
  }
}
