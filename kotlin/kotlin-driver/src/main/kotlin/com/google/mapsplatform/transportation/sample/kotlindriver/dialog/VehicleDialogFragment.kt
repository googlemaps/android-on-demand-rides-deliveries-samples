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
package com.google.mapsplatform.transportation.sample.kotlindriver.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.google.mapsplatform.transportation.sample.kotlindriver.R

/** A dialog to specify vehicle settings. */
class VehicleDialogFragment
private constructor(
  /** Initial vehicle id. */
  private val vehicleId: String,
  /** Optional callback to be run when an action button is clicked. */
  private val listener: OnDialogResultListener
) : DialogFragment() {
  /** Listener interface for dialog result callbacks. */
  interface OnDialogResultListener {
    /**
     * Runs when a dialog action button is clicked.
     *
     * @param vehicleId the vehicle id input value.
     */
    fun onResult(vehicleId: String)
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val inflater = requireActivity().layoutInflater
    val view = inflater.inflate(R.layout.vehicle_info_dialog, null)
    val editText = view.findViewById<EditText>(R.id.vehicle_id)
    editText.setText(vehicleId)
    return AlertDialog.Builder(activity)
      .setView(view)
      .setPositiveButton(R.string.save_label) { dialog: DialogInterface?, id: Int ->
        val newVehicleId = editText.text.toString()
        if (!TextUtils.isEmpty(newVehicleId)) {
          listener.onResult(newVehicleId)
        }
      }
      .setNegativeButton(R.string.cancel_label) { dialog: DialogInterface?, id: Int -> }
      .create()
  }

  companion object {
    private val TAG = VehicleDialogFragment::class.java.name
    fun newInstance(vehicleId: String, listener: OnDialogResultListener): VehicleDialogFragment {
      return VehicleDialogFragment(vehicleId, listener)
    }
  }
}
