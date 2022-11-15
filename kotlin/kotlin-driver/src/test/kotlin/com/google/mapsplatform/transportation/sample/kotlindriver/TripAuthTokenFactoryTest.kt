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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.AuthTokenContext
import com.google.common.truth.Truth.assertThat
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.response.TokenResponse
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.service.LocalProviderService
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verifyBlocking
import com.nhaarman.mockitokotlin2.whenever
import java.net.ConnectException
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TripAuthTokenFactoryTest {
  private val providerMock =
    mock<LocalProviderService> {
      onBlocking { fetchAuthToken(any()) } doReturn TokenResponse(token = FETCHED_TOKEN)
    }

  private val tripAuthTokenFactory = TripAuthTokenFactory(providerMock)

  @Test
  fun getToken_returnsFetchedToken() {
    assertThat(tripAuthTokenFactory.getToken(AUTH_TOKEN_CONTEXT)).isEqualTo(FETCHED_TOKEN)
  }

  @Test
  fun getToken_sendsVehicleIdToProvider() {
    tripAuthTokenFactory.getToken(AUTH_TOKEN_CONTEXT)

    verifyBlocking(providerMock, times(1)) { fetchAuthToken(VEHICLE_ID) }
  }

  @Test
  fun getToken_surfacesExceptionWhenProviderFails() {
    runBlocking {
      whenever(providerMock.fetchAuthToken(any())).doAnswer {
        throw ConnectException("Failed to connect")
      }
    }

    assertFailsWith<RuntimeException>("Could not get auth token") {
      tripAuthTokenFactory.getToken(AUTH_TOKEN_CONTEXT)
    }
  }

  private companion object {
    const val FETCHED_TOKEN = "mockedToken"
    const val VEHICLE_ID = "mockedVehicle"
    val AUTH_TOKEN_CONTEXT = AuthTokenContext.builder().setVehicleId(VEHICLE_ID).build()
  }
}
