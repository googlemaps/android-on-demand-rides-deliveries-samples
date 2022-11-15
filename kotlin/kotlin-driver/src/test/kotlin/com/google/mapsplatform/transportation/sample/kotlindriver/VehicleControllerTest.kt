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

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.libraries.mapsplatform.transportation.driver.api.ridesharing.RidesharingDriverApi
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.RoadSnappedLocationProvider
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.request.VehicleSettings
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.response.VehicleModel
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.response.Waypoint
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.service.LocalProviderService
import com.google.mapsplatform.transportation.sample.kotlindriver.state.TripStatus
import com.google.mapsplatform.transportation.sample.kotlindriver.utils.TripUtils
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class VehicleControllerTest {
  private lateinit var vehicleController: VehicleController

  @Mock private val mockNavigator: Navigator = mock()
  @Mock private val driverApi: RidesharingDriverApi = mock()
  @Mock private val mockedPresenter: Presenter = mock()
  @Mock private val roadSnappedLocationProvider: RoadSnappedLocationProvider = mock()
  private val application = ApplicationProvider.getApplicationContext<Context>() as Application
  private val mockedDriverApi = mockStatic(RidesharingDriverApi::class.java)
  private val mockedNavigationApi = mockStatic(NavigationApi::class.java)
  private val testCoroutineDispatcher = UnconfinedTestDispatcher()
  private val testCoroutineScope = TestScope(testCoroutineDispatcher)

  private val mockedLocalSettings =
    mock<LocalSettings> { on { getVehicleId() } doReturn VEHICLE_ID }

  private val providerServiceMock =
    mock<LocalProviderService> {
      onBlocking { registerVehicle(VEHICLE_ID) } doReturn VEHICLE_MODEL
      onBlocking { createOrUpdateVehicle(VEHICLE_SETTINGS) } doReturn VEHICLE_MODEL
      onBlocking { getVehicleModelFlow(VEHICLE_ID) } doReturn flowOf(VEHICLE_MODEL)
    }

  @Before
  fun setUp() {
    whenever(mockNavigator.getSimulator()).thenReturn(mock())
    whenever(driverApi.ridesharingVehicleReporter).thenReturn(mock())

    mockedDriverApi
      .`when`<RidesharingDriverApi>({ RidesharingDriverApi.createInstance(any()) })
      .thenReturn(driverApi)

    mockedNavigationApi
      .`when`<RoadSnappedLocationProvider>({ NavigationApi.getRoadSnappedLocationProvider(any()) })
      .thenReturn(roadSnappedLocationProvider)

    vehicleController =
      VehicleController(
        mockNavigator,
        MoreExecutors.newDirectExecutorService(),
        application as Context,
        testCoroutineScope,
        providerServiceMock,
        mockedLocalSettings,
      )

    vehicleController.setPresenter(mockedPresenter)
  }

  @After
  fun tearDown() {
    mockedDriverApi.close()
    mockedNavigationApi.close()
  }

  private fun initializeApi() {
    vehicleController.initializeApi(
      application,
      VEHICLE_MODEL,
    )
  }

  @Test
  fun initializeApi_initializesVehicleSettings() {
    initializeApi()

    val vehicleSettings = vehicleController.vehicleSettings

    assertThat(vehicleSettings.vehicleId).isEqualTo(VEHICLE_ID)
    assertThat(vehicleSettings.backToBackEnabled).isEqualTo(VEHICLE_MODEL.backToBackEnabled)
    assertThat(vehicleSettings.maximumCapacity).isEqualTo(VEHICLE_MODEL.maximumCapacity)
  }

  @Test
  fun initializeApi_startsVehicleModelFlow() {
    initializeApi()

    verify(providerServiceMock, times(1)).getVehicleModelFlow(VEHICLE_ID)
  }

  @Test
  fun initializeApi_updatesUiForVehicleModelWaypoints() {
    val currentTripIds = listOf(TRIP_ID)

    whenever(providerServiceMock.getVehicleModelFlow(VEHICLE_ID))
      .thenReturn(
        flowOf(
          VehicleModel(
            name = VEHICLE_MODEL.name,
            supportedTripTypes = listOf(TripUtils.SHARED_TRIP_TYPE),
            backToBackEnabled = false,
            maximumCapacity = 6,
            currentTripsIds = currentTripIds,
            waypoints =
              listOf(
                Waypoint(
                  tripId = TRIP_ID,
                  waypointType = TripUtils.DROP_OFF_WAYPOINT_TYPE,
                )
              )
          )
        )
      )

    initializeApi()

    Robolectric.flushForegroundThreadScheduler()

    verify(mockedPresenter, times(1)).showTripId(TRIP_ID)
    verify(mockedPresenter, times(1)).showTripStatus(TripStatus.NEW)
    verify(mockedPresenter, times(1)).showMatchedTripIds(currentTripIds)
  }

  @Test
  fun initializeApi_updatesUiForVehicleModelNoWaypoints() {
    val currentTripIds = emptyList<String>()
    whenever(providerServiceMock.getVehicleModelFlow(VEHICLE_ID))
      .thenReturn(
        flowOf(
          VehicleModel(
            name = VEHICLE_MODEL.name,
            supportedTripTypes = listOf(TripUtils.SHARED_TRIP_TYPE),
            backToBackEnabled = false,
            maximumCapacity = 6,
            currentTripsIds = currentTripIds,
            waypoints = emptyList()
          )
        )
      )

    initializeApi()

    Robolectric.flushForegroundThreadScheduler()

    verify(mockedPresenter, times(1)).showTripId(VehicleController.NO_TRIP_ID)
    verify(mockedPresenter, times(1)).showTripStatus(TripStatus.UNKNOWN_TRIP_STATUS)
    verify(mockedPresenter, times(1)).showMatchedTripIds(currentTripIds)
  }

  @Test
  fun initVehicleAndReporter_registersStoredVehicleId() {
    runBlocking {
      vehicleController.initVehicleAndReporter(application)

      verify(providerServiceMock, times(1)).registerVehicle(VEHICLE_ID)
    }
  }

  @Test
  fun initVehicleAndReporter_updatesVehicleSettings() {
    runBlocking { vehicleController.initVehicleAndReporter(application) }

    val vehicleSettings = vehicleController.vehicleSettings
    assertThat(vehicleSettings.vehicleId).isEqualTo(VEHICLE_ID)
    assertThat(vehicleSettings.backToBackEnabled).isEqualTo(VEHICLE_MODEL.backToBackEnabled)
    assertThat(vehicleSettings.maximumCapacity).isEqualTo(VEHICLE_MODEL.maximumCapacity)
  }

  @Test
  fun updateVehicleSettings_storesVehicleId() {
    runBlocking {
      vehicleController.updateVehicleSettings(application, VehicleSettings(vehicleId = VEHICLE_ID))
    }

    verify(mockedLocalSettings, times(1)).saveVehicleId(VEHICLE_ID)
  }

  @Test
  fun updateVehicleSettings_updatesVehicleSettings() {
    runBlocking {
      vehicleController.updateVehicleSettings(application, VehicleSettings(vehicleId = VEHICLE_ID))
    }

    val vehicleSettings = vehicleController.vehicleSettings
    assertThat(vehicleSettings.vehicleId).isEqualTo(VEHICLE_ID)
    assertThat(vehicleSettings.backToBackEnabled).isEqualTo(VEHICLE_MODEL.backToBackEnabled)
    assertThat(vehicleSettings.maximumCapacity).isEqualTo(VEHICLE_MODEL.maximumCapacity)
  }

  private companion object {
    const val VEHICLE_ID = "test-vehicle"
    const val TRIP_ID = "testTrip"

    val VEHICLE_MODEL =
      VehicleModel(
        name = "providers/test/vehicles/$VEHICLE_ID",
        supportedTripTypes = listOf(TripUtils.EXCLUSIVE_TRIP_TYPE),
        backToBackEnabled = true,
        maximumCapacity = 1,
      )

    val VEHICLE_SETTINGS = VehicleSettings(vehicleId = VEHICLE_ID)
  }
}
