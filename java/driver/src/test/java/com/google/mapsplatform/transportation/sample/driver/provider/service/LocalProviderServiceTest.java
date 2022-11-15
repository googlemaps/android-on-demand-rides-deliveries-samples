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
package com.google.mapsplatform.transportation.sample.driver.provider.service;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.mapsplatform.transportation.sample.driver.provider.request.VehicleSettings;
import com.google.mapsplatform.transportation.sample.driver.provider.response.VehicleModel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import retrofit2.HttpException;
import retrofit2.Response;

@RunWith(JUnit4.class)
public final class LocalProviderServiceTest {
  @Rule public final MockitoRule mockito = MockitoJUnit.rule();
  @Mock private RestProvider restProviderMock;

  private static final String VEHICLE_ID = "testVehicle";
  private static final String VEHICLE_NAME = "providers/provider/vehicles/" + VEHICLE_ID;

  private static final ResponseBody RESPONSE_ERROR_MOCK =
      ResponseBody.create(MediaType.parse("text/plain"), "Test Server Error");

  private static final HttpException HTTP_NOT_FOUND_EXCEPTION_MOCK =
      new HttpException(Response.error(404, RESPONSE_ERROR_MOCK));

  private static final HttpException HTTP_INTERNAL_SERVER_EXCEPTION_MOCK =
      new HttpException(Response.error(500, RESPONSE_ERROR_MOCK));

  private static final VehicleSettings VEHICLE_SETTINGS =
      new VehicleSettings(
          /* vehicleId= */ VEHICLE_ID,
          /* backToBackEnabled= */ true,
          /* maximumCapacity= */ 5,
          /* supportedTripTypes= */ ImmutableList.of());

  private LocalProviderService localProviderService;

  @Before
  public void setUp() {
    localProviderService =
        new LocalProviderService(restProviderMock, Executors.newSingleThreadExecutor());
  }

  @Test
  public void registerVehicle_returnsVehicleWhenExisting() throws Exception {
    VehicleModel getVehicleMock = new VehicleModel();

    when(restProviderMock.getVehicle(eq(VEHICLE_ID))).thenReturn(immediateFuture(getVehicleMock));

    VehicleModel registeredVehicle = localProviderService.registerVehicle(VEHICLE_ID).get();

    assertThat(registeredVehicle).isEqualTo(getVehicleMock);
  }

  @Test
  public void registerVehicle_createsVehicleWhenNotExisting() throws Exception {
    when(restProviderMock.getVehicle(eq(VEHICLE_ID)))
        .thenReturn(immediateFailedFuture(HTTP_NOT_FOUND_EXCEPTION_MOCK));

    VehicleModel createVehicleMock = new VehicleModel();

    when(restProviderMock.createVehicle(any())).thenReturn(immediateFuture(createVehicleMock));

    VehicleModel registeredVehicle = localProviderService.registerVehicle(VEHICLE_ID).get();

    assertThat(registeredVehicle).isEqualTo(createVehicleMock);
  }

  @Test
  public void registerVehicle_surfacesExceptionWhenUnknown() throws Exception {
    when(restProviderMock.getVehicle(eq(VEHICLE_ID)))
        .thenReturn(immediateFailedFuture(HTTP_INTERNAL_SERVER_EXCEPTION_MOCK));

    ExecutionException thrown =
        assertThrows(
            ExecutionException.class, () -> localProviderService.registerVehicle(VEHICLE_ID).get());

    assertThat(thrown).hasCauseThat().isInstanceOf(HttpException.class);
    assertThat(thrown)
        .hasCauseThat()
        .hasMessageThat()
        .contains(HTTP_INTERNAL_SERVER_EXCEPTION_MOCK.getMessage());
  }

  @Test
  public void createOrUpdateVehicle_updatesVehicleWhenExisting() throws Exception {
    VehicleModel updatedVehicle = new VehicleModel();

    when(restProviderMock.updateVehicle(eq(VEHICLE_ID), eq(VEHICLE_SETTINGS)))
        .thenReturn(immediateFuture(updatedVehicle));

    assertThat(localProviderService.createOrUpdateVehicle(VEHICLE_SETTINGS).get())
        .isEqualTo(updatedVehicle);
  }

  @Test
  public void createOrUpdateVehicle_createsVehicleWhenNotExisting() throws Exception {
    when(restProviderMock.updateVehicle(eq(VEHICLE_ID), eq(VEHICLE_SETTINGS)))
        .thenReturn(immediateFailedFuture(HTTP_NOT_FOUND_EXCEPTION_MOCK));

    VehicleModel createdVehicle = new VehicleModel();

    when(restProviderMock.createVehicle(any())).thenReturn(immediateFuture(createdVehicle));

    assertThat(localProviderService.createOrUpdateVehicle(VEHICLE_SETTINGS).get())
        .isEqualTo(createdVehicle);
  }

  @Test
  public void createOrUpdateVehicle_surfacesExceptionWhenUnknown() throws Exception {
    when(restProviderMock.updateVehicle(eq(VEHICLE_ID), eq(VEHICLE_SETTINGS)))
        .thenReturn(immediateFailedFuture(HTTP_INTERNAL_SERVER_EXCEPTION_MOCK));

    ExecutionException thrown =
        assertThrows(
            ExecutionException.class,
            () -> localProviderService.createOrUpdateVehicle(VEHICLE_SETTINGS).get());

    assertThat(thrown).hasCauseThat().isInstanceOf(HttpException.class);
    assertThat(thrown)
        .hasCauseThat()
        .hasMessageThat()
        .contains(HTTP_INTERNAL_SERVER_EXCEPTION_MOCK.getMessage());
  }
}
