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

import android.content.Context;
import android.content.SharedPreferences;

/** Stores vehicleId on disk. */
public class VehicleIdStore {

  private static final String KEY_VEHICLE_ID = "VEHICLE_ID";
  private static final String DEFAULT_VEHICLE_ID = "Vehicle_1";

  private final SharedPreferences sharedPrefs;

  public VehicleIdStore(Context context) {
    sharedPrefs =
        context.getSharedPreferences(
            context.getString(R.string.shared_preference_key), Context.MODE_PRIVATE);
  }

  /** Saves vehicle Id on disk. */
  public void save(String vehicleId) {
    sharedPrefs.edit().putString(KEY_VEHICLE_ID, vehicleId).commit();
  }

  /** Reads vehicle Id on disk or return default "Vehicle_1" if absent */
  public String readOrDefault() {
    return sharedPrefs.getString(KEY_VEHICLE_ID, DEFAULT_VEHICLE_ID);
  }
}
