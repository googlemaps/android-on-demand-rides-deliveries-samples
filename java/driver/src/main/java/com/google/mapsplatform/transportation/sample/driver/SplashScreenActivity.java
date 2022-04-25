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

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

/** Activity for the splash screen shown upon app initialization. */
public class SplashScreenActivity extends AppCompatActivity {

  private static final int APP_PERMISSION_REQUEST_CODE = 99;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    String[] permissionsToRequest = PermissionUtils.getPermissionsToRequest(this);

    if (permissionsToRequest.length == 0) {
      loadMainActivity();
    } else {
      ActivityCompat.requestPermissions(this, permissionsToRequest, APP_PERMISSION_REQUEST_CODE);
    }
  }

  /** Determine if the needed permissions are granted. If not then alert user. */
  @Override
  public void onRequestPermissionsResult(
      int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (requestCode != APP_PERMISSION_REQUEST_CODE) {
      return;
    }

    String[] permissionsToRequest = PermissionUtils.getPermissionsToRequest(this);

    if (permissionsToRequest.length == 0) {
      loadMainActivity();
    } else {
      String missingPermissionsText = String.join(", ", permissionsToRequest);

      new AlertDialog.Builder(this)
          .setTitle("")
          .setMessage(
              getResources().getString(R.string.msg_require_permissions, missingPermissionsText))
          .setPositiveButton(
              android.R.string.ok,
              (DialogInterface dialog, int which) -> {
                dialog.dismiss();
                ActivityCompat.requestPermissions(
                    this, permissionsToRequest, APP_PERMISSION_REQUEST_CODE);
              })
          .create()
          .show();
    }
  }

  private void loadMainActivity() {
    startActivity(new Intent(this, MainActivity.class));
    finish();
  }
}
