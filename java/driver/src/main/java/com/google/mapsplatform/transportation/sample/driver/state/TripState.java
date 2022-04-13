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
package com.google.mapsplatform.transportation.sample.driver.state;

import com.google.mapsplatform.transportation.sample.driver.config.DriverTripConfig;

/**
 * Class that represents the state of the current trip. State is represented by 'Status' as well as
 * 'waypointIndex'. In case of multi-destination trips, waypointIndex will contain
 * intermediateDestination (waypointIndex - 1).
 */
public final class TripState {
  private int waypointIndex = -1;
  private TripStatus status = TripStatus.UNKNOWN_TRIP_STATUS;
  private String backToBackTripId = "";

  private static final String INTERMEDIATE_DESTINATION_WAYPOINT_TYPE =
      "INTERMEDIATE_DESTINATION_WAYPOINT_TYPE";

  private static final String DROP_OFF_WAYPOINT_TYPE = "DROP_OFF_WAYPOINT_TYPE";

  private final DriverTripConfig serverTripConfig;

  public TripState(DriverTripConfig serverTripConfig) {
    this.serverTripConfig = serverTripConfig;
  }

  // Getter for the current trip waypoint index.
  public int getWaypointIndex() {
    return waypointIndex;
  }

  // Getter for the current trip status.
  public TripStatus getStatus() {
    return status;
  }

  // Returns true if driver is on the last intermediate destination of the current trip.
  public boolean isOnLastIntermediateDestination() {
    return waypointIndex + 1 < serverTripConfig.getWaypoints().length
        && serverTripConfig
            .getWaypoint(waypointIndex)
            .getWaypointType()
            .equals(INTERMEDIATE_DESTINATION_WAYPOINT_TYPE)
        && serverTripConfig
            .getWaypoint(waypointIndex + 1)
            .getWaypointType()
            .equals(DROP_OFF_WAYPOINT_TYPE);
  }

  // Return trip if current trip has more then 2 destinations (pickup, dropoff).
  public boolean hasIntermediateDestinations() {
    return serverTripConfig.getWaypoints().length > 2;
  }

  // Resets the current trip state.
  public void resetTripState() {
    status = TripStatus.UNKNOWN_TRIP_STATUS;
    waypointIndex = -1;
  }

  // Sets the current trip state as matched. Example: has been paired but is not yet in route.
  public void setTripMatchedState() {
    status = TripStatus.NEW;
    waypointIndex = -1;
  }

  // Sets the tripId assigned as B2B while the current trip is in course.
  public void setBackToBackTripId(String tripId) {
    this.backToBackTripId = tripId;
  }

  // Gets the tripId assigned as B2B while the current trip is in course.
  public String getBackToBackTripId() {
    return backToBackTripId;
  }

  // Returns the current trip ID.
  public String getTripId() {
    return serverTripConfig.getTripId();
  }

  // Returns the route token.
  public String getRouteToken() {
    return serverTripConfig.getRouteToken();
  }

  // Returns the list of trip waypoints.
  public DriverTripConfig.Waypoint[] getWaypoints() {
    return serverTripConfig.getWaypoints();
  }

  // Returns the initial vehicle locaiton reported to the server.
  public DriverTripConfig.Point getInitialVehicleLocation() {
    return serverTripConfig.getVehicleLocation();
  }

  // Machine state processor that calculates the next state based on the current state parameters.
  public void processNextState() {
    status = getNextStatus();

    if (status == TripStatus.ENROUTE_TO_PICKUP
        || status == TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION
        || status == TripStatus.ENROUTE_TO_DROPOFF) {
      waypointIndex++;
    }
  }

  private TripStatus getNextStatus() {
    switch (status) {
      case NEW:
        return TripStatus.ENROUTE_TO_PICKUP;

      case ENROUTE_TO_PICKUP:
        return TripStatus.ARRIVED_AT_PICKUP;

      case ARRIVED_AT_PICKUP:
        if (hasIntermediateDestinations()) {
          return TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION;
        }

        return TripStatus.ENROUTE_TO_DROPOFF;

      case ENROUTE_TO_INTERMEDIATE_DESTINATION:
        return TripStatus.ARRIVED_AT_INTERMEDIATE_DESTINATION;

      case ARRIVED_AT_INTERMEDIATE_DESTINATION:
        if (isOnLastIntermediateDestination()) {
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
