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
package com.google.mapsplatform.transportation.sample.kotlindriver.provider.request

import com.google.mapsplatform.transportation.sample.kotlindriver.utils.TripUtils.EXCLUSIVE_TRIP_TYPE

/** Body object for vehicle settings. It is used for creating/updating vehicle. */
data class VehicleSettings(
  val vehicleId: String = "",
  val backToBackEnabled: Boolean = false,
  val maximumCapacity: Int = 5,
  val supportedTripTypes: List<String> = listOf(EXCLUSIVE_TRIP_TYPE),
)
