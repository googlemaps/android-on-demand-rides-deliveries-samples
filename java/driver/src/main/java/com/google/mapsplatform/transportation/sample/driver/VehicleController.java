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

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import androidx.core.content.ContextCompat;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.AuthTokenContext;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.AuthTokenContext.AuthTokenFactory;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.DriverContext;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.DriverContext.StatusListener.StatusCode;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.DriverContext.StatusListener.StatusLevel;
import com.google.android.libraries.mapsplatform.transportation.driver.api.ridesharing.RidesharingDriverApi;
import com.google.android.libraries.mapsplatform.transportation.driver.api.ridesharing.vehiclereporter.RidesharingVehicleReporter;
import com.google.android.libraries.mapsplatform.transportation.driver.api.ridesharing.vehiclereporter.RidesharingVehicleReporter.VehicleState;
import com.google.android.libraries.navigation.NavigationApi;
import com.google.android.libraries.navigation.Navigator;
import com.google.android.libraries.navigation.RoadSnappedLocationProvider;
import com.google.android.libraries.navigation.Waypoint;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.mapsplatform.transportation.sample.driver.config.DriverTripConfig;
import com.google.mapsplatform.transportation.sample.driver.provider.ProviderUtils;
import com.google.mapsplatform.transportation.sample.driver.provider.response.VehicleResponse;
import com.google.mapsplatform.transportation.sample.driver.provider.service.LocalProviderService;
import com.google.mapsplatform.transportation.sample.driver.state.TripStatus;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/** Controls vehicle related functionalities. */
public class VehicleController {

  // Controls the relative speed of the simulator.
  private static final float SIMULATOR_SPEED_MULTIPLIER = 5.0f;

  // Faster location update interval during journey sharing.
  private static final long JOURNEY_SHARING_LOCATION_UPDATE_INTERVAL_SECONDS = 1;

  // Pattern of a FleetEngine full qualified a vehicle name.
  private static final Pattern VEHICLE_NAME_FORMAT =
      Pattern.compile("providers/(.*)/vehicles/(.*)");

  // Index of the vehicle ID on a group matcher.
  private static final int VEHICLE_ID_INDEX = 2;

  private static final int ON_TRIP_FINISHED_DELAY_SECONDS = 5;

  private static final String PICKUP_WAYPOINT = "PICKUP_WAYPOINT_TYPE";
  private static final String DROP_OFF_WAYPOINT = "DROP_OFF_WAYPOINT_TYPE";
  private static final String TAG = "VehicleController";

  // Location update interval when the vehicle is waiting for a trip match.
  private static final long DEFAULT_LOCATION_UPDATE_INTERVAL_SECONDS = 10;

  private final Navigator navigator;
  private final LocalProviderService providerService;
  private final TripAuthTokenFactory authTokenFactory = new TripAuthTokenFactory();

  private final ExecutorService executor;
  private final Executor mainExecutor;
  private final ScheduledExecutorService scheduledExecutor =
      Executors.newSingleThreadScheduledExecutor();
  private final Executor sequentialExecutor;
  private final AtomicBoolean isVehicleOnline = new AtomicBoolean(false);
  private final VehicleSimulator vehicleSimulator;
  private final VehicleIdStore vehicleIdStore;

  private WeakReference<Presenter> presenterRef = new WeakReference<>(null);
  private TripStatus tripStatus = TripStatus.UNKNOWN_TRIP_STATUS;
  private @MonotonicNonNull RidesharingVehicleReporter vehicleReporter;
  private @MonotonicNonNull ListenableFuture<VehicleResponse> registerVehicleFuture;
  @Nullable private DriverTripConfig tripConfig;

  public VehicleController(
      Navigator navigator,
      RoadSnappedLocationProvider roadSnappedLocationProvider,
      ExecutorService executor,
      Context context) {
    this.navigator = navigator;
    vehicleSimulator = new VehicleSimulator(navigator.getSimulator());
    this.executor = executor;
    sequentialExecutor = MoreExecutors.newSequentialExecutor(executor);
    mainExecutor = ContextCompat.getMainExecutor(context);
    providerService =
        new LocalProviderService(
            LocalProviderService.createRestProvider(ProviderUtils.getProviderBaseUrl(context)),
            this.executor,
            scheduledExecutor,
            roadSnappedLocationProvider);
    vehicleIdStore = new VehicleIdStore(context);
  }

  /**
   * Initialize vehicle and associated {@link RidesharingVehicleReporter}.
   *
   * @param application Application instance.
   * @return a signal that the async initialization is done, the boolean value can be ignored.
   */
  public ListenableFuture<Boolean> initVehicleAndReporter(Application application) {
    registerVehicleFuture = initVehicle();
    ListenableFuture<RidesharingVehicleReporter> vehicleReporterFuture =
        Futures.transform(
            registerVehicleFuture,
            vehicleResponse ->
                RidesharingDriverApi.createInstance(
                        DriverContext.builder(application)
                            .setNavigator(requireNonNull(navigator))
                            .setProviderId(ProviderUtils.getProviderId(application))
                            .setVehicleId(vehicleIdStore.readOrDefault())
                            .setAuthTokenFactory(authTokenFactory)
                            .setRoadSnappedLocationProvider(
                                NavigationApi.getRoadSnappedLocationProvider(application))
                            .setStatusListener(VehicleController::logLocationUpdate)
                            .build())
                    .getRidesharingVehicleReporter(),
            executor);
    Futures.addCallback(
        vehicleReporterFuture,
        new FutureCallback<RidesharingVehicleReporter>() {
          @Override
          public void onSuccess(RidesharingVehicleReporter vehicleReporter) {
            VehicleController.this.vehicleReporter = vehicleReporter;
          }

          @Override
          public void onFailure(Throwable t) {
            Log.e(TAG, "initVehicleAndReporter() failed.", t);
          }
        },
        executor);
    return Futures.transform(vehicleReporterFuture, vehicleReporter -> true, executor);
  }

  /** Initialize a vehicle. */
  public ListenableFuture<VehicleResponse> initVehicle() {
    ListenableFuture<VehicleResponse> registerVehicleFuture =
        providerService.registerVehicle(vehicleIdStore.readOrDefault());
    Futures.addCallback(
        registerVehicleFuture,
        new FutureCallback<VehicleResponse>() {
          @Override
          public void onSuccess(VehicleResponse vehicleResponse) {
            vehicleIdStore.save(extractVehicleId(vehicleResponse.getName()));
          }

          @Override
          public void onFailure(Throwable t) {}
        },
        executor);
    return registerVehicleFuture;
  }

  /** Poll available trip to be paired. */
  public void pollTrip() {
    ListenableFuture<DriverTripConfig> tripConfigFuture =
        Futures.transformAsync(
            registerVehicleFuture,
            vehicleResponse ->
                providerService.pollAvailableTrip(extractVehicleId(vehicleResponse.getName())),
            executor);
    Futures.addCallback(
        tripConfigFuture,
        new FutureCallback<DriverTripConfig>() {
          @Override
          public void onSuccess(DriverTripConfig tripConfig) {
            sequentialExecutor.execute(() -> VehicleController.this.tripConfig = tripConfig);
            Presenter presenter = presenterRef.get();
            if (presenter != null) {
              mainExecutor.execute(
                  () -> {
                    presenter.showTripId(tripConfig.getTripId());
                    presenter.showTripStatus(TripStatus.NEW);
                  });
            }
            setVehicleOnline();
            updateTripStatus(TripStatus.NEW);
          }

          @Override
          public void onFailure(Throwable t) {}
        },
        executor);
  }

  private void setVehicleOnline() {
    if (isVehicleOnline.get()) {
      return;
    }
    isVehicleOnline.set(true);
    vehicleReporter.setLocationReportingInterval(DEFAULT_LOCATION_UPDATE_INTERVAL_SECONDS, SECONDS);

    // enableLocationTracking() must be called before setting state to Online.
    vehicleReporter.enableLocationTracking();
    vehicleReporter.setVehicleState(VehicleState.ONLINE);
  }

  private void setVehicleOffline() {
    if (!isVehicleOnline.get()) {
      return;
    }
    isVehicleOnline.set(false);
    vehicleReporter.setVehicleState(VehicleState.OFFLINE);
    vehicleReporter.disableLocationTracking();
  }

  /** Updates {@link TripStatus} to the controller. */
  public void updateTripStatus(TripStatus status) {
    if (tripStatus == status) {
      return;
    }
    if (tripConfig == null) {
      // Recommend using Guava's SequentialExecutor to change internal state if it's will be
      // accessed in multiple threads.
      sequentialExecutor.execute(() -> tripStatus = TripStatus.UNKNOWN_TRIP_STATUS);
      return;
    }
    sequentialExecutor.execute(() -> tripStatus = status);

    if (status != TripStatus.UNKNOWN_TRIP_STATUS) {
      providerService.updateTripStatus(tripConfig.getTripId(), status.toString());
    }

    updateVehicleByTripStatus(status);
  }

  /** Sets presenter to the controller so it can invokes UI related callbacks. */
  public void setPresenter(Presenter presenter) {
    presenterRef = new WeakReference<>(presenter);
  }

  public TripStatus getTripStatus() {
    return tripStatus;
  }

  private void updateVehicleByTripStatus(TripStatus status) {
    DriverTripConfig driverTripConfig = requireNonNull(tripConfig);
    switch (status) {
      case UNKNOWN_TRIP_STATUS:
        vehicleSimulator.pause();
        break;
      case NEW:
        setNextWaypoint(PICKUP_WAYPOINT, false);
        vehicleSimulator.setLocation(driverTripConfig.getVehicleLocation());
        break;
      case ENROUTE_TO_PICKUP:
      case ENROUTE_TO_DROPOFF:
        startJourneySharing();
        vehicleSimulator.start(SIMULATOR_SPEED_MULTIPLIER);
        break;
      case ARRIVED_AT_PICKUP:
        stopJourneySharing();
        setNextWaypoint(DROP_OFF_WAYPOINT, true);
        break;
      case COMPLETE:
      case CANCELED:
        stopJourneySharing();
        vehicleSimulator.unsetLocation();
        setVehicleOffline();
        onTripFinishedOrCancelled();
        break;
    }
  }

  // Starts simulating journey sharing. Reduces the update interval.
  private void startJourneySharing() {
    requireNonNull(vehicleReporter)
        .setLocationReportingInterval(JOURNEY_SHARING_LOCATION_UPDATE_INTERVAL_SECONDS, SECONDS);
    navigator.startGuidance();
  }

  // Stops journey sharing by turning off guidance and reducing the location reporting frequency.
  private void stopJourneySharing() {
    requireNonNull(vehicleReporter)
        .setLocationReportingInterval(DEFAULT_LOCATION_UPDATE_INTERVAL_SECONDS, SECONDS);
    navigator.stopGuidance();
    navigator.clearDestinations();
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void setNextWaypoint(String waypointType, boolean isDropoff) {
    DriverTripConfig driverTripConfig = requireNonNull(tripConfig);
    DriverTripConfig.Waypoint waypoint = driverTripConfig.getWaypoint(waypointType);
    if (waypoint == null) {
      return;
    }
    Waypoint destinationWaypoint =
        Waypoint.builder()
            .setLatLng(
                waypoint.getLocation().getPoint().getLatitude(),
                waypoint.getLocation().getPoint().getLongitude())
            .setTitle(driverTripConfig.getProjectId() + waypointType)
            .build();

    if (isDropoff) {
      navigator.setDestinations(
          Lists.newArrayList(destinationWaypoint),
          driverTripConfig.getRouteToken());
      return;
    }
    navigator.setDestination(destinationWaypoint);
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void onTripFinishedOrCancelled() {
    Presenter presenter = presenterRef.get();
    if (presenter == null) {
      return;
    }
    mainExecutor.execute(() -> presenter.showTripId("No trip assigned"));

    scheduledExecutor.schedule(
        () -> {
          updateTripStatus(TripStatus.UNKNOWN_TRIP_STATUS);
          pollTrip();
        },
        ON_TRIP_FINISHED_DELAY_SECONDS,
        SECONDS);
  }

  private static void logLocationUpdate(
      StatusLevel statusLevel, StatusCode statusCode, String statusMsg) {
    Log.i("STATUS", statusLevel + " " + statusCode + ": " + statusMsg);
  }

  private static String extractVehicleId(String vehicleName) {
    Matcher matches = VEHICLE_NAME_FORMAT.matcher(vehicleName);
    if (matches.matches()) {
      return matches.group(VEHICLE_ID_INDEX);
    }
    return vehicleName;
  }

  private class TripAuthTokenFactory implements AuthTokenFactory {

    @Override
    public String getToken(AuthTokenContext context) {
      try {
        return requireNonNull(providerService.fetchAuthToken().get());
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
        Log.e(TAG, "getToken failed.", e);
      }
      return null;
    }
  }
}
