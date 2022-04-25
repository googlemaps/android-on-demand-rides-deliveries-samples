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
package com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.service

import com.google.common.util.concurrent.ListenableFuture
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.model.WaypointData
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.response.GetTripResponse
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.response.TokenResponse
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.response.TripResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** Abstraction of expected REST endpoints implemented by a Provider. */
interface RestProvider {
  @POST("trip/new")
  fun createSingleExclusiveTrip(@Body waypoint: WaypointData): ListenableFuture<TripResponse>

  @GET("trip/{tripId}")
  fun getTrip(@Path("tripId") tripId: String): ListenableFuture<GetTripResponse>

  @GET("token/consumer/{tripId}")
  fun getConsumerToken(@Path("tripId") tripId: String): ListenableFuture<TokenResponse>
}
