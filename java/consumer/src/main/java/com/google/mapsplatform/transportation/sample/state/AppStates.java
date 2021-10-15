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
package com.google.mapsplatform.transportation.sample.state;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;
import java.lang.annotation.Retention;

/** Indicates different app states. */
@Retention(SOURCE)
@IntDef({
  AppStates.UNINITIALIZED,
  AppStates.INITIALIZED,
  AppStates.SELECTING_DROPOFF,
  AppStates.SELECTING_PICKUP,
  AppStates.JOURNEY_SHARING,
  AppStates.TRIP_CANCELED,
  AppStates.TRIP_COMPLETE
})
public @interface AppStates {
  int UNINITIALIZED = 0;
  int INITIALIZED = 1;
  int SELECTING_DROPOFF = 2;
  int SELECTING_PICKUP = 3;
  int JOURNEY_SHARING = 5;
  int TRIP_CANCELED = 6;
  int TRIP_COMPLETE = 7;
}
