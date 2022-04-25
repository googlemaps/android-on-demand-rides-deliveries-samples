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
package com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.model

/** A basic state machine with FleetEngine Trip status. */
enum class TripStatus {
  // Status values are broadcast to indicate current journey state. The values are defined
  // in trips.proto.
  UNKNOWN_TRIP_STATUS {
    override val nextStatus: TripStatus
      get() = this

    override val code: Int
      get() = UNKNOWN_CODE
  },
  NEW {
    override val nextStatus: TripStatus
      get() = ENROUTE_TO_PICKUP

    override val code: Int
      get() {
        return NEW_TRIP_CODE
      }
  },
  ENROUTE_TO_PICKUP {
    override val nextStatus: TripStatus
      get() = ARRIVED_AT_PICKUP

    override val code: Int
      get() = ENROUTE_TO_PICKUP_CODE
  },
  ARRIVED_AT_PICKUP {
    override val nextStatus: TripStatus
      get() = ENROUTE_TO_DROPOFF

    override val code: Int
      get() = ARRIVED_AT_PICKUP_CODE
  },
  ENROUTE_TO_DROPOFF {
    override val nextStatus: TripStatus
      get() = COMPLETE

    override val code: Int
      get() = ENROUTE_TO_DROPOFF_CODE
  },
  COMPLETE {
    override val nextStatus: TripStatus
      get() = this

    override val code: Int
      get() = COMPLETE_CODE
  },
  CANCELED {
    override val nextStatus: TripStatus
      get() = this

    override val code: Int
      get() = CANCELED_CODE
  };

  /**
   * The next logical status based on the current one. Will return itself if it is a terminal state.
   *
   * @return next status or itself if terminal.
   */
  abstract val nextStatus: TripStatus

  /** Returns the FleetEngine numerical code for the given trip status. */
  abstract val code: Int

  companion object {
    // Internal trip status codes according to trips.proto
    private const val UNKNOWN_CODE = 0
    private const val NEW_TRIP_CODE = 1
    private const val ENROUTE_TO_PICKUP_CODE = 2
    private const val ARRIVED_AT_PICKUP_CODE = 3
    private const val ENROUTE_TO_DROPOFF_CODE = 4
    private const val COMPLETE_CODE = 5
    private const val CANCELED_CODE = 6

    /**
     * Parse the status in string to get the status code.
     *
     * @param status trip status in string format
     * @return trip status code
     */
    fun parse(status: String): Int = valueOf(status).code
  }
}
