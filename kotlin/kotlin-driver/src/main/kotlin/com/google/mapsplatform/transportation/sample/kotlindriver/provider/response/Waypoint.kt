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

/**
 * Waypoint given by the sample provider
 *
 * <p>Sample JSON structure { "location": { "point": { "latitude": 3.456, "longitude": 4.567 } },
 * "waypointType: "DROP_OFF_WAYPOINT_TYPE", "tripId": "trip" }
 */
class Waypoint(
  /** Trip ID the waypoint belongs to. */
  @SerializedName("tripId") val tripId: String = "",

  /** Returns the waypoint location. */
  @SerializedName("location") val location: Location? = null,

  /** Returns the type of waypoint, pickup, dropoff, or intermediateDestination. */
  @SerializedName("waypointType") val waypointType: String = "",
) {
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
    @SerializedName(value = "longitude") val longitude: Double = 0.0,
  )
}
