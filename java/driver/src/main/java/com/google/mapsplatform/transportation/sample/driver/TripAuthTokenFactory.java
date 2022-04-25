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

import static java.util.Objects.requireNonNull;

import android.app.Application;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.AuthTokenContext;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.AuthTokenContext.AuthTokenFactory;
import com.google.android.libraries.navigation.RoadSnappedLocationProvider;
import com.google.mapsplatform.transportation.sample.driver.provider.ProviderUtils;
import com.google.mapsplatform.transportation.sample.driver.provider.response.TokenResponse;
import com.google.mapsplatform.transportation.sample.driver.provider.service.LocalProviderService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

class TripAuthTokenFactory implements AuthTokenFactory {
  private String token;
  private long expiryTimeMs = 0;
  private String vehicleId;
  private final LocalProviderService providerService;

  TripAuthTokenFactory(
      Application application,
      Executor executor,
      RoadSnappedLocationProvider roadSnappedLocationProvider) {

    providerService =
        new LocalProviderService(
            LocalProviderService.createRestProvider(ProviderUtils.getProviderBaseUrl(application)),
            executor,
            roadSnappedLocationProvider);
  }

  @Override
  public String getToken(AuthTokenContext context) {
    String vehicleId = requireNonNull(context.getVehicleId());
    if (System.currentTimeMillis() > expiryTimeMs || !vehicleId.equals(this.vehicleId)) {
      fetchNewToken(vehicleId);
    }
    return token;
  }

  private void fetchNewToken(String vehicleId) {
    try {
      TokenResponse tokenResponse = providerService.fetchAuthToken(vehicleId).get();
      token = requireNonNull(tokenResponse.getToken());

      // The expiry time could be an hour from now, but just to try and avoid
      // passing expired tokens, we subtract 10 minutes from that time.
      long tenMinutesInMillis = 10 * 60 * 1000;
      expiryTimeMs = tokenResponse.getExpirationTimestamp().getMillis() - tenMinutesInMillis;
      this.vehicleId = vehicleId;
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException("Could not get auth token", e);
    }
  }
}
