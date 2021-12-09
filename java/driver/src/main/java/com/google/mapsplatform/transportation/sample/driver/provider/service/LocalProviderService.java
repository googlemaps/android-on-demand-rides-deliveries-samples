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
package com.google.mapsplatform.transportation.sample.driver.provider.service;

import android.location.Location;
import android.util.Log;
import com.google.android.libraries.navigation.RoadSnappedLocationProvider;
import com.google.android.libraries.navigation.RoadSnappedLocationProvider.LocationListener;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.mapsplatform.transportation.sample.driver.config.DriverTripConfig;
import com.google.mapsplatform.transportation.sample.driver.config.DriverTripConfig.Point;
import com.google.mapsplatform.transportation.sample.driver.provider.request.TripStatusBody;
import com.google.mapsplatform.transportation.sample.driver.provider.request.VehicleIdBody;
import com.google.mapsplatform.transportation.sample.driver.provider.response.GetTripResponse;
import com.google.mapsplatform.transportation.sample.driver.provider.response.TokenResponse;
import com.google.mapsplatform.transportation.sample.driver.provider.response.TripData;
import com.google.mapsplatform.transportation.sample.driver.provider.response.VehicleResponse;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import javax.annotation.Nullable;
import retrofit2.Retrofit;
import retrofit2.adapter.guava.GuavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/** Communicates with stub local provider. */
public class LocalProviderService {
  private static final String TAG = "LocalProviderService";
  public static final int GET_TRIP_RETRY_INTERVAL_MILLIS = 5000;
  /** String splitter for a slash. */
  private static final Splitter SPLITTER = Splitter.on('/');
  /** Index of a project id on trip name that has been split. */
  private static final int PROJECT_ID_INDEX = 1;

  private static final int TRIP_NAME_SEGMENT_COUNT = 4;

  private final RestProvider restProvider;
  private final Executor executor;
  private final ScheduledExecutorService scheduledExecutor;
  private final RoadSnappedLocationProvider roadSnappedLocationProvider;

  public LocalProviderService(
      RestProvider restProvider,
      Executor executor,
      ScheduledExecutorService scheduledExecutor,
      RoadSnappedLocationProvider roadSnappedLocationProvider) {
    this.restProvider = restProvider;
    this.executor = executor;
    this.scheduledExecutor = scheduledExecutor;
    this.roadSnappedLocationProvider = roadSnappedLocationProvider;
  }

  /** Fetch JWT token from provider. */
  public ListenableFuture<String> fetchAuthToken() {
    return Futures.transform(restProvider.getAuthToken(), TokenResponse::getToken, executor);
  }

  /**
   * Verifies periodically with the Journey Sharing provider for an available trip using an
   * asynchronous task. When a trip is found the task is completed and the user location is fetched
   * to complete the trip configuration and update the view model.
   *
   * @param vehicleId vehicle id to be matched on a trip.
   */
  public ListenableFuture<DriverTripConfig> pollAvailableTrip(String vehicleId) {
    ListenableFuture<DriverTripConfig> tripConfigFuture = fetchAvailableTrip(vehicleId);
    ListenableFuture<Location> roadSnappedLocationFuture = getRoadSnappedLocationFuture();
    return mergeTripConfigWithRoadSnappedLocation(tripConfigFuture, roadSnappedLocationFuture);
  }

  /**
   * Registers a vehicle on Journey Sharing provider using an asynchronous task. If the registration
   * is successful, the view model is updated with the vehicle name.
   *
   * @param vehicleId vehicle id to be registered.
   */
  public ListenableFuture<VehicleResponse> registerVehicle(String vehicleId) {
    return fetchOrCreateVehicle(vehicleId);
  }

  /**
   * Updates the trip stats on a remote provider.
   *
   * @param tripId ID of the trip being updated.
   * @param status Fleet-Engine compatible name of the status.
   */
  public void updateTripStatus(String tripId, String status) {
    TripStatusBody body = new TripStatusBody();
    body.setStatus(status);
    ListenableFuture<TripData> future = restProvider.updateTripStatus(tripId, body);
    Futures.addCallback(
        future,
        new FutureCallback<TripData>() {
          @Override
          public void onSuccess(TripData tripData) {
            Log.i(
                TAG, String.format("Successfully updated trip %s with status %s.", tripId, status));
          }

          @Override
          public void onFailure(Throwable t) {
            Log.e(TAG, String.format("Error updating trip %s with status %s.", tripId, status), t);
          }
        },
        executor);
  }

  private ListenableFuture<VehicleResponse> fetchOrCreateVehicle(String vehicleId) {
    SettableFuture<VehicleResponse> resultFuture = SettableFuture.create();
    ListenableFuture<VehicleResponse> responseFuture = restProvider.getVehicle(vehicleId);
    Futures.addCallback(
        responseFuture,
        new FutureCallback<VehicleResponse>() {
          @Override
          public void onSuccess(VehicleResponse vehicleResponse) {
            resultFuture.set(vehicleResponse);
          }

          @Override
          public void onFailure(Throwable t) {
            VehicleIdBody vehicleIdBody = new VehicleIdBody();
            vehicleIdBody.setVehicleId(vehicleId);
            resultFuture.setFuture(restProvider.createVehicle(vehicleIdBody));
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

  private ListenableFuture<DriverTripConfig> fetchAvailableTrip(String vehicleId) {
    ListenableFuture<String> tokenFuture = fetchAuthToken();
    ListenableFuture<GetTripResponse> availableTripFuture = fetchAvailableTripWithRetires();
    return Futures.whenAllSucceed(tokenFuture, availableTripFuture)
        .call(
            () -> {
              GetTripResponse availableTripResponse = Futures.getDone(availableTripFuture);
              TripData availableTrip = availableTripResponse.getTripData();
              String tripName = availableTrip.getName();
              // Expects trip name to be on format providers/<projectId>/trips/<tripId>
              List<String> parts = SPLITTER.splitToList(tripName);
              String tripId = Iterables.getLast(parts);
              String projectId = parts.get(PROJECT_ID_INDEX);
              DriverTripConfig config = new DriverTripConfig();
              config.setTripId(tripId);
              config.setVehicleId(vehicleId);
              config.setProjectId(projectId);
              config.setWaypoints(availableTrip.getWaypoints());
              config.setRouteToken(availableTripResponse.getRouteToken());
              return config;
            },
            executor);
  }

  private ListenableFuture<GetTripResponse> fetchAvailableTripWithRetires() {
    return new RetryingFuture(scheduledExecutor)
        .runWithRetries(
            restProvider::getAvailableTrip,
            RetryingFuture.RUN_FOREVER,
            GET_TRIP_RETRY_INTERVAL_MILLIS,
            LocalProviderService::isTripValid);
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

  private static boolean isTripValid(GetTripResponse tripResponse) {
    TripData availableTrip = tripResponse.getTripData();
    if (availableTrip == null) {
      return false;
    }
    List<String> parts = SPLITTER.splitToList(availableTrip.getName());
    return parts.size() == TRIP_NAME_SEGMENT_COUNT;
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
