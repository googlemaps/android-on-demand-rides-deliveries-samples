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
package com.google.mapsplatform.transportation.sample.kotlinconsumer

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

/** Activity for the splash screen shown upon app initialization. */
class SplashScreenActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Check for location permissions, prompting if not granted.
    if (!PermissionUtils.hasRequiredRuntimePermissions(this)) {
      ActivityCompat.requestPermissions(
        this,
        PermissionUtils.PERMISSIONS_TO_REQUEST,
        REQUEST_LOCATION_PERMISSION_CODE
      )
    } else {
      loadMainActivity()
    }
  }

  /** Determine if the needed permissions are granted. If not then alert user. */
  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode != REQUEST_LOCATION_PERMISSION_CODE) {
      return
    }
    if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      loadMainActivity()
      return
    }

    // Tell the user that we need location permissions.
    AlertDialog.Builder(this)
      .setTitle("")
      .setMessage(R.string.msg_require_permissions)
      .setPositiveButton(android.R.string.ok) { dialogInterface: DialogInterface, i: Int ->
        dialogInterface.dismiss()
        ActivityCompat.requestPermissions(
          this,
          PermissionUtils.PERMISSIONS_TO_REQUEST,
          REQUEST_LOCATION_PERMISSION_CODE
        )
      }
      .create()
      .show()
  }

  private fun loadMainActivity() {
    startActivity(Intent(this, SampleAppActivity::class.java))
    finish()
  }

  companion object {
    private const val REQUEST_LOCATION_PERMISSION_CODE = 99
  }
}
