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
package com.google.mapsplatform.transportation.sample;

import static com.google.mapsplatform.transportation.sample.state.AppStates.INITIALIZED;
import static com.google.mapsplatform.transportation.sample.state.AppStates.JOURNEY_SHARING;
import static com.google.mapsplatform.transportation.sample.state.AppStates.SELECTING_DROPOFF;
import static com.google.mapsplatform.transportation.sample.state.AppStates.SELECTING_PICKUP;
import static com.google.mapsplatform.transportation.sample.state.AppStates.UNINITIALIZED;
import static java.util.Objects.requireNonNull;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.lifecycle.ViewModelProviders;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.mapsplatform.transportation.consumer.ConsumerApi;
import com.google.android.libraries.mapsplatform.transportation.consumer.managers.TripModel;
import com.google.android.libraries.mapsplatform.transportation.consumer.managers.TripModelManager;
import com.google.android.libraries.mapsplatform.transportation.consumer.model.TerminalLocation;
import com.google.android.libraries.mapsplatform.transportation.consumer.model.Trip.TripStatus;
import com.google.android.libraries.mapsplatform.transportation.consumer.model.TripInfo;
import com.google.android.libraries.mapsplatform.transportation.consumer.sessions.JourneySharingSession;
import com.google.android.libraries.mapsplatform.transportation.consumer.view.ConsumerController;
import com.google.android.libraries.mapsplatform.transportation.consumer.view.ConsumerGoogleMap;
import com.google.android.libraries.mapsplatform.transportation.consumer.view.ConsumerGoogleMap.ConsumerMapReadyCallback;
import com.google.android.libraries.mapsplatform.transportation.consumer.view.ConsumerMapView;
import com.google.android.material.snackbar.Snackbar;
import com.google.mapsplatform.transportation.sample.provider.ProviderUtils;
import com.google.mapsplatform.transportation.sample.provider.model.TripData;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/** Main activity for the sample application. */
public class SampleAppActivity extends AppCompatActivity implements ConsumerViewModel.JourneySharingListener {

  private static final String TAG = "SampleAppActivity";

  private static final long MINUTE_IN_MILLIS = 1000 * 60;
  private static final int REQUEST_LOCATION_PERMISSION_CODE = 99;
  /** Default zoom of initial map state. */
  private static final int DEFAULT_ZOOM = 16;
  /** Default Map location if failed to receive FLP location. Defaulted to Google MTV. */
  private static final LatLng DEFAULT_MAP_LOCATION = new LatLng(37.423061, -122.084051);

  // The current journey sharing trip status.
  private TextView tripStatusView;
  // Displays the users trip id.
  private TextView tripIdView;
  // Displays the users vehicle id.
  private TextView vehicleIdView;
  // Indicates the Eta to the next waypoint in minutes.
  private TextView etaView;
  // Indicates the remaining distance to the next waypoint.
  private TextView remainingDistanceView;
  // The ridesharing map.
  private ConsumerMapView consumerMapView;
  // Multipurpose button depending on the app state (could be for selecting, dropoff, pickup, or requesting trip)
  private Button actionButton;
  // Dropoff pin in the center of the map.
  private View dropoffPin;
  // Pickup pin in the center of the map.
  private View pickupPin;
  // The reference to the map that is being displayed.
  @MonotonicNonNull private ConsumerGoogleMap googleMap;
  // The last location of the device.
  @Nullable private LatLng lastLocation;
  // Map of markers on google map to indicate pickup and dropoff.
  private final Map<Integer, Marker> mapMarkers = new HashMap<>();

  // ViewModel for the consumer sample app.
  private ConsumerViewModel consumerViewModel;

  @MonotonicNonNull private TripModelManager tripModelManager;
  @MonotonicNonNull private ConsumerController consumerController;

  // Session monitoring the current active trip.
  @Nullable private JourneySharingSession journeySharingSession;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    initViews();

    // Get the ViewModel.
    consumerViewModel = ViewModelProviders.of(this).get(ConsumerViewModel.class);
    consumerViewModel.setJourneySharingListener(this);

    // If the permission is granted, then continue to initialize. Otherwise wait until
    // the permission is granted in onRequestPermissionResult().
    if (hasRequiredPermissions()) {
      initializeSdk();
    } else {
      // Permissions are granted, initialize the application.
      ActivityCompat.requestPermissions(
          this, new String[] {permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION_CODE);
    }

    Log.i(TAG, "Consumer SDK version: " + ConsumerApi.getConsumerSDKVersion());
  }

  /**
   * Start the asynchronous call to get the map api and subsequently the consumer controller. The
   * ConsumerGoogleMap is returned, is used to access the ConsumerController. Observers are created
   * that update the UI based on observed trip data.
   */
  private void initializeSdk() {
    showStartupLocation();

    consumerMapView.getConsumerGoogleMapAsync(
        new ConsumerMapReadyCallback() {
          @Override
          public void onConsumerMapReady(ConsumerGoogleMap consumerGoogleMap) {
            // Safe to do so as controller will only be nullified during consumerMap's onDestroy()
            consumerController = requireNonNull(consumerGoogleMap.getConsumerController());
            Task<ConsumerApi> consumerApiTask =
                ConsumerApi.initialize(
                    SampleAppActivity.this,
                    ProviderUtils.getProviderId(SampleAppActivity.this),
                    new TripAuthTokenFactory(getApplication()));
            consumerApiTask.addOnSuccessListener(
                consumerApi ->
                    tripModelManager = requireNonNull(consumerApi.getTripModelManager()));
            consumerApiTask.addOnFailureListener(
                task -> Log.e(TAG, "ConsumerApi Initialization Error:\n" + task.getMessage()));
            ConsumerMarkerUtils.setCustomMarkers(consumerController, SampleAppActivity.this);
            setupViewBindings();
            googleMap = consumerGoogleMap;
            centerCamera();
            setupMapListener();
          }
        },
        /* fragmentActivity= */ this,
        /* googleMapOptions= */ null);
  }

  @Override
  public TripModel startJourneySharing(TripData tripData) {
    TripModel trip = requireNonNull(tripModelManager).getTripModel(tripData.tripName());
    journeySharingSession = JourneySharingSession.createInstance(trip);
    requireNonNull(consumerController).showSession(journeySharingSession);
    return trip;
  }

  @Override
  public void stopJourneySharing() {
    if (journeySharingSession != null) {
      journeySharingSession.stop();
      journeySharingSession = null;
    }
    requireNonNull(consumerController).hideAllSessions();
  }

  /** Center the camera to the last location of the device. */
  private void centerCamera() {
    if (lastLocation != null) {
      requireNonNull(googleMap)
          .moveCamera(CameraUpdateFactory.newLatLngZoom(lastLocation, DEFAULT_ZOOM));
    }
  }

  @Override
  public void updateCurrentLocation(LatLng latLang) {
    lastLocation = latLang;
  }

  @SuppressLint("MissingPermission")
  private void showStartupLocation() {
    FusedLocationProviderClient fusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(this);
    fusedLocationProviderClient
        .getLastLocation()
        .addOnCompleteListener(
            task -> {
              if (task.isSuccessful() && task.getResult() != null) {
                Location location = task.getResult();
                lastLocation = new LatLng(location.getLatitude(), location.getLongitude());
              } else {
                lastLocation = DEFAULT_MAP_LOCATION;
              }
              if (googleMap != null) {
                centerCamera();
              }
            });
  }

  /** Setting up map idle listeners to select pickup and dropoff locations. */
  private void setupMapListener() {
    requireNonNull(googleMap)
        .setOnCameraIdleListener(
            () -> {
              CameraPosition cameraPosition = requireNonNull(googleMap).getCameraPosition();
              if (cameraPosition == null) {
                return;
              }
              int state = consumerViewModel.getAppState().getValue();
              LatLng cameraLocation = cameraPosition.target;
              if (state == SELECTING_DROPOFF) {
                consumerViewModel.setDropoffLocation(cameraLocation);
                updateMarker(
                    ConsumerMarkerType.DROPOFF_POINT,
                    TerminalLocation.builder(cameraLocation).build());
              } else if (state == SELECTING_PICKUP) {
                consumerViewModel.setPickupLocation(cameraLocation);
                updateMarker(
                    ConsumerMarkerType.PICKUP_POINT,
                    TerminalLocation.builder(cameraLocation).build());
              }
            });
  }

  /** Update marker on the map based on camera location. */
  private void updateMarker(@ConsumerMarkerType Integer markerType, TerminalLocation location) {
    Marker marker = mapMarkers.get(markerType);
    if (marker == null) {
      marker =
          requireNonNull(googleMap)
              .addMarker(
                  ConsumerMarkerUtils.getConsumerMarkerOptions(this, markerType)
                      .position(location.getLatLng()));
      mapMarkers.put(markerType, marker);
    } else {
      // adjust position of existing marker
      marker.setPosition(location.getLatLng());
    }
  }

  /**
   * When first selecting pickup, the idle handler on map is not initiated since the map was
   * already idle when selecting initial pickup. Select the current location as pickup initially.
   */
  private void maybeSelectInitialPickup() {
    if (googleMap.getCameraPosition() == null) {
      return;
    }
    LatLng cameraLocation = googleMap.getCameraPosition().target;
    consumerViewModel.setDropoffLocation(cameraLocation);
    updateMarker(
        ConsumerMarkerType.PICKUP_POINT, TerminalLocation.builder(cameraLocation).build());
  }

  /** Display the trip status based on the observed trip status. */
  private void displayTripStatus(int status) {
    switch (status) {
      case TripStatus.NEW:
        if (consumerViewModel.isTripMatched()) {
          setTripStatusTitle(R.string.state_enroute_to_pickup);
        } else {
          setTripStatusTitle(R.string.state_new);
        }
        actionButton.setVisibility(View.INVISIBLE);
        break;
      case TripStatus.ENROUTE_TO_PICKUP:
        setTripStatusTitle(R.string.state_enroute_to_pickup);
        break;
      case TripStatus.ARRIVED_AT_PICKUP:
        setTripStatusTitle(R.string.state_arrived_at_pickup);
        break;
      case TripStatus.ENROUTE_TO_DROPOFF:
        setTripStatusTitle(R.string.state_enroute_to_dropoff);
        break;
      case TripStatus.COMPLETE:
      case TripStatus.CANCELED:
        setTripStatusTitle(R.string.state_end_of_trip);
        hideTripData();
        break;
      default:
        break;
    }
  }

  /** Display the reported map state and show trip data when in journey sharing state. */
  private void displayAppState(int state) {
    switch (state) {
      case SELECTING_PICKUP:
        setTripStatusTitle(R.string.state_select_pickup);
        centerCamera();
        pickupPin.setVisibility(View.VISIBLE);
        break;
      case SELECTING_DROPOFF:
        setTripStatusTitle(R.string.state_select_dropoff);
        pickupPin.setVisibility(View.INVISIBLE);
        dropoffPin.setVisibility(View.VISIBLE);
        maybeSelectInitialPickup();
        break;
      case JOURNEY_SHARING:
        dropoffPin.setVisibility(View.INVISIBLE);
        setTripStatusTitle(R.string.state_enroute_to_pickup);
        break;
      case INITIALIZED:
        tripStatusView.setVisibility(View.INVISIBLE);
        resetActionButton();
        removeAllMarkers();
        hideTripData();
        break;
      default:
        hideTripData();
        break;
    }
  }

  /** Remove markers on the map. */
  private void removeAllMarkers() {
    for (Map.Entry<Integer, Marker> entry : mapMarkers.entrySet()) {
      entry.getValue().remove();
    }
    mapMarkers.clear();
  }

  /** Set button to be initial state. */
  private void resetActionButton() {
    actionButton.setText(R.string.request_button_label);
    actionButton.setVisibility(View.VISIBLE);
    Drawable roundedButton = actionButton.getBackground();
    DrawableCompat.setTint(roundedButton,
        ContextCompat.getColor(this, R.color.actionable));
  }

  private void hideTripData() {
    tripIdView.setVisibility(View.INVISIBLE);
    vehicleIdView.setVisibility(View.INVISIBLE);
    etaView.setVisibility(View.INVISIBLE);
    remainingDistanceView.setVisibility(View.INVISIBLE);
  }

  /** Sets the UI state based on the trip state. */
  private void setTripStatusTitle(int resId) {
    tripStatusView.setText(resId);
    tripStatusView.setVisibility(View.VISIBLE);
  }

  /** Updates the displayed trip Id. */
  private void displayTripId(@Nullable String tripId) {
    if (tripId == null) {
      tripIdView.setText("");
      return;
    }
    tripIdView.setText(getResources().getString(R.string.trip_id_label, tripId));
    tripIdView.setVisibility(View.VISIBLE);
  }

  private void displayTripInfo(@Nullable TripInfo tripInfo) {
    if (tripInfo == null || tripInfo.getVehicleId() == null) {
      vehicleIdView.setText("");
      return;
    }
    vehicleIdView.setText(
        getResources().getString(R.string.vehicle_id_label, tripInfo.getVehicleId()));
    displayTripStatus(tripInfo.getTripStatus());

    int visibility = (tripInfo.getTripStatus() == TripStatus.COMPLETE
        || tripInfo.getTripStatus() == TripStatus.CANCELED) ? View.INVISIBLE : View.VISIBLE;
    vehicleIdView.setVisibility(visibility);
  }

  private void onActionButtonTapped(View view) {
    int currentState = consumerViewModel.getAppState().getValue();
    switch (currentState) {
      case INITIALIZED:
        centerCamera();
        // fall through
      case UNINITIALIZED:
        actionButton.setText(R.string.pickup_label);
        consumerViewModel.setState(SELECTING_PICKUP);
        break;
      case SELECTING_PICKUP:
        actionButton.setText(R.string.dropoff_label);
        consumerViewModel.setState(SELECTING_DROPOFF);
        break;
      case SELECTING_DROPOFF:
        // Create trip
        consumerViewModel.startSingleExclusiveTrip();
        break;
    }
  }

  /** Display the minutes remaining to the next waypoint. */
  private void displayEta(@Nullable Long etaValue) {
    if (etaValue == null) {
      etaView.setText("");
      return;
    }
    final Date current = new Date();
    final long diff = etaValue - current.getTime();
    final int minutes = (int) (double) (diff / MINUTE_IN_MILLIS);
    etaView.setVisibility(View.VISIBLE);
    if (minutes < 1) {
      etaView.setText(getResources().getString(R.string.eta_within_one_minute));
      return;
    }
    etaView.setText(
        getResources().getQuantityString(R.plurals.eta_format_string, minutes, minutes));
  }

  /** Displays the remaining distance to the next waypoint. */
  private void displayRemainingDistance(@Nullable Integer remainingDistanceMeters) {
    if (remainingDistanceMeters == null || remainingDistanceMeters <= 0) {
      remainingDistanceView.setText("");
      return;
    }
    if (remainingDistanceMeters >= 1000) {
      // Display remaining distance in km.
      remainingDistanceView.setText(
          getString(R.string.distance_format_string_km, remainingDistanceMeters / 1000.0));
    } else {
      // Display remaining distance in m.
      remainingDistanceView.setText(
          getResources()
              .getString(R.string.distance_format_string_meters, remainingDistanceMeters));
    }
    remainingDistanceView.setVisibility(View.VISIBLE);
  }

  private void displayErrorMessage(@StringRes int message) {
    Snackbar.make(tripStatusView, message, Snackbar.LENGTH_SHORT).show();
  }

  private void initViews() {
    // Find the UI views for later updates.
    tripStatusView = findViewById(R.id.tripStatus);
    tripIdView = findViewById(R.id.tripId);
    vehicleIdView = findViewById(R.id.vehicleId);
    etaView = findViewById(R.id.eta);
    remainingDistanceView = findViewById(R.id.remainingDistance);
    consumerMapView = findViewById(R.id.consumer_map_view);
    pickupPin = findViewById(R.id.pickup_pin);
    dropoffPin = findViewById(R.id.dropoff_pin);
    actionButton = findViewById(R.id.actionButton);
    actionButton.setOnClickListener(this::onActionButtonTapped);
    resetActionButton();
  }

  private void setupViewBindings() {
    // Start observing trip data.
    consumerViewModel
        .getTripStatus()
        .observe(SampleAppActivity.this, SampleAppActivity.this::displayTripStatus);
    consumerViewModel
        .getAppState()
        .observe(SampleAppActivity.this, SampleAppActivity.this::displayAppState);
    consumerViewModel
        .getTripId()
        .observe(SampleAppActivity.this, SampleAppActivity.this::displayTripId);
    consumerViewModel
        .getTripInfo()
        .observe(SampleAppActivity.this, SampleAppActivity.this::displayTripInfo);
    consumerViewModel
        .getNextWaypointEta()
        .observe(SampleAppActivity.this, SampleAppActivity.this::displayEta);
    consumerViewModel
        .getRemainingDistanceMeters()
        .observe(SampleAppActivity.this, SampleAppActivity.this::displayRemainingDistance);
    consumerViewModel
        .getErrorMessage()
        .observe(SampleAppActivity.this, SampleAppActivity.this::displayErrorMessage);
  }

  private boolean hasRequiredPermissions() {
    return ContextCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED
        && ContextCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION)
        == PackageManager.PERMISSION_GRANTED;
  }

  /**
   * Callback from checking permission. If it is granted, the initialize the application, otherwise
   * the application display a dialog indicating the missing permission.
   */
  @Override
  public void onRequestPermissionsResult(
      int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode != REQUEST_LOCATION_PERMISSION_CODE) {
      return;
    }
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      initializeSdk();
      return;
    }
    // Tell the user that we need location permissions.
    new AlertDialog.Builder(this)
        .setTitle("")
        .setMessage(R.string.msg_require_permissions)
        .setPositiveButton(
            android.R.string.ok,
            (dialogInterface, i) -> {
              dialogInterface.dismiss();
              ActivityCompat.requestPermissions(
                  this,
                  new String[]{permission.ACCESS_FINE_LOCATION, permission.ACCESS_COARSE_LOCATION},
                  REQUEST_LOCATION_PERMISSION_CODE);
            })
        .create()
        .show();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    consumerViewModel.unregisterTripCallback();
  }
}
