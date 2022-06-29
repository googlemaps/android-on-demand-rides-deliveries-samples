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
package com.google.mapsplatform.transportation.sample.driver.utils;

import com.google.mapsplatform.transportation.sample.driver.provider.response.Waypoint;
import com.google.mapsplatform.transportation.sample.driver.state.TripState;
import com.google.mapsplatform.transportation.sample.driver.state.TripStatus;

/** Provides utility methods to facilitate Trip state management. */
public final class TripUtils {
  public static final String PICKUP_WAYPOINT_TYPE = "PICKUP_WAYPOINT_TYPE";

  public static final String INTERMEDIATE_DESTINATION_WAYPOINT_TYPE =
      "INTERMEDIATE_DESTINATION_WAYPOINT_TYPE";

  public static final String DROP_OFF_WAYPOINT_TYPE = "DROP_OFF_WAYPOINT_TYPE";

  /**
   * Returns the initial accepted trip state.
   *
   * @param tripId Trip to create the state for.
   */
  public static TripState getInitialTripState(String tripId) {
    return TripState.create(tripId, TripStatus.NEW);
  }

  /**
   * Machine state processor that calculates the next state based on the current state parameters.
   *
   * @param tripState base state used as input for the processor.
   * @param nextWaypointOfTrip next waypoint to process for the trip the state belongs to.
   */
  public static TripState getNextTripState(TripState tripState, Waypoint nextWaypointOfTrip) {
    TripStatus nextStatus = getNextStatus(tripState, nextWaypointOfTrip);

    if (nextStatus == TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION) {
      int currentIntermediateDestinationIndex = tripState.intermediateDestinationIndex();

      return TripState.create(
          tripState.tripId(), nextStatus, currentIntermediateDestinationIndex + 1);
    }

    return TripState.create(tripState.tripId(), nextStatus);
  }

  /** Determines if the given status is a type of 'ENROUTE'. */
  public static boolean isTripStatusEnroute(TripStatus status) {
    return status == TripStatus.ENROUTE_TO_PICKUP
        || status == TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION
        || status == TripStatus.ENROUTE_TO_DROPOFF;
  }

  /**
   * Returns an updated 'ENROUTE' state for the given waypoint. Depending on the waypoint type, it
   * will return a different type of 'ENROUTE' status.
   */
  public static TripState getEnrouteStateForWaypoint(Waypoint waypoint, TripState currentState) {
    TripState state;

    switch (waypoint.getWaypointType()) {
      case TripUtils.PICKUP_WAYPOINT_TYPE:
        state = TripState.create(waypoint.getTripId(), TripStatus.ENROUTE_TO_PICKUP);
        break;

      case TripUtils.DROP_OFF_WAYPOINT_TYPE:
        state = TripState.create(waypoint.getTripId(), TripStatus.ENROUTE_TO_DROPOFF);
        break;

      case TripUtils.INTERMEDIATE_DESTINATION_WAYPOINT_TYPE:
        state =
            TripState.create(
                waypoint.getTripId(),
                TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION,
                currentState.intermediateDestinationIndex() + 1);
        break;

      default:
        throw new IllegalStateException("Invalid waypoint type for nextWaypoint");
    }

    return state;
  }

  private static TripStatus getNextStatus(TripState tripState, Waypoint nextWaypointOfTrip) {
    switch (tripState.tripStatus()) {
      case NEW:
        return TripStatus.ENROUTE_TO_PICKUP;

      case ENROUTE_TO_PICKUP:
        return TripStatus.ARRIVED_AT_PICKUP;

      case ARRIVED_AT_PICKUP:
        if (nextWaypointOfTrip.getWaypointType().equals(INTERMEDIATE_DESTINATION_WAYPOINT_TYPE)) {
          return TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION;
        }

        return TripStatus.ENROUTE_TO_DROPOFF;

      case ENROUTE_TO_INTERMEDIATE_DESTINATION:
        return TripStatus.ARRIVED_AT_INTERMEDIATE_DESTINATION;

      case ARRIVED_AT_INTERMEDIATE_DESTINATION:
        if (nextWaypointOfTrip.getWaypointType().equals(DROP_OFF_WAYPOINT_TYPE)) {
          return TripStatus.ENROUTE_TO_DROPOFF;
        }

        return TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION;

      case ENROUTE_TO_DROPOFF:
        return TripStatus.COMPLETE;

      default:
        return TripStatus.UNKNOWN_TRIP_STATUS;
    }
  }
}
