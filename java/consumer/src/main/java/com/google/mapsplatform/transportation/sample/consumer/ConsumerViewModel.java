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
package com.google.mapsplatform.transportation.sample.consumer;

import android.app.Application;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.mapsplatform.transportation.consumer.managers.TripModel;
import com.google.android.libraries.mapsplatform.transportation.consumer.managers.TripModelCallback;
import com.google.android.libraries.mapsplatform.transportation.consumer.model.Trip;
import com.google.android.libraries.mapsplatform.transportation.consumer.model.TripInfo;
import com.google.android.libraries.mapsplatform.transportation.consumer.model.TripName;
import com.google.android.libraries.mapsplatform.transportation.consumer.model.TripWaypoint;
import com.google.android.libraries.mapsplatform.transportation.consumer.model.VehicleLocation;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mapsplatform.transportation.sample.consumer.provider.ProviderUtils;
import com.google.mapsplatform.transportation.sample.consumer.provider.model.CreateTripRequest;
import com.google.mapsplatform.transportation.sample.consumer.provider.model.TripData;
import com.google.mapsplatform.transportation.sample.consumer.provider.model.TripStatus;
import com.google.mapsplatform.transportation.sample.consumer.provider.response.TripResponse;
import com.google.mapsplatform.transportation.sample.consumer.provider.response.WaypointResponse;
import com.google.mapsplatform.transportation.sample.consumer.provider.service.LocalProviderService;
import com.google.mapsplatform.transportation.sample.consumer.state.AppStates;
import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * ViewModel for the Consumer Sample application. It provides observables for the current
 * application status such as trip completed and journey sharing which it maintains using
 * StateChangeCallback. When in journey sharing it has observables for the details of the trip
 * including eta, distance remaining and waypoint data.
 */
public class ConsumerViewModel extends AndroidViewModel {

  /** Amount of time until an idle state reset should be delayed before applying changes. */
  private static final int IDLE_STATE_RESET_DELAY_SECONDS = 3;

  interface JourneySharingListener {

    /** Starts journey sharing rendering and return that {@link TripModel}. */
    TripModel startJourneySharing(TripData tripData);

    /** Stops journey sharing rendering. */
    void stopJourneySharing();

    /** Updates current location. */
    void updateCurrentLocation(LatLng latLang);
  }

  private static final String TAG = "ConsumerViewModel";

  // LiveData for current application map state.
  private final MutableLiveData<Integer> appState = new MutableLiveData<>();

  // LiveData for dropoff location.
  private final MutableLiveData<LatLng> pickupLocation = new MutableLiveData<>();

  // LiveData for pickup location.
  private final MutableLiveData<LatLng> dropoffLocation = new MutableLiveData<>();

  // LiveData for selected intermediate destinations. (multi-destination trips)
  private final MutableLiveData<ImmutableList<LatLng>> intermediateDestinations =
      new MutableLiveData<>();

  // LiveData of the current trip info from a trip refresh.
  private final MutableLiveData<Integer> tripStatus = new MutableLiveData<>();

  // LiveData of the current trip Id.
  private final MutableLiveData<String> tripId = new MutableLiveData<>();

  // LiveData of the remaining distance to the next waypoint.
  private final MutableLiveData<Integer> remainingDistanceMeters = new MutableLiveData<>();

  // LiveData of the ETA to the next waypoint.
  private final MutableLiveData<Long> nextWaypointEta = new MutableLiveData<>();

  // Latest trip data including: time remaining, distance remaining and etc.
  private final MutableLiveData<TripInfo> tripInfo = new MutableLiveData<>();

  // The current active trip, meant for create journey sharing session and observe session.
  private final MutableLiveData<TripModel> trip = new MutableLiveData<>();

  // LiveData of the 'Trip shared' switch value.
  private final MutableLiveData<Boolean> isSharedTripType = new MutableLiveData<>();

  // LiveData containing a potential list of waypoints the driver is going through getting to the
  // next current trip waypoint (B2B or Shared pool).
  private final MutableLiveData<ImmutableList<TripWaypoint>> otherTripWaypoints =
      new MutableLiveData<>(ImmutableList.of());

  // Latest error message.
  private final SingleLiveEvent<Integer> errorMessage = new SingleLiveEvent<>();

  private final LocalProviderService providerService;
  private final ExecutorService executor = Executors.newCachedThreadPool();
  private final Executor mainExecutor;
  private final ScheduledExecutorService scheduledExecutor =
      Executors.newSingleThreadScheduledExecutor();

  private WeakReference<JourneySharingListener> journeySharingListener = new WeakReference<>(null);

  /**
   * Initializes the ConsumerApi and if successful initiates TripManager configuration.
   *
   * @param application Application context.
   */
  public ConsumerViewModel(Application application) {
    super(application);
    providerService =
        new LocalProviderService(
            LocalProviderService.createRestProvider(ProviderUtils.getProviderBaseUrl(application)),
            executor,
            scheduledExecutor);
    appState.setValue(AppStates.UNINITIALIZED);
    mainExecutor = ContextCompat.getMainExecutor(application);
  }

  /** Creates a trip in the sample provider. */
  public void createTrip() {
    String tripType =
        isSharedTripType.getValue() != null && isSharedTripType.getValue() ? "SHARED" : "EXCLUSIVE";

    CreateTripRequest request = new CreateTripRequest();

    request.setPickup(pickupLocation.getValue());
    request.setDropoff(dropoffLocation.getValue());
    request.setIntermediateDestinations(intermediateDestinations.getValue());
    request.setTripType(tripType);

    ListenableFuture<TripResponse> tripResponseFuture = providerService.createTrip(request);

    handleCreateTripResponse(tripResponseFuture);
  }

  private void handleCreateTripResponse(ListenableFuture<TripResponse> tripResponseFuture) {
    Futures.addCallback(
        tripResponseFuture,
        new FutureCallback<TripResponse>() {
          @Override
          public void onSuccess(TripResponse result) {
            Log.i(TAG, String.format("Successfully created trip %s.", result.getTripName()));
            tripStatus.postValue(TripStatus.parse(result.getTripStatus()));

            ListenableFuture<TripData> tripDataFuture =
                providerService.fetchMatchedTrip(result.getTripName());
            handleFetchMatchedTripResponse(tripDataFuture);
          }

          @Override
          public void onFailure(Throwable e) {
            Log.e(TAG, "Failed to create trip.", e);
            setErrorMessage(e);
          }
        },
        executor);
  }

  private void handleFetchMatchedTripResponse(ListenableFuture<TripData> tripDataFuture) {
    Futures.addCallback(
        tripDataFuture,
        new FutureCallback<TripData>() {
          @Override
          public void onSuccess(TripData tripData) {
            mainExecutor.execute(() -> startJourneySharing(tripData));
          }

          @Override
          public void onFailure(Throwable e) {
            Log.e(TAG, "Failed to match trip with a driver.", e);
            setErrorMessage(e);
          }
        },
        executor);
  }

  public void startJourneySharing(TripData tripData) {
    if (appState.getValue() != AppStates.CONFIRMING_TRIP) {
      Log.e(
          TAG,
          String.format(
              "App state should be `SELECTING_DROPOFF` but is %d, journey sharing cannot be"
                  + " started.",
              appState.getValue()));
      return;
    }
    List<WaypointResponse> waypoints = tripData.waypoints();
    JourneySharingListener listener = journeySharingListener.get();

    if (waypoints.size() < 2 || listener == null) {
      return;
    }
    TripModel tripModel = listener.startJourneySharing(tripData);
    trip.setValue(tripModel);
    appState.setValue(AppStates.JOURNEY_SHARING);
    tripModel.registerTripCallback(tripCallback);
  }

  /**
   * Updates trip status livedata value. Resets app state to INITIALIZED if Journey Sharing is in a
   * terminal or error state (COMPLETE, CANCELED, or UNKNOWN).
   */
  private void updateTripStatus(int status) {
    tripStatus.setValue(status);
    if (status == Trip.TripStatus.COMPLETE
        || status == Trip.TripStatus.CANCELED
        || status == Trip.TripStatus.UNKNOWN_TRIP_STATUS) {
      stopJourneySharing();
      intermediateDestinations.setValue(ImmutableList.of());
    }
  }

  private void stopJourneySharing() {
    unregisterTripCallback();
    JourneySharingListener listener = journeySharingListener.get();
    if (listener != null) {
      listener.stopJourneySharing();
    }
    ScheduledFuture<?> unused =
        scheduledExecutor.schedule(
            () -> mainExecutor.execute(() -> appState.setValue(AppStates.INITIALIZED)),
            IDLE_STATE_RESET_DELAY_SECONDS,
            TimeUnit.SECONDS);
  }

  /** Unregisters callback as part of cleanup. */
  public void unregisterTripCallback() {
    TripModel tripModel = trip.getValue();
    if (tripModel != null) {
      tripModel.unregisterTripCallback(tripCallback);
    }
    tripInfo.setValue(null);
  }

  public void setJourneySharingListener(JourneySharingListener journeySharingListener) {
    this.journeySharingListener = new WeakReference<>(journeySharingListener);
  }

  public void setIsSharedTripType(boolean isShared) {
    isSharedTripType.setValue(isShared);
  }

  /** Set the app state. */
  public void setState(@AppStates int state) {
    appState.setValue(state);
  }

  /** Returns the current AppState. */
  public LiveData<Integer> getAppState() {
    return appState;
  }

  /** Returns the current trip TripInfo updated with each trip refresh */
  public LiveData<TripInfo> getTripInfo() {
    return tripInfo;
  }

  /** Checks if a trip is matched (i.e. vehicleId is present in the trip info. */
  public boolean isTripMatched() {
    TripInfo tripInfo = this.tripInfo.getValue();
    return tripInfo != null && !Strings.isNullOrEmpty(tripInfo.getVehicleId());
  }

  /** Determines if the driver is currently working on another trip's waypoint. */
  public boolean isDriverInOtherTripWaypoint() {
    return !getOtherTripWaypoints().getValue().isEmpty();
  }

  /** Gets the current waypoint type when driver is currently working on another trip. */
  public @TripWaypoint.WaypointType int getOtherTripWaypointType() {
    if (!isDriverInOtherTripWaypoint()) {
      return TripWaypoint.WaypointType.UNKNOWN;
    }

    return getOtherTripWaypoints().getValue().get(0).getWaypointType();
  }

  /** Returns the current trip {@link Trip.TripStatus} for each status change during the trip. */
  public LiveData<Integer> getTripStatus() {
    return tripStatus;
  }

  /** Returns ETA to the next waypoint of the trip. */
  public LiveData<Long> getNextWaypointEta() {
    return nextWaypointEta;
  }

  /** Returns the distance in meters to the next waypoint. */
  public LiveData<Integer> getRemainingDistanceMeters() {
    return remainingDistanceMeters;
  }

  /** Returns the trip id. */
  public LiveData<String> getTripId() {
    return tripId;
  }

  /** Returns the list of other trip waypoints (B2B/Shared pool support) */
  public LiveData<ImmutableList<TripWaypoint>> getOtherTripWaypoints() {
    return otherTripWaypoints;
  }

  /** Updates the location container (pickup or dropoff) given by the current app state. */
  public void updateLocationPointForState(LatLng cameraLocation) {
    @AppStates int state = this.appState.getValue();

    if (state != AppStates.SELECTING_PICKUP && state != AppStates.SELECTING_DROPOFF) {
      return;
    }

    if (state == AppStates.SELECTING_PICKUP) {
      setPickupLocation(cameraLocation);
    } else {
      setDropoffLocation(cameraLocation);
    }
  }

  /** Set the selected dropoff location. */
  public void setDropoffLocation(LatLng location) {
    dropoffLocation.setValue(location);
  }

  /** Gets the selected dropoff location (or null if not selected) */
  public LatLng getDropoffLocation() {
    return dropoffLocation.getValue();
  }

  /** Set the selected pickup location. */
  public void setPickupLocation(LatLng location) {
    pickupLocation.setValue(location);
  }

  /** Gets the selected pickup location (or null if not selected) */
  public LatLng getPickupLocation() {
    return pickupLocation.getValue();
  }

  /**
   * Adds the current selected location (contained in dropoffLocation) to the list of intermediate
   * destinations.
   */
  public void addIntermediateDestination() {
    LatLng destination = dropoffLocation.getValue();

    if (destination == null) {
      return;
    }

    if (intermediateDestinations.getValue() == null) {
      intermediateDestinations.setValue(ImmutableList.of());
    }

    intermediateDestinations.setValue(
        ImmutableList.<LatLng>builder()
            .addAll(intermediateDestinations.getValue())
            .add(destination)
            .build());
  }

  /** Retrieves the list of intermediate destinations added so far to the trip. */
  public List<LatLng> getIntermediateDestinations() {
    return intermediateDestinations.getValue();
  }

  public SingleLiveEvent<Integer> getErrorMessage() {
    return errorMessage;
  }

  private void setErrorMessage(Throwable e) {
    if (e instanceof ConnectException) {
      mainExecutor.execute(() -> errorMessage.setValue(R.string.msg_provider_connection_error));
    }
  }

  /** Trip callbacks registered during */
  private final TripModelCallback tripCallback =
      new TripModelCallback() {
        @Override
        public void onTripActiveRouteRemainingDistanceUpdated(
            TripInfo tripInfo, @Nullable Integer distanceMeters) {
          remainingDistanceMeters.setValue(distanceMeters);
        }

        @Override
        public void onTripUpdated(TripInfo tripInfo) {
          ConsumerViewModel.this.tripInfo.setValue(tripInfo);
          tripId.setValue(TripName.create(tripInfo.getTripName()).getTripId());
        }

        @Override
        public void onTripStatusUpdated(TripInfo tripInfo, @Trip.TripStatus int status) {
          updateTripStatus(tripInfo.getTripStatus());
        }

        @Override
        public void onTripETAToNextWaypointUpdated(
            TripInfo tripInfo, @Nullable Long timestampMillis) {
          nextWaypointEta.setValue(timestampMillis);
        }

        @Override
        public void onTripVehicleLocationUpdated(
            TripInfo tripInfo, @Nullable VehicleLocation vehicleLocation) {
          maybeUpdateCurrentLocation(vehicleLocation);
        }

        @Override
        public void onTripRemainingWaypointsUpdated(
            TripInfo tripInfo, List<TripWaypoint> waypointList) {
          ImmutableList.Builder<TripWaypoint> otherTripWaypointsBuilder =
              new ImmutableList.Builder<>();

          String currentTripId = TripName.create(tripInfo.getTripName()).getTripId();

          for (TripWaypoint tripWaypoint : waypointList) {
            if (tripWaypoint.getTripId().equals(currentTripId)) {
              break;
            }

            otherTripWaypointsBuilder.add(tripWaypoint);
          }

          otherTripWaypoints.setValue(otherTripWaypointsBuilder.build());
        }
      };

  private void maybeUpdateCurrentLocation(@Nullable VehicleLocation vehicleLocation) {
    JourneySharingListener listener = journeySharingListener.get();
    if (appState.getValue() == AppStates.JOURNEY_SHARING
        && listener != null
        && vehicleLocation != null) {
      listener.updateCurrentLocation(vehicleLocation.getLatLng());
    }
  }
}
