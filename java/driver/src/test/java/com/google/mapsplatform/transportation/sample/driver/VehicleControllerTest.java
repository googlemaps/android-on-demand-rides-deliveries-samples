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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import com.google.android.libraries.navigation.Navigator;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.mapsplatform.transportation.sample.driver.provider.response.VehicleModel;
import com.google.mapsplatform.transportation.sample.driver.provider.response.Waypoint;
import com.google.mapsplatform.transportation.sample.driver.provider.service.LocalProviderService;
import com.google.mapsplatform.transportation.sample.driver.state.TripStatus;
import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/** Unit tests for {@link VehicleController}. */
@RunWith(JUnit4.class)
public final class VehicleControllerTest {
  @Rule public final MockitoRule mockito = MockitoJUnit.rule();

  @Mock private Context contextMock;
  @Mock private ExecutorService executorServiceMock;
  @Mock private Navigator navigatorMock;
  @Mock private LocalProviderService localproviderServiceMock;
  @Mock private LocalSettings localSettingsMock;
  @Mock private Presenter presenterMock;

  private static final String TRIP_ID = "mockTripId";
  private static final String VEHICLE_NAME = "mockVehicleName";
  private final ImmutableList<String> currentTripIds = ImmutableList.of(TRIP_ID);
  private VehicleModel vehicleModel;

  private VehicleController vehicleController;

  @Before
  public void setUp() {
    vehicleController =
        new VehicleController(
            navigatorMock,
            executorServiceMock,
            contextMock,
            localproviderServiceMock,
            localSettingsMock);

    vehicleController.setPresenter(presenterMock);

    vehicleModel = new VehicleModel();
    vehicleModel.setName(VEHICLE_NAME);
    vehicleModel.setCurrentTripsIds(currentTripIds);

    when(localproviderServiceMock.registerVehicle(TRIP_ID))
        .thenReturn(Futures.immediateFuture(vehicleModel));
  }

  @Test
  public void onVehicleStateUpdate_updatesUiForVehicleModelWaypoints() {
    Waypoint tripWaypoint = new Waypoint();
    tripWaypoint.setTripId(TRIP_ID);

    vehicleModel.setWaypoints(ImmutableList.of(tripWaypoint));

    vehicleController.onVehicleStateUpdate(vehicleModel);

    verify(presenterMock).showTripId(TRIP_ID);
    verify(presenterMock).showTripStatus(TripStatus.NEW);
    verify(presenterMock).showMatchedTripIds(currentTripIds);
  }

  @Test
  public void onVehicleStateUpdate_updatesUiForVehicleModelNoWaypoints() {
    vehicleModel.setWaypoints(ImmutableList.of());

    vehicleController.onVehicleStateUpdate(vehicleModel);

    verify(presenterMock).showTripId(VehicleController.NO_TRIP_ID);
    verify(presenterMock).showTripStatus(TripStatus.UNKNOWN_TRIP_STATUS);
    verify(presenterMock).showMatchedTripIds(currentTripIds);
  }
}
