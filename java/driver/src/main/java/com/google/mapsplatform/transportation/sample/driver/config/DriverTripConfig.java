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
package com.google.mapsplatform.transportation.sample.driver.config;

import com.google.mapsplatform.transportation.sample.driver.provider.response.Waypoint;

/** Driver Trip Config JSON access methods. */
public class DriverTripConfig {

  /** Returns the trip id. */
  public String getTripId() {
    return tripId;
  }

  /** Returns the vehicle id for the trip. */
  public String getVehicleId() {
    return vehicleId;
  }

  /** Returns the vehicle starting location for the trip. */
  public Waypoint.Point getVehicleLocation() {
    return vehicleLocation;
  }

  /** Returns the array of Waypoints for the trip. */
  public Waypoint[] getWaypoints() {
    return waypoints;
  }

  private String tripId;

  private String vehicleId;

  private Waypoint.Point vehicleLocation;

  private Waypoint[] waypoints = new Waypoint[0];

  /**
   * Returns the waypoint at given index.
   *
   * @param index Waypoint index
   * @return The {@link Waypoint} found or null.
   */
  public Waypoint getWaypoint(int index) {
    if (index < 0 || index == waypoints.length) {
      return null;
    }

    return waypoints[index];
  }

  public void setTripId(String tripId) {
    this.tripId = tripId;
  }

  public void setVehicleId(String vehicleId) {
    this.vehicleId = vehicleId;
  }

  public void setVehicleLocation(Waypoint.Point vehicleLocation) {
    this.vehicleLocation = vehicleLocation;
  }

  public void setWaypoints(Waypoint[] waypoints) {
    this.waypoints = waypoints;
  }
}
