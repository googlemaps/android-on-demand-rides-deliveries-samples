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
package com.google.mapsplatform.transportation.sample.driver;

import com.google.mapsplatform.transportation.sample.driver.state.TripStatus;

/** Presents information on the main page. */
public interface Presenter {

  /** Show trip ID in the main page. */
  void showTripId(String tripId);

  /** Show Next trip ID in the main page. */
  void showNextTripId(String tripId);

  /** Show trip status in the main page. */
  void showTripStatus(TripStatus status);
}
