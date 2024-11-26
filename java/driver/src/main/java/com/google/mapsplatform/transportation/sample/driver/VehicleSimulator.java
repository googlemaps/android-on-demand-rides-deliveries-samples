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
package com.google.mapsplatform.transportation.sample.driver;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.navigation.Navigator;
import com.google.android.libraries.navigation.SimulationOptions;
import com.google.android.libraries.navigation.Simulator;
import com.google.mapsplatform.transportation.sample.driver.provider.response.Waypoint;

/**
 * Commands to simulate the vehicle driving along a route. It interacts with the {@link Navigator}
 * to make the vehicle move along its route to a specified location.
 */
class VehicleSimulator {

  private final Simulator simulator;
  private final LocalSettings localSettings;

  /**
   * Default constructor for {@link VehicleSimulator}.
   *
   * @param simulator the actual simulator object obtained from {@link Navigator} that will update
   *     locations.
   * @param localSettings settings that are aware of the simulation enabling.
   */
  VehicleSimulator(Simulator simulator, LocalSettings localSettings) {
    this.simulator = simulator;
    this.localSettings = localSettings;
  }

  /** Sets the user location to be used for simulation. */
  void setLocation(Waypoint.Point location) {
    if (location != null) {
      simulator.setUserLocation(new LatLng(location.getLatitude(), location.getLongitude()));
    }
  }

  /**
   * Starts a simulation to the location defined by {@link #setLocation(Waypoint.Point)} along a
   * route calculated by the {@link Navigator}.
   */
  void start(float speedMultiplier) {
    if (localSettings.getIsSimulationEnabled()) {
      simulator.simulateLocationsAlongExistingRoute(
          new SimulationOptions().speedMultiplier(speedMultiplier));
    }
  }

  /** Pauses a simulation that can later be resumed. */
  void pause() {
    simulator.pause();
  }

  /** Resets the position of the {@link Navigator} and pauses navigation. */
  void unsetLocation() {
    simulator.unsetUserLocation();
  }
}
