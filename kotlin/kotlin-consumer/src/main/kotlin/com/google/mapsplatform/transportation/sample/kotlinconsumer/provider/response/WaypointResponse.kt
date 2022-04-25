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
package com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.response

import com.google.gson.annotations.SerializedName

/**
 * Waypoint given by the sampleprovider
 *
 * Sample JSON structure { "location": { "point": { "latitude": 3.456, "longitude": 4.567 } },
 * "waypointType: "DROP_OFF_WAYPOINT_TYPE" }
 */
class WaypointResponse(
  @SerializedName("location") val location: Location? = null,
  @SerializedName("waypointType") private val waypointType: String? = null
) {

  /** Represents terminal location json object. */
  class Location(@SerializedName(value = "point") val point: Point?)

  /** Represents LatLng point for a given location. */
  class Point(
    @SerializedName(value = "latitude") val latitude: Float?,
    @SerializedName(value = "longitude") val longitude: Float?
  )
}
