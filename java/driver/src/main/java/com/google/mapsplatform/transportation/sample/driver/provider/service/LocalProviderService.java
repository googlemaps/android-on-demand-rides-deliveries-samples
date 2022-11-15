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
package com.google.mapsplatform.transportation.sample.driver.provider.service;

import static com.google.mapsplatform.transportation.sample.driver.utils.VehicleUtils.DEFAULT_MAXIMUM_CAPACITY;
import static com.google.mapsplatform.transportation.sample.driver.utils.VehicleUtils.DEFAULT_SUPPORTED_TRIP_TYPES;

import android.util.Log;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mapsplatform.transportation.sample.driver.provider.request.TripUpdateBody;
import com.google.mapsplatform.transportation.sample.driver.provider.request.VehicleSettings;
import com.google.mapsplatform.transportation.sample.driver.provider.response.TokenResponse;
import com.google.mapsplatform.transportation.sample.driver.provider.response.TripModel;
import com.google.mapsplatform.transportation.sample.driver.provider.response.VehicleModel;
import com.google.mapsplatform.transportation.sample.driver.state.TripState;
import com.google.mapsplatform.transportation.sample.driver.state.TripStatus;
import java.util.concurrent.Executor;
import retrofit2.HttpException;
import retrofit2.Retrofit;
import retrofit2.adapter.guava.GuavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/** Communicates with stub local provider. */
public class LocalProviderService {
  private static final String TAG = "LocalProviderService";

  private final RestProvider restProvider;
  private final Executor executor;

  public LocalProviderService(RestProvider restProvider, Executor executor) {
    this.restProvider = restProvider;
    this.executor = executor;
  }

  /** Fetch JWT token from provider. */
  public ListenableFuture<TokenResponse> fetchAuthToken(String vehicleId) {
    return restProvider.getAuthToken(vehicleId);
  }

  /**
   * Registers a vehicle on Journey Sharing provider using an asynchronous task. If the registration
   * is successful, the view model is updated with the vehicle name.
   *
   * @param vehicleId vehicle id to be registered.
   */
  public ListenableFuture<VehicleModel> registerVehicle(String vehicleId) {
    return Futures.catchingAsync(
        restProvider.getVehicle(vehicleId),
        HttpException.class,
        (exception) -> {
          if (isNotFoundHttpException(exception)) {
            return restProvider.createVehicle(
                new VehicleSettings(
                    vehicleId,
                    /* backToBackEnabled= */ false,
                    DEFAULT_MAXIMUM_CAPACITY,
                    DEFAULT_SUPPORTED_TRIP_TYPES));

          } else {
            return Futures.immediateFailedFuture(exception);
          }
        },
        executor);
  }

  /**
   * Updates the trip status/intermediateDestinationIndex on a remote provider.
   *
   * @param tripState current state of the trip to update. Contains tripId, status, and intermediate
   *     destination index. If the current trip status is {@code
   *     ENROUTE_TO_INTERMEDIATE_DESTINATION}, it will include the intermediate destination index in
   *     the request.
   * @return the updated trip model from the provider.
   */
  public ListenableFuture<TripModel> updateTripStatus(TripState tripState) {
    TripUpdateBody updateBody = new TripUpdateBody();
    updateBody.setStatus(tripState.tripStatus().toString());

    if (tripState.tripStatus() == TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION) {
      updateBody.setIntermediateDestinationIndex(tripState.intermediateDestinationIndex());
    }

    return updateTrip(tripState.tripId(), updateBody);
  }

  /**
   * Creates or updates a 'Vehicle' in the sample provider based on the given settings.
   *
   * @param vehicleSettings the settings to use while creating/updating the vehicle.
   * @return Future that resolves to the updated 'VehicleModel'.
   */
  public ListenableFuture<VehicleModel> createOrUpdateVehicle(VehicleSettings vehicleSettings) {
    return Futures.catchingAsync(
        restProvider.updateVehicle(vehicleSettings.getVehicleId(), vehicleSettings),
        HttpException.class,
        (exception) -> {
          if (isNotFoundHttpException(exception)) {
            return restProvider.createVehicle(vehicleSettings);

          } else {
            return Futures.immediateFailedFuture(exception);
          }
        },
        executor);
  }

  private ListenableFuture<TripModel> updateTrip(String tripId, TripUpdateBody updateBody) {
    ListenableFuture<TripModel> future = restProvider.updateTrip(tripId, updateBody);

    Futures.addCallback(
        future,
        new FutureCallback<TripModel>() {
          @Override
          public void onSuccess(TripModel tripModel) {
            Log.i(
                TAG,
                String.format(
                    "Successfully updated trip %s with %s.", tripId, updateBody.toString()));
          }

          @Override
          public void onFailure(Throwable t) {
            Log.e(
                TAG,
                String.format("Error updating trip %s with %s.", tripId, updateBody.toString()),
                t);
          }
        },
        executor);

    return future;
  }

  /** Fetches a vehicle identified by 'vehicleId' from the sample provider service. */
  public ListenableFuture<VehicleModel> fetchVehicle(String vehicleId) {
    return restProvider.getVehicle(vehicleId);
  }

  /** Gets a Retrofit implementation of the Journey Sharing REST provider. */
  public static RestProvider createRestProvider(String baseUrl) {
    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(baseUrl)
            .addCallAdapterFactory(GuavaCallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    return retrofit.create(RestProvider.class);
  }

  private static boolean isNotFoundHttpException(HttpException httpException) {
    return httpException.code() == 404;
  }
}
