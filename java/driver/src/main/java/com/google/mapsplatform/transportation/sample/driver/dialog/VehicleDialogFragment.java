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
package com.google.mapsplatform.transportation.sample.driver.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.google.mapsplatform.transportation.sample.driver.R;

/** A dialog to specify vehicle settings. */
public class VehicleDialogFragment extends DialogFragment {

  /** Listener interface for dialog result callbacks. */
  public interface OnDialogResultListener {

    /**
     * Runs when a dialog action button is clicked.
     *
     * @param vehicleId the vehicle id input value.
     */
    void onResult(String vehicleId);
  }

  private static final String TAG = VehicleDialogFragment.class.getName();

  /** Initial vehicle id. */
  @Nullable private final String vehicleId;
  /** Optional callback to be run when an action button is clicked. */
  private final OnDialogResultListener listener;

  private VehicleDialogFragment(@Nullable String vehicleId, OnDialogResultListener listener) {
    this.vehicleId = vehicleId;
    this.listener = listener;
  }

  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    LayoutInflater inflater = requireActivity().getLayoutInflater();
    View view = inflater.inflate(R.layout.vehicle_info_dialog, null);
    EditText editText = view.findViewById(R.id.vehicle_id);
    editText.setText(vehicleId);
    return new AlertDialog.Builder(getActivity())
        .setView(view)
        .setPositiveButton(
            R.string.save_label,
            (dialog, id) -> {
              String newVehicleId = editText.getText().toString();
              if (!TextUtils.isEmpty(newVehicleId)) {
                listener.onResult(newVehicleId);
              }
            })
        .setNegativeButton(R.string.cancel_label, (dialog, id) -> {})
        .create();
  }

  public static VehicleDialogFragment newInstance(
      @Nullable String vehicleId, OnDialogResultListener listener) {
    return new VehicleDialogFragment(vehicleId, listener);
  }
}
