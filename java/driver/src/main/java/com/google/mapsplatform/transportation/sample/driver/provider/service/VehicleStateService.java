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
package com.google.mapsplatform.transportation.sample.driver.provider.service;

import android.util.Log;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mapsplatform.transportation.sample.driver.provider.response.VehicleModel;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

/**
 * Service that polls the 'Vehicle' state when its been started. It pushes the response via the
 * 'VehicleStateListener' listener.
 */
public final class VehicleStateService extends AbstractScheduledService {

  /** Listener for state updates. */
  public interface VehicleStateListener {
    void onVehicleStateUpdate(VehicleModel vehicleModel);
  }

  private final LocalProviderService localProviderService;
  private final String vehicleId;
  private final WeakReference<VehicleStateListener> listenerRef;

  public VehicleStateService(
      LocalProviderService localProviderService, String vehicleId, VehicleStateListener listener) {
    this.localProviderService = localProviderService;
    this.vehicleId = vehicleId;
    this.listenerRef = new WeakReference<VehicleStateListener>(listener);
  }

  @Override
  protected void startUp() {
    Log.i("VehicleStateService", "startUp");
  }

  @Override
  protected void shutDown() {
    Log.i("VehicleStateService", "shutDown");
  }

  @Override
  protected void runOneIteration() {
    Log.i("VehicleStateService", "runOneIteration start");

    ListenableFuture<VehicleModel> responseFuture = localProviderService.fetchVehicle(vehicleId);

    Futures.addCallback(
        responseFuture,
        new FutureCallback<VehicleModel>() {
          @Override
          public void onSuccess(VehicleModel vehicleModel) {
            if (!VehicleStateService.this.isRunning()) {
              return;
            }

            VehicleStateListener listener = listenerRef.get();

            if (listener == null) {
              return;
            }

            listener.onVehicleStateUpdate(vehicleModel);

            for (String tripID : vehicleModel.getCurrentTripsIds()) {
              Log.i("VehicleStateService", String.format("runOneIteration tripId=%s", tripID));
            }
          }

          @Override
          public void onFailure(Throwable t) {}
        },
        this.executor());

    Log.i("VehicleStateService", "runOneIteration end");
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedDelaySchedule(/*initialDelay=*/ 0L, /*delay=*/ 3L, TimeUnit.SECONDS);
  }
}
