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

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.CameraPerspective
import com.google.android.libraries.mapsplatform.transportation.driver.api.ridesharing.RidesharingDriverApi
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.NavigationApi.NavigatorListener
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.SupportNavigationFragment
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.mapsplatform.transportation.sample.kotlindriver.dialog.VehicleDialogFragment
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.ProviderUtils
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.request.VehicleSettings
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.service.LocalProviderService
import com.google.mapsplatform.transportation.sample.kotlindriver.state.TripStatus
import java.net.ConnectException
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

/*
 * NOTE: BEFORE BUILDING THIS APPLICATION YOU MUST COPY THE GOOGLE NAVIGATION API
 * ANDROID LIBRARY (AAR FILE) INTO THE libs/ DIRECTORY OF THIS APP.
 */
/** Main activity for the DriverSDK test app. */
class MainActivity : AppCompatActivity(), Presenter {
  private val executor = Executors.newCachedThreadPool()
  private lateinit var localSettings: LocalSettings
  private lateinit var navFragment: SupportNavigationFragment
  private lateinit var simulationStatusText: TextView
  private lateinit var tripIdText: TextView
  private lateinit var matchedTripIdsText: TextView
  private lateinit var textVehicleId: TextView
  private lateinit var actionButton: Button
  private lateinit var tripCard: CardView
  private lateinit var vehicleController: VehicleController

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    localSettings = LocalSettings(this)
    simulationStatusText = findViewById(R.id.simulation_status)
    actionButton = findViewById(R.id.action_button)
    val editVehicleIdButton = findViewById<Button>(R.id.edit_button)
    editVehicleIdButton.setOnClickListener { onEditVehicleButtonClicked() }
    tripCard = findViewById(R.id.trip_card)
    tripIdText = findViewById(R.id.trip_id_label)
    matchedTripIdsText = findViewById(R.id.matched_trip_ids_label)
    textVehicleId = findViewById(R.id.menu_vehicle_id)
    textVehicleId.text = localSettings.getVehicleId()
    setupNavFragment()
    updateActionButton(TRIP_VIEW_INITIAL_STATE, null)
    actionButton.setOnClickListener { onActionButtonClicked() }
    showTripId(VehicleController.NO_TRIP_ID)
    showMatchedTripIds(emptyList())
    // Ensure the screen stays on during navigation.
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    initializeSDKs()
    Log.i(TAG, "Driver SDK version: " + RidesharingDriverApi.getDriverSdkVersion())
    Log.i(TAG, "Navigation SDK version: " + NavigationApi.getNavSDKVersion())
  }

  private fun initializeSDKs() {
    NavigationApi.getNavigator(
      this,
      object : NavigatorListener {
        override fun onNavigatorReady(navigator: Navigator) {
          vehicleController =
            VehicleController(
              navigator,
              executor,
              context = this@MainActivity,
              coroutineScope = this@MainActivity.lifecycleScope,
              providerService =
                LocalProviderService(
                  LocalProviderService.createRestProvider(
                    ProviderUtils.getProviderBaseUrl(application)
                  ),
                ),
              localSettings,
            )
          vehicleController.setPresenter(this@MainActivity)
          initVehicleAndPollTrip()
        }

        override fun onError(@NavigationApi.ErrorCode errorCode: Int) {
          logNavigationApiInitError(errorCode)
        }
      }
    )
  }

  private fun initVehicleAndPollTrip() {
    lifecycleScope.launch {
      try {
        vehicleController.initVehicleAndReporter(application)
      } catch (e: ConnectException) {
        Snackbar.make(
            tripCard,
            R.string.msg_provider_connection_error,
            BaseTransientBottomBar.LENGTH_INDEFINITE
          )
          .setAction(R.string.button_retry) { initVehicleAndPollTrip() }
          .show()
      }
    }
  }

  private fun setupNavFragment() {
    navFragment = SupportNavigationFragment.newInstance()
    supportFragmentManager
      .beginTransaction()
      .add(R.id.nav_fragment_frame, navFragment, null)
      .setReorderingAllowed(true)
      .commit()
  }

  private fun onEditVehicleButtonClicked() {
    val fragment: VehicleDialogFragment =
      VehicleDialogFragment.newInstance(
        localSettings,
        vehicleController.vehicleSettings,
        object : VehicleDialogFragment.OnDialogResultListener {
          override fun onResult(vehicleSettings: VehicleSettings) {
            textVehicleId.text = vehicleSettings.vehicleId
            localSettings.saveVehicleId(vehicleSettings.vehicleId)

            lifecycleScope.launch {
              vehicleController.updateVehicleSettings(application, vehicleSettings)
            }
          }
        }
      )
    fragment.show(supportFragmentManager, "VehicleInfoDialog")
  }

  private fun onActionButtonClicked() {
    vehicleController.processNextState()
  }

  // Permissions are managed by the 'SplashScreenActivity'
  @SuppressLint("MissingPermission")
  private fun updateCameraPerspective(isTilted: Boolean) {
    navFragment.getMapAsync { googleMap: GoogleMap ->
      googleMap.followMyLocation(
        if (isTilted) CameraPerspective.TILTED else CameraPerspective.TOP_DOWN_NORTH_UP
      )
    }
  }

  private fun updateActionButton(visibility: Int, @StringRes resourceId: Int?) {
    resourceId?.also { actionButton.setText(it) }
    actionButton.visibility = visibility
  }

  override fun showTripId(tripId: String) {
    if (tripId != VehicleController.NO_TRIP_ID) {
      tripIdText.text = resources.getString(R.string.trip_id_label, tripId)
      return
    }
    val noTripFoundText = resources.getString(R.string.status_unknown)
    tripIdText.text = resources.getString(R.string.trip_id_label, noTripFoundText)
  }

  override fun showMatchedTripIds(tripIds: List<String>) {
    val text =
      if (tripIds.isEmpty()) resources.getString(R.string.status_unknown)
      else tripIds.joinToString()

    matchedTripIdsText.text = resources.getString(R.string.matched_trip_ids_label, text)
  }

  override fun showTripStatus(status: TripStatus) {
    var resourceId = R.string.status_idle
    var buttonVisibility = View.VISIBLE
    var cardVisibility = View.VISIBLE

    // Determines whether the camera perspective should be tilted.
    var isCameraTilted = false
    when (status) {
      TripStatus.UNKNOWN_TRIP_STATUS -> {
        buttonVisibility = TRIP_VIEW_INITIAL_STATE
        cardVisibility = View.VISIBLE
        simulationStatusText.setText(resourceId)
      }
      TripStatus.NEW -> {
        simulationStatusText.setText(R.string.status_new)
        resourceId = R.string.button_start_trip
      }
      TripStatus.ENROUTE_TO_PICKUP -> {
        simulationStatusText.setText(R.string.status_enroute_to_pickup)
        resourceId = R.string.button_arrived_at_pickup
        isCameraTilted = true
      }
      TripStatus.ARRIVED_AT_PICKUP -> {
        simulationStatusText.setText(R.string.status_arrived_at_pickup)
        resourceId =
          if (vehicleController.isNextCurrentTripWaypointIntermediate()) {
            R.string.button_enroute_to_intermediate_stop
          } else R.string.button_enroute_to_dropoff
        isCameraTilted = true
      }
      TripStatus.ENROUTE_TO_DROPOFF -> {
        simulationStatusText.setText(R.string.status_enroute_to_dropoff)
        resourceId = R.string.button_trip_complete
        isCameraTilted = true
      }
      TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION -> {
        simulationStatusText.setText(R.string.status_enroute_to_intermediate_location)
        resourceId = R.string.button_arrived_at_intermediate_stop
        isCameraTilted = true
      }
      TripStatus.ARRIVED_AT_INTERMEDIATE_DESTINATION -> {
        simulationStatusText.setText(R.string.status_arrived_to_intermediate_location)
        resourceId =
          if (vehicleController.isNextCurrentTripWaypointIntermediate()) {
            R.string.button_enroute_to_intermediate_stop
          } else R.string.button_enroute_to_dropoff
        isCameraTilted = true
      }
      TripStatus.COMPLETE -> {
        resourceId = R.string.status_complete
        buttonVisibility = View.GONE
        simulationStatusText.setText(resourceId)
      }
      TripStatus.CANCELED -> {
        resourceId = R.string.status_canceled
        buttonVisibility = View.GONE
        simulationStatusText.setText(resourceId)
      }
    }
    tripCard.visibility = cardVisibility
    updateActionButton(buttonVisibility, resourceId)
    updateCameraPerspective(isCameraTilted)
  }

  override fun enableActionButton(enabled: Boolean) {
    actionButton.isEnabled = enabled
  }

  override fun onDestroy() {
    vehicleController.cleanUp()
    super.onDestroy()
  }

  companion object {
    private const val TAG = "MainActivity"
    private const val TRIP_VIEW_INITIAL_STATE = View.GONE
    private fun logNavigationApiInitError(errorCode: Int) {
      when (errorCode) {
        NavigationApi.ErrorCode.NOT_AUTHORIZED ->
          Log.w(
            TAG,
            "Note: If this message is displayed, you may need to check that your API_KEY is" +
              " specified correctly in AndroidManifest.xml and is been enabled to" +
              " access the Navigation API."
          )
        NavigationApi.ErrorCode.TERMS_NOT_ACCEPTED ->
          Log.w(
            TAG,
            "Error loading Navigation API: User did not accept the Navigation Terms of Use."
          )
        else -> Log.w(TAG, "Error loading Navigation API: $errorCode")
      }
    }
  }
}
