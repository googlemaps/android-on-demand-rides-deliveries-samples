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
package com.google.mapsplatform.transportation.sample.kotlindriver.state

import com.google.mapsplatform.transportation.sample.kotlindriver.config.DriverTripConfig

/**
 * Class that represents the state of the current trip. State is represented by 'Status' as well as
 * 'waypointIndex'. In case of multi-destination trips, waypointIndex will contain
 * intermediateDestination (waypointIndex - 1).
 */
class TripState(private val serverTripConfig: DriverTripConfig) {
  // Getter for the current trip waypoint index.
  var waypointIndex = -1
    private set

  // Getter for the current trip status.
  var status = TripStatus.UNKNOWN_TRIP_STATUS
    private set

  // Sets the tripId assigned as B2B while the current trip is in course.
  // Gets the tripId assigned as B2B while the current trip is in course.
  var backToBackTripId: String? = ""

  // Returns true if driver is on the last intermediate destination of the current trip.
  val isOnLastIntermediateDestination: Boolean
    get() =
      waypointIndex + 1 < serverTripConfig.waypoints.size &&
        (serverTripConfig.getWaypoint(waypointIndex)?.waypointType ==
          INTERMEDIATE_DESTINATION_WAYPOINT_TYPE) &&
        (serverTripConfig.getWaypoint(waypointIndex + 1)?.waypointType == DROP_OFF_WAYPOINT_TYPE)

  // Return trip if current trip has more then 2 destinations (pickup, dropoff).
  fun hasIntermediateDestinations(): Boolean = serverTripConfig.waypoints.size > 2

  // Sets the current trip state as matched. Example: has been paired but is not yet in route.
  fun setTripMatchedState() {
    status = TripStatus.NEW
    waypointIndex = -1
  }

  // Returns the current trip ID.
  val tripId: String?
    get() = serverTripConfig.tripId

  // Returns the route token.
  val routeToken: String?
    get() = serverTripConfig.routeToken

  // Returns the list of trip waypoints.
  val waypoints: List<DriverTripConfig.Waypoint>
    get() = serverTripConfig.waypoints

  // Returns the initial vehicle locaiton reported to the server.
  val initialVehicleLocation: DriverTripConfig.Point?
    get() = serverTripConfig.vehicleLocation

  // Machine state processor that calculates the next state based on the current state parameters.
  fun processNextState() {
    status = nextStatus
    if (status == TripStatus.ENROUTE_TO_PICKUP ||
        status == TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION ||
        status == TripStatus.ENROUTE_TO_DROPOFF
    ) {
      waypointIndex++
    }
  }

  private val nextStatus: TripStatus
    get() =
      when (status) {
        TripStatus.NEW -> TripStatus.ENROUTE_TO_PICKUP
        TripStatus.ENROUTE_TO_PICKUP -> TripStatus.ARRIVED_AT_PICKUP
        TripStatus.ARRIVED_AT_PICKUP -> {
          if (hasIntermediateDestinations()) {
            TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION
          } else TripStatus.ENROUTE_TO_DROPOFF
        }
        TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION ->
          TripStatus.ARRIVED_AT_INTERMEDIATE_DESTINATION
        TripStatus.ARRIVED_AT_INTERMEDIATE_DESTINATION -> {
          if (isOnLastIntermediateDestination) {
            TripStatus.ENROUTE_TO_DROPOFF
          } else TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION
        }
        TripStatus.ENROUTE_TO_DROPOFF -> TripStatus.COMPLETE
        else -> TripStatus.UNKNOWN_TRIP_STATUS
      }

  companion object {
    private const val INTERMEDIATE_DESTINATION_WAYPOINT_TYPE =
      "INTERMEDIATE_DESTINATION_WAYPOINT_TYPE"
    private const val DROP_OFF_WAYPOINT_TYPE = "DROP_OFF_WAYPOINT_TYPE"
  }
}
