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
package com.google.mapsplatform.transportation.sample.kotlinconsumer.provider

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import java.lang.IllegalStateException

/** Provides local provider related utilities. */
object ProviderUtils {
  private const val TAG = "ProviderUtils"
  private const val PROVIDER_ID_KEY = "com.google.mapsplatform.transportation.sample.provider_id"
  private const val PROVIDER_URL_KEY = "com.google.mapsplatform.transportation.sample.provider_url"

  /**
   * Gets provider Id to communicate to provider server.
   *
   * @return providerId.
   * @throws IllegalStateException if providerId can't be obtained. This stops the sample app from
   * functioning correctly.
   * @throws NullPointerException if manifest metadata doesn't have providerId info.
   */
  fun getProviderId(context: Context): String {
    val metadata = getAppMetadata(context)
    check(metadata.containsKey(PROVIDER_ID_KEY)) {
      "App metadata in manifest does not contain provider Id."
    }
    return metadata.getString(PROVIDER_ID_KEY)!!
  }

  /**
   * Gets provider base URL to communicate to provider server.
   *
   * @return providerId.
   * @throws IllegalStateException if provider base URL can't be obtained. This stops the sample app
   * from functioning correctly.
   * @throws NullPointerException if manifest metadata doesn't have provider base URL info.
   */
  fun getProviderBaseUrl(context: Context): String {
    val metadata = getAppMetadata(context)
    check(metadata.containsKey(PROVIDER_URL_KEY)) {
      "App metadata in manifest does not contain provider base URL."
    }
    return metadata.getString(PROVIDER_URL_KEY)!!
  }

  private fun getAppMetadata(context: Context): Bundle {
    val packageName = context.packageName
    val applicationInfo: ApplicationInfo
    return try {
      applicationInfo =
        context.packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
      applicationInfo.metaData
    } catch (e: PackageManager.NameNotFoundException) {
      Log.e(TAG, "Cannot find Manifest metadata in package : $packageName")
      throw IllegalStateException(e)
    }
  }
}
