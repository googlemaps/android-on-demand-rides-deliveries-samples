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
package com.google.mapsplatform.transportation.sample.consumer.provider.service;

import com.google.android.libraries.mapsplatform.transportation.consumer.model.TripName;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mapsplatform.transportation.sample.consumer.provider.model.CreateTripRequest;
import com.google.mapsplatform.transportation.sample.consumer.provider.model.TripData;
import com.google.mapsplatform.transportation.sample.consumer.provider.model.TripStatus;
import com.google.mapsplatform.transportation.sample.consumer.provider.response.GetTripResponse;
import com.google.mapsplatform.transportation.sample.consumer.provider.response.TokenResponse;
import com.google.mapsplatform.transportation.sample.consumer.provider.response.TripResponse;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import retrofit2.Retrofit;
import retrofit2.adapter.guava.GuavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Provider service for a locally hosted Sample Journeysharing provider.
 *
 * <p>Default base URL: http://10.0.2.2:8888/
 */
public class LocalProviderService {

  private static final String TAG = "LocalProviderService";

  public static final int GET_TRIP_RETRY_INTERVAL_MILLIS = 5000;
  /** Time between attempts of a trip polling routine. */
  private final RestProvider provider;

  private final Executor executor;
  private final ScheduledExecutorService scheduledExecutor;

  public LocalProviderService(
      RestProvider provider, Executor executor, ScheduledExecutorService scheduledExecutor) {
    this.provider = provider;
    this.executor = executor;
    this.scheduledExecutor = scheduledExecutor;
  }

  public ListenableFuture<TripResponse> createTrip(CreateTripRequest createTripRequest) {
    return provider.createTrip(createTripRequest);
  }

  public ListenableFuture<TokenResponse> fetchAuthToken(String tripId) {
    return provider.getConsumerToken(tripId);
  }

  public ListenableFuture<TripData> fetchMatchedTrip(String tripName) {
    String tripId = TripName.create(tripName).getTripId();
    ListenableFuture<GetTripResponse> getTripResponseFuture = fetchMatchedTripWithRetries(tripId);
    return Futures.transform(
        getTripResponseFuture,
        getTripResponse -> {
          TripResponse trip = getTripResponse.getTrip();
          return TripData.newBuilder()
              .setTripId(tripId)
              .setTripName(trip.getTripName())
              .setVehicleId(trip.getVehicleId())
              .setTripStatus(TripStatus.parse(trip.getTripStatus()))
              .setWaypoints(Lists.newArrayList(trip.getWaypoints()))
              .build();
        },
        executor);
  }

  private ListenableFuture<GetTripResponse> fetchMatchedTripWithRetries(String tripId) {
    return new RetryingFuture(scheduledExecutor)
        .runWithRetries(
            () -> provider.getTrip(tripId),
            RetryingFuture.RUN_FOREVER,
            GET_TRIP_RETRY_INTERVAL_MILLIS,
            LocalProviderService::isTripValid);
  }

  private static boolean isTripValid(GetTripResponse response) {
    return response != null
        && response.getTrip() != null
        && response.getTrip().getVehicleId() != null
        && !response.getTrip().getVehicleId().isEmpty();
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
