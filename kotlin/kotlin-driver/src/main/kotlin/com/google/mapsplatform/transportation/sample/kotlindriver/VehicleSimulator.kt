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
package com.google.mapsplatform.transportation.sample.kotlindriver

import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.SimulationOptions
import com.google.android.libraries.navigation.Simulator
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.response.Waypoint

/**
 * Commands to simulate the vehicle driving along a route. It interacts with the [Navigator] to make
 * the vehicle move along its route to a specified location.
 *
 * @property simulator the actual simulator object obtained from [Navigator] that will update
 * locations.
 * @propery localSettings settings that are aware of the simulation enabling.
 */
internal class VehicleSimulator(
  private val simulator: Simulator,
  private val localSettings: LocalSettings,
) {
  /** Sets the user location to be used for simulation. */
  fun setLocation(location: Waypoint.Point): Unit =
    simulator.setUserLocation(LatLng(location.latitude, location.longitude))

  /**
   * Starts a simulation to the location defined by [.setLocation] along a route calculated by the
   * [Navigator].
   */
  fun start(speedMultiplier: Float) {
    if (localSettings.getIsSimulationEnabled()) {
      simulator.simulateLocationsAlongExistingRoute(
        SimulationOptions().speedMultiplier(speedMultiplier)
      )
    }
  }

  /** Pauses a simulation that can later be resumed. */
  fun pause() {
    simulator.pause()
  }

  /** Resets the position of the [Navigator] and pauses navigation. */
  fun unsetLocation() {
    simulator.unsetUserLocation()
  }
}
