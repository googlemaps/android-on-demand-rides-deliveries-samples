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
package com.google.mapsplatform.transportation.sample.kotlinconsumer.state

import androidx.annotation.IntDef
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/** Indicates different app states. */
@Retention(RetentionPolicy.SOURCE)
@IntDef(
  AppStates.UNINITIALIZED,
  AppStates.INITIALIZED,
  AppStates.SELECTING_DROPOFF,
  AppStates.SELECTING_PICKUP,
  AppStates.CONFIRMING_TRIP,
  AppStates.JOURNEY_SHARING,
  AppStates.TRIP_CANCELED,
  AppStates.TRIP_COMPLETE
)
annotation class AppStates {
  companion object {
    const val UNINITIALIZED = 0
    const val INITIALIZED = 1
    const val SELECTING_DROPOFF = 2
    const val SELECTING_PICKUP = 3
    const val CONFIRMING_TRIP = 4
    const val JOURNEY_SHARING = 5
    const val TRIP_CANCELED = 6
    const val TRIP_COMPLETE = 7
  }
}
