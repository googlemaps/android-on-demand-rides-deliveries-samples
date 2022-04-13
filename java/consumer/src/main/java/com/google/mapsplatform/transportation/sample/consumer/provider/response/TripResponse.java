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
package com.google.mapsplatform.transportation.sample.consumer.provider.response;

import com.google.gson.annotations.SerializedName;

/** Non-extensive POJO representation of a Trip response object. */
public class TripResponse {

  @SerializedName("name")
  private String tripName;

  @SerializedName("tripStatus")
  private String tripStatus;

  @SerializedName("waypoints")
  private WaypointResponse[] waypoints;

  @SerializedName("vehicleId")
  private String vehicleId;

  public WaypointResponse[] getWaypoints() {
    return waypoints;
  }

  public void setWaypoints(WaypointResponse[] waypoints) {
    this.waypoints = waypoints;
  }

  public String getTripName() {
    return tripName;
  }

  public void setTripName(String tripName) {
    this.tripName = tripName;
  }

  public String getTripStatus() {
    return tripStatus;
  }

  public void setTripStatus(String tripStatus) {
    this.tripStatus = tripStatus;
  }

  public String getVehicleId() {
    return vehicleId;
  }

  public void setVehicleId(String vehicleId) {
    this.vehicleId = vehicleId;
  }
}
