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
package com.google.mapsplatform.transportation.sample.kotlindriver.utils

import com.google.mapsplatform.transportation.sample.kotlindriver.provider.response.Waypoint
import com.google.mapsplatform.transportation.sample.kotlindriver.state.TripState
import com.google.mapsplatform.transportation.sample.kotlindriver.state.TripStatus

/** Contains utility methods and constants for dealing with trips in the application. */
object TripUtils {
  val PICKUP_WAYPOINT_TYPE = "PICKUP_WAYPOINT_TYPE"
  val INTERMEDIATE_DESTINATION_WAYPOINT_TYPE = "INTERMEDIATE_DESTINATION_WAYPOINT_TYPE"
  val DROP_OFF_WAYPOINT_TYPE = "DROP_OFF_WAYPOINT_TYPE"
  val SHARED_TRIP_TYPE = "SHARED"
  val EXCLUSIVE_TRIP_TYPE = "EXCLUSIVE"

  private val ENROUTE_TRIP_STATUSES =
    listOf(
      TripStatus.ENROUTE_TO_PICKUP,
      TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION,
      TripStatus.ENROUTE_TO_DROPOFF,
    )

  private val ARRIVED_TRIP_STATUSES =
    listOf(
      TripStatus.ARRIVED_AT_PICKUP,
      TripStatus.ARRIVED_AT_INTERMEDIATE_DESTINATION,
      TripStatus.COMPLETE
    )

  /** Returns the initial state for a trip after it has been accepted. */
  fun getInitialTripState(tripId: String): TripState = TripState(tripId, TripStatus.NEW)

  /**
   * Processes the next state for the trip based on its current state (which includes status and
   * intermediate destination index) as well as the next waypoint to visit for it.
   */
  fun getNextTripState(tripState: TripState, nextWaypointOfTrip: Waypoint?): TripState {
    val nextStatus = getNextTripStatus(tripState, nextWaypointOfTrip)

    val intermediateDestinationIndex =
      if (nextStatus == TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION) {
        tripState.intermediateDestinationIndex + 1
      } else tripState.intermediateDestinationIndex

    return TripState(tripState.tripId, nextStatus, intermediateDestinationIndex)
  }

  /** Gets the 'ENROUTE' state for a trip based on its waypoint. */
  fun getEnrouteStateForWaypoint(currentState: TripState, nextWaypoint: Waypoint): TripState =
    if (currentState.tripStatus == TripStatus.COMPLETE) currentState
    else {
      when (nextWaypoint.waypointType) {
        PICKUP_WAYPOINT_TYPE -> TripState(currentState.tripId, TripStatus.ENROUTE_TO_PICKUP)
        INTERMEDIATE_DESTINATION_WAYPOINT_TYPE ->
          TripState(
            currentState.tripId,
            TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION,
            currentState.intermediateDestinationIndex + 1
          )
        DROP_OFF_WAYPOINT_TYPE -> TripState(currentState.tripId, TripStatus.ENROUTE_TO_DROPOFF)
        else -> throw IllegalStateException("Invalid waypoint type")
      }
    }

  /** Determines if a trip has a status that is considered enroute. */
  fun isTripStatusEnroute(tripStatus: TripStatus) = tripStatus in ENROUTE_TRIP_STATUSES

  /** Determines if a trip has a status that is considered arrived. */
  fun isTripStatusArrived(tripStatus: TripStatus) = tripStatus in ARRIVED_TRIP_STATUSES

  /**
   * Calculates the next status for a trip based on its current status as well as the next waypoint
   * type.
   */
  private fun getNextTripStatus(tripState: TripState, nextWaypointOfTrip: Waypoint?): TripStatus =
    when (tripState.tripStatus) {
      TripStatus.NEW -> TripStatus.ENROUTE_TO_PICKUP
      TripStatus.ENROUTE_TO_PICKUP -> TripStatus.ARRIVED_AT_PICKUP
      TripStatus.ARRIVED_AT_PICKUP -> {
        if (nextWaypointOfTrip?.waypointType == INTERMEDIATE_DESTINATION_WAYPOINT_TYPE) {
          TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION
        } else {
          TripStatus.ENROUTE_TO_DROPOFF
        }
      }
      TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION ->
        TripStatus.ARRIVED_AT_INTERMEDIATE_DESTINATION
      TripStatus.ARRIVED_AT_INTERMEDIATE_DESTINATION -> {
        if (nextWaypointOfTrip?.waypointType == DROP_OFF_WAYPOINT_TYPE) {
          TripStatus.ENROUTE_TO_DROPOFF
        } else {
          TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION
        }
      }
      TripStatus.ENROUTE_TO_DROPOFF -> TripStatus.COMPLETE
      else -> TripStatus.UNKNOWN_TRIP_STATUS
    }
}
