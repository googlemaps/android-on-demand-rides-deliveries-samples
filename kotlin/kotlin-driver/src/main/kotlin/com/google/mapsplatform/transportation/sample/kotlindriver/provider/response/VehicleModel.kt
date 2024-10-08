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
package com.google.mapsplatform.transportation.sample.kotlindriver.provider.response

import com.google.gson.annotations.SerializedName

/** Non-extensive POJO representation of a Vehicle object. */
class VehicleModel(
  @SerializedName("name") val name: String = "",
  @SerializedName("vehicleState") val vehicleState: String = "",
  @SerializedName("waypoints") val waypoints: List<Waypoint> = listOf(),
  @SerializedName("currentTripsIds") val currentTripsIds: List<String> = listOf(),
  @SerializedName("backToBackEnabled") val backToBackEnabled: Boolean = false,
  @SerializedName("supportedTripTypes") val supportedTripTypes: List<String> = listOf(),
  @SerializedName("maximumCapacity") val maximumCapacity: Int = 5,
)
