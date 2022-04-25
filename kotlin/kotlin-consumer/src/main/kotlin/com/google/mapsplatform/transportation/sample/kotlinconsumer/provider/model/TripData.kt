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
package com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.model

import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.response.WaypointResponse

/**
 * Relevant trip information from the response from sample provider along with the consumer token to
 * start journeysharing and display information on the app.
 */
data class TripData(
  val tripName: String,
  val tripStatus: Int,

  /** List of waypoints for dropoff and pickup point. */
  val waypoints: List<WaypointResponse>,
  val vehicleId: String,
  val tripId: String
)
