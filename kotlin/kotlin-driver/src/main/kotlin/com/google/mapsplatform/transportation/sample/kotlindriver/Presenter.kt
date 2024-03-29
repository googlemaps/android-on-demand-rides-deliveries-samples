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
package com.google.mapsplatform.transportation.sample.kotlindriver

import com.google.mapsplatform.transportation.sample.kotlindriver.state.TripStatus

/** Presents information on the main page. */
interface Presenter {
  /** Show current waypoint trip ID in the info panel. */
  fun showTripId(tripId: String)

  /** Show list of trip IDs assigned to the Vehicle. */
  fun showMatchedTripIds(tripIds: List<String>)

  /** Show trip status in the info panel. */
  fun showTripStatus(status: TripStatus)

  /** Enable/disable action button. */
  fun enableActionButton(enabled: Boolean)
}
