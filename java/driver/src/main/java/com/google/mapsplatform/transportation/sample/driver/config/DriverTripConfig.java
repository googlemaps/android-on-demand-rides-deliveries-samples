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
package com.google.mapsplatform.transportation.sample.driver.config;

import com.google.gson.annotations.SerializedName;

/** Driver Trip Config JSON access methods. */
public class DriverTripConfig {

  /** Returns the trip Google project id. */
  public String getProjectId() {
    return projectId;
  }

  /** Returns the trip id. */
  public String getTripId() {
    return tripId;
  }

  /** Returns the vehicle id for the trip. */
  public String getVehicleId() {
    return vehicleId;
  }

  /** Returns the vehicle starting location for the trip. */
  public Point getVehicleLocation() {
    return vehicleLocation;
  }

  /** Returns the array of Waypoints for the trip. */
  public Waypoint[] getWaypoints() {
    return waypoints;
  }

  /** Returns the route token of the trip. */
  public String getRouteToken() {
    return routeToken;
  }

  @SerializedName("project_id")
  private String projectId;

  @SerializedName("trip_id")
  private String tripId;

  @SerializedName("vehicle_id")
  private String vehicleId;

  @SerializedName("vehicle_location")
  private Point vehicleLocation;

  @SerializedName("waypoints")
  private Waypoint[] waypoints = new Waypoint[0];

  private String routeToken;

  /** Represents the waypoint consisting of a location and type of the waypoint. */
  public static class Waypoint {

    /** Returns the waypoint location. */
    public Location getLocation() {
      return location;
    }

    /** Returns the type of waypoint, pickup, dropoff, or intermediateDestination. */
    public String getWaypointType() {
      return waypointType;
    }

    @SerializedName("location")
    private Location location;

    public void setLocation(Location location) {
      this.location = location;
    }

    public void setWaypointType(String waypointType) {
      this.waypointType = waypointType;
    }

    @SerializedName("waypointType")
    private String waypointType;
  }

  /** Represents a location consisting of a single point. */
  public static class Location {

    /** Returns the point that defines the location. */
    public Point getPoint() {
      return point;
    }

    public void setPoint(Point point) {
      this.point = point;
    }

    @SerializedName(value = "point")
    private Point point;
  }

  /** Represents a point which is latitude and longitude coordinates. */
  public static class Point {

    /** Returns the latitude component of the point. */
    public double getLatitude() {
      return latitude;
    }

    /** Returns the longitude component of the point. */
    public double getLongitude() {
      return longitude;
    }

    public void setLatitude(double latitude) {
      this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
      this.longitude = longitude;
    }

    @SerializedName(value = "latitude")
    private double latitude;

    @SerializedName(value = "longitude")
    private double longitude;
  }

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

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public void setTripId(String tripId) {
    this.tripId = tripId;
  }

  public void setVehicleId(String vehicleId) {
    this.vehicleId = vehicleId;
  }

  public void setVehicleLocation(Point vehicleLocation) {
    this.vehicleLocation = vehicleLocation;
  }

  public void setWaypoints(Waypoint[] waypoints) {
    this.waypoints = waypoints;
  }

  public void setRouteToken(String routeToken) {
    this.routeToken = routeToken;
  }
}
