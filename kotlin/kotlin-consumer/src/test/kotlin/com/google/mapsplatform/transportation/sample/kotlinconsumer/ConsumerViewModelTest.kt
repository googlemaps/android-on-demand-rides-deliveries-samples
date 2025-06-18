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
package com.google.mapsplatform.transportation.sample.kotlinconsumer

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.google.common.truth.Truth.assertThat
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.model.TripData
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.model.TripStatus
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.response.TripResponse
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.response.WaypointResponse
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.service.LocalProviderService
import com.google.mapsplatform.transportation.sample.kotlinconsumer.state.AppStates
import java.net.ConnectException
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class ConsumerViewModelTest {
  private val providerServiceMock =
    mock<LocalProviderService> {
      onBlocking { createTrip(any()) } doReturn MOCKED_TRIP_RESPONSE
      onBlocking { fetchMatchedTrip(any()) } doReturn MOCKED_TRIP_DATA
    }

  private val journeySharingListenerMock =
    mock<ConsumerViewModel.JourneySharingListener> {
      on { startJourneySharing(any()) } doReturn mock()
    }

  private val consumerViewModel =
    ConsumerViewModel(
      providerServiceMock,
    )

  @Before
  fun setUp() {
    consumerViewModel.setJourneySharingListener(journeySharingListenerMock)
  }

  @Test
  fun createTrip_updatesTripStatusLiveData() {
    consumerViewModel.createTrip()

    Robolectric.flushForegroundThreadScheduler()

    assertThat(consumerViewModel.tripStatusLiveData.value).isEqualTo(TripStatus.NEW.ordinal)
  }

  @Test
  fun createTrip_startsJourneySharingWhenConfirmingTrip() {
    consumerViewModel.appState = AppStates.CONFIRMING_TRIP

    consumerViewModel.createTrip()

    Robolectric.flushForegroundThreadScheduler()

    verify(journeySharingListenerMock, times(1)).startJourneySharing(eq(MOCKED_TRIP_DATA))
  }

  @Test
  fun createTrip_createsTripWithProperArguments() {
    consumerViewModel.appState = AppStates.CONFIRMING_TRIP

    val pickupLocation = LatLng(324.323, 494.232)
    val dropoffLocation = LatLng(124.3234, 2342.232)

    consumerViewModel.pickupLocation = pickupLocation
    consumerViewModel.dropoffLocation = dropoffLocation

    consumerViewModel.createTrip()

    Robolectric.flushForegroundThreadScheduler()

    verifyBlocking(providerServiceMock, times(1)) { fetchMatchedTrip(TRIP_NAME) }
  }

  @Test
  fun createTrip_DoesNotStartJourneySharingWhenNotConfirming() {
    consumerViewModel.appState = AppStates.UNINITIALIZED

    consumerViewModel.createTrip()

    Robolectric.flushForegroundThreadScheduler()

    verify(journeySharingListenerMock, times(0)).startJourneySharing(any())
  }

  @Test
  fun createTrip_setsErrorMessageOnCreateTripFailure() {
    runBlocking {
      whenever(providerServiceMock.createTrip(any())).doAnswer {
        throw ConnectException("Failed to connect to provider")
      }
    }

    consumerViewModel.createTrip()

    Robolectric.flushForegroundThreadScheduler()

    assertThat(consumerViewModel.errorMessage.value)
      .isEqualTo(R.string.msg_provider_connection_error)
  }

  @Test
  fun createTrip_setsErrorMessageOnGetTripFailure() {
    runBlocking {
      whenever(providerServiceMock.fetchMatchedTrip(any())).doAnswer {
        throw ConnectException("Failed to connect to provider")
      }
    }

    consumerViewModel.createTrip()

    Robolectric.flushForegroundThreadScheduler()

    assertThat(consumerViewModel.errorMessage.value)
      .isEqualTo(R.string.msg_provider_connection_error)
  }

  private companion object {
    const val TRIP_ID = "testTrip"
    const val TRIP_NAME = "providers/provider/trips/$TRIP_ID"
    const val VEHICLE_ID = "testVehicle"

    val MOCKED_TRIP_RESPONSE =
      TripResponse(
        tripName = TRIP_NAME,
        tripStatus = "NEW",
        vehicleId = TRIP_NAME,
        waypoints =
          listOf(
            WaypointResponse(waypointType = "PICKUP_WAYPOINT_TYPE"),
            WaypointResponse(waypointType = "DROP_OFF_WAYPOINT_TYPE")
          )
      )

    val MOCKED_TRIP_DATA =
      TripData(
        tripId = TRIP_ID,
        tripName = TRIP_NAME,
        vehicleId = VEHICLE_ID,
        tripStatus = TripStatus.NEW.ordinal,
        waypoints = MOCKED_TRIP_RESPONSE.waypoints
      )
  }
}
