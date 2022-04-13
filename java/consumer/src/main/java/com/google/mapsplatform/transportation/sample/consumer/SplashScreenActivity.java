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

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

/** Activity for the splash screen shown upon app initialization. */
public class SplashScreenActivity extends AppCompatActivity {

  private static final int REQUEST_LOCATION_PERMISSION_CODE = 99;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Check for location permissions, prompting if not granted.
    if (!PermissionUtils.hasRequiredRuntimePermissions(this)) {
      ActivityCompat.requestPermissions(
          this, PermissionUtils.PERMISSIONS_TO_REQUEST, REQUEST_LOCATION_PERMISSION_CODE);
    } else {
      loadMainActivity();
    }
  }

  /** Determine if the needed permissions are granted. If not then alert user. */
  @Override
  public void onRequestPermissionsResult(
      int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (requestCode != REQUEST_LOCATION_PERMISSION_CODE) {
      return;
    }

    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      loadMainActivity();
      return;
    }

    // Tell the user that we need location permissions.
    new AlertDialog.Builder(this)
        .setTitle("")
        .setMessage(R.string.msg_require_permissions)
        .setPositiveButton(
            android.R.string.ok,
            (dialogInterface, i) -> {
              dialogInterface.dismiss();
              ActivityCompat.requestPermissions(
                  this, PermissionUtils.PERMISSIONS_TO_REQUEST, REQUEST_LOCATION_PERMISSION_CODE);
            })
        .create()
        .show();
  }

  private void loadMainActivity() {
    startActivity(new Intent(this, SampleAppActivity.class));
    finish();
  }
}
