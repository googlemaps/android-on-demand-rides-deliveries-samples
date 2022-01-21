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
package com.google.mapsplatform.transportation.sample.provider.response;

import androidx.annotation.VisibleForTesting;
import com.google.gson.annotations.SerializedName;

/**
 * Waypoint given by the sampleprovider
 *
 * <p>Sample JSON structure { "location": { "point": { "latitude": 3.456, "longitude": 4.567 } },
 * "waypointType: "DROP_OFF_WAYPOINT_TYPE" }
 */
public class WaypointResponse {
  @SerializedName("location")
  private Location location;

  @SerializedName("waypointType")
  private String waypointType;

  public Location getLocation() {
    return location;
  }

  @VisibleForTesting
  public void setLocation(Location location) {
    this.location = location;
  }

  /** Represents terminal location json object. */
  public static class Location {
    @SerializedName(value = "point")
    private Point point;

    public Point getPoint() {
      return point;
    }

    @VisibleForTesting
    public void setPoint(Point point) {
      this.point = point;
    }
  }

  /** Represents LatLng point for a given location. */
  public static class Point {
    @SerializedName(value = "latitude_")
    private double latitude;

    @SerializedName(value = "longitude_")
    private double longitude;

    public double getLatitude() {
      return latitude;
    }

    public double getLongitude() {
      return longitude;
    }

    @VisibleForTesting
    public void setLatitude(double latitude) {
      this.latitude = latitude;
    }

    @VisibleForTesting
    public void setLongitude(double longitude) {
      this.longitude = longitude;
    }
  }
}
