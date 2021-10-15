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
// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.mapsplatform.transportation.sample.driver.state;

/** A basic state machine with FleetEngine Trip status. */
public enum TripStatus {

  // Status values are broadcast to indicate current journey state. The values are defined
  // in trips.proto.

  UNKNOWN_TRIP_STATUS {
    @Override
    public TripStatus getNextStatus() {
      return this;
    }
  },
  NEW {
    @Override
    public TripStatus getNextStatus() {
      return ENROUTE_TO_PICKUP;
    }
  },
  ENROUTE_TO_PICKUP {
    @Override
    public TripStatus getNextStatus() {
      return ARRIVED_AT_PICKUP;
    }
  },
  ARRIVED_AT_PICKUP {
    @Override
    public TripStatus getNextStatus() {
      return ENROUTE_TO_DROPOFF;
    }
  },
  ENROUTE_TO_DROPOFF {
    @Override
    public TripStatus getNextStatus() {
      return COMPLETE;
    }
  },
  COMPLETE {
    @Override
    public TripStatus getNextStatus() {
      return this;
    }
  },
  CANCELED {
    @Override
    public TripStatus getNextStatus() {
      return this;
    }
  };

  /**
   * The next logical status based on the current one. Will return itself if it is a terminal state.
   *
   * @return next status or itself if terminal.
   */
  public abstract TripStatus getNextStatus();

  /**
   * Convenience method to determine if the current status is terminal.
   *
   * @return true if it is a terminal state.
   */
  public boolean isTerminalState() {
    return getNextStatus() == this;
  }
}
