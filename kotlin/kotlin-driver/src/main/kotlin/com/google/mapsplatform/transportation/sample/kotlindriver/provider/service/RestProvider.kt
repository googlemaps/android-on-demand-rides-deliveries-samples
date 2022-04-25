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
package com.google.mapsplatform.transportation.sample.kotlindriver.provider.service

import com.google.common.util.concurrent.ListenableFuture
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.request.CreateVehicleBody
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.request.TripUpdateBody
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.response.GetTripResponse
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.response.TokenResponse
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.response.TripData
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.response.VehicleResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface RestProvider {
  @GET("vehicle/{id}")
  fun getVehicle(@Path("id") vehicle: String): ListenableFuture<VehicleResponse>

  @GET("token/driver/{vehicleId}")
  fun getAuthToken(@Path("vehicleId") vehicleId: String): ListenableFuture<TokenResponse>

  @GET("trip/{tripId}")
  fun getAvailableTrip(@Path("tripId") id: String): ListenableFuture<GetTripResponse>

  @POST("vehicle/new")
  fun createVehicle(@Body body: CreateVehicleBody): ListenableFuture<VehicleResponse>

  @PUT("trip/{id}")
  fun updateTrip(@Path("id") id: String, @Body body: TripUpdateBody): ListenableFuture<TripData>
}
