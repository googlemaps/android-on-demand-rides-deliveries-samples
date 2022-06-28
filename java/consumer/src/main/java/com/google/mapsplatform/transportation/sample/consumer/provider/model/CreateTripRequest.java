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
package com.google.mapsplatform.transportation.sample.consumer.provider.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.common.collect.ImmutableList;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Java object for pickup/dropoff points for sample provider.
 *
 * <p>Sample JSON object: { pickup: { latitude: 37.423061, longitude: -122.084051 }, dropoff: {
 * latitude: 37.411895, longitude: -122.094247 }, intermediateDestinations: [{ latitude: 35.12345,
 * longitude: -120.12343 }], tripType: "EXCLUSIVE" }
 */
public class CreateTripRequest {

  @SerializedName("pickup")
  @Expose
  private LatLng pickup;

  @SerializedName("dropoff")
  @Expose
  private LatLng dropoff;

  @SerializedName("intermediateDestinations")
  @Expose
  private ImmutableList<LatLng> intermediateDestinations;

  @SerializedName("tripType")
  @Expose
  private String tripType;

  public LatLng getPickup() {
    return pickup;
  }

  public void setPickup(LatLng pickup) {
    this.pickup = pickup;
  }

  public LatLng getDropoff() {
    return dropoff;
  }

  public void setDropoff(LatLng dropoff) {
    this.dropoff = dropoff;
  }

  public ImmutableList<LatLng> getIntermediateDestinations() {
    return intermediateDestinations;
  }

  public void setIntermediateDestinations(ImmutableList<LatLng> intermediateDestinations) {
    this.intermediateDestinations = intermediateDestinations;
  }

  public void setTripType(String tripType) {
    this.tripType = tripType;
  }

  public String getTripType() {
    return tripType;
  }
}
