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

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import androidx.core.content.ContextCompat;
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
import com.google.mapsplatform.transportation.sample.driver.provider.service.VehicleStateService;
import com.google.mapsplatform.transportation.sample.driver.state.TripState;
import com.google.mapsplatform.transportation.sample.driver.state.TripStatus;
import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/** Controls vehicle related functionalities. */
public class VehicleController implements VehicleStateService.VehicleStateListener {

  // Controls the relative speed of the simulator.
  private static final float SIMULATOR_SPEED_MULTIPLIER = 5.0f;

  // Faster location update interval during journey sharing.
  private static final long JOURNEY_SHARING_LOCATION_UPDATE_INTERVAL_SECONDS = 1;

  // Pattern of a FleetEngine full qualified a vehicle name.
  private static final Pattern VEHICLE_NAME_FORMAT =
      Pattern.compile("providers/(.*)/vehicles/(.*)");

  public static final String NO_TRIP_ID = "";

  // Index of the vehicle ID on a group matcher.
  private static final int VEHICLE_ID_INDEX = 2;

  private static final int ON_TRIP_FINISHED_DELAY_SECONDS = 5;

  private static final String TAG = "VehicleController";

  // Location update interval when the vehicle is waiting for a trip match.
  private static final long DEFAULT_LOCATION_UPDATE_INTERVAL_SECONDS = 10;

  private final Navigator navigator;
  private final LocalProviderService providerService;
  private final TripAuthTokenFactory authTokenFactory;

  private final ExecutorService executor;
  private final Executor mainExecutor;
  private final ScheduledExecutorService scheduledExecutor =
      Executors.newSingleThreadScheduledExecutor();
  private final Executor sequentialExecutor;
  private final VehicleSimulator vehicleSimulator;
  private final VehicleIdStore vehicleIdStore;

  private VehicleStateService vehicleStateService;

  private WeakReference<Presenter> presenterRef = new WeakReference<>(null);

  private @MonotonicNonNull RidesharingVehicleReporter vehicleReporter;
  private @Nullable TripState tripState;

  public VehicleController(
      Application application,
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
            LocalProviderService.createRestProvider(ProviderUtils.getProviderBaseUrl(application)),
            this.executor,
            roadSnappedLocationProvider);

    authTokenFactory = new TripAuthTokenFactory(application, executor, roadSnappedLocationProvider);

    vehicleIdStore = new VehicleIdStore(context);
  }

  @Override
  public void onVehicleStateUpdate(VehicleResponse updatedVehicle) {
    // No trip loaded but there's a paired trip.
    if (tripState == null && updatedVehicle.getCurrentTripsIds().size() > 0) {
      String tripId = updatedVehicle.getCurrentTripsIds().get(0);

      fetchTrip(tripId);
      return;
    }

    // There's an ongoing trip and a second trip has been matched (B2B).
    if (tripState != null && updatedVehicle.getCurrentTripsIds().size() == 2) {
      String backToBackTripId = updatedVehicle.getCurrentTripsIds().get(1);
      tripState.setBackToBackTripId(backToBackTripId);

      Presenter presenter = presenterRef.get();

      if (presenter != null) {
        mainExecutor.execute(() -> presenter.showNextTripId(backToBackTripId));
      }
    }
  }

  /**
   * Initialize vehicle and associated {@link RidesharingVehicleReporter}.
   *
   * @param application Application instance.
   * @return a signal that the async initialization is done, the boolean value can be ignored.
   */
  public ListenableFuture<Boolean> initVehicleAndReporter(Application application) {
    // If there is a previous instance RidesharingDriverApi (we're updating vehicleId), we clear it
    // to get a fresh one for the new vehicle.
    if (RidesharingDriverApi.getInstance() != null) {
      RidesharingDriverApi.clearInstance();
    }

    ListenableFuture<VehicleResponse> registerVehicleFuture = initVehicle();
    ListenableFuture<Boolean> future =
        Futures.transform(
            registerVehicleFuture,
            vehicleResponse -> {
              vehicleReporter =
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
                      .getRidesharingVehicleReporter();
              return true;
            },
            executor);
    Futures.addCallback(
        future,
        new FutureCallback<Boolean>() {
          @Override
          public void onSuccess(Boolean result) {
            setVehicleOnline();
            startVehiclePeriodicUpdate();
          }

          @Override
          public void onFailure(Throwable t) {
            Log.e(TAG, "initVehicleAndReporter() failed.", t);
          }
        },
        executor);
    return future;
  }

  private void fetchTrip(String tripId) {
    ListenableFuture<DriverTripConfig> tripConfigFuture =
        providerService.fetchTrip(tripId, vehicleIdStore.readOrDefault());

    Futures.addCallback(
        tripConfigFuture,
        new FutureCallback<DriverTripConfig>() {
          @Override
          public void onSuccess(DriverTripConfig tripConfig) {
            sequentialExecutor.execute(
                () -> {
                  tripState = new TripState(tripConfig);
                  tripState.setTripMatchedState();
                  updateServerAndUiTripState();
                });

            Presenter presenter = presenterRef.get();
            if (presenter != null) {
              mainExecutor.execute(
                  () -> {
                    presenter.showTripId(tripConfig.getTripId());
                    presenter.showTripStatus(TripStatus.NEW);
                  });
            }
          }

          @Override
          public void onFailure(Throwable t) {}
        },
        executor);
  }

  /** Cleans up active resources prior to activity onDestroy, mainly to prevent memory leaks. */
  public void cleanUp() {
    stopVehiclePeriodicUpdate();
  }

  private void startVehiclePeriodicUpdate() {
    if (vehicleStateService != null) {
      vehicleStateService.stopAsync();
    }

    vehicleStateService =
        new VehicleStateService(providerService, vehicleIdStore.readOrDefault(), this);

    vehicleStateService.startAsync();
  }

  private void stopVehiclePeriodicUpdate() {
    if (vehicleStateService != null) {
      vehicleStateService.stopAsync();
    }
  }

  /** Initialize a vehicle. */
  private ListenableFuture<VehicleResponse> initVehicle() {
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

  private void setVehicleOnline() {
    vehicleReporter.setLocationReportingInterval(DEFAULT_LOCATION_UPDATE_INTERVAL_SECONDS, SECONDS);

    // enableLocationTracking() must be called before setting state to Online.
    vehicleReporter.enableLocationTracking();
    vehicleReporter.setVehicleState(VehicleState.ONLINE);
  }

  /** Updates {@link TripStatus} to the controller. */
  public void processNextState() {
    // Use Guava's SequentialExecutor to change the internal state because it is
    // accessed in multiple threads.
    sequentialExecutor.execute(
        () -> {
          if (tripState == null) {
            return;
          }

          tripState.processNextState();
          updateServerAndUiTripState();
        });
  }

  /**
   * Takes current trip 'Status' and updates it in the server. Depending on the status, it might
   * also update 'intermediateDestinationIndex' (multi-destination support). Then, it updates the UI
   * (via the Presenter) to reflect the update.
   */
  private void updateServerAndUiTripState() {
    TripStatus updatedStatus =
        tripState != null ? tripState.getStatus() : TripStatus.UNKNOWN_TRIP_STATUS;

    if (updatedStatus != TripStatus.UNKNOWN_TRIP_STATUS) {
      if (updatedStatus == TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION) {
        providerService.updateTripStatusWithIntermediateDestinationIndex(
            tripState.getTripId(), updatedStatus.toString(), tripState.getWaypointIndex() - 1);
      } else {
        providerService.updateTripStatus(tripState.getTripId(), updatedStatus.toString());
      }
    }

    mainExecutor.execute(
        () -> {
          Presenter presenter = presenterRef.get();
          if (presenter == null) {
            return;
          }
          presenter.showTripStatus(updatedStatus);
        });

    updateVehicleByTripStatus(updatedStatus);
  }

  /** Sets presenter to the controller so it can invokes UI related callbacks. */
  public void setPresenter(Presenter presenter) {
    presenterRef = new WeakReference<>(presenter);
  }

  /**
   * Updates Vehicle state (navigator waypoint, simulator, journey sharing) based on the given trip
   * status.
   */
  private void updateVehicleByTripStatus(TripStatus status) {
    TripState currentTripState = requireNonNull(tripState);

    switch (status) {
      case UNKNOWN_TRIP_STATUS:
        vehicleSimulator.pause();
        break;

      case NEW:
        setNextWaypointOnNavigator();
        vehicleSimulator.setLocation(currentTripState.getInitialVehicleLocation());
        break;

      case ENROUTE_TO_PICKUP:
      case ENROUTE_TO_DROPOFF:
      case ENROUTE_TO_INTERMEDIATE_DESTINATION:
        startJourneySharing();
        vehicleSimulator.start(SIMULATOR_SPEED_MULTIPLIER);
        break;

      case ARRIVED_AT_PICKUP:
      case ARRIVED_AT_INTERMEDIATE_DESTINATION:
        stopJourneySharing();
        setNextWaypointOnNavigator();
        break;

      case COMPLETE:
      case CANCELED:
        stopJourneySharing();
        vehicleSimulator.unsetLocation();
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

  /**
   * Given the current waypoint, it determines what the next one will be and sets the navigator
   * destination to it.
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  private void setNextWaypointOnNavigator() {
    TripState currentTripState = requireNonNull(tripState);

    int waypointIndex = currentTripState.getWaypointIndex();

    DriverTripConfig.Waypoint waypoint = tripState.getWaypoints()[waypointIndex + 1];

    if (waypoint == null) {
      return;
    }

    Waypoint destinationWaypoint =
        Waypoint.builder()
            .setLatLng(
                waypoint.getLocation().getPoint().getLatitude(),
                waypoint.getLocation().getPoint().getLongitude())
            .setTitle(waypoint.getWaypointType())
            .build();

    navigator.setDestinations(Lists.newArrayList(destinationWaypoint), tripState.getRouteToken());
  }

  // Returns true if current trip has intermediate destinations (multi-destination support).
  public boolean hasIntermediateDestinations() {
    return requireNonNull(tripState).hasIntermediateDestinations();
  }

  // Returns true if driver is on the last intermediate destination (and hence, dropoff is next).
  public boolean isOnLastIntermediateDestination() {
    return requireNonNull(tripState).isOnLastIntermediateDestination();
  }

  private void onTripFinishedWithBackToBack() {
    // Trip has already been reset.
    if (tripState == null) {
      return;
    }

    String backToBackTripId = tripState.getBackToBackTripId();
    showTripIds(backToBackTripId, NO_TRIP_ID);

    tripState = null;

    fetchTrip(backToBackTripId);
  }

  private void showTripIds(String currentTripId, String nextTripId) {
    Presenter presenter = presenterRef.get();
    if (presenter == null) {
      return;
    }

    mainExecutor.execute(
        () -> {
          presenter.showTripId(currentTripId);
          presenter.showNextTripId(nextTripId);
        });
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void onTripFinishedOrCancelled() {
    // Trip has already been reset.
    if (tripState == null) {
      return;
    }

    if (!tripState.getBackToBackTripId().isEmpty()) {
      onTripFinishedWithBackToBack();
      return;
    }

    showTripIds("No trip assigned", NO_TRIP_ID);

    scheduledExecutor.schedule(
        () -> {
          tripState = null;
          updateServerAndUiTripState();
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
}
