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
package com.google.mapsplatform.transportation.sample.kotlindriver.provider.service

import android.location.Location
import android.util.Log
import com.google.android.libraries.navigation.RoadSnappedLocationProvider
import com.google.common.base.Splitter
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.google.mapsplatform.transportation.sample.kotlindriver.config.DriverTripConfig
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.request.CreateVehicleBody
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.request.TripUpdateBody
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.response.GetTripResponse
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.response.TokenResponse
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.response.TripData
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.response.VehicleResponse
import java.util.concurrent.Executor
import retrofit2.Retrofit
import retrofit2.adapter.guava.GuavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

/** Communicates with stub local provider. */
@Suppress("UnstableApiUsage")
class LocalProviderService(
  private val restProvider: RestProvider,
  private val executor: Executor,
  private val roadSnappedLocationProvider: RoadSnappedLocationProvider
) {
  /** Fetch JWT token from provider. */
  fun fetchAuthToken(vehicleId: String): ListenableFuture<TokenResponse> {
    return restProvider.getAuthToken(vehicleId)
  }

  /** Fetches a trip identified by 'tripId' from the sample provider service. */
  fun fetchTrip(tripId: String, vehicleId: String): ListenableFuture<DriverTripConfig> {
    val tripConfigFuture = fetchAvailableTrip(tripId, vehicleId)
    val roadSnappedLocationFuture = roadSnappedLocationFuture
    return mergeTripConfigWithRoadSnappedLocation(tripConfigFuture, roadSnappedLocationFuture)
  }

  /**
   * Registers a vehicle on Journey Sharing provider using an asynchronous task. If the registration
   * is successful, the view model is updated with the vehicle name.
   *
   * @param vehicleId vehicle id to be registered.
   */
  fun registerVehicle(vehicleId: String): ListenableFuture<VehicleResponse> {
    return fetchOrCreateVehicle(vehicleId)
  }

  /**
   * Updates the trip status on a remote provider.
   *
   * @param tripId ID of the trip being updated.
   * @param status Fleet-Engine compatible name of the status.
   */
  fun updateTripStatus(tripId: String, status: String): Unit =
    updateTrip(tripId, TripUpdateBody(status = status))

  /**
   * Updates the trip status/intermediateDstinationIndex on a remote provider.
   *
   * @param tripId ID of the trip being updated.
   * @param status Fleet-Engine compatible name of the status.
   * @param intermediateDestinationIndex Index pointing to the current intermediate destination.
   */
  fun updateTripStatusWithIntermediateDestinationIndex(
    tripId: String,
    status: String,
    intermediateDestinationIndex: Int
  ) {
    updateTrip(
      tripId,
      TripUpdateBody(status = status, intermediateDestinationIndex = intermediateDestinationIndex)
    )
  }

  private fun updateTrip(tripId: String, updateBody: TripUpdateBody) {
    val future = restProvider.updateTrip(tripId, updateBody)
    Futures.addCallback(
      future,
      object : FutureCallback<TripData> {
        override fun onSuccess(tripData: TripData?) {
          Log.i(
            TAG,
            String.format("Successfully updated trip %s with %s.", tripId, updateBody.toString())
          )
        }

        override fun onFailure(t: Throwable) {
          Log.e(
            TAG,
            String.format("Error updating trip %s with %s.", tripId, updateBody.toString()),
            t
          )
        }
      },
      executor
    )
  }

  /** Fetches a vehicle identified by 'vehicleId' from the sample provider service. */
  fun fetchVehicle(vehicleId: String): ListenableFuture<VehicleResponse> {
    return restProvider.getVehicle(vehicleId)
  }

  private fun fetchOrCreateVehicle(vehicleId: String): ListenableFuture<VehicleResponse> {
    val resultFuture = SettableFuture.create<VehicleResponse>()
    val responseFuture = restProvider.getVehicle(vehicleId)
    Futures.addCallback(
      responseFuture,
      object : FutureCallback<VehicleResponse> {
        override fun onSuccess(vehicleResponse: VehicleResponse?) {
          resultFuture.set(vehicleResponse)
        }

        override fun onFailure(t: Throwable) {
          resultFuture.setFuture(
            restProvider.createVehicle(
              CreateVehicleBody(vehicleId = vehicleId, backToBackEnabled = true)
            )
          )
        }
      },
      executor
    )
    return resultFuture
  }

  private fun mergeTripConfigWithRoadSnappedLocation(
    tripConfigFuture: ListenableFuture<DriverTripConfig>,
    roadSnappedLocationFuture: ListenableFuture<Location>
  ): ListenableFuture<DriverTripConfig> {
    return Futures.whenAllSucceed(tripConfigFuture, roadSnappedLocationFuture)
      .call(
        {
          val tripConfig = Futures.getDone(tripConfigFuture)
          val roadSnappedLocation = Futures.getDone(roadSnappedLocationFuture)
          tripConfig.vehicleLocation = getDriverPoint(roadSnappedLocation)
          tripConfig
        },
        executor
      )
  }

  private fun fetchAvailableTrip(
    tripId: String,
    vehicleId: String
  ): ListenableFuture<DriverTripConfig> {
    val availableTripFuture = restProvider.getAvailableTrip(tripId)
    return Futures.transform(
      availableTripFuture,
      { availableTripResponse: GetTripResponse? ->
        val availableTrip = availableTripResponse!!.tripData
        val tripName = availableTrip?.name!!
        // Expects trip name to be on format providers/<projectId>/trips/<tripId>
        val parts = SPLITTER.splitToList(tripName)
        val projectId = parts[PROJECT_ID_INDEX]
        DriverTripConfig(
          projectId = projectId,
          tripId = tripId,
          vehicleId = vehicleId,
          waypoints = availableTrip.waypoints,
          routeToken = availableTripResponse.routeToken
        )
      },
      executor
    )
  }

  private val roadSnappedLocationFuture: ListenableFuture<Location>
    get() {
      val locationFuture = SettableFuture.create<Location>()
      val locationListener: RoadSnappedLocationProvider.LocationListener =
        object : RoadSnappedLocationProvider.LocationListener {
          override fun onLocationChanged(location: Location) {
            locationFuture.set(location)
          }

          override fun onRawLocationUpdate(location: Location) {
            // Ignore
          }
        }
      Futures.addCallback(
        locationFuture,
        object : FutureCallback<Location> {
          override fun onSuccess(result: Location?) {
            roadSnappedLocationProvider.removeLocationListener(locationListener)
          }

          override fun onFailure(t: Throwable) {
            locationFuture.setException(t)
            roadSnappedLocationProvider.removeLocationListener(locationListener)
          }
        },
        executor
      )
      roadSnappedLocationProvider.addLocationListener(locationListener)
      return locationFuture
    }

  companion object {
    private const val TAG = "LocalProviderService"

    /** String splitter for a slash. */
    private val SPLITTER = Splitter.on('/')

    /** Index of a project id on trip name that has been split. */
    private const val PROJECT_ID_INDEX = 1

    private fun getDriverPoint(location: Location): DriverTripConfig.Point =
      DriverTripConfig.Point(latitude = location.latitude, longitude = location.longitude)

    /** Gets a Retrofit implementation of the Journey Sharing REST provider. */
    fun createRestProvider(baseUrl: String): RestProvider {
      val retrofit =
        Retrofit.Builder()
          .baseUrl(baseUrl)
          .addCallAdapterFactory(GuavaCallAdapterFactory.create())
          .addConverterFactory(GsonConverterFactory.create())
          .build()
      return retrofit.create(RestProvider::class.java)
    }
  }
}
