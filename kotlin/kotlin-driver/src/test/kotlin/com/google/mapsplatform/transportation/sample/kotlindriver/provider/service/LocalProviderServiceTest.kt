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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.request.TripUpdateBody
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.request.VehicleSettings
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.response.TripModel
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.response.VehicleModel
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.response.Waypoint
import com.google.mapsplatform.transportation.sample.kotlindriver.state.TripState
import com.google.mapsplatform.transportation.sample.kotlindriver.state.TripStatus
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class LocalProviderServiceTest {
  private val restProviderMock =
    mock<RestProvider> {
      onBlocking { updateTrip(any(), any()) } doReturn TRIP_MODEL_MOCK
      onBlocking { createVehicle(any()) } doReturn CREATE_VEHICLE_MODEL_MOCK
      onBlocking { getVehicle(any()) } doReturn GET_VEHICLE_MODEL_MOCK
      onBlocking { updateVehicle(any(), any()) } doReturn UPDATE_VEHICLE_MODEL_MOCK
    }

  private val localProviderService = LocalProviderService(restProviderMock)

  @Test
  fun updateTripStatus_returnsTripModelFromProvider() {
    val tripStateMock = TripState(TRIP_ID_MOCK, TripStatus.ENROUTE_TO_PICKUP)

    val tripModel = runBlocking { localProviderService.updateTripStatus(tripStateMock) }

    assertThat(tripModel).isEqualTo(TRIP_MODEL_MOCK)
  }

  @Test
  fun updateTripStatus_doesNotSendIntermediateDestinationIndex() {
    val tripStateMock = TripState(TRIP_ID_MOCK, TripStatus.ENROUTE_TO_DROPOFF)

    runBlocking { localProviderService.updateTripStatus(tripStateMock) }

    verifyBlocking(restProviderMock, times(1)) {
      updateTrip(tripStateMock.tripId, TripUpdateBody(tripStateMock.tripStatus.toString(), null))
    }
  }

  @Test
  fun updateTripStatus_sendsIntermediateDestinationIndexOnEnroute() {
    val tripStateMock =
      TripState(
        tripId = TRIP_ID_MOCK,
        tripStatus = TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION,
        intermediateDestinationIndex = 1
      )

    runBlocking { localProviderService.updateTripStatus(tripStateMock) }

    verifyBlocking(restProviderMock, times(1)) {
      updateTrip(
        tripStateMock.tripId,
        TripUpdateBody(
          tripStateMock.tripStatus.toString(),
          tripStateMock.intermediateDestinationIndex
        )
      )
    }
  }

  @Test
  fun registerVehicle_returnsVehicleWhenExisting() {
    val vehicleModel = runBlocking { localProviderService.registerVehicle(VEHICLE_ID_MOCK) }

    assertThat(vehicleModel).isEqualTo(GET_VEHICLE_MODEL_MOCK)
  }

  @Test
  fun registerVehicle_createsVehicleWhenNotExisting() {
    val vehicleModel = runBlocking {
      whenever(restProviderMock.getVehicle(any())).doAnswer { throw HTTP_NOT_FOUND_EXCEPTION_MOCK }

      localProviderService.registerVehicle(VEHICLE_ID_MOCK)
    }

    assertThat(vehicleModel).isEqualTo(CREATE_VEHICLE_MODEL_MOCK)
  }

  @Test
  fun registerVehicle_throwsWhenNot404HttpException() {
    runBlocking {
      whenever(restProviderMock.getVehicle(any())).doAnswer {
        throw HTTP_INTERNAL_SERVER_EXCEPTION_MOCK
      }

      assertFailsWith<HttpException>(HTTP_INTERNAL_SERVER_EXCEPTION_MOCK.message) {
        localProviderService.registerVehicle(VEHICLE_ID_MOCK)
      }
    }
  }

  @Test
  fun registerVehicle_throwsWhenNotHttpException() {
    runBlocking {
      val exception = RuntimeException("Non http")
      whenever(restProviderMock.getVehicle(any())).doAnswer { throw exception }

      assertFailsWith<RuntimeException>(exception.message) {
        localProviderService.registerVehicle(VEHICLE_ID_MOCK)
      }
    }
  }

  @Test
  fun createOrUpdateVehicle_updatesWhenVehicleExists() {
    val vehicleModel = runBlocking { localProviderService.createOrUpdateVehicle(VehicleSettings()) }

    assertThat(vehicleModel).isEqualTo(UPDATE_VEHICLE_MODEL_MOCK)
  }

  @Test
  fun createOrUpdateVehicle_createsWhenVehicleDoesntExist() {
    val vehicleModel = runBlocking {
      whenever(restProviderMock.updateVehicle(any(), any())).doAnswer {
        throw HTTP_NOT_FOUND_EXCEPTION_MOCK
      }

      localProviderService.createOrUpdateVehicle(VehicleSettings())
    }

    assertThat(vehicleModel).isEqualTo(CREATE_VEHICLE_MODEL_MOCK)
  }

  @Test
  fun createOrUpdateVehicle_throwsWhenNot404HttpException() {
    runBlocking {
      whenever(restProviderMock.updateVehicle(any(), any())).doAnswer {
        throw HTTP_INTERNAL_SERVER_EXCEPTION_MOCK
      }

      assertFailsWith<HttpException>(HTTP_INTERNAL_SERVER_EXCEPTION_MOCK.message) {
        localProviderService.createOrUpdateVehicle(VehicleSettings())
      }
    }
  }

  @Test
  fun createOrUpdateVehicle_throwsWhenNotHttpException() {
    runBlocking {
      val exception = RuntimeException("Non http")
      whenever(restProviderMock.updateVehicle(any(), any())).doAnswer { throw exception }

      assertFailsWith<RuntimeException>(exception.message) {
        localProviderService.createOrUpdateVehicle(VehicleSettings())
      }
    }
  }

  @Test
  fun getVehicleFlow_emitsASequence() {
    runBlocking {
      val emittedList =
        listOf(
          VehicleModel(name = "providers/test/vehicles/getVehicle1"),
          VehicleModel(name = "providers/test/vehicles/getVehicle2"),
          VehicleModel(name = "providers/test/vehicles/getVehicle3")
        )

      whenever(restProviderMock.getVehicle(any()))
        .thenReturn(emittedList.get(0), emittedList.get(1), emittedList.get(2))

      val vehicleModelFlow = localProviderService.getVehicleModelFlow(VEHICLE_ID_MOCK)

      assertThat(vehicleModelFlow.take(3).toList()).isEqualTo(emittedList)
    }
  }

  private companion object {
    const val TRIP_ID_MOCK = "testTrip"
    const val VEHICLE_ID_MOCK = "testVehicle"

    val TRIP_MODEL_MOCK =
      TripModel(
        name = "tripName",
        tripStatus = "NEW",
        waypoints =
          listOf(
            Waypoint(waypointType = "PICKUP_WAYPOINT_TYPE"),
            Waypoint(waypointType = "DROP_OFF_WAYPOINT_TYPE")
          )
      )

    val GET_VEHICLE_MODEL_MOCK = VehicleModel(name = "providers/test/vehicles/getVehicleId")
    val UPDATE_VEHICLE_MODEL_MOCK = VehicleModel(name = "providers/test/vehicles/updateVehicleId")

    val CREATE_VEHICLE_MODEL_MOCK = VehicleModel(name = "providers/test/vehicles/createVehicleId")

    val RESPONSE_ERROR_MOCK =
      ResponseBody.create(MediaType.parse("text/plain"), "Test Server Error")

    val HTTP_NOT_FOUND_EXCEPTION_MOCK =
      HttpException(Response.error<String>(404, RESPONSE_ERROR_MOCK))

    val HTTP_INTERNAL_SERVER_EXCEPTION_MOCK =
      HttpException(Response.error<String>(500, RESPONSE_ERROR_MOCK))
  }
}
