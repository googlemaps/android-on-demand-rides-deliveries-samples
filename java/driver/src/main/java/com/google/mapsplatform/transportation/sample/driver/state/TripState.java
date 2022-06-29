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
package com.google.mapsplatform.transportation.sample.driver.state;

import com.google.auto.value.AutoValue;

/**
 * Class to represent the different states a 'Trip' might go through combining 'tripStatus' and
 * 'intermediateDestinationIndex'.
 */
@AutoValue
public abstract class TripState {
  /** Default/initial value which indicates an intermediate destination is not visited yet. */
  private static int INTERMEDIATE_DESTINATION_INDEX_DEFAULT_VALUE = -1;

  public abstract String tripId();

  public abstract TripStatus tripStatus();

  public abstract int intermediateDestinationIndex();

  public static TripState create(String tripId, TripStatus tripStatus) {
    return new AutoValue_TripState(
        tripId, tripStatus, INTERMEDIATE_DESTINATION_INDEX_DEFAULT_VALUE);
  }

  public static TripState create(String tripId, TripStatus tripStatus, Integer intermediateIndex) {
    return new AutoValue_TripState(tripId, tripStatus, intermediateIndex);
  }
}
