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

import android.Manifest.permission
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.util.ArrayList

/** Class containing utility methods around permission management. */
object PermissionUtils {
  // Runtime permissions required by the app to function properly.
  private val REQUIRED_PERMISSIONS =
    arrayOf(
      permission.ACCESS_FINE_LOCATION,
      permission.WRITE_EXTERNAL_STORAGE,
      permission.READ_PHONE_STATE
    )

  /**
   * Only needed for OS12 devices. Api level 30< uses 'permission.BLUETOOTH' which has
   * 'dangerousLevel' 'normal' and its automatically approved.
   */
  private val REQUIRED_PERMISSIONS_OS_12PLUS =
    arrayOf(
      permission.ACCESS_FINE_LOCATION,
      permission.WRITE_EXTERNAL_STORAGE,
      permission.READ_PHONE_STATE,
      permission.BLUETOOTH_CONNECT
    )

  fun getPermissionsToRequest(context: Context?): Array<String> {
    val requiredPermissions =
      if (Build.VERSION.SDK_INT >= 31) REQUIRED_PERMISSIONS_OS_12PLUS else REQUIRED_PERMISSIONS
    val permissionsToRequest: MutableList<String> = ArrayList()
    for (permissionToCheck in requiredPermissions) {
      if (ContextCompat.checkSelfPermission(context!!, permissionToCheck) !=
          PackageManager.PERMISSION_GRANTED
      ) {
        permissionsToRequest.add(permissionToCheck)
        /**
         * Although 'Driver' requires 'ACCESS_FINE_LOCATION', we need to request
         * 'ACCESS_COARSE_LOCATION' as well as some Android 12 builds ignore the permission request
         * if both aren't requested in the single runtime check. Please see:
         * https://developer.android.com/training/location/permissions#approximate-request
         */
        if (permissionToCheck == permission.ACCESS_FINE_LOCATION) {
          permissionsToRequest.add(permission.ACCESS_COARSE_LOCATION)
        }
      }
    }
    return permissionsToRequest.toTypedArray()
  }
}
