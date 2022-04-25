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

import androidx.annotation.IntDef
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/** Types of markers that are drawn by the sample app. */
@Retention(RetentionPolicy.SOURCE)
@IntDef(
  ConsumerMarkerType.PICKUP_POINT,
  ConsumerMarkerType.DROPOFF_POINT,
  ConsumerMarkerType.INTERMEDIATE_DESTINATION_POINT,
  ConsumerMarkerType.PREVIOUS_TRIP_PENDING_POINT
)
annotation class ConsumerMarkerType {
  companion object {
    /** Marker representing the pickup location. */
    const val PICKUP_POINT = 1

    /** Marker representing the dropoff location. */
    const val DROPOFF_POINT = 2

    /** Marker representing an intermediate destination of a trip */
    const val INTERMEDIATE_DESTINATION_POINT = 3

    /** Marker representing the pending waypoints of a previous trip (B2B). */
    const val PREVIOUS_TRIP_PENDING_POINT = 4
  }
}
