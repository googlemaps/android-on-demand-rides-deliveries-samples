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

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

/** Activity for the splash screen shown upon app initialization. */
class SplashScreenActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val permissionsToRequest = PermissionUtils.getPermissionsToRequest(this)
    if (permissionsToRequest.isEmpty()) {
      loadMainActivity()
    } else {
      ActivityCompat.requestPermissions(this, permissionsToRequest, APP_PERMISSION_REQUEST_CODE)
    }
  }

  /** Determine if the needed permissions are granted. If not then alert user. */
  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode != APP_PERMISSION_REQUEST_CODE) {
      return
    }
    val permissionsToRequest = PermissionUtils.getPermissionsToRequest(this)
    if (permissionsToRequest.isEmpty()) {
      loadMainActivity()
    } else {
      val missingPermissionsText = permissionsToRequest.joinToString(separator = ", ")
      AlertDialog.Builder(this)
        .setTitle("")
        .setMessage(resources.getString(R.string.msg_require_permissions, missingPermissionsText))
        .setPositiveButton(android.R.string.ok) { dialog: DialogInterface, _: Int ->
          dialog.dismiss()
          ActivityCompat.requestPermissions(this, permissionsToRequest, APP_PERMISSION_REQUEST_CODE)
        }
        .create()
        .show()
    }
  }

  private fun loadMainActivity() {
    startActivity(Intent(this, MainActivity::class.java))
    finish()
  }

  companion object {
    private const val APP_PERMISSION_REQUEST_CODE = 99
  }
}
