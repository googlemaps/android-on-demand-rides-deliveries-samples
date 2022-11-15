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

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.mapsplatform.transportation.consumer.managers.TripModel
import com.google.android.libraries.mapsplatform.transportation.consumer.managers.TripModelCallback
import com.google.android.libraries.mapsplatform.transportation.consumer.model.Trip
import com.google.android.libraries.mapsplatform.transportation.consumer.model.TripInfo
import com.google.android.libraries.mapsplatform.transportation.consumer.model.TripName
import com.google.android.libraries.mapsplatform.transportation.consumer.model.TripWaypoint
import com.google.android.libraries.mapsplatform.transportation.consumer.model.VehicleLocation
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.model.CreateTripRequest
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.model.TripData
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.model.TripStatus
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.response.TripResponse
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.response.WaypointResponse
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.service.LocalProviderService
import com.google.mapsplatform.transportation.sample.kotlinconsumer.state.AppStates
import java.lang.ref.WeakReference
import java.net.ConnectException
import java.util.Timer
import kotlin.concurrent.schedule
import kotlin.concurrent.timerTask
import kotlin.reflect.KProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Defined to use 'MutableLiveData' as delegate. */
private inline operator fun <T> MutableLiveData<T>.getValue(
  thisObj: Any?,
  property: KProperty<*>
): T? = value

private inline operator fun <T> MutableLiveData<T>.setValue(
  thisObj: Any?,
  property: KProperty<*>,
  value: T
) {
  this.value = value
}

/**
 * ViewModel for the Consumer Sample application. It provides observables for the current
 * application status such as trip completed and journey sharing which it maintains using
 * StateChangeCallback. When in journey sharing it has observables for the details of the trip
 * including eta, distance remaining and waypoint data.
 *
 * @property providerService object used to communicate with the remote sample provider.
 */
class ConsumerViewModel(
  private val providerService: LocalProviderService,
) : ViewModel() {
  interface JourneySharingListener {
    /** Starts journey sharing rendering and return that [TripModel]. */
    fun startJourneySharing(tripData: TripData): TripModel

    /** Stops journey sharing rendering. */
    fun stopJourneySharing()

    /** Updates current location. */
    fun updateCurrentLocation(latLang: LatLng)
  }

  // Latest error message.
  val errorMessage = SingleLiveEvent<Int>()
  private var journeySharingListener = WeakReference<JourneySharingListener?>(null)

  /** Checks if a trip is matched (i.e. vehicleId is present in the trip info. */
  val isTripMatched: Boolean
    get() = tripInfoLiveData.value?.vehicleId?.isNotEmpty() ?: false

  /** Determines if the driver is currently working on another trip's waypoint. */
  val isDriverInOtherTripWaypoint: Boolean
    get() = otherTripWaypoints.isNotEmpty()

  /** Current trip model. */
  private var currentTripModel: TripModel? by MutableLiveData()

  /** The selected pickup location. */
  var pickupLocation: LatLng? by MutableLiveData()

  /** The selected dropoff location. */
  var dropoffLocation: LatLng? by MutableLiveData()

  /** Represents whether the current trip is shared. */
  var isSharedTripType: Boolean? by MutableLiveData(false)

  /** LiveData for the list intermediate destinations selected for the trip. */
  private val intermediateDestinationsLiveData = MutableLiveData<List<LatLng>>()

  /** LiveData for the list of intermediate destinations selected for the trip. */
  var intermediateDestinations: List<LatLng>
    get() = intermediateDestinationsLiveData.value ?: emptyList()
    private set(value) {
      intermediateDestinationsLiveData.value = value
    }

  /** LiveData for the current trip [Trip.TripStatus] for each status change during the trip. */
  val tripStatusLiveData = MutableLiveData<Int>()

  /** LiveData for the current trip id. */
  var tripIdLiveData = MutableLiveData<String?>()

  /** LiveData for the distance in meters to the next waypoint. */
  val remainingDistanceMetersLiveData = MutableLiveData<Int?>()

  /** LiveData for the ETA to the next waypoint of the trip. */
  val nextWaypointEtaLiveData = MutableLiveData<Long?>()

  /** LiveData for the current trip TripInfo updated with each trip refresh. */
  val tripInfoLiveData = MutableLiveData<TripInfo?>()

  /** The current trip TripInfo updated with each trip refresh. */
  val tripInfo: TripInfo?
    get() = tripInfoLiveData.value

  /** LiveData for the list of other trip waypoints (B2B/Shared pool support) */
  val otherTripWaypointsLiveData = MutableLiveData<List<TripWaypoint>>(emptyList())

  /** List of other trip waypoints (B2B/Shared pool support) */
  private var otherTripWaypoints: List<TripWaypoint>
    get() = otherTripWaypointsLiveData.value ?: emptyList()
    set(value) {
      otherTripWaypointsLiveData.value = value
    }

  /** LiveData for the current app state. */
  val appStateLiveData = MutableLiveData<Int>()

  /** Enum to identify the current app state. */
  @AppStates
  var appState: Int
    get() = appStateLiveData.value ?: AppStates.UNINITIALIZED
    set(value) {
      appStateLiveData.value = value
    }

  /** Current waypoint type when driver is currently working on another trip. */
  @TripWaypoint.WaypointType
  val otherTripWaypointType: Int
    get() =
      if (!isDriverInOtherTripWaypoint) {
        TripWaypoint.WaypointType.UNKNOWN
      } else {
        otherTripWaypoints.first()?.waypointType ?: TripWaypoint.WaypointType.UNKNOWN
      }

  init {
    appState = AppStates.UNINITIALIZED
  }

  /** Creates a trip in the sample provider and waits for it to be matched. */
  fun createTrip() =
    viewModelScope.launch {
      val createTripRequest =
        CreateTripRequest(
          pickup = pickupLocation,
          dropoff = dropoffLocation,
          intermediateDestinations = intermediateDestinations,
          tripType = if (isSharedTripType == true) SHARED_TRIP_TYPE else EXCLUSIVE_TRIP_TYPE,
        )

      try {
        val tripResponse: TripResponse = providerService.createTrip(createTripRequest)
        val tripName = tripResponse.tripName

        Log.i(TAG, "Successfully created trip $tripName.")

        tripStatusLiveData.postValue(TripStatus.parse(tripResponse.tripStatus))

        val matchedTripResponse = providerService.fetchMatchedTrip(tripName)

        Log.i(TAG, "Successfully matched trip $tripName.")

        executeOnMainThread { startJourneySharing(matchedTripResponse) }
      } catch (e: Throwable) {
        Log.e(TAG, "Failed to create trip.", e)
        setErrorMessage(e)
      }
    }

  fun startJourneySharing(tripData: TripData) {
    if (appState != AppStates.CONFIRMING_TRIP) {
      Log.e(
        TAG,
        "App state should be `SELECTING_DROPOFF` but is $appState, journey sharing cannot be started.",
      )
      return
    }
    val waypoints: List<WaypointResponse> = tripData.waypoints
    val listener = journeySharingListener.get()
    if (waypoints.size < 2 || listener == null) {
      return
    }
    val tripModel = listener.startJourneySharing(tripData)
    currentTripModel = tripModel
    appState = AppStates.JOURNEY_SHARING
    tripModel.registerTripCallback(tripCallback)
  }

  /**
   * Updates trip status livedata value. Resets app state to INITIALIZED if Journey Sharing is in a
   * terminal or error state (COMPLETE, CANCELED, or UNKNOWN).
   */
  private fun updateTripStatus(status: Int) {
    tripStatusLiveData.value = status
    if (
      status == Trip.TripStatus.COMPLETE ||
        status == Trip.TripStatus.CANCELED ||
        status == Trip.TripStatus.UNKNOWN_TRIP_STATUS
    ) {
      stopJourneySharing()
      intermediateDestinations = emptyList()
    }
  }

  private fun stopJourneySharing() {
    unregisterTripCallback()
    val listener = journeySharingListener.get()
    listener?.stopJourneySharing()

    Timer()
      .schedule(
        timerTask { executeOnMainThread { appState = AppStates.INITIALIZED } },
        IDLE_STATE_RESET_DELAY_MILISECONDS
      )
  }

  /** Unregisters callback as part of cleanup. */
  fun unregisterTripCallback() {
    val tripModel = currentTripModel
    tripModel?.unregisterTripCallback(tripCallback)
    tripInfoLiveData.value = null
  }

  fun setJourneySharingListener(journeySharingListener: JourneySharingListener?) {
    this.journeySharingListener = WeakReference(journeySharingListener)
  }

  /** Updates the location container (pickup or dropoff) given by the current app state. */
  fun updateLocationPointForState(cameraLocation: LatLng) {
    if (appState != AppStates.SELECTING_PICKUP && appState != AppStates.SELECTING_DROPOFF) {
      return
    }
    if (appState == AppStates.SELECTING_PICKUP) {
      pickupLocation = cameraLocation
    } else {
      dropoffLocation = cameraLocation
    }
  }

  /**
   * Adds the current selected location (contained in dropoffLocation) to the list of intermediate
   * destinations.
   */
  fun addIntermediateDestination() {
    val destination = dropoffLocation ?: return

    intermediateDestinations =
      mutableListOf<LatLng>().apply {
        addAll(intermediateDestinations)
        add(destination)
      }
  }

  private fun setErrorMessage(e: Throwable) {
    if (e is ConnectException) {
      executeOnMainThread { errorMessage.setValue(R.string.msg_provider_connection_error) }
    }
  }

  /** Trip callbacks registered during */
  private val tripCallback: TripModelCallback =
    object : TripModelCallback() {
      override fun onTripActiveRouteRemainingDistanceUpdated(
        tripInfo: TripInfo,
        distanceMeters: Int?
      ) {
        remainingDistanceMetersLiveData.value = distanceMeters
      }

      override fun onTripUpdated(tripInfo: TripInfo) {
        this@ConsumerViewModel.tripInfoLiveData.value = tripInfo
        tripIdLiveData.value = TripName.create(tripInfo.tripName).tripId
      }

      override fun onTripStatusUpdated(tripInfo: TripInfo, @Trip.TripStatus status: Int) {
        updateTripStatus(tripInfo.tripStatus)
      }

      override fun onTripETAToNextWaypointUpdated(tripInfo: TripInfo, timestampMillis: Long?) {
        nextWaypointEtaLiveData.value = timestampMillis
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
        otherTripWaypoints = waypoints
      }
    }

  private fun maybeUpdateCurrentLocation(vehicleLocation: VehicleLocation?) {
    val listener = journeySharingListener.get()
    if (appState == AppStates.JOURNEY_SHARING && listener != null && vehicleLocation != null) {
      listener.updateCurrentLocation(vehicleLocation.latLng)
    }
  }

  /** Takes a function as parameter that gets executed in the Main thread. */
  private fun executeOnMainThread(toExecute: () -> Unit) {
    viewModelScope.launch(Dispatchers.Main) { toExecute() }
  }

  companion object {
    /** Amount of time until an idle state reset should be delayed before applying changes. */
    private const val IDLE_STATE_RESET_DELAY_MILISECONDS = 3000L
    private const val TAG = "ConsumerViewModel"
    private const val SHARED_TRIP_TYPE = "SHARED"
    private const val EXCLUSIVE_TRIP_TYPE = "EXCLUSIVE"
  }

  /**
   * Factory class defined to allow creating 'ConsumerViewModel' instances passing a
   * LocalProviderService.
   */
  class Factory(
    private val localProviderService: LocalProviderService,
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return modelClass.cast(ConsumerViewModel(localProviderService))
        ?: throw IllegalArgumentException("Illegal ViewModel class: $modelClass")
    }
  }
}
