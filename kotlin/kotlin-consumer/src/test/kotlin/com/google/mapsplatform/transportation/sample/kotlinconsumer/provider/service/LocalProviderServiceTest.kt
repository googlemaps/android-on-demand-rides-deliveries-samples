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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.model.TripData
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.model.TripStatus
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.response.GetTripResponse
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.response.TripResponse
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.response.WaypointResponse
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.net.ConnectException
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalProviderServiceTest {
  private val restProviderMock =
    mock<RestProvider> {
      onBlocking { createTrip(any()) } doReturn TRIP_RESPONSE_MOCK
      onBlocking { getTrip(any()) } doReturn GET_TRIP_RESPONSE_MOCK
    }

  private val localProviderService = LocalProviderService(restProviderMock)

  @Test
  fun fetchMatchedTrip_callsProviderUntilValidResponse() {
    runBlocking {
      whenever(restProviderMock.getTrip(any()))
        .thenReturn(
          INVALID_GET_TRIP_RESPONSE_MOCK,
          INVALID_GET_TRIP_RESPONSE_MOCK,
          GET_TRIP_RESPONSE_MOCK
        )

      localProviderService.fetchMatchedTrip(TRIP_NAME)

      verify(restProviderMock, times(3)).getTrip(TRIP_ID)
    }
  }

  @Test
  fun fetchMatchedTrip_returnsTripData() {
    runBlocking {
      val tripData = localProviderService.fetchMatchedTrip(TRIP_NAME)

      assertThat(tripData)
        .isEqualTo(
          TripData(
            tripId = TRIP_ID,
            tripName = TRIP_NAME,
            vehicleId = TRIP_RESPONSE_MOCK.vehicleId,
            waypoints = TRIP_RESPONSE_MOCK.waypoints,
            tripStatus = TripStatus.NEW.ordinal,
          )
        )
    }
  }

  @Test
  fun fetchMatchedTrip_surfacesException() {
    runBlocking {
      val errorMessage = "Failed to connect to provider"

      whenever(restProviderMock.getTrip(any())).thenAnswer { throw ConnectException(errorMessage) }

      assertFailsWith<ConnectException>(errorMessage) {
        localProviderService.fetchMatchedTrip(TRIP_NAME)
      }
    }
  }

  private companion object {
    const val TRIP_ID = "testTrip"
    const val TRIP_NAME = "providers/provider/trips/$TRIP_ID"

    val TRIP_RESPONSE_MOCK =
      TripResponse(
        tripName = TRIP_NAME,
        tripStatus = "NEW",
        vehicleId = "testVehicle",
        waypoints =
          listOf(
            WaypointResponse(waypointType = "PICKUP_WAYPOINT_TYPE"),
            WaypointResponse(waypointType = "DROP_OFF_WAYPOINT_TYPE")
          )
      )

    val INVALID_GET_TRIP_RESPONSE_MOCK = GetTripResponse()
    val GET_TRIP_RESPONSE_MOCK = GetTripResponse(trip = TRIP_RESPONSE_MOCK)
  }
}
