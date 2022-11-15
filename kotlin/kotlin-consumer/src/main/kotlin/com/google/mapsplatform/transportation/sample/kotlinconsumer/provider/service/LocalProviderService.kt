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
package com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.service

import com.google.android.libraries.mapsplatform.transportation.consumer.model.TripName
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.model.CreateTripRequest
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.model.TripData
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.model.TripStatus
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.response.GetTripResponse
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import retrofit2.Retrofit
import retrofit2.adapter.guava.GuavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Provider service for a locally hosted Sample Journeysharing provider.
 *
 * Default base URL: http://10.0.2.2:8080/
 */
class LocalProviderService(
  /** Time between attempts of a trip polling routine. */
  private val provider: RestProvider,
) {
  /** Creates a trip in the Provider based on the data given in request. */
  suspend fun createTrip(request: CreateTripRequest) = provider.createTrip(request)

  /** Fetches an auth token for the given trip from the Provider. */
  suspend fun fetchAuthToken(tripId: String) = provider.getConsumerToken(tripId)

  /** Polls the Provider until the given trip is matched to a vehicle. */
  suspend fun fetchMatchedTrip(tripName: String): TripData {
    val tripId = TripName.create(tripName).tripId

    while (true) {
      val tripResponse = provider.getTrip(tripId)

      if (isTripMatched(tripResponse)) {
        val trip = tripResponse.trip!!

        return TripData(
          tripId = tripId,
          tripName = trip.tripName,
          vehicleId = trip.vehicleId,
          tripStatus = TripStatus.parse(trip.tripStatus),
          waypoints = trip.waypoints
        )
      }

      // Poll interval delay.
      delay(5.seconds)
    }
  }

  companion object {
    private const val TAG = "LocalProviderService"

    private fun isTripMatched(response: GetTripResponse?): Boolean =
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
