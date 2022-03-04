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
package com.google.mapsplatform.transportation.sample;

import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Types of markers that are drawn by the sample app. */
@Retention(RetentionPolicy.SOURCE)
@IntDef({ConsumerMarkerType.PICKUP_POINT, ConsumerMarkerType.DROPOFF_POINT})
public @interface ConsumerMarkerType {
  /** Marker representing the pickup location. */
  int PICKUP_POINT = 1;

  /** Marker representing the dropoff location. */
  int DROPOFF_POINT = 2;
}
