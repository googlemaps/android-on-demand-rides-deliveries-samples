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
package com.google.mapsplatform.transportation.sample.consumer.provider.model;

/** A basic state machine with FleetEngine Trip status. */
public enum TripStatus {

  // Status values are broadcast to indicate current journey state. The values are defined
  // in trips.proto.

  UNKNOWN_TRIP_STATUS {
    @Override
    public TripStatus getNextStatus() {
      return this;
    }

    @Override
    public int getCode() {
      return UNKNOWN_CODE;
    }
  },
  NEW {
    @Override
    public TripStatus getNextStatus() {
      return ENROUTE_TO_PICKUP;
    }

    @Override
    public int getCode() {
      return NEW_TRIP_CODE;
    }
  },
  ENROUTE_TO_PICKUP {
    @Override
    public TripStatus getNextStatus() {
      return ARRIVED_AT_PICKUP;
    }

    @Override
    public int getCode() {
      return ENROUTE_TO_PICKUP_CODE;
    }
  },
  ARRIVED_AT_PICKUP {
    @Override
    public TripStatus getNextStatus() {
      return ENROUTE_TO_DROPOFF;
    }

    @Override
    public int getCode() {
      return ARRIVED_AT_PICKUP_CODE;
    }
  },
  ENROUTE_TO_DROPOFF {
    @Override
    public TripStatus getNextStatus() {
      return COMPLETE;
    }

    @Override
    public int getCode() {
      return ENROUTE_TO_DROPOFF_CODE;
    }
  },
  COMPLETE {
    @Override
    public TripStatus getNextStatus() {
      return this;
    }

    @Override
    public int getCode() {
      return COMPLETE_CODE;
    }
  },
  CANCELED {
    @Override
    public TripStatus getNextStatus() {
      return this;
    }

    @Override
    public int getCode() {
      return CANCELED_CODE;
    }
  };

  // Internal trip status codes according to trips.proto

  private static final int UNKNOWN_CODE = 0;
  private static final int NEW_TRIP_CODE = 1;
  private static final int ENROUTE_TO_PICKUP_CODE = 2;
  private static final int ARRIVED_AT_PICKUP_CODE = 3;
  private static final int ENROUTE_TO_DROPOFF_CODE = 4;
  private static final int COMPLETE_CODE = 5;
  private static final int CANCELED_CODE = 6;

  /**
   * The next logical status based on the current one. Will return itself if it is a terminal state.
   *
   * @return next status or itself if terminal.
   */
  public abstract TripStatus getNextStatus();

  /** Returns the FleetEngine numerical code for the given trip status. */
  public abstract int getCode();

  /**
   * Parse the status in string to get the status code.
   *
   * @param status trip status in string format
   * @return trip status code
   */
  public static int parse(String status) {
    TripStatus statusEnum = valueOf(status);
    return statusEnum.getCode();
  }
}
