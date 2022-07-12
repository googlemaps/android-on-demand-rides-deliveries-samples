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
import com.google.android.libraries.navigation.ListenableResultFuture;
import com.google.android.libraries.navigation.NavigationApi;
import com.google.android.libraries.navigation.Navigator;
import com.google.android.libraries.navigation.RoadSnappedLocationProvider;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.mapsplatform.transportation.sample.driver.provider.ProviderUtils;
import com.google.mapsplatform.transportation.sample.driver.provider.request.VehicleSettings;
import com.google.mapsplatform.transportation.sample.driver.provider.response.TripModel;
import com.google.mapsplatform.transportation.sample.driver.provider.response.VehicleModel;
import com.google.mapsplatform.transportation.sample.driver.provider.response.Waypoint;
import com.google.mapsplatform.transportation.sample.driver.provider.service.LocalProviderService;
import com.google.mapsplatform.transportation.sample.driver.provider.service.VehicleStateService;
import com.google.mapsplatform.transportation.sample.driver.state.TripState;
import com.google.mapsplatform.transportation.sample.driver.state.TripStatus;
import com.google.mapsplatform.transportation.sample.driver.utils.TripUtils;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  private @MonotonicNonNull VehicleSettings vehicleSettings;

  private @Nullable Waypoint currentWaypoint;
  private @Nullable Waypoint nextWaypoint;
  private @Nullable Waypoint nextWaypointOfCurrentTrip;

  private List<Waypoint> waypoints = ImmutableList.of();
  private List<String> matchedTripIds = ImmutableList.of();

  private Map<String, TripState> tripStates = new HashMap<>();

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
  public void onVehicleStateUpdate(VehicleModel updatedVehicle) {
    waypoints = updatedVehicle.getWaypoints();
    matchedTripIds = updatedVehicle.getCurrentTripsIds();

    // Reset and recalculate waypoints every server state update.
    currentWaypoint = null;
    nextWaypoint = null;
    nextWaypointOfCurrentTrip = null;

    if (!waypoints.isEmpty()) {
      Waypoint updatedNextWaypointOfCurrentTrip = null;

      for (int index = 0; index < waypoints.size(); index++) {
        Waypoint waypoint = waypoints.get(index);

        Log.i(TAG, String.format("%s %s", waypoint.getTripId(), waypoint.getWaypointType()));

        if (!tripStates.containsKey(waypoint.getTripId())) {
          acceptTrip(waypoint);

          if (currentWaypoint == null) {
            updateUiForWaypoint(waypoint);
            setWaypointDestination(waypoint);
          }
        }

        /**
         * When 'Vehicle' state is updated from the 'Provider', the client needs to update the
         * current waypoint pointer so the UI can be refreshed accordingly. It also keeps track of
         * the second/next waypoint for convenience, this is utilized at the moment where the
         * current waypoint status changes to 'ARRIVED/COMPLETE'.
         */
        if (index == 0) {
          currentWaypoint = waypoint;
        } else if (index == 1) {
          nextWaypoint = waypoint;
        }

        if (index > 0
            && updatedNextWaypointOfCurrentTrip == null
            && currentWaypoint.getTripId().equals(waypoint.getTripId())) {
          updatedNextWaypointOfCurrentTrip = waypoint;
        }
      }

      nextWaypointOfCurrentTrip = updatedNextWaypointOfCurrentTrip;
    } else {
      stopJourneySharing();
    }

    updateUiForWaypoint(currentWaypoint);
    enableActionButton(true);
  }

  /**
   * Initialize vehicle and associated {@link RidesharingVehicleReporter}.
   *
   * @param application Application instance.
   * @return a signal that the async initialization is done, the boolean value can be ignored.
   */
  public ListenableFuture<Boolean> initVehicleAndReporter(Application application) {
    ListenableFuture<VehicleModel> registerVehicleFuture = initVehicle();
    return initializeApi(application, registerVehicleFuture);
  }

  /**
   * Updates the stored vehicle settings. In case vehicle is updated, a new reporter has to be
   * instantiated.
   *
   * @param application Application instance.
   * @param vehicleSettings the updated vehicle settings.
   * @return a signal that the async initialization is done, the boolean value can be ignored.
   */
  public ListenableFuture<Boolean> updateVehicleSettings(
      Application application, VehicleSettings vehicleSettings) {
    ListenableFuture<VehicleModel> updateVehicleFuture = updateVehicle(vehicleSettings);
    return initializeApi(application, updateVehicleFuture);
  }

  private ListenableFuture<Boolean> initializeApi(
      Application application, ListenableFuture<VehicleModel> vehicleFuture) {
    // If there is a previous instance RidesharingDriverApi (we're updating vehicleId), we clear it
    // to get a fresh one for the new vehicle.
    if (RidesharingDriverApi.getInstance() != null) {
      RidesharingDriverApi.clearInstance();
    }

    ListenableFuture<Boolean> future =
        Futures.transform(
            vehicleFuture,
            vehicleModel -> {
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

              vehicleSettings = new VehicleSettings();
              vehicleSettings.setVehicleId(extractVehicleId(vehicleModel.getName()));
              vehicleSettings.setMaximumCapacity(vehicleModel.getMaximumCapacity());
              vehicleSettings.setSupportedTripTypes(vehicleModel.getSupportedTripTypes());
              vehicleSettings.setBackToBackEnabled(vehicleModel.getBackToBackEnabled());

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
            Log.e(TAG, "initializeApi() failed.", t);
          }
        },
        executor);

    return future;
  }

  /** Returns the settings for the active 'Vehicle'. */
  public VehicleSettings getVehicleSettings() {
    return requireNonNull(vehicleSettings);
  }

  /** Creates/Updates a Vehicle with the given settings depending of the provider state. */
  private ListenableFuture<VehicleModel> updateVehicle(VehicleSettings vehicleSettings) {
    ListenableFuture<VehicleModel> vehicleModelFuture =
        providerService.createOrUpdateVehicle(vehicleSettings);

    Futures.addCallback(
        vehicleModelFuture,
        new FutureCallback<VehicleModel>() {
          @Override
          public void onSuccess(VehicleModel vehicleModel) {
            vehicleIdStore.save(extractVehicleId(vehicleModel.getName()));
          }

          @Override
          public void onFailure(Throwable t) {
            Log.e(TAG, "updateVehicle() failed.", t);
          }
        },
        executor);

    return vehicleModelFuture;
  }

  private void acceptTrip(Waypoint tripWaypoint) {
    updateTripStatusInServer(TripUtils.getInitialTripState(tripWaypoint.getTripId()));
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
  private ListenableFuture<VehicleModel> initVehicle() {
    ListenableFuture<VehicleModel> vehicleModelFuture =
        providerService.registerVehicle(vehicleIdStore.readOrDefault());

    Futures.addCallback(
        vehicleModelFuture,
        new FutureCallback<VehicleModel>() {
          @Override
          public void onSuccess(VehicleModel vehicleModel) {
            vehicleIdStore.save(extractVehicleId(vehicleModel.getName()));
          }

          @Override
          public void onFailure(Throwable t) {
            Log.e(TAG, "initVehicle() failed.", t);
          }
        },
        executor);

    return vehicleModelFuture;
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
          String tripId = currentWaypoint.getTripId();

          TripState previousTripState = tripStates.get(tripId);
          TripState updatedTripState =
              TripUtils.getNextTripState(previousTripState, nextWaypointOfCurrentTrip);

          TripStatus updatedTripStatus = updatedTripState.tripStatus();

          Futures.addCallback(
              updateTripStatusInServer(updatedTripState),
              new FutureCallback<TripModel>() {
                @Override
                public void onSuccess(TripModel tripModel) {
                  if (nextWaypoint != null && TripUtils.isTripStatusArrived(updatedTripStatus)) {
                    advanceNextWaypointOnArrival(nextWaypoint);
                  }
                }

                @Override
                public void onFailure(Throwable t) {}
              },
              executor);

          updateNavigationForWaypoints(updatedTripState);
          enableActionButton(false);

          Log.i(
              TAG,
              String.format(
                  "Previous trip status: %s Current trip status: %s",
                  previousTripState.tripStatus(), updatedTripStatus));
        });
  }

  private void updateNavigationForWaypoints(TripState updatedCurrentTripState) {
    switch (updatedCurrentTripState.tripStatus()) {
      case ENROUTE_TO_PICKUP:
        startJourneySharing();
        navigateToWaypoint(currentWaypoint);
        break;

      case ARRIVED_AT_PICKUP:
      case ARRIVED_AT_INTERMEDIATE_DESTINATION:
      case COMPLETE:
        if (nextWaypoint != null) {
          navigateToWaypoint(nextWaypoint);
        }

      default:
        break;
    }
  }

  private void advanceNextWaypointOnArrival(Waypoint nextWaypoint) {
    TripState updatedStateForNextWaypoint =
        TripUtils.getEnrouteStateForWaypoint(
            nextWaypoint, tripStates.get(nextWaypoint.getTripId()));

    updateTripStatusInServer(updatedStateForNextWaypoint);
  }

  private void enableActionButton(boolean enabled) {
    Presenter presenter = presenterRef.get();

    if (presenter != null) {
      mainExecutor.execute(() -> presenter.enableActionButton(enabled));
    }
  }

  private void updateUiForWaypoint(Waypoint waypoint) {
    Presenter presenter = presenterRef.get();

    if (presenter != null) {
      mainExecutor.execute(
          () -> {
            if (waypoint == null) {
              presenter.showTripId(NO_TRIP_ID);
              presenter.showTripStatus(TripStatus.UNKNOWN_TRIP_STATUS);
              presenter.showMatchedTripIds(ImmutableList.of());
              return;
            }

            TripState tripState = tripStates.get(waypoint.getTripId());

            presenter.showTripId(tripState.tripId());
            presenter.showTripStatus(tripState.tripStatus());
            presenter.showMatchedTripIds(matchedTripIds);
          });
    }
  }

  private ListenableFuture<TripModel> updateTripStatusInServer(TripState updatedState) {
    tripStates.put(updatedState.tripId(), updatedState);

    TripStatus updatedStatus = updatedState.tripStatus();

    if (updatedStatus == TripStatus.UNKNOWN_TRIP_STATUS) {
      return Futures.immediateFailedFuture(new IllegalStateException("Invalid trip status"));
    }

    if (updatedStatus == TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION) {
      return providerService.updateTripStatusWithIntermediateDestinationIndex(
          updatedState.tripId(),
          updatedStatus.toString(),
          updatedState.intermediateDestinationIndex());
    } else {
      return providerService.updateTripStatus(updatedState.tripId(), updatedStatus.toString());
    }
  }

  /** Sets presenter to the controller so it can invokes UI related callbacks. */
  public void setPresenter(Presenter presenter) {
    presenterRef = new WeakReference<>(presenter);
  }

  // Starts simulating journey sharing. Reduces the update interval.
  private void startJourneySharing() {
    requireNonNull(vehicleReporter)
        .setLocationReportingInterval(JOURNEY_SHARING_LOCATION_UPDATE_INTERVAL_SECONDS, SECONDS);
  }

  // Stops journey sharing by turning off guidance and reducing the location reporting frequency.
  private void stopJourneySharing() {
    requireNonNull(vehicleReporter)
        .setLocationReportingInterval(DEFAULT_LOCATION_UPDATE_INTERVAL_SECONDS, SECONDS);
    navigator.stopGuidance();
    navigator.clearDestinations();

    vehicleSimulator.unsetLocation();
  }

  private void setWaypointDestination(Waypoint waypoint) {
    com.google.android.libraries.navigation.Waypoint destinationWaypoint =
        com.google.android.libraries.navigation.Waypoint.builder()
            .setLatLng(
                waypoint.getLocation().getPoint().getLatitude(),
                waypoint.getLocation().getPoint().getLongitude())
            .setTitle(waypoint.getWaypointType())
            .build();

    // If the loaded trip has a 'routeToken' generated it would be set to NavSDK here.
    navigator.setDestination(destinationWaypoint);
  }

  /**
   * Given the current waypoint, it determines what the next one will be and sets the navigator
   * destination to it.
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  private void navigateToWaypoint(Waypoint waypoint) {
    if (waypoint == null) {
      navigator.stopGuidance();
      return;
    }

    ListenableResultFuture<Navigator.RouteStatus> pendingRoute =
        navigator.setDestination(
            com.google.android.libraries.navigation.Waypoint.builder()
                .setLatLng(
                    waypoint.getLocation().getPoint().getLatitude(),
                    waypoint.getLocation().getPoint().getLongitude())
                .setTitle(waypoint.getWaypointType())
                .build());

    pendingRoute.setOnResultListener(
        new ListenableResultFuture.OnResultListener<Navigator.RouteStatus>() {
          @Override
          public void onResult(Navigator.RouteStatus code) {
            switch (code) {
              case OK:
                navigator.startGuidance();
                vehicleSimulator.start(SIMULATOR_SPEED_MULTIPLIER);
                break;

              case NO_ROUTE_FOUND:
              case NETWORK_ERROR:
              case ROUTE_CANCELED:
              default:
                Log.e(TAG, "Failed to set a route to next waypoint");
                break;
            }
          }
        });
  }

  // Returns true if current trip has intermediate destinations (multi-destination support).
  public boolean isNextCurrentTripWaypointIntermediate() {
    return nextWaypointOfCurrentTrip != null
        && nextWaypointOfCurrentTrip
            .getWaypointType()
            .equals(TripUtils.INTERMEDIATE_DESTINATION_WAYPOINT_TYPE);
  }

  private static void logLocationUpdate(
      StatusLevel statusLevel, StatusCode statusCode, String statusMsg) {
    String message = "Location update: " + statusLevel + " " + statusCode + ": " + statusMsg;

    if (statusLevel == StatusLevel.ERROR) {
      Log.e(TAG, message);
    } else {
      Log.i(TAG, message);
    }
  }

  private static String extractVehicleId(String vehicleName) {
    Matcher matches = VEHICLE_NAME_FORMAT.matcher(vehicleName);
    if (matches.matches()) {
      return matches.group(VEHICLE_ID_INDEX);
    }
    return vehicleName;
  }
}
