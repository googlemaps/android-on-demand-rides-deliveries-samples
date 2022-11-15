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

import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.AuthTokenContext
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.service.LocalProviderService
import kotlinx.coroutines.runBlocking

internal class TripAuthTokenFactory(private val providerService: LocalProviderService) :
  AuthTokenContext.AuthTokenFactory {
  private var token: String? = null
  private var expiryTimeMs: Long = 0
  private var vehicleId: String? = null

  override fun getToken(context: AuthTokenContext): String {
    val vehicleId = context.vehicleId!!
    if (System.currentTimeMillis() > expiryTimeMs || vehicleId != this.vehicleId) {
      fetchNewToken(vehicleId)
    }
    return token!!
  }

  private fun fetchNewToken(vehicleId: String) = runBlocking {
    try {
      val tokenResponse = providerService.fetchAuthToken(vehicleId)
      token = tokenResponse.token!!

      // The expiry time could be an hour from now, but just to try and avoid
      // passing expired tokens, we subtract 10 minutes from that time.
      val tenMinutesInMillis = (10 * 60 * 1000).toLong()
      expiryTimeMs = tokenResponse.expirationTimestamp.millis - tenMinutesInMillis
      this@TripAuthTokenFactory.vehicleId = vehicleId
    } catch (e: Exception) {
      throw RuntimeException("Could not get auth token", e)
    }
  }
}
