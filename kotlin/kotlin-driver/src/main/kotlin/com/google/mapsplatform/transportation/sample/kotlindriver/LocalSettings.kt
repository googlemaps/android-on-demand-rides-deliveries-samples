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
import android.content.Context
import android.content.SharedPreferences

/**
 * Manages application settings that can persist across sessions.
 *
 * @property context the activity context which will be used to access preferences.
 */
@SuppressLint("ApplySharedPref")
class LocalSettings(context: Context) {
  private val sharedPrefs: SharedPreferences =
    context.getSharedPreferences(
      context.getString(R.string.shared_preference_key),
      Context.MODE_PRIVATE
    )

  /**
   * Saves the "vehicleId" setting".
   *
   * @param vehicleId the value of "vehicleId" to store.
   */
  fun saveVehicleId(vehicleId: String?) {
    sharedPrefs.edit().putString(KEY_VEHICLE_ID, vehicleId).commit()
  }

  /** Gets the stored "vehicleId" setting. In case there is not one, it returns a default. */
  fun getVehicleId(): String =
    sharedPrefs.getString(KEY_VEHICLE_ID, DEFAULT_VEHICLE_ID) ?: DEFAULT_VEHICLE_ID

  /**
   * Saves the "is simulation enabled" setting".
   *
   * @param isSimulationEnabled the value of "is simulation enabled" to store.
   */
  fun saveIsSimulationEnabled(isSimulationEnabled: Boolean) {
    sharedPrefs.edit().putBoolean(KEY_IS_SIMULATION_ENABLED, isSimulationEnabled).commit()
  }

  /**
   * Gets the stored "is simulation enabled" setting. In case there is not one, it returns a
   * default.
   */
  fun getIsSimulationEnabled(): Boolean =
    sharedPrefs.getBoolean(KEY_IS_SIMULATION_ENABLED, DEFAULT_IS_SIMULATION_ENABLED)

  private companion object {
    const val KEY_VEHICLE_ID = "VEHICLE_ID"
    const val DEFAULT_VEHICLE_ID = "Vehicle_1"

    const val KEY_IS_SIMULATION_ENABLED = "IS_SIMULATION_ENABLED"
    const val DEFAULT_IS_SIMULATION_ENABLED = true
  }
}
