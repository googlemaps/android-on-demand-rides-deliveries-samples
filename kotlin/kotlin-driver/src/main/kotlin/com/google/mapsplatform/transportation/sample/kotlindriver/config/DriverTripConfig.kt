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
package com.google.mapsplatform.transportation.sample.kotlindriver.config

import com.google.mapsplatform.transportation.sample.kotlindriver.provider.response.Waypoint

/** Driver Trip Config JSON access methods. */
class DriverTripConfig(
  /** Returns the trip id. */
  val tripId: String? = null,

  /** Returns the vehicle id for the trip. */
  val vehicleId: String? = null,

  /** Returns the vehicle starting location for the trip. */
  var vehicleLocation: Waypoint.Point? = null,

  /** Returns the array of Waypoints for the trip. */
  val waypoints: List<Waypoint> = listOf(),
) {
  /**
   * Returns the waypoint at given index.
   *
   * @param index Waypoint index
   * @return The [Waypoint] found or null.
   */
  fun getWaypoint(index: Int): Waypoint? {
    return if (index < 0 || index == waypoints.size) {
      null
    } else {
      waypoints[index]
    }
  }
}
