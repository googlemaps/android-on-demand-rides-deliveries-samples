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
package com.google.mapsplatform.transportation.sample.driver.provider.request;

import java.util.List;

/** Body object for vehicle settings. It is used for creating/updating vehicle. */
public class VehicleSettings {
  private String vehicleId;

  private Boolean backToBackEnabled;

  private int maximumCapacity;

  private List<String> supportedTripTypes;

  public VehicleSettings(
      String vehicleId,
      Boolean backToBackEnabled,
      int maximumCapacity,
      List<String> supportedTripTypes) {
    this.vehicleId = vehicleId;
    this.backToBackEnabled = backToBackEnabled;
    this.maximumCapacity = maximumCapacity;
    this.supportedTripTypes = supportedTripTypes;
  }

  public String getVehicleId() {
    return vehicleId;
  }

  public Boolean getBackToBackEnabled() {
    return backToBackEnabled;
  }

  public List<String> getSupportedTripTypes() {
    return supportedTripTypes;
  }

  public int getMaximumCapacity() {
    return maximumCapacity;
  }

  public void setVehicleId(String vehicleId) {
    this.vehicleId = vehicleId;
  }

  public void setBackToBackEnabled(Boolean backToBackEnabled) {
    this.backToBackEnabled = backToBackEnabled;
  }

  public void setSupportedTripTypes(List<String> supportedTripTypes) {
    this.supportedTripTypes = supportedTripTypes;
  }

  public void setMaximumCapacity(int maximumCapacity) {
    this.maximumCapacity = maximumCapacity;
  }
}
