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
package com.google.mapsplatform.transportation.sample.consumer;

import android.Manifest.permission;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

/** Class containing utility methods around permission management. */
public final class PermissionUtils {
  public static final String[] PERMISSIONS_TO_REQUEST =
      new String[] {permission.ACCESS_FINE_LOCATION, permission.ACCESS_COARSE_LOCATION}; // Any of

  /**
   * Consumer App works properly with either FINE or COARSE location as it's just required to
   * capture initial location. Then user moves pickup location manually.
   */
  public static boolean hasRequiredRuntimePermissions(Context context) {
    return ContextCompat.checkSelfPermission(context, permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(context, permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
  }
}
