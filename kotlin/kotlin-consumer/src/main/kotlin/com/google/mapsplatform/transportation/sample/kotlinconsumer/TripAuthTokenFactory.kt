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
package com.google.mapsplatform.transportation.sample.kotlinconsumer

import android.app.Application
import com.google.android.libraries.mapsplatform.transportation.consumer.auth.AuthTokenContext
import com.google.android.libraries.mapsplatform.transportation.consumer.auth.AuthTokenFactory
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.ProviderUtils
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.response.TokenResponse
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.service.LocalProviderService
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

/** A factory for returning auth tokens for the currently assigned trip */
internal class TripAuthTokenFactory(application: Application) : AuthTokenFactory {
  private var token: String? = null
  private var expiryTimeMs: Long = 0
  private var tripId: String? = null
  private val providerService: LocalProviderService

  init {
    val executor = Executors.newCachedThreadPool()
    val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
    providerService =
      LocalProviderService(
        LocalProviderService.createRestProvider(ProviderUtils.getProviderBaseUrl(application)),
        executor,
        scheduledExecutor
      )
  }

  override fun getToken(context: AuthTokenContext): String {
    val tripId = context.tripId!!
    if (System.currentTimeMillis() > expiryTimeMs || tripId != this.tripId) {
      fetchNewToken(tripId)
    }
    return token!!
  }

  private fun fetchNewToken(tripId: String) {
    try {
      val tokenResponse: TokenResponse = providerService.fetchAuthToken(tripId).get()
      token = tokenResponse.token!!

      // The expiry time could be an hour from now, but just to try and avoid
      // passing expired tokens, we subtract 10 minutes from that time.
      val tenMinutesInMillis = (10 * 60 * 1000).toLong()
      expiryTimeMs = tokenResponse.expirationTimestamp.millis - tenMinutesInMillis
      this.tripId = tripId
    } catch (e: InterruptedException) {
      throw RuntimeException("Could not get auth token", e)
    } catch (e: ExecutionException) {
      throw RuntimeException("Could not get auth token", e)
    }
  }
}
