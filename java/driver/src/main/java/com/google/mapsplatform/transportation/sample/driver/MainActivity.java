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

import android.app.Application;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.gms.maps.GoogleMap.CameraPerspective;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.libraries.navigation.NavigationApi;
import com.google.android.libraries.navigation.Navigator;
import com.google.android.libraries.navigation.SupportNavigationFragment;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mapsplatform.transportation.sample.driver.dialog.VehicleDialogFragment;
import com.google.mapsplatform.transportation.sample.driver.state.TripStatus;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * NOTE: BEFORE BUILDING THIS APPLICATION YOU MUST COPY THE GOOGLE NAVIGATION API
 * ANDROID LIBRARY (AAR FILE) INTO THE libs/ DIRECTORY OF THIS APP.
 */

/** Main activity for the DriverSDK test app. */
public final class MainActivity extends AppCompatActivity implements Presenter {

  private static final String TAG = "MainActivity";
  private static final int TRIP_VIEW_INITIAL_STATE = View.GONE;

  private final ExecutorService executor = Executors.newCachedThreadPool();

  private VehicleIdStore vehicleIdStore;
  private SupportNavigationFragment navFragment;
  private TextView simulationStatusText;
  private TextView tripMatchText;
  private TextView textVehicleId;
  private Button actionButton;
  private CardView tripCard;
  private VehicleController vehicleController;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    vehicleIdStore = new VehicleIdStore(this);
    simulationStatusText = findViewById(R.id.simulation_status);
    actionButton = findViewById(R.id.action_button);
    Button editVehicleIdButton = findViewById(R.id.edit_button);
    editVehicleIdButton.setOnClickListener(this::onEditVehicleButtonClicked);
    tripCard = findViewById(R.id.trip_card);
    tripMatchText = findViewById(R.id.trip_match);
    textVehicleId = findViewById(R.id.menu_vehicle_id);
    textVehicleId.setText(vehicleIdStore.readOrDefault());
    setupNavFragment();
    updateActionButton(TRIP_VIEW_INITIAL_STATE, null);
    actionButton.setOnClickListener(this::onActionButtonClicked);

    // Ensure the screen stays on during navigation.
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    initializeSDKs();
  }

  private void initializeSDKs() {
    NavigationApi.getNavigator(
        this,
        new NavigationApi.NavigatorListener() {

          @Override
          public void onNavigatorReady(Navigator navigator) {
            Application app = (Application) getApplicationContext();
            vehicleController =
                new VehicleController(
                    navigator,
                    requireNonNull(NavigationApi.getRoadSnappedLocationProvider(app)),
                    executor,
                    MainActivity.this);
            vehicleController.setPresenter(MainActivity.this);
            ListenableFuture<Boolean> done = vehicleController.initVehicleAndReporter(app);
            Futures.addCallback(
                done,
                new FutureCallback<Boolean>() {
                  @Override
                  public void onSuccess(Boolean result) {
                    vehicleController.pollTrip();
                  }

                  @Override
                  public void onFailure(Throwable ignored) { }
                },
                executor);
          }

          @Override
          public void onError(@NavigationApi.ErrorCode int errorCode) {
            logNavigationApiInitError(errorCode);
          }
        });
  }

  private void setupNavFragment() {
    navFragment = SupportNavigationFragment.newInstance();
    getSupportFragmentManager()
        .beginTransaction()
        .add(R.id.nav_fragment_frame, navFragment, null)
        .setReorderingAllowed(true)
        .commit();
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void onEditVehicleButtonClicked(View view) {
    VehicleDialogFragment fragment =
        VehicleDialogFragment.newInstance(
            vehicleIdStore.readOrDefault(),
            vehicleId -> {
              textVehicleId.setText(vehicleId);
              vehicleIdStore.save(vehicleId);
              vehicleController.initVehicle();
            });
    fragment.show(getSupportFragmentManager(), "VehicleInfoDialog");
  }

  private void onActionButtonClicked(View view) {
    TripStatus currentStatus = vehicleController.getTripStatus();
    if (!currentStatus.isTerminalState()) {
      TripStatus nextStatus = currentStatus.getNextStatus();
      vehicleController.updateTripStatus(nextStatus);
      // controller will call presenter to show NEW when polling succeeds.
      if (nextStatus != TripStatus.NEW) {
        showTripStatus(nextStatus);
      }
    }
  }

  private void updateCameraPerspective(Boolean isTilted) {
    navFragment.getMapAsync(
        (OnMapReadyCallback)
            googleMap ->
                googleMap.followMyLocation(
                    isTilted ? CameraPerspective.TILTED : CameraPerspective.TOP_DOWN_NORTH_UP));
  }

  private void updateActionButton(Integer visibility, @Nullable Integer resourceId) {
    if (resourceId != null) {
      actionButton.setText(resourceId);
    }

    actionButton.setVisibility(visibility);
  }

  @Override
  public void showTripId(String tripId) {
    tripMatchText.setText(tripId);
  }

  @Override
  public void showTripStatus(TripStatus status) {
    int resourceId = R.string.status_idle;
    int buttonVisibility = View.VISIBLE;
    int cardVisibility = View.VISIBLE;

    // Determines whether the camera perspective should be tilted.
    boolean isCameraTilted = false;

    switch (status) {
      case UNKNOWN_TRIP_STATUS:
        buttonVisibility = TRIP_VIEW_INITIAL_STATE;
        cardVisibility = TRIP_VIEW_INITIAL_STATE;
        simulationStatusText.setText(resourceId);
        break;
      case NEW:
        simulationStatusText.setText(R.string.status_new);
        resourceId = R.string.button_start_trip;
        break;
      case ENROUTE_TO_PICKUP:
        simulationStatusText.setText(R.string.status_enroute_to_pickup);
        resourceId = R.string.button_arrived_at_pickup;
        isCameraTilted = true;
        break;
      case ARRIVED_AT_PICKUP:
        simulationStatusText.setText(R.string.status_arrived_at_pickup);
        resourceId = R.string.button_enroute_to_dropoff;
        isCameraTilted = true;
        break;
      case ENROUTE_TO_DROPOFF:
        simulationStatusText.setText(R.string.status_enroute_to_dropoff);
        resourceId = R.string.button_trip_complete;
        isCameraTilted = true;
        break;
      case COMPLETE:
        resourceId = R.string.status_complete;
        buttonVisibility = View.GONE;
        simulationStatusText.setText(resourceId);
        break;
      case CANCELED:
        resourceId = R.string.status_canceled;
        buttonVisibility = View.GONE;
        simulationStatusText.setText(resourceId);
        break;
    }

    tripCard.setVisibility(cardVisibility);
    updateActionButton(buttonVisibility, resourceId);
    updateCameraPerspective(isCameraTilted);
  }

  private static void logNavigationApiInitError(int errorCode) {
    switch (errorCode) {
      case NavigationApi.ErrorCode.NOT_AUTHORIZED:
        Log.w(
            TAG,
            "Note: If this message is displayed, you may need to check that your API_KEY is"
                + " specified correctly in AndroidManifest.xml and is been enabled to"
                + " access the Navigation API.");
        break;
      case NavigationApi.ErrorCode.TERMS_NOT_ACCEPTED:
        Log.w(
            TAG,
            "Error loading Navigation API: User did not " + "accept the Navigation Terms of Use.");
        break;
      default:
        Log.w(TAG, String.format("Error loading Navigation API: %d", errorCode));
    }
  }
}
