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
package com.google.mapsplatform.transportation.sample.kotlindriver.provider.service

import android.util.Log
import com.google.common.util.concurrent.AbstractScheduledService
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.response.VehicleResponse
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

/**
 * Service that polls the 'Vehicle' state when its been started. It pushes the response via the
 * 'VehicleStateListener' listener.
 */
class VehicleStateService(
  private val localProviderService: LocalProviderService,
  private val vehicleId: String,
  listener: VehicleStateListener
) : AbstractScheduledService() {
  /** Listener for state updates. */
  interface VehicleStateListener {
    fun onVehicleStateUpdate(vehicleResponse: VehicleResponse)
  }

  private val listenerRef: WeakReference<VehicleStateListener> = WeakReference(listener)
  override fun startUp() {
    Log.i("VehicleStateService", "startUp")
  }

  override fun shutDown() {
    Log.i("VehicleStateService", "shutDown")
  }

  override fun runOneIteration() {
    Log.i("VehicleStateService", "runOneIteration start")
    val responseFuture = localProviderService.fetchVehicle(vehicleId)
    Futures.addCallback(
      responseFuture,
      object : FutureCallback<VehicleResponse> {
        override fun onSuccess(vehicleResponse: VehicleResponse?) {
          if (!this@VehicleStateService.isRunning) {
            return
          }
          val listener = listenerRef.get() ?: return
          vehicleResponse ?: return
          listener.onVehicleStateUpdate(vehicleResponse)
          for (tripID in vehicleResponse.currentTripsIds) {
            Log.i("VehicleStateService", String.format("runOneIteration %s", tripID))
          }
        }

        override fun onFailure(t: Throwable) {}
      },
      executor()
    )
    Log.i("VehicleStateService", "runOneIteration end")
  }

  override fun scheduler(): Scheduler {
    return Scheduler.newFixedDelaySchedule(/*initialDelay=*/ 0L, DELAY_IN_SECONDS, TimeUnit.SECONDS)
  }

  companion object {
    /** Delay of next process iteration happens after the current one is done. */
    private const val DELAY_IN_SECONDS: Long = 10 // 10 seconds
  }
}
