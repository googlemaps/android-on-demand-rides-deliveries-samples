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
package com.google.mapsplatform.transportation.sample.kotlindriver

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

/** Stores vehicleId on disk. */
class VehicleIdStore(context: Context) {
  private val sharedPrefs: SharedPreferences =
    context.getSharedPreferences(
      context.getString(R.string.shared_preference_key),
      Context.MODE_PRIVATE
    )

  /** Saves vehicle Id on disk. */
  @SuppressLint("ApplySharedPref")
  fun save(vehicleId: String?) {
    sharedPrefs.edit().putString(KEY_VEHICLE_ID, vehicleId).commit()
  }

  /** Reads vehicle Id on disk or return default "Vehicle_1" if absent */
  fun readOrDefault(): String {
    return sharedPrefs.getString(KEY_VEHICLE_ID, DEFAULT_VEHICLE_ID)!!
  }

  companion object {
    private const val KEY_VEHICLE_ID = "VEHICLE_ID"
    private const val DEFAULT_VEHICLE_ID = "Vehicle_1"
  }
}
