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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.libraries.navigation.SimulationOptions
import com.google.android.libraries.navigation.Simulator
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.refEq
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for [VehicleSimulator]. */
@RunWith(AndroidJUnit4::class)
class VehicleSimulatorTest {
  private val mockedNavigator = mock<Simulator>()
  private val mockedLocalSettings = mock<LocalSettings>()

  private val vehicleSimulator = VehicleSimulator(mockedNavigator, mockedLocalSettings)

  @Test
  fun start_simulatesWhenEnabled() {
    whenever(mockedLocalSettings.getIsSimulationEnabled()).thenReturn(true)

    vehicleSimulator.start(SPEED_MULTIPLIER)

    verify(mockedNavigator, times(1))
      .simulateLocationsAlongExistingRoute(
        refEq(SimulationOptions().speedMultiplier(SPEED_MULTIPLIER))
      )
  }

  @Test
  fun start_doesNotSimulateWhenDisabled() {
    whenever(mockedLocalSettings.getIsSimulationEnabled()).thenReturn(false)

    vehicleSimulator.start(SPEED_MULTIPLIER)

    verify(mockedNavigator, never()).simulateLocationsAlongExistingRoute(any())
  }

  private companion object {
    const val SPEED_MULTIPLIER = 5.0f
  }
}
