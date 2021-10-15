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
package com.google.mapsplatform.transportation.sample.driver.provider.service;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mapsplatform.transportation.sample.driver.provider.request.TripStatusBody;
import com.google.mapsplatform.transportation.sample.driver.provider.request.VehicleIdBody;
import com.google.mapsplatform.transportation.sample.driver.provider.response.GetTripResponse;
import com.google.mapsplatform.transportation.sample.driver.provider.response.TokenResponse;
import com.google.mapsplatform.transportation.sample.driver.provider.response.TripData;
import com.google.mapsplatform.transportation.sample.driver.provider.response.VehicleResponse;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

interface RestProvider {

  @GET("vehicle/{id}")
  ListenableFuture<VehicleResponse> getVehicle(@Path("id") String vehicle);

  @GET("token/driver")
  ListenableFuture<TokenResponse> getAuthToken();

  @GET("trip/")
  ListenableFuture<GetTripResponse> getAvailableTrip();

  @POST("vehicle/new")
  ListenableFuture<VehicleResponse> createVehicle(@Body VehicleIdBody body);

  @PUT("trip/{id}")
  ListenableFuture<TripData> updateTripStatus(@Path("id") String id, @Body TripStatusBody body);
}
