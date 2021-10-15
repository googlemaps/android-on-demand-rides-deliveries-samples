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
// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.mapsplatform.transportation.sample.driver;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.navigation.Navigator;
import com.google.android.libraries.navigation.SimulationOptions;
import com.google.android.libraries.navigation.Simulator;
import com.google.mapsplatform.transportation.sample.driver.config.DriverTripConfig;

/**
 * Commands to simulate the vehicle driving along a route. It interacts with the {@link Navigator}
 * to make the vehicle move along its route to a specified location.
 */
class VehicleSimulator {

  private final Simulator simulator;

  VehicleSimulator(Simulator simulator) {
    this.simulator = simulator;
  }

  /** Sets the user location to be used for simulation. */
  void setLocation(DriverTripConfig.Point location) {
    if (location != null) {
      simulator.setUserLocation(new LatLng(location.getLatitude(), location.getLongitude()));
    }
  }

  /**
   * Starts a simulation to the location defined by {@link #setLocation(DriverTripConfig.Point)}
   * along a route calculated by the Navigator.
   */
  void start(float speedMultiplier) {
    simulator.simulateLocationsAlongExistingRoute(
        new SimulationOptions().speedMultiplier(speedMultiplier));
  }

  /** Pauses a simulation that can later be resumed. */
  void pause() {
    simulator.pause();
  }

  /** Stops the current simulation. */
  void unsetLocation() {
    simulator.unsetUserLocation();
  }
}
