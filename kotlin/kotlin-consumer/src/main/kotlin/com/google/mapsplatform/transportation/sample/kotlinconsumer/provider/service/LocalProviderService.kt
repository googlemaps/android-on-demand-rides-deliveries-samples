/* Copyright 2020 Google LLC
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
package com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.service

import com.google.android.libraries.mapsplatform.transportation.consumer.model.TripName
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.model.TripData
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.model.TripStatus
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.model.WaypointData
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.response.GetTripResponse
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.response.TokenResponse
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.response.TripResponse
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService
import retrofit2.Retrofit
import retrofit2.adapter.guava.GuavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Provider service for a locally hosted Sample Journeysharing provider.
 *
 * Default base URL: http://10.0.2.2:8888/
 */
class LocalProviderService(
  /** Time between attempts of a trip polling routine. */
  private val provider: RestProvider,
  private val executor: Executor,
  private val scheduledExecutor: ScheduledExecutorService
) {

  fun createSingleExclusiveTrip(waypoint: WaypointData): ListenableFuture<TripResponse> {
    return provider.createSingleExclusiveTrip(waypoint)
  }

  fun fetchAuthToken(tripId: String): ListenableFuture<TokenResponse> {
    return provider.getConsumerToken(tripId)
  }

  fun fetchMatchedTrip(tripName: String): ListenableFuture<TripData> {
    val tripId = TripName.create(tripName).tripId
    val getTripResponseFuture: ListenableFuture<GetTripResponse> =
      fetchMatchedTripWithRetries(tripId)
    return Futures.transform(
      getTripResponseFuture,
      { getTripResponse: GetTripResponse? ->
        val trip: TripResponse = getTripResponse!!.trip!!
        TripData(
          tripId = tripId,
          tripName = trip.tripName!!,
          vehicleId = trip.vehicleId!!,
          tripStatus = TripStatus.parse(trip.tripStatus!!),
          waypoints = trip.waypoints
        )
      },
      executor
    )
  }

  private fun fetchMatchedTripWithRetries(tripId: String): ListenableFuture<GetTripResponse> {
    return RetryingFuture(scheduledExecutor).runWithRetries(
        { provider.getTrip(tripId) },
        RetryingFuture.RUN_FOREVER,
        GET_TRIP_RETRY_INTERVAL_MILLIS.toLong()
      ) { response: GetTripResponse? -> isTripValid(response) }
  }

  companion object {
    private const val TAG = "LocalProviderService"
    const val GET_TRIP_RETRY_INTERVAL_MILLIS = 5000

    private fun isTripValid(response: GetTripResponse?): Boolean =
      response?.trip?.vehicleId?.isNotEmpty() ?: false

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
