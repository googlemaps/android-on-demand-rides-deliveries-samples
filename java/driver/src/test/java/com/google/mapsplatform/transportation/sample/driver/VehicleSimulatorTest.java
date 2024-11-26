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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.android.libraries.navigation.SimulationOptions;
import com.google.android.libraries.navigation.Simulator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/** Unit tests for {@link VehicleSimulator}. */
@RunWith(JUnit4.class)
public final class VehicleSimulatorTest {
  @Rule public final MockitoRule mockito = MockitoJUnit.rule();

  @Mock private Simulator simulatorMock;
  @Mock private LocalSettings localSettingsMock;

  private static final float SPEED_MULTIPLIER = 5.0f;

  private VehicleSimulator vehicleSimulator;

  @Before
  public void setUp() {
    vehicleSimulator = new VehicleSimulator(simulatorMock, localSettingsMock);
  }

  @Test
  public void start_simulatesWhenEnabled() {
    when(localSettingsMock.getIsSimulationEnabled()).thenReturn(true);

    vehicleSimulator.start(SPEED_MULTIPLIER);

    verify(simulatorMock)
        .simulateLocationsAlongExistingRoute(
            refEq(new SimulationOptions().speedMultiplier(SPEED_MULTIPLIER)));
  }

  @Test
  public void start_doesNotSimulateWhenDisabled() {
    when(localSettingsMock.getIsSimulationEnabled()).thenReturn(false);

    vehicleSimulator.start(SPEED_MULTIPLIER);

    verify(simulatorMock, never()).simulateLocationsAlongExistingRoute(any());
  }
}
