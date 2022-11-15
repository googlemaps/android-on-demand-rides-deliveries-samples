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

import com.google.android.libraries.mapsplatform.transportation.consumer.auth.AuthTokenContext
import com.google.android.libraries.mapsplatform.transportation.consumer.auth.AuthTokenFactory
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.response.TokenResponse
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.service.LocalProviderService
import kotlinx.coroutines.runBlocking

/**
 * A factory for returning auth tokens for the currently assigned trip.
 *
 * @property providerService object used to communicate with the remote sample provider.
 */
internal class TripAuthTokenFactory(private val providerService: LocalProviderService) :
  AuthTokenFactory {
  private var token: String? = null
  private var expiryTimeMs: Long = 0
  private var tripId: String? = null

  override fun getToken(context: AuthTokenContext): String {
    val tripId = context.tripId!!
    if (System.currentTimeMillis() > expiryTimeMs || tripId != this.tripId) {
      fetchNewToken(tripId)
    }
    return token!!
  }

  private fun fetchNewToken(tripId: String) = runBlocking {
    try {
      val tokenResponse: TokenResponse = providerService.fetchAuthToken(tripId)
      token = tokenResponse.token!!

      // The expiry time could be an hour from now, but just to try and avoid
      // passing expired tokens, we subtract 10 minutes from that time.
      val tenMinutesInMillis = (10 * 60 * 1000).toLong()
      expiryTimeMs = tokenResponse.expirationTimestamp.millis - tenMinutesInMillis
      this@TripAuthTokenFactory.tripId = tripId
    } catch (e: Exception) {
      throw RuntimeException("Could not get auth token", e)
    }
  }
}
