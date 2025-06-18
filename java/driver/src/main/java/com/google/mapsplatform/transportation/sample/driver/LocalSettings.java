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

import android.content.Context;
import android.content.SharedPreferences;

/** Manages application settings that can persist across sessions. */
public final class LocalSettings {

  private static final String KEY_VEHICLE_ID = "VEHICLE_ID";
  private static final String DEFAULT_VEHICLE_ID = "Vehicle_1";

  private static final String KEY_IS_SIMULATION_ENABLED = "IS_SIMULATION_ENABLED";
  private static final boolean DEFAULT_IS_SIMULATION_ENABLED = true;

  private final SharedPreferences sharedPrefs;

  /**
   * Default constructor for {@link LocalSettings}.
   *
   * @param context the activity context which will be used to access preferences.
   */
  public LocalSettings(Context context) {
    sharedPrefs =
        context.getSharedPreferences(
            context.getString(R.string.shared_preference_key), Context.MODE_PRIVATE);
  }

  /**
   * Saves the "vehicleId" setting".
   *
   * @param vehicleId the value of "vehicleId" to store.
   */
  public void saveVehicleId(String vehicleId) {
    sharedPrefs.edit().putString(KEY_VEHICLE_ID, vehicleId).commit();
  }

  /** Gets the stored "vehicleId" setting. In case there is not one, it returns a default. */
  public String getVehicleId() {
    return sharedPrefs.getString(KEY_VEHICLE_ID, DEFAULT_VEHICLE_ID);
  }

  /**
   * Saves the "is simulation enabled" setting".
   *
   * @param isSimulationEnabled the value of "is simulation enabled" to store.
   */
  public void saveIsSimulationEnabled(boolean isSimulationEnabled) {
    sharedPrefs.edit().putBoolean(KEY_IS_SIMULATION_ENABLED, isSimulationEnabled).commit();
  }

  /**
   * Gets the stored "is simulation enabled" setting. In case there is not one, it returns a
   * default.
   */
  public boolean getIsSimulationEnabled() {
    return sharedPrefs.getBoolean(KEY_IS_SIMULATION_ENABLED, DEFAULT_IS_SIMULATION_ENABLED);
  }
}
