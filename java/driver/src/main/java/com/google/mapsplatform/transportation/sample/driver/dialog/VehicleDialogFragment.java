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
package com.google.mapsplatform.transportation.sample.driver.dialog;

import static com.google.mapsplatform.transportation.sample.driver.utils.TripUtils.EXCLUSIVE_TRIP_TYPE;
import static com.google.mapsplatform.transportation.sample.driver.utils.TripUtils.SHARED_TRIP_TYPE;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.google.common.collect.ImmutableList;
import com.google.mapsplatform.transportation.sample.driver.LocalSettings;
import com.google.mapsplatform.transportation.sample.driver.R;
import com.google.mapsplatform.transportation.sample.driver.provider.request.VehicleSettings;
import java.util.Arrays;

/** A dialog to specify vehicle settings. */
public class VehicleDialogFragment extends DialogFragment {

  /** Listener interface for dialog result callbacks. */
  public interface OnDialogResultListener {

    /**
     * Runs when a dialog action button is clicked.
     *
     * @param vehicleSettings the updated vehicle settings.
     */
    void onResult(VehicleSettings vehicleSettings);
  }

  private static final String TAG = VehicleDialogFragment.class.getName();

  private final LocalSettings localSettings;

  private final VehicleSettings vehicleSettings;

  /** Optional callback to be run when an action button is clicked. */
  private final OnDialogResultListener listener;

  private VehicleDialogFragment(
      LocalSettings localSettings,
      VehicleSettings vehicleSettings,
      OnDialogResultListener listener) {
    this.localSettings = localSettings;
    this.vehicleSettings = vehicleSettings;
    this.listener = listener;
  }

  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    LayoutInflater inflater = requireActivity().getLayoutInflater();
    View view = inflater.inflate(R.layout.vehicle_info_dialog, null);

    EditText editText = view.findViewById(R.id.vehicle_id);
    editText.setText(localSettings.getVehicleId());

    Spinner vehicleCapacitySpinner = view.findViewById(R.id.vehicle_capacity_spinner);

    vehicleCapacitySpinner.setAdapter(
        ArrayAdapter.createFromResource(
            getContext(),
            R.array.vehicle_capacities_array,
            android.R.layout.simple_spinner_dropdown_item));

    int maximumCapacityValue = vehicleSettings.getMaximumCapacity();

    vehicleCapacitySpinner.setSelection(
        getVehicleCapacityIndex(getContext().getResources(), maximumCapacityValue), true);

    CheckBox backToBackEnabledCheckBox = view.findViewById(R.id.back_to_back_checkbox);
    boolean backToBackEnabledValue = vehicleSettings.getBackToBackEnabled();

    backToBackEnabledCheckBox.setChecked(backToBackEnabledValue);

    CheckBox sharedTripTypeCheckbox = view.findViewById(R.id.trip_type_shared);
    CheckBox exclusiveTripTypeCheckbox = view.findViewById(R.id.trip_type_exclusive);

    boolean sharedTripTypeEnabled = vehicleSettings.getSupportedTripTypes().contains("SHARED");
    boolean exclusiveTripTypeEnabled =
        vehicleSettings.getSupportedTripTypes().contains("EXCLUSIVE");

    sharedTripTypeCheckbox.setChecked(sharedTripTypeEnabled);
    exclusiveTripTypeCheckbox.setChecked(exclusiveTripTypeEnabled);

    CheckBox simulationEnabledCheckbox = view.findViewById(R.id.simulate_locations_checkbox);
    simulationEnabledCheckbox.setChecked(localSettings.getIsSimulationEnabled());

    return new AlertDialog.Builder(getActivity())
        .setView(view)
        .setPositiveButton(
            R.string.save_label,
            (dialog, id) -> {
              String newVehicleId = editText.getText().toString();

              if (!TextUtils.isEmpty(newVehicleId)) {
                int maximumCapacity =
                    Integer.parseInt(String.valueOf(vehicleCapacitySpinner.getSelectedItem()));

                boolean backToBackEnabled = backToBackEnabledCheckBox.isChecked();

                ImmutableList.Builder<String> supportedTripTypes = ImmutableList.builder();
                if (sharedTripTypeCheckbox.isChecked()) {
                  supportedTripTypes.add(SHARED_TRIP_TYPE);
                }

                if (exclusiveTripTypeCheckbox.isChecked()) {
                  supportedTripTypes.add(EXCLUSIVE_TRIP_TYPE);
                }

                VehicleSettings settings =
                    new VehicleSettings(
                        newVehicleId,
                        backToBackEnabled,
                        maximumCapacity,
                        supportedTripTypes.build());

                listener.onResult(settings);
                localSettings.saveIsSimulationEnabled(simulationEnabledCheckbox.isChecked());
              }
            })
        .setNegativeButton(R.string.cancel_label, (dialog, id) -> {})
        .create();
  }

  /**
   * Creates an instance of a {@link VehicleDialogFragment}.
   *
   * @param localSettings the user's local settings.
   * @param vehicleSettings the vehicle's remote settings.
   * @param listener gets notified when vehicle has been updated.
   */
  public static VehicleDialogFragment newInstance(
      LocalSettings localSettings,
      VehicleSettings vehicleSettings,
      OnDialogResultListener listener) {
    return new VehicleDialogFragment(localSettings, vehicleSettings, listener);
  }

  // Returns the index in the Vehicle Capacities array of the saved vehicle capacity.
  private static int getVehicleCapacityIndex(Resources resources, int maximumCapacity) {
    return Arrays.asList(resources.getStringArray(R.array.vehicle_capacities_array))
        .indexOf(String.valueOf(maximumCapacity));
  }
}
