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
package com.google.mapsplatform.transportation.sample.kotlindriver.state

/** A basic state machine with FleetEngine Trip status. */
enum class TripStatus {
  // Status values are broadcast to indicate current journey state. The values are defined
  // in trips.proto.
  UNKNOWN_TRIP_STATUS,
  NEW,
  ENROUTE_TO_PICKUP,
  ARRIVED_AT_PICKUP,
  ARRIVED_AT_INTERMEDIATE_DESTINATION,
  ENROUTE_TO_INTERMEDIATE_DESTINATION,
  ENROUTE_TO_DROPOFF,
  COMPLETE,
  CANCELED;
}
