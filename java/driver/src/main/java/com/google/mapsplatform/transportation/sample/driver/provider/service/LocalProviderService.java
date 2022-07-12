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

import android.location.Location;
import android.util.Log;
import com.google.android.libraries.navigation.RoadSnappedLocationProvider;
import com.google.android.libraries.navigation.RoadSnappedLocationProvider.LocationListener;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.mapsplatform.transportation.sample.driver.config.DriverTripConfig;
import com.google.mapsplatform.transportation.sample.driver.provider.request.TripUpdateBody;
import com.google.mapsplatform.transportation.sample.driver.provider.request.VehicleSettings;
import com.google.mapsplatform.transportation.sample.driver.provider.response.GetTripResponse;
import com.google.mapsplatform.transportation.sample.driver.provider.response.TokenResponse;
import com.google.mapsplatform.transportation.sample.driver.provider.response.TripModel;
import com.google.mapsplatform.transportation.sample.driver.provider.response.VehicleModel;
import com.google.mapsplatform.transportation.sample.driver.provider.response.Waypoint.Point;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import retrofit2.Retrofit;
import retrofit2.adapter.guava.GuavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/** Communicates with stub local provider. */
public class LocalProviderService {
  private static final String TAG = "LocalProviderService";

  private final RestProvider restProvider;
  private final Executor executor;
  private final RoadSnappedLocationProvider roadSnappedLocationProvider;

  public LocalProviderService(
      RestProvider restProvider,
      Executor executor,
      RoadSnappedLocationProvider roadSnappedLocationProvider) {
    this.restProvider = restProvider;
    this.executor = executor;
    this.roadSnappedLocationProvider = roadSnappedLocationProvider;
  }

  /** Fetch JWT token from provider. */
  public ListenableFuture<TokenResponse> fetchAuthToken(String vehicleId) {
    return restProvider.getAuthToken(vehicleId);
  }

  /** Fetches a trip identified by 'tripId' from the sample provider service. */
  public ListenableFuture<DriverTripConfig> fetchTrip(String tripId, String vehicleId) {
    ListenableFuture<DriverTripConfig> tripConfigFuture = fetchAvailableTrip(tripId, vehicleId);
    ListenableFuture<Location> roadSnappedLocationFuture = getRoadSnappedLocationFuture();
    return mergeTripConfigWithRoadSnappedLocation(tripConfigFuture, roadSnappedLocationFuture);
  }

  /**
   * Registers a vehicle on Journey Sharing provider using an asynchronous task. If the registration
   * is successful, the view model is updated with the vehicle name.
   *
   * @param vehicleId vehicle id to be registered.
   */
  public ListenableFuture<VehicleModel> registerVehicle(String vehicleId) {
    return fetchOrCreateVehicle(vehicleId);
  }

  /**
   * Updates the trip status on a remote provider.
   *
   * @param tripId ID of the trip being updated.
   * @param status Fleet-Engine compatible name of the status.
   */
  public ListenableFuture<TripModel> updateTripStatus(String tripId, String status) {
    TripUpdateBody updateBody = new TripUpdateBody();
    updateBody.setStatus(status);

    return updateTrip(tripId, updateBody);
  }

  /**
   * Updates the trip status/intermediateDstinationIndex on a remote provider.
   *
   * @param tripId ID of the trip being updated.
   * @param status Fleet-Engine compatible name of the status.
   * @param intermediateDestinationIndex Index pointing to the current intermediate destination.
   */
  public ListenableFuture<TripModel> updateTripStatusWithIntermediateDestinationIndex(
      String tripId, String status, int intermediateDestinationIndex) {
    TripUpdateBody updateBody = new TripUpdateBody();
    updateBody.setStatus(status);
    updateBody.setIntermediateDestinationIndex(intermediateDestinationIndex);

    return updateTrip(tripId, updateBody);
  }

  /**
   * Creates or updates a 'Vehicle' in the sample provider based on the given settings.
   *
   * @param vehicleSettings the settings to use while creating/updating the vehicle.
   * @return Future that resolves to the updated 'VehicleModel'.
   */
  public ListenableFuture<VehicleModel> createOrUpdateVehicle(VehicleSettings vehicleSettings) {
    SettableFuture<VehicleModel> resultFuture = SettableFuture.create();

    Futures.addCallback(
        restProvider.getVehicle(vehicleSettings.getVehicleId()),
        new FutureCallback<VehicleModel>() {
          @Override
          public void onSuccess(VehicleModel vehicle) {
            resultFuture.setFuture(
                restProvider.updateVehicle(vehicleSettings.getVehicleId(), vehicleSettings));
          }

          @Override
          public void onFailure(Throwable t) {
            resultFuture.setFuture(restProvider.createVehicle(vehicleSettings));
          }
        },
        executor);

    return resultFuture;
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

  private ListenableFuture<VehicleModel> fetchOrCreateVehicle(String vehicleId) {
    SettableFuture<VehicleModel> resultFuture = SettableFuture.create();
    ListenableFuture<VehicleModel> responseFuture = restProvider.getVehicle(vehicleId);
    Futures.addCallback(
        responseFuture,
        new FutureCallback<VehicleModel>() {
          @Override
          public void onSuccess(VehicleModel vehicleModel) {
            resultFuture.set(vehicleModel);
          }

          @Override
          public void onFailure(Throwable t) {
            VehicleSettings vehicleSettings = new VehicleSettings();
            vehicleSettings.setVehicleId(vehicleId);

            resultFuture.setFuture(restProvider.createVehicle(vehicleSettings));
          }
        },
        executor);
    return resultFuture;
  }

  private ListenableFuture<DriverTripConfig> mergeTripConfigWithRoadSnappedLocation(
      ListenableFuture<DriverTripConfig> tripConfigFuture,
      ListenableFuture<Location> roadSnappedLocationFuture) {
    return Futures.whenAllSucceed(tripConfigFuture, roadSnappedLocationFuture)
        .call(
            () -> {
              DriverTripConfig tripConfig = Futures.getDone(tripConfigFuture);
              Location roadSnappedLocation = Futures.getDone(roadSnappedLocationFuture);
              tripConfig.setVehicleLocation(getDriverPoint(roadSnappedLocation));
              return tripConfig;
            },
            executor);
  }

  private ListenableFuture<DriverTripConfig> fetchAvailableTrip(String tripId, String vehicleId) {
    ListenableFuture<GetTripResponse> availableTripFuture = restProvider.getAvailableTrip(tripId);

    return Futures.transform(
        availableTripFuture,
        availableTripResponse -> {
          TripModel availableTrip = availableTripResponse.getTripModel();

          DriverTripConfig config = new DriverTripConfig();

          config.setTripId(tripId);
          config.setVehicleId(vehicleId);
          config.setWaypoints(availableTrip.getWaypoints());

          return config;
        },
        executor);
  }

  private ListenableFuture<Location> getRoadSnappedLocationFuture() {
    SettableFuture<Location> locationFuture = SettableFuture.create();
    LocationListener locationListener =
        new LocationListener() {
          @Override
          public void onLocationChanged(Location location) {
            locationFuture.set(location);
          }

          @Override
          public void onRawLocationUpdate(Location location) {
            // Ignore
          }
        };

    Futures.addCallback(
        locationFuture,
        new FutureCallback<Location>() {
          @Override
          public void onSuccess(@Nullable Location result) {
            roadSnappedLocationProvider.removeLocationListener(locationListener);
          }

          @Override
          public void onFailure(Throwable t) {
            locationFuture.setException(t);
            roadSnappedLocationProvider.removeLocationListener(locationListener);
          }
        },
        executor);
    roadSnappedLocationProvider.addLocationListener(locationListener);

    return locationFuture;
  }

  private static Point getDriverPoint(@Nullable Location location) {
    if (location == null) {
      throw new IllegalStateException("Location must be set for vehicle point configuration.");
    }

    Point vehiclePoint = new Point();
    vehiclePoint.setLatitude(location.getLatitude());
    vehiclePoint.setLongitude(location.getLongitude());

    return vehiclePoint;
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
}
