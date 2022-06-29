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
package com.google.mapsplatform.transportation.sample.driver.provider.response;

import com.google.gson.annotations.SerializedName;

/**
 * Waypoint given by the sample provider
 *
 * <p>Sample JSON structure { "location": { "point": { "latitude": 3.456, "longitude": 4.567 } },
 * "waypointType: "DROP_OFF_WAYPOINT_TYPE", "tripId": "trip" }
 */
public final class Waypoint {

  @SerializedName("location")
  private Location location;

  @SerializedName("waypointType")
  private String waypointType;

  @SerializedName("tripId")
  private String tripId;

  /** Returns the location for the given waypoint. */
  public Location getLocation() {
    return location;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  /** Returns the type of waypoint, pickup, dropoff, or intermediateDestination. */
  public String getWaypointType() {
    return waypointType;
  }

  public void setWaypointType(String waypointType) {
    this.waypointType = waypointType;
  }

  /** Returns the trip id this waypoint belongs to. */
  public String getTripId() {
    return tripId;
  }

  public void setTripId(String tripId) {
    this.tripId = tripId;
  }

  /** Represents terminal location json object. */
  public static class Location {
    @SerializedName(value = "point")
    private Point point;

    public Point getPoint() {
      return point;
    }

    public void setPoint(Point point) {
      this.point = point;
    }
  }

  /** Represents LatLng point for a given location. */
  public static class Point {
    @SerializedName(value = "latitude")
    private double latitude;

    @SerializedName(value = "longitude")
    private double longitude;

    public double getLatitude() {
      return latitude;
    }

    public double getLongitude() {
      return longitude;
    }

    public void setLatitude(double latitude) {
      this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
      this.longitude = longitude;
    }
  }
}
