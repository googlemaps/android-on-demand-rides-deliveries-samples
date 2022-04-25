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

import android.app.Application
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.mapsplatform.transportation.consumer.managers.TripModel
import com.google.android.libraries.mapsplatform.transportation.consumer.managers.TripModelCallback
import com.google.android.libraries.mapsplatform.transportation.consumer.model.Trip
import com.google.android.libraries.mapsplatform.transportation.consumer.model.TripInfo
import com.google.android.libraries.mapsplatform.transportation.consumer.model.TripName
import com.google.android.libraries.mapsplatform.transportation.consumer.model.TripWaypoint
import com.google.android.libraries.mapsplatform.transportation.consumer.model.VehicleLocation
import com.google.common.util.concurrent.FutureCallback as FutureCallback1
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.ProviderUtils
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.model.TripData
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.model.TripStatus
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.model.WaypointData
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.response.TripResponse
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.response.WaypointResponse
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.service.LocalProviderService
import com.google.mapsplatform.transportation.sample.kotlinconsumer.state.AppStates
import java.lang.ref.WeakReference
import java.net.ConnectException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * ViewModel for the Consumer Sample application. It provides observables for the current
 * application status such as trip completed and journey sharing which it maintains using
 * StateChangeCallback. When in journey sharing it has observables for the details of the trip
 * including eta, distance remaining and waypoint data.
 */
class ConsumerViewModel(application: Application) : AndroidViewModel(application) {
  interface JourneySharingListener {
    /** Starts journey sharing rendering and return that [TripModel]. */
    fun startJourneySharing(tripData: TripData): TripModel

    /** Stops journey sharing rendering. */
    fun stopJourneySharing()

    /** Updates current location. */
    fun updateCurrentLocation(latLang: LatLng)
  }

  // LiveData for current application map state.
  private val appState = MutableLiveData<Int>()

  // LiveData for dropoff location.
  private val pickupLocation = MutableLiveData<LatLng>()

  // LiveData for pickup location.
  private val dropoffLocation = MutableLiveData<LatLng>()

  // LiveData for selected intermediate destinations. (multi-destination trips)
  private val intermediateDestinations = MutableLiveData<List<LatLng>?>()

  // LiveData of the current trip info from a trip refresh.
  private val tripStatus = MutableLiveData<Int>()

  // LiveData of the current trip Id.
  private val tripId = MutableLiveData<String?>()

  // LiveData of the remaining distance to the next waypoint.
  private val remainingDistanceMeters = MutableLiveData<Int?>()

  // LiveData of the ETA to the next waypoint.
  private val nextWaypointEta = MutableLiveData<Long?>()

  // Latest trip data including: time remaining, distance remaining and etc.
  private val tripInfo = MutableLiveData<TripInfo?>()

  // The current active trip, meant for create journey sharing session and observe session.
  private val trip = MutableLiveData<TripModel>()

  // LiveData containing a potential list of waypoints the driver is going through before the
  // current trip (B2B)
  private val previousTripWaypoints = MutableLiveData<List<TripWaypoint>?>()

  // Latest error message.
  val errorMessage = SingleLiveEvent<Int>()
  private val providerService: LocalProviderService
  private val executor = Executors.newCachedThreadPool()
  private val mainExecutor: Executor
  private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
  private var journeySharingListener = WeakReference<JourneySharingListener?>(null)

  /** Initializes the ConsumerApi and if successful initiates TripManager configuration. */
  init {
    providerService =
      LocalProviderService(
        LocalProviderService.createRestProvider(ProviderUtils.getProviderBaseUrl(application)),
        executor,
        scheduledExecutor
      )
    appState.value = AppStates.UNINITIALIZED
    mainExecutor = ContextCompat.getMainExecutor(application)
  }

  /** Creates a trip in the sample provider. */
  fun startSingleExclusiveTrip() {
    val singleExclusiveTrip: ListenableFuture<TripResponse> =
      providerService.createSingleExclusiveTrip(
        createWaypointData(
          pickupLocation.value,
          dropoffLocation.value,
          intermediateDestinations.value
        )
      )
    handleCreateSingleExclusiveTripResponse(singleExclusiveTrip)
  }

  private fun handleCreateSingleExclusiveTripResponse(
    singleExclusiveTrip: ListenableFuture<TripResponse>
  ) {

    Futures.addCallback(
      singleExclusiveTrip,
      object : FutureCallback1<TripResponse> {
        override fun onSuccess(result: TripResponse?) {
          result?.tripStatus ?: return
          result.tripName ?: return

          Log.i(TAG, String.format("Successfully created trip %s.", result.tripName))
          tripStatus.postValue(TripStatus.parse(result.tripStatus))
          val tripDataFuture: ListenableFuture<TripData> =
            providerService.fetchMatchedTrip(result.tripName)
          handleFetchMatchedTripResponse(tripDataFuture)
        }

        override fun onFailure(e: Throwable) {
          Log.e(TAG, "Failed to create trip.", e)
          setErrorMessage(e)
        }
      },
      executor
    )
  }

  private fun handleFetchMatchedTripResponse(tripDataFuture: ListenableFuture<TripData>) {
    Futures.addCallback(
      tripDataFuture,
      object : FutureCallback1<TripData> {
        override fun onSuccess(tripData: TripData?) {
          tripData?.also { mainExecutor.execute { startJourneySharing(it) } }
        }

        override fun onFailure(e: Throwable) {
          Log.e(TAG, "Failed to match trip with a driver.", e)
          setErrorMessage(e)
        }
      },
      executor
    )
  }

  fun startJourneySharing(tripData: TripData) {
    if (appState.value != AppStates.CONFIRMING_TRIP) {
      Log.e(
        TAG,
        String.format(
          "App state should be `SELECTING_DROPOFF` but is %d, journey sharing cannot be" +
            " started.",
          appState.value
        )
      )
      return
    }
    val waypoints: List<WaypointResponse> = tripData.waypoints
    val listener = journeySharingListener.get()
    if (waypoints.size < 2 || listener == null) {
      return
    }
    val tripModel = listener.startJourneySharing(tripData)
    trip.value = tripModel
    appState.value = AppStates.JOURNEY_SHARING
    tripModel.registerTripCallback(tripCallback)
  }

  /**
   * Updates trip status livedata value. Resets app state to INITIALIZED if Journey Sharing is in a
   * terminal or error state (COMPLETE, CANCELED, or UNKNOWN).
   */
  private fun updateTripStatus(status: Int) {
    tripStatus.value = status
    if (status == Trip.TripStatus.COMPLETE ||
        status == Trip.TripStatus.CANCELED ||
        status == Trip.TripStatus.UNKNOWN_TRIP_STATUS
    ) {
      stopJourneySharing()
      intermediateDestinations.value = listOf()
    }
  }

  private fun stopJourneySharing() {
    unregisterTripCallback()
    val listener = journeySharingListener.get()
    listener?.stopJourneySharing()
    scheduledExecutor.schedule(
      { mainExecutor.execute { appState.setValue(AppStates.INITIALIZED) } },
      IDLE_STATE_RESET_DELAY_SECONDS.toLong(),
      TimeUnit.SECONDS
    )
  }

  /** Unregisters callback as part of cleanup. */
  fun unregisterTripCallback() {
    val tripModel = trip.value
    tripModel?.unregisterTripCallback(tripCallback)
    tripInfo.value = null
  }

  fun setJourneySharingListener(journeySharingListener: JourneySharingListener?) {
    this.journeySharingListener = WeakReference(journeySharingListener)
  }

  /** Set the app state. */
  fun setState(@AppStates state: Int) {
    appState.value = state
  }

  /** Returns the current AppState. */
  fun getAppState(): LiveData<Int> {
    return appState
  }

  /** Returns the current trip TripInfo updated with each trip refresh */
  fun getTripInfo(): LiveData<TripInfo?> {
    return tripInfo
  }

  /** Checks if a trip is matched (i.e. vehicleId is present in the trip info. */
  val isTripMatched: Boolean
    get() = tripInfo.value?.vehicleId?.isNotEmpty() ?: false

  /**
   * Checks if the next waypoint is part of the current user trip (if not, driver might be dropping
   * someone else as part of a B2B trip). Only call when there's a trip assigned.
   */
  val isDriverCompletingAnotherTrip: Boolean
    get() = with(getPreviousTripWaypoints().value) { this?.isNotEmpty() ?: false }

  /** Returns the current trip [Trip.TripStatus] for each status change during the trip. */
  fun getTripStatus(): LiveData<Int> {
    return tripStatus
  }

  /** Returns ETA to the next waypoint of the trip. */
  fun getNextWaypointEta(): LiveData<Long?> {
    return nextWaypointEta
  }

  /** Returns the distance in meters to the next waypoint. */
  fun getRemainingDistanceMeters(): LiveData<Int?> {
    return remainingDistanceMeters
  }

  /** Returns the trip id. */
  fun getTripId(): LiveData<String?> {
    return tripId
  }

  /** Returns the list of previous trip waypoints (B2B support) */
  fun getPreviousTripWaypoints(): LiveData<List<TripWaypoint>?> {
    return previousTripWaypoints
  }

  /** Updates the location container (pickup or dropoff) given by the current app state. */
  fun updateLocationPointForState(cameraLocation: LatLng) {
    @AppStates val state = appState.value!!
    if (state != AppStates.SELECTING_PICKUP && state != AppStates.SELECTING_DROPOFF) {
      return
    }
    if (state == AppStates.SELECTING_PICKUP) {
      setPickupLocation(cameraLocation)
    } else {
      setDropoffLocation(cameraLocation)
    }
  }

  /** Set the selected dropoff location. */
  fun setDropoffLocation(location: LatLng) {
    dropoffLocation.value = location
  }

  /** Gets the selected dropoff location (or null if not selected) */
  fun getDropoffLocation(): LatLng? {
    return dropoffLocation.value
  }

  /** Set the selected pickup location. */
  fun setPickupLocation(location: LatLng) {
    pickupLocation.value = location
  }

  /** Gets the selected pickup location (or null if not selected) */
  fun getPickupLocation(): LatLng? {
    return pickupLocation.value
  }

  /**
   * Adds the current selected location (contained in dropoffLocation) to the list of intermediate
   * destinations.
   */
  fun addIntermediateDestination() {
    val destination = dropoffLocation.value ?: return

    if (intermediateDestinations.value == null) {
      intermediateDestinations.value = listOf()
    }

    intermediateDestinations.value =
      mutableListOf<LatLng>().apply {
        addAll(intermediateDestinations.value!!)
        add(destination)
      }
  }

  /** Retrieves the list of intermediate destinations added so far to the trip. */
  fun getIntermediateDestinations(): List<LatLng>? {
    return intermediateDestinations.value
  }

  private fun setErrorMessage(e: Throwable) {
    if (e is ConnectException) {
      mainExecutor.execute { errorMessage.setValue(R.string.msg_provider_connection_error) }
    }
  }

  /** Creates a WaypointData object which contains all the locations used to create a trip. */
  private fun createWaypointData(
    pickup: LatLng?,
    dropoff: LatLng?,
    intermediateDestinations: List<LatLng>?
  ): WaypointData =
    WaypointData(
      pickup = pickup,
      dropoff = dropoff,
      intermediateDestinations = intermediateDestinations ?: listOf()
    )

  /** Trip callbacks registered during */
  private val tripCallback: TripModelCallback =
    object : TripModelCallback() {
      override fun onTripActiveRouteRemainingDistanceUpdated(
        tripInfo: TripInfo,
        distanceMeters: Int?
      ) {
        remainingDistanceMeters.value = distanceMeters
      }

      override fun onTripUpdated(tripInfo: TripInfo) {
        this@ConsumerViewModel.tripInfo.value = tripInfo
        tripId.value = TripName.create(tripInfo.tripName).tripId
      }

      override fun onTripStatusUpdated(tripInfo: TripInfo, @Trip.TripStatus status: Int) {
        updateTripStatus(tripInfo.tripStatus)
      }

      override fun onTripETAToNextWaypointUpdated(tripInfo: TripInfo, timestampMillis: Long?) {
        nextWaypointEta.value = timestampMillis
      }

      override fun onTripVehicleLocationUpdated(
        tripInfo: TripInfo,
        vehicleLocation: VehicleLocation?
      ) {
        maybeUpdateCurrentLocation(vehicleLocation)
      }

      override fun onTripRemainingWaypointsUpdated(
        tripInfo: TripInfo,
        waypointList: List<TripWaypoint>
      ) {
        val waypoints = mutableListOf<TripWaypoint>()
        val currentTripId = TripName.create(tripInfo.tripName).tripId
        for (tripWaypoint in waypointList) {
          if (tripWaypoint.tripId == currentTripId) {
            break
          }
          waypoints.add(tripWaypoint)
        }
        previousTripWaypoints.value = waypoints
      }
    }

  private fun maybeUpdateCurrentLocation(vehicleLocation: VehicleLocation?) {
    val listener = journeySharingListener.get()
    if (appState.value == AppStates.JOURNEY_SHARING && listener != null && vehicleLocation != null
    ) {
      listener.updateCurrentLocation(vehicleLocation.latLng)
    }
  }

  companion object {
    /** Amount of time until an idle state reset should be delayed before applying changes. */
    private const val IDLE_STATE_RESET_DELAY_SECONDS = 3
    private const val TAG = "ConsumerViewModel"
  }
}
