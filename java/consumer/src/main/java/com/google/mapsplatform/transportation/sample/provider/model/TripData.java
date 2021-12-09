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
package com.google.mapsplatform.transportation.sample.provider.model;

import com.google.auto.value.AutoValue;
import com.google.mapsplatform.transportation.sample.provider.response.WaypointResponse;

import java.util.List;

/**
 * Relevant trip information from the response from sample provider along with
 * the consumer token to start journeysharing and display information on the app.
 */
@AutoValue
public abstract class TripData {

  public abstract String tripName();
  public abstract int tripStatus();

  /** List of waypoints for dropoff and pickup point. */
  public abstract List<WaypointResponse> waypoints();

  public abstract String vehicleId();
  public abstract String tripId();

  /** Consumer token to connect with FleetEngine. */
  public abstract String token();

  public static Builder newBuilder() {
    return new AutoValue_TripData.Builder();
  }

  /** Builder for TripData. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setTripName(String tripName);
    public abstract Builder setTripStatus(int tripStatus);

    public abstract Builder setWaypoints(List<WaypointResponse> waypoints);

    public abstract Builder setVehicleId(String vehicleId);
    public abstract Builder setTripId(String tripId);
    public abstract Builder setToken(String token);

    public abstract TripData build();
  }
}
