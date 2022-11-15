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

import android.annotation.SuppressLint;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.gms.maps.GoogleMap.CameraPerspective;
import com.google.android.libraries.mapsplatform.transportation.driver.api.ridesharing.RidesharingDriverApi;
import com.google.android.libraries.navigation.NavigationApi;
import com.google.android.libraries.navigation.Navigator;
import com.google.android.libraries.navigation.SupportNavigationFragment;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mapsplatform.transportation.sample.driver.dialog.VehicleDialogFragment;
import com.google.mapsplatform.transportation.sample.driver.state.TripStatus;
import java.net.ConnectException;
import java.util.List;
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
  private TextView tripIdText;
  private TextView matchedTripIdsText;
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
    tripIdText = findViewById(R.id.trip_id_label);
    matchedTripIdsText = findViewById(R.id.matched_trip_ids_label);
    textVehicleId = findViewById(R.id.menu_vehicle_id);
    textVehicleId.setText(vehicleIdStore.readOrDefault());
    setupNavFragment();
    updateActionButton(TRIP_VIEW_INITIAL_STATE, null);
    actionButton.setOnClickListener(this::onActionButtonClicked);

    showTripId(VehicleController.NO_TRIP_ID);
    showMatchedTripIds(ImmutableList.of());

    // Ensure the screen stays on during navigation.
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    initializeSDKs();

    Log.i(TAG, "Driver SDK version: " + RidesharingDriverApi.getDriverSdkVersion());
    Log.i(TAG, "Navigation SDK version: " + NavigationApi.getNavSDKVersion());
  }

  private void initializeSDKs() {
    NavigationApi.getNavigator(
        this,
        new NavigationApi.NavigatorListener() {
          @Override
          public void onNavigatorReady(Navigator navigator) {
            Application app = (Application) getApplicationContext();
            vehicleController =
                new VehicleController(getApplication(), navigator, executor, MainActivity.this);
            vehicleController.setPresenter(MainActivity.this);
            initVehicleAndPollTrip();
          }

          @Override
          public void onError(@NavigationApi.ErrorCode int errorCode) {
            logNavigationApiInitError(errorCode);
          }
        });
  }

  private void initVehicleAndPollTrip() {
    Application app = (Application) getApplicationContext();
    ListenableFuture<Boolean> future = vehicleController.initVehicleAndReporter(app);
    Futures.addCallback(
        future,
        new FutureCallback<Boolean>() {
          @Override
          public void onSuccess(Boolean result) {}

          @Override
          public void onFailure(Throwable e) {
            if (e instanceof ConnectException) {
              Snackbar.make(
                      tripCard, R.string.msg_provider_connection_error, Snackbar.LENGTH_INDEFINITE)
                  .setAction(R.string.button_retry, view -> initVehicleAndPollTrip())
                  .show();
            }
          }
        },
        executor);
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
            vehicleController.getVehicleSettings(),
            updatedVehicleSettings -> {
              textVehicleId.setText(updatedVehicleSettings.getVehicleId());

              vehicleController.updateVehicleSettings(
                  (Application) getApplicationContext(), updatedVehicleSettings);
            });

    fragment.show(getSupportFragmentManager(), "VehicleInfoDialog");
  }

  private void onActionButtonClicked(View view) {
    vehicleController.processNextState();
  }

  // Permissions are managed by the 'SplashScreenActivity'
  @SuppressLint("MissingPermission")
  private void updateCameraPerspective(Boolean isTilted) {
    navFragment.getMapAsync(
        googleMap ->
            googleMap.followMyLocation(
                isTilted ? CameraPerspective.TILTED : CameraPerspective.TOP_DOWN_NORTH_UP));
  }

  private void updateActionButton(Integer visibility, @Nullable @StringRes Integer resourceId) {
    if (resourceId != null) {
      actionButton.setText(resourceId);
    }

    actionButton.setVisibility(visibility);
  }

  @Override
  public void showTripId(String tripId) {
    if (!tripId.equals(VehicleController.NO_TRIP_ID)) {
      tripIdText.setText(getResources().getString(R.string.trip_id_label, tripId));
      return;
    }

    String noTripFoundText = getResources().getString(R.string.status_unknown);
    tripIdText.setText(getResources().getString(R.string.trip_id_label, noTripFoundText));
  }

  @Override
  public void showMatchedTripIds(List<String> tripIds) {
    matchedTripIdsText.setVisibility(View.VISIBLE);

    if (tripIds.isEmpty()) {
      String noTripFoundText = getResources().getString(R.string.status_unknown);

      matchedTripIdsText.setText(
          getResources().getString(R.string.matched_trip_ids_label, noTripFoundText));

      return;
    }

    String text = Joiner.on(", ").join(tripIds);

    matchedTripIdsText.setText(getResources().getString(R.string.matched_trip_ids_label, text));
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
        cardVisibility = View.VISIBLE;
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
        resourceId =
            vehicleController.isNextCurrentTripWaypointIntermediate()
                ? R.string.button_enroute_to_intermediate_stop
                : R.string.button_enroute_to_dropoff;
        isCameraTilted = true;
        break;

      case ENROUTE_TO_DROPOFF:
        simulationStatusText.setText(R.string.status_enroute_to_dropoff);
        resourceId = R.string.button_trip_complete;
        isCameraTilted = true;
        break;

      case ENROUTE_TO_INTERMEDIATE_DESTINATION:
        simulationStatusText.setText(R.string.status_enroute_to_intermediate_location);
        resourceId = R.string.button_arrived_at_intermediate_stop;
        isCameraTilted = true;
        break;

      case ARRIVED_AT_INTERMEDIATE_DESTINATION:
        simulationStatusText.setText(R.string.status_arrived_to_intermediate_location);
        resourceId =
            !vehicleController.isNextCurrentTripWaypointIntermediate()
                ? R.string.button_enroute_to_dropoff
                : R.string.button_enroute_to_intermediate_stop;
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

  @Override
  public void enableActionButton(boolean enabled) {
    actionButton.setEnabled(enabled);
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

  @Override
  protected void onDestroy() {
    vehicleController.cleanUp();

    super.onDestroy();
  }
}
