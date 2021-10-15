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
package com.google.mapsplatform.transportation.sample.provider;

import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;

/** Provides local provider related utilities. */
public class ProviderUtils {
  private static final String TAG = "ProviderUtils";
  private static final String PROVIDER_ID_KEY = "com.google.mapsplatform.transportation.sample.provider_id";
  private static final String PROVIDER_URL_KEY = "com.google.mapsplatform.transportation.sample.provider_url";

  /**
   * Gets provider Id to communicate to provider server.
   *
   * @return providerId.
   * @throws IllegalStateException if providerId can't be obtained. This stops the sample app from
   *     functioning correctly.
   * @throws NullPointerException if manifest metadata doesn't have providerId info.
   */
  public static String getProviderId(Context context) {
    Bundle metadata = getAppMetadata(context);
    if (!metadata.containsKey(PROVIDER_ID_KEY)) {
      throw new IllegalStateException("App metadata in manifest does not contain provider Id.");
    }
    return requireNonNull(metadata.getString(PROVIDER_ID_KEY));
  }

  /**
   * Gets provider base URL to communicate to provider server.
   *
   * @return providerId.
   * @throws IllegalStateException if provider base URL can't be obtained. This stops the sample app
   *     from functioning correctly.
   * @throws NullPointerException if manifest metadata doesn't have provider base URL info.
   */
  public static String getProviderBaseUrl(Context context) {
    Bundle metadata = getAppMetadata(context);
    if (!metadata.containsKey(PROVIDER_URL_KEY)) {
      throw new IllegalStateException(
          "App metadata in manifest does not contain provider base URL.");
    }
    return requireNonNull(metadata.getString(PROVIDER_URL_KEY));
  }

  private static Bundle getAppMetadata(Context context) {
    String packageName = context.getPackageName();
    ApplicationInfo applicationInfo;
    try {
      applicationInfo =
          context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);
      return applicationInfo.metaData;
    } catch (NameNotFoundException e) {
      Log.e(TAG, "Cannot find Manifest metadata in package : " + packageName);
      throw new IllegalStateException(e);
    }
  }

  private ProviderUtils() {}
}
