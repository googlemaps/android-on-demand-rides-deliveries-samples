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
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.DriverContext.StatusListener.StatusCode
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.DriverContext.StatusListener.StatusLevel
import com.google.android.libraries.mapsplatform.transportation.driver.api.ridesharing.RidesharingDriverApi
import com.google.android.libraries.mapsplatform.transportation.driver.api.ridesharing.vehiclereporter.RidesharingVehicleReporter
import com.google.android.libraries.mapsplatform.transportation.driver.api.ridesharing.vehiclereporter.RidesharingVehicleReporter.VehicleState
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.RoadSnappedLocationProvider
import com.google.android.libraries.navigation.Waypoint
import com.google.common.collect.Lists
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.mapsplatform.transportation.sample.kotlindriver.config.DriverTripConfig
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.ProviderUtils
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.response.VehicleResponse
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.service.LocalProviderService
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.service.VehicleStateService
import com.google.mapsplatform.transportation.sample.kotlindriver.state.TripState
import com.google.mapsplatform.transportation.sample.kotlindriver.state.TripStatus
import java.lang.ref.WeakReference
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/** Controls vehicle related functionalities. */
@Suppress("UnstableApiUsage")
class VehicleController(
  application: Application,
  private val navigator: Navigator,
  roadSnappedLocationProvider: RoadSnappedLocationProvider,
  private val executor: ExecutorService,
  context: Context
) : VehicleStateService.VehicleStateListener {
  private val providerService: LocalProviderService =
    LocalProviderService(
      LocalProviderService.createRestProvider(ProviderUtils.getProviderBaseUrl(application)),
      this.executor,
      roadSnappedLocationProvider
    )
  private val authTokenFactory: TripAuthTokenFactory =
    TripAuthTokenFactory(application, executor, roadSnappedLocationProvider)
  private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)
  private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
  private val sequentialExecutor: Executor = MoreExecutors.newSequentialExecutor(executor)
  private val vehicleSimulator: VehicleSimulator = VehicleSimulator(navigator.simulator)
  private val vehicleIdStore: VehicleIdStore = VehicleIdStore(context)
  private var vehicleStateService: VehicleStateService? = null
  private var presenterRef = WeakReference<Presenter>(null)
  private lateinit var vehicleReporter: RidesharingVehicleReporter
  private var tripState: TripState? = null

  override fun onVehicleStateUpdate(vehicleResponse: VehicleResponse) {
    // No trip loaded but there's a paired trip.
    if (tripState == null && vehicleResponse.currentTripsIds.isNotEmpty()) {
      val tripId = vehicleResponse.currentTripsIds[0]
      fetchTrip(tripId)
      return
    }

    // There's an ongoing trip and a second trip has been matched (B2B).
    if (tripState != null && vehicleResponse.currentTripsIds.size == 2) {
      val backToBackTripId = vehicleResponse.currentTripsIds[1]
      tripState?.also { it.backToBackTripId = backToBackTripId }
      presenterRef.get()?.also { mainExecutor.execute { it.showNextTripId(backToBackTripId) } }
    }
  }

  /**
   * Initialize vehicle and associated [RidesharingVehicleReporter].
   *
   * @param application Application instance.
   * @return a signal that the async initialization is done, the boolean value can be ignored.
   */
  @Suppress("UnstableApiUsage")
  fun initVehicleAndReporter(application: Application): ListenableFuture<Boolean> {
    // If there is a previous instance RidesharingDriverApi (we're updating vehicleId), we clear it
    // to get a fresh one for the new vehicle.
    if (RidesharingDriverApi.getInstance() != null) {
      RidesharingDriverApi.clearInstance()
    }
    val future =
      Futures.transform(
        initVehicle(),
        {
          vehicleReporter =
            RidesharingDriverApi.createInstance(
                DriverContext.builder(application)
                  .setNavigator(navigator)
                  .setProviderId(ProviderUtils.getProviderId(application))
                  .setVehicleId(vehicleIdStore.readOrDefault())
                  .setAuthTokenFactory(authTokenFactory)
                  .setRoadSnappedLocationProvider(
                    NavigationApi.getRoadSnappedLocationProvider(application)
                  )
                  .setStatusListener {
                    statusLevel: StatusLevel,
                    statusCode: StatusCode,
                    statusMsg: String ->
                    logLocationUpdate(statusLevel, statusCode, statusMsg)
                  }
                  .build()
              )
              .ridesharingVehicleReporter
          true
        },
        executor
      )
    Futures.addCallback(
      future,
      object : FutureCallback<Boolean?> {
        override fun onSuccess(result: Boolean?) {
          setVehicleOnline()
          startVehiclePeriodicUpdate()
        }

        override fun onFailure(t: Throwable) {
          Log.e(TAG, "initVehicleAndReporter() failed.", t)
        }
      },
      executor
    )
    return future
  }

  private fun fetchTrip(tripId: String) {
    val tripConfigFuture = providerService.fetchTrip(tripId, vehicleIdStore.readOrDefault())
    Futures.addCallback(
      tripConfigFuture,
      object : FutureCallback<DriverTripConfig> {
        override fun onSuccess(tripConfig: DriverTripConfig?) {
          sequentialExecutor.execute {
            tripConfig?.also { tripState = TripState(it) }
            tripState?.also { it.setTripMatchedState() }
            updateServerAndUiTripState()
          }
          presenterRef.get()?.also { presenter ->
            mainExecutor.execute {
              tripConfig?.tripId?.also { tripId -> presenter.showTripId(tripId) }
              presenter.showTripStatus(TripStatus.NEW)
            }
          }
        }

        override fun onFailure(t: Throwable) {}
      },
      executor
    )
  }

  /** Cleans up active resources prior to activity onDestroy, mainly to prevent memory leaks. */
  fun cleanUp() {
    stopVehiclePeriodicUpdate()
  }

  private fun startVehiclePeriodicUpdate() {
    vehicleStateService?.run { stopAsync() }
    vehicleStateService = VehicleStateService(providerService, vehicleIdStore.readOrDefault(), this)
    vehicleStateService?.run { startAsync() }
  }

  private fun stopVehiclePeriodicUpdate() {
    vehicleStateService?.run { stopAsync() }
  }

  /** Initialize a vehicle. */
  private fun initVehicle(): ListenableFuture<VehicleResponse> {
    val registerVehicleFuture = providerService.registerVehicle(vehicleIdStore.readOrDefault())
    Futures.addCallback(
      registerVehicleFuture,
      object : FutureCallback<VehicleResponse> {
        override fun onSuccess(vehicleResponse: VehicleResponse?) {
          vehicleResponse?.name?.also { vehicleIdStore.save(extractVehicleId(it)) }
        }

        override fun onFailure(t: Throwable) {}
      },
      executor
    )
    return registerVehicleFuture
  }

  private fun setVehicleOnline() {
    vehicleReporter.setLocationReportingInterval(
      DEFAULT_LOCATION_UPDATE_INTERVAL_SECONDS,
      TimeUnit.SECONDS
    )

    // enableLocationTracking() must be called before setting state to Online.
    vehicleReporter.enableLocationTracking()
    vehicleReporter.setVehicleState(VehicleState.ONLINE)
  }

  /** Updates [TripStatus] to the controller. */
  fun processNextState() {
    // Use Guava's SequentialExecutor to change the internal state because it is
    // accessed in multiple threads.
    sequentialExecutor.execute {
      tripState?.run { processNextState() }
      updateServerAndUiTripState()
    }
  }

  /**
   * Takes current trip 'Status' and updates it in the server. Depending on the status, it might
   * also update 'intermediateDestinationIndex' (multi-destination support). Then, it updates the UI
   * (via the Presenter) to reflect the update.
   */
  private fun updateServerAndUiTripState() {
    val currentTripState = tripState ?: return
    val tripId = currentTripState.tripId ?: return

    val updatedStatus = currentTripState.status
    if (updatedStatus != TripStatus.UNKNOWN_TRIP_STATUS) {
      if (updatedStatus == TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION) {
        providerService.updateTripStatusWithIntermediateDestinationIndex(
          tripId,
          updatedStatus.toString(),
          currentTripState.waypointIndex - 1
        )
      } else {
        providerService.updateTripStatus(tripId, updatedStatus.toString())
      }
    }
    mainExecutor.execute { presenterRef.get()?.apply { showTripStatus(updatedStatus) } }
    updateVehicleByTripStatus(updatedStatus)
  }

  /** Sets presenter to the controller so it can invokes UI related callbacks. */
  fun setPresenter(presenter: Presenter?) {
    presenterRef = WeakReference(presenter)
  }

  /**
   * Updates Vehicle state (navigator waypoint, simulator, journey sharing) based on the given trip
   * status.
   */
  private fun updateVehicleByTripStatus(status: TripStatus?) {
    val currentTripState = tripState ?: return
    when (status) {
      TripStatus.UNKNOWN_TRIP_STATUS -> vehicleSimulator.pause()
      TripStatus.NEW -> {
        setNextWaypointOnNavigator()
        currentTripState.initialVehicleLocation?.also { vehicleSimulator.setLocation(it) }
      }
      TripStatus.ENROUTE_TO_PICKUP,
      TripStatus.ENROUTE_TO_DROPOFF,
      TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION -> {
        startJourneySharing()
        vehicleSimulator.start(SIMULATOR_SPEED_MULTIPLIER)
      }
      TripStatus.ARRIVED_AT_PICKUP, TripStatus.ARRIVED_AT_INTERMEDIATE_DESTINATION -> {
        stopJourneySharing()
        setNextWaypointOnNavigator()
      }
      TripStatus.COMPLETE, TripStatus.CANCELED -> {
        stopJourneySharing()
        vehicleSimulator.unsetLocation()
        onTripFinishedOrCancelled()
      }
      else -> {}
    }
  }

  // Starts simulating journey sharing. Reduces the update interval.
  private fun startJourneySharing() {
    vehicleReporter.setLocationReportingInterval(
      JOURNEY_SHARING_LOCATION_UPDATE_INTERVAL_SECONDS,
      TimeUnit.SECONDS
    )
    navigator.startGuidance()
  }

  // Stops journey sharing by turning off guidance and reducing the location reporting frequency.
  private fun stopJourneySharing() {
    vehicleReporter.setLocationReportingInterval(
      DEFAULT_LOCATION_UPDATE_INTERVAL_SECONDS,
      TimeUnit.SECONDS
    )
    navigator.stopGuidance()
    navigator.clearDestinations()
  }

  /**
   * Given the current waypoint, it determines what the next one will be and sets the navigator
   * destination to it.
   */
  private fun setNextWaypointOnNavigator() {
    val currentTripState = tripState ?: return
    val waypointIndex = currentTripState.waypointIndex
    val waypoint = currentTripState.waypoints[waypointIndex + 1]
    val locationPoint = waypoint.location?.point ?: return
    val destinationWaypoint =
      Waypoint.builder()
        .setLatLng(locationPoint.latitude, locationPoint.longitude)
        .setTitle(waypoint.waypointType)
        .build()
    navigator.setDestinations(Lists.newArrayList(destinationWaypoint), currentTripState.routeToken)
  }

  // Returns true if current trip has intermediate destinations (multi-destination support).
  fun hasIntermediateDestinations(): Boolean = tripState?.hasIntermediateDestinations() ?: false

  // Returns true if driver is on the last intermediate destination (and hence, dropoff is next).
  val isOnLastIntermediateDestination: Boolean
    get() = tripState?.isOnLastIntermediateDestination ?: false

  private fun onTripFinishedWithBackToBack() {
    // Trip has already been reset.
    val currentTripState = tripState ?: return
    val backToBackTripId = currentTripState.backToBackTripId ?: return
    showTripIds(backToBackTripId, NO_TRIP_ID)
    tripState = null
    fetchTrip(backToBackTripId)
  }

  @Suppress("SameParameterValue")
  private fun showTripIds(currentTripId: String, nextTripId: String) {
    val presenter = presenterRef.get() ?: return
    mainExecutor.execute {
      presenter.showTripId(currentTripId)
      presenter.showNextTripId(nextTripId)
    }
  }

  private fun onTripFinishedOrCancelled() {
    // Trip has already been reset.
    val currentTripState = tripState ?: return

    if (currentTripState.backToBackTripId?.isNotEmpty() == true) {
      onTripFinishedWithBackToBack()
      return
    }
    showTripIds("No trip assigned", NO_TRIP_ID)
    scheduledExecutor.schedule(
      {
        tripState = null
        updateServerAndUiTripState()
      },
      ON_TRIP_FINISHED_DELAY_SECONDS.toLong(),
      TimeUnit.SECONDS
    )
  }

  companion object {
    // Controls the relative speed of the simulator.
    private const val SIMULATOR_SPEED_MULTIPLIER = 5.0f

    // Faster location update interval during journey sharing.
    private const val JOURNEY_SHARING_LOCATION_UPDATE_INTERVAL_SECONDS: Long = 1

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
      statusMsg: String
    ) {
      Log.i("STATUS", "$statusLevel $statusCode: $statusMsg")
    }

    private fun extractVehicleId(vehicleName: String): String? {
      val matches = VEHICLE_NAME_FORMAT.matcher(vehicleName)
      return if (matches.matches()) {
        matches.group(VEHICLE_ID_INDEX)
      } else vehicleName
    }
  }
}
