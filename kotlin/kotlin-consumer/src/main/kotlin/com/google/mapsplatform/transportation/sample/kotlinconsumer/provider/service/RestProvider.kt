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
package com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.service

import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.model.CreateTripRequest
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.response.GetTripResponse
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.response.TokenResponse
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.response.TripResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** Abstraction of expected REST endpoints implemented by a Provider. */
interface RestProvider {
  @POST("trip/new") suspend fun createTrip(@Body createTripRequest: CreateTripRequest): TripResponse

  @GET("trip/{tripId}") suspend fun getTrip(@Path("tripId") tripId: String): GetTripResponse

  @GET("token/consumer/{tripId}")
  suspend fun getConsumerToken(@Path("tripId") tripId: String): TokenResponse
}
