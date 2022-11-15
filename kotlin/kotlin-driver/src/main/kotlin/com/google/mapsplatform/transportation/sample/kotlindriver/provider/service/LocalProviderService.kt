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

import android.util.Log
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.request.TripUpdateBody
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.request.VehicleSettings
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.response.TokenResponse
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.response.TripModel
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.response.VehicleModel
import com.google.mapsplatform.transportation.sample.kotlindriver.state.TripState
import com.google.mapsplatform.transportation.sample.kotlindriver.state.TripStatus
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.adapter.guava.GuavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

/** Communicates with stub local provider. */
@Suppress("UnstableApiUsage")
class LocalProviderService(
  private val restProvider: RestProvider,
) {
  /** Fetch JWT token from provider. */
  suspend fun fetchAuthToken(vehicleId: String): TokenResponse =
    restProvider.getAuthToken(vehicleId)

  /**
   * Updates the trip status/intermediateDestinationIndex on a remote provider.
   *
   * @param tripState current state of the trip to update. Contains tripId, status, and intermediate
   * destination index. If the current trip status is ENROUTE_TO_INTERMEDIATE_DESTINATION, it will
   * include the intermediate destination index in the request.
   * @return the updated trip model from the provider.
   */
  suspend fun updateTripStatus(tripState: TripState): TripModel {
    val (tripId, tripStatus, intermediateDestinationIndex) = tripState
    val tripStatusString = tripStatus.toString()

    return if (tripStatus == TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION) {
      updateTrip(tripId, TripUpdateBody(tripStatusString, intermediateDestinationIndex))
    } else {
      updateTrip(tripId, TripUpdateBody(tripStatusString))
    }
  }

  /**
   * Registers a vehicle on Journey Sharing provider using an asynchronous task. If the registration
   * is successful, the view model is updated with the vehicle name.
   *
   * @param vehicleId vehicle id to be registered.
   * @return retrieved vehicle model.
   */
  suspend fun registerVehicle(vehicleId: String): VehicleModel {
    return try {
      restProvider.getVehicle(vehicleId)
    } catch (httpException: HttpException) {
      if (isNotFoundHttpException(httpException)) {
        restProvider.createVehicle(
          VehicleSettings(
            vehicleId = vehicleId,
          )
        )
      } else {
        throw httpException
      }
    }
  }

  /**
   * Creates or updates a 'Vehicle' in the sample provider based on the given settings.
   *
   * @param vehicleSettings the settings to use while creating/updating the vehicle.
   * @return created/updated vehicle model.
   */
  suspend fun createOrUpdateVehicle(vehicleSettings: VehicleSettings): VehicleModel {
    return try {
      restProvider.updateVehicle(vehicleSettings.vehicleId, vehicleSettings)
    } catch (httpException: HttpException) {
      if (isNotFoundHttpException(httpException)) {
        restProvider.createVehicle(vehicleSettings)
      } else {
        throw httpException
      }
    }
  }

  /**
   * Gets a flow that can be used to fetch the state of a vehicle model from the remove provider.
   *
   * @param vehicleId the id of the vehicle to retrieve
   * @return cold flow that can be used to fetch a vehicle model.
   */
  fun getVehicleModelFlow(vehicleId: String): Flow<VehicleModel> =
    flow {
        while (true) {
          emit(restProvider.getVehicle(vehicleId))
          delay(3.seconds)
        }
      }
      .onStart({ Log.i(TAG, "Starting polling for $vehicleId") })
      .onCompletion({ Log.i(TAG, "Ending polling for $vehicleId") })

  private suspend fun updateTrip(tripId: String, updateBody: TripUpdateBody): TripModel =
    restProvider.updateTrip(tripId, updateBody).also {
      Log.i(TAG, "Successfully updated trip $tripId with $updateBody.")
    }

  companion object {
    private const val TAG = "LocalProviderService"

    private fun isNotFoundHttpException(httpException: HttpException) = httpException.code() == 404

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
