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
package com.google.mapsplatform.transportation.sample.consumer;

import static java.util.Objects.requireNonNull;

import android.app.Application;
import com.google.android.libraries.mapsplatform.transportation.consumer.auth.AuthTokenContext;
import com.google.android.libraries.mapsplatform.transportation.consumer.auth.AuthTokenFactory;
import com.google.mapsplatform.transportation.sample.consumer.provider.ProviderUtils;
import com.google.mapsplatform.transportation.sample.consumer.provider.response.TokenResponse;
import com.google.mapsplatform.transportation.sample.consumer.provider.service.LocalProviderService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/** A factory for returning auth tokens for the currently assigned trip */
class TripAuthTokenFactory implements AuthTokenFactory {
  private String token;
  private long expiryTimeMs = 0;
  private String tripId;
  private final LocalProviderService providerService;

  public TripAuthTokenFactory(Application application) {
    ExecutorService executor = Executors.newCachedThreadPool();
    ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    providerService =
        new LocalProviderService(
            LocalProviderService.createRestProvider(ProviderUtils.getProviderBaseUrl(application)),
            executor,
            scheduledExecutor);
  }

  @Override
  public String getToken(AuthTokenContext context) {
    String tripId = requireNonNull(context.getTripId());
    if (System.currentTimeMillis() > expiryTimeMs || !tripId.equals(this.tripId)) {
      fetchNewToken(tripId);
    }
    return token;
  }

  private void fetchNewToken(String tripId) {
    try {
      TokenResponse tokenResponse = providerService.fetchAuthToken(tripId).get();
      token = requireNonNull(tokenResponse.getToken());

      // The expiry time could be an hour from now, but just to try and avoid
      // passing expired tokens, we subtract 10 minutes from that time.
      long tenMinutesInMillis = 10 * 60 * 1000;
      expiryTimeMs = tokenResponse.getExpirationTimestamp().getMillis() - tenMinutesInMillis;
      this.tripId = tripId;
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException("Could not get auth token", e);
    }
  }
}
