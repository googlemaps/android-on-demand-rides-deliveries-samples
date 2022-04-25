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

import com.google.gson.annotations.SerializedName

/** Driver Trip Config JSON access methods. */
class DriverTripConfig(
  /** Returns the trip Google project id. */
  @SerializedName("project_id") val projectId: String? = null,

  /** Returns the trip id. */
  @SerializedName("trip_id") val tripId: String? = null,

  /** Returns the vehicle id for the trip. */
  @SerializedName("vehicle_id") val vehicleId: String? = null,

  /** Returns the vehicle starting location for the trip. */
  @SerializedName("vehicle_location") var vehicleLocation: Point? = null,

  /** Returns the array of Waypoints for the trip. */
  @SerializedName("waypoints") val waypoints: List<Waypoint> = listOf(),
  /** Returns the route token of the trip. */
  val routeToken: String? = null
) {
  /** Represents the waypoint consisting of a location and type of the waypoint. */
  class Waypoint(
    /** Returns the waypoint location. */
    @SerializedName("location") val location: Location? = null,

    /** Returns the type of waypoint, pickup, dropoff, or intermediateDestination. */
    @SerializedName("waypointType") val waypointType: String? = null
  )

  /** Represents a location consisting of a single point. */
  class Location(
    /** Returns the point that defines the location. */
    @SerializedName(value = "point") val point: Point? = null
  )

  /** Represents a point which is latitude and longitude coordinates. */
  class Point(
    /** Returns the latitude component of the point. */
    @SerializedName(value = "latitude") val latitude: Double = 0.0,

    /** Returns the longitude component of the point. */
    @SerializedName(value = "longitude") val longitude: Double = 0.0
  )

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
