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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.libraries.mapsplatform.transportation.consumer.auth.AuthTokenContext
import com.google.common.truth.Truth.assertThat
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.response.TokenResponse
import com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.service.LocalProviderService
import java.net.ConnectException
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class TripAuthTokenFactoryTest {
  private val providerServiceMock =
    mock<LocalProviderService> {
      onBlocking { fetchAuthToken(any()) } doReturn TokenResponse(token = FETCHED_TOKEN)
    }

  private val tripAuthTokenFactory = TripAuthTokenFactory(providerServiceMock)

  @Test
  fun getToken_returnsFetchedToken() {
    assertThat(tripAuthTokenFactory.getToken(AUTH_TOKEN_CONTEXT)).isEqualTo(FETCHED_TOKEN)
  }

  @Test
  fun getToken_sendsTripIdToProvider() {
    tripAuthTokenFactory.getToken(AUTH_TOKEN_CONTEXT)

    verifyBlocking(providerServiceMock, times(1)) { fetchAuthToken(TRIP_ID) }
  }

  @Test
  fun getToken_surfacesExceptionWhenProviderFails() {
    runBlocking {
      whenever(providerServiceMock.fetchAuthToken(any())).doAnswer {
        throw ConnectException("Failed to connect")
      }
    }

    assertFailsWith<RuntimeException>("Could not get auth token") {
      tripAuthTokenFactory.getToken(AUTH_TOKEN_CONTEXT)
    }
  }

  private companion object {
    const val FETCHED_TOKEN = "fetchedToken"
    const val TRIP_ID = "testTrip"
    val AUTH_TOKEN_CONTEXT = AuthTokenContext.builder().setTripId(TRIP_ID).build()
  }
}
