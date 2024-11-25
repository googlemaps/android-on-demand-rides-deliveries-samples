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

import android.graphics.Color
import com.google.android.libraries.mapsplatform.transportation.consumer.model.PolylineType
import com.google.android.libraries.mapsplatform.transportation.consumer.model.TrafficData.SpeedReadingInterval.SpeedType
import com.google.android.libraries.mapsplatform.transportation.consumer.model.TrafficStyle
import com.google.android.libraries.mapsplatform.transportation.consumer.view.ConsumerMapStyle

/** Contains utility methods that deal with polyline styling for a Consumer map. */
object PolylineStyles {
  private val TRAFFIC_STYLE =
    TrafficStyle.builder()
      .setTrafficVisibility(true)
      .setTrafficColor(SpeedType.NO_DATA, Color.GRAY)
      .setTrafficColor(SpeedType.NORMAL, Color.BLUE)
      .setTrafficColor(SpeedType.SLOW, Color.YELLOW)
      .setTrafficColor(SpeedType.TRAFFIC_JAM, Color.RED)
      .build()

  /**
   * Applies traffic styling for the ACTIVE_ROUTE polyline. This enables the 'Traffic Aware
   * Polyline' feature.
   *
   * @param consumerController the map controller which provides access to the styles configurator.
   */
  fun enableTrafficAwarePolyline(consumerMapStyle: ConsumerMapStyle) {
    consumerMapStyle.setPolylineTrafficStyle(PolylineType.ACTIVE_ROUTE, TRAFFIC_STYLE)
  }
}
