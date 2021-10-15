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
// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.mapsplatform.transportation.sample.provider.service;


import com.google.common.util.concurrent.ListenableFuture;
import com.google.mapsplatform.transportation.sample.provider.model.WaypointData;
import com.google.mapsplatform.transportation.sample.provider.response.CustomRouteResponse;
import com.google.mapsplatform.transportation.sample.provider.response.GetTripResponse;
import com.google.mapsplatform.transportation.sample.provider.response.TokenResponse;
import com.google.mapsplatform.transportation.sample.provider.response.TripResponse;

import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Abstraction of expected REST endpoints implemented by a Provider. */
interface RestProvider {

  @POST("trip/new")
  ListenableFuture<TripResponse> createSingleExclusiveTrip(@Body WaypointData waypoint);

  @GET("trip/{tripId}")
  ListenableFuture<GetTripResponse> getTrip(@Path("tripId") String tripId);

  @GET("token/consumer")
  ListenableFuture<TokenResponse> getConsumerToken();

  @GET("custom-route")
  ListenableFuture<CustomRouteResponse> getCustomRoute(
      @Query("pickup") String pickup, @Query("dropoff") String dropoff);
}
