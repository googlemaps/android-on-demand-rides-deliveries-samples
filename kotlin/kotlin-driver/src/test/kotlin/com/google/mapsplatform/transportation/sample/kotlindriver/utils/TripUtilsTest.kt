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
package com.google.mapsplatform.transportation.sample.kotlindriver.utils

import com.google.common.truth.Truth.assertThat
import com.google.mapsplatform.transportation.sample.kotlindriver.provider.response.Waypoint
import com.google.mapsplatform.transportation.sample.kotlindriver.state.TripState
import com.google.mapsplatform.transportation.sample.kotlindriver.state.TripStatus
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TripUtilsTest {
  @Test
  fun getInitialState_returnsExpectedState() {
    val tripId = "testTrip"
    val state = TripUtils.getInitialTripState(tripId)

    assertThat(state)
      .isEqualTo(
        TripState(tripId = tripId, tripStatus = TripStatus.NEW, intermediateDestinationIndex = -1)
      )
  }

  @Test
  fun isTripStatusEnroute_returnsTrueForPickup() {
    assertThat(TripUtils.isTripStatusEnroute(TripStatus.ENROUTE_TO_PICKUP)).isTrue()
  }

  @Test
  fun isTripStatusEnroute_returnsTrueForDropofff() {
    assertThat(TripUtils.isTripStatusEnroute(TripStatus.ENROUTE_TO_DROPOFF)).isTrue()
  }

  @Test
  fun isTripStatusEnroute_returnsTrueForIntermediate() {
    assertThat(
        TripUtils.isTripStatusEnroute(TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION),
      )
      .isTrue()
  }

  @Test
  fun isTripStatusEnroute_returnsFalseForNewStatus() {
    assertThat(TripUtils.isTripStatusEnroute(TripStatus.NEW)).isFalse()
  }

  @Test
  fun isTripStatusEnroute_returnsFalseForArrivedAtPickupStatus() {
    assertThat(TripUtils.isTripStatusEnroute(TripStatus.ARRIVED_AT_PICKUP)).isFalse()
  }

  @Test
  fun isTripStatusEnroute_returnsFalseForArrivedAtIntermediateStatus() {
    assertThat(TripUtils.isTripStatusEnroute(TripStatus.ARRIVED_AT_INTERMEDIATE_DESTINATION))
      .isFalse()
  }

  @Test
  fun isTripStatusEnroute_returnsFalseForCompleteStatus() {
    assertThat(TripUtils.isTripStatusEnroute(TripStatus.COMPLETE)).isFalse()
  }

  @Test
  fun isTripStatusArrived_returnsTrueForPickup() {
    assertThat(TripUtils.isTripStatusArrived(TripStatus.ARRIVED_AT_PICKUP)).isTrue()
  }

  @Test
  fun isTripStatusArrived_returnsTrueForDropofff() {
    assertThat(TripUtils.isTripStatusArrived(TripStatus.COMPLETE)).isTrue()
  }

  @Test
  fun isTripStatusArrived_returnsTrueForIntermediate() {
    assertThat(
        TripUtils.isTripStatusArrived(TripStatus.ARRIVED_AT_INTERMEDIATE_DESTINATION),
      )
      .isTrue()
  }

  @Test
  fun isTripStatusArrived_returnsFalseForNewStatus() {
    assertThat(TripUtils.isTripStatusArrived(TripStatus.NEW)).isFalse()
  }

  @Test
  fun isTripStatusArrived_returnsFalseForEnrouteDropoffStatus() {
    assertThat(TripUtils.isTripStatusArrived(TripStatus.ENROUTE_TO_DROPOFF)).isFalse()
  }

  @Test
  fun isTripStatusArrived_returnsFalseForEnrouteIntermediateStatus() {
    assertThat(TripUtils.isTripStatusArrived(TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION))
      .isFalse()
  }

  @Test
  fun getEnrouteStateForWaypoint_returnsEnrouteToPickup() {
    val currentState = TripState(tripId = "testTrip", tripStatus = TripStatus.NEW)
    val waypoint = Waypoint(waypointType = TripUtils.PICKUP_WAYPOINT_TYPE)

    assertThat(TripUtils.getEnrouteStateForWaypoint(currentState, waypoint))
      .isEqualTo(TripState(currentState.tripId, TripStatus.ENROUTE_TO_PICKUP))
  }

  @Test
  fun getEnrouteStateForWaypoint_returnsEnrouteToDropoff() {
    val currentState = TripState(tripId = "testTrip", tripStatus = TripStatus.NEW)
    val waypoint = Waypoint(waypointType = TripUtils.DROP_OFF_WAYPOINT_TYPE)

    assertThat(TripUtils.getEnrouteStateForWaypoint(currentState, waypoint))
      .isEqualTo(TripState(currentState.tripId, TripStatus.ENROUTE_TO_DROPOFF))
  }

  @Test
  fun getEnrouteStateForWaypoint_returnsEnrouteToIntermediateDestination() {
    val currentState =
      TripState(
        tripId = "testTrip",
        tripStatus = TripStatus.ARRIVED_AT_INTERMEDIATE_DESTINATION,
        intermediateDestinationIndex = 3
      )
    val waypoint = Waypoint(waypointType = TripUtils.INTERMEDIATE_DESTINATION_WAYPOINT_TYPE)

    assertThat(TripUtils.getEnrouteStateForWaypoint(currentState, waypoint))
      .isEqualTo(
        TripState(
          tripId = currentState.tripId,
          tripStatus = TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION,
          intermediateDestinationIndex = currentState.intermediateDestinationIndex + 1
        )
      )
  }

  @Test
  fun getEnrouteStateForWaypoint_returnsCurrentStateForCompletedTrip() {
    val currentState =
      TripState(
        tripId = "testTrip",
        tripStatus = TripStatus.COMPLETE,
      )
    val waypoint = Waypoint(waypointType = TripUtils.INTERMEDIATE_DESTINATION_WAYPOINT_TYPE)

    assertThat(TripUtils.getEnrouteStateForWaypoint(currentState, waypoint)).isEqualTo(currentState)
  }

  @Test
  fun getEnrouteStateForWaypoint_throwsForInvalidWaypoint() {
    assertFailsWith<IllegalStateException>("Invalid waypoint type") {
      val currentState =
        TripState(
          tripId = "testTrip",
          tripStatus = TripStatus.NEW,
        )

      TripUtils.getEnrouteStateForWaypoint(currentState, Waypoint(waypointType = "INVALID"))
    }
  }

  @Test
  fun getNextTripState_returnsEnrouteToPickupForNew() {
    val currentState =
      TripState(
        tripId = "testTrip",
        tripStatus = TripStatus.NEW,
      )

    val nextState =
      TripUtils.getNextTripState(
        currentState,
        Waypoint(waypointType = TripUtils.PICKUP_WAYPOINT_TYPE)
      )

    assertThat(nextState).isEqualTo(TripState(currentState.tripId, TripStatus.ENROUTE_TO_PICKUP))
  }

  @Test
  fun getNextTripState_returnsArrivedForEnrouteToPickup() {
    val currentState =
      TripState(
        tripId = "testTrip",
        tripStatus = TripStatus.ENROUTE_TO_PICKUP,
      )

    val nextState =
      TripUtils.getNextTripState(
        currentState,
        Waypoint(waypointType = TripUtils.PICKUP_WAYPOINT_TYPE)
      )

    assertThat(nextState).isEqualTo(TripState(currentState.tripId, TripStatus.ARRIVED_AT_PICKUP))
  }

  @Test
  fun getNextTripState_returnsEnrouteToIntermediateForArrivedAtPickup() {
    val currentState =
      TripState(
        tripId = "testTrip",
        tripStatus = TripStatus.ARRIVED_AT_PICKUP,
      )

    val nextState =
      TripUtils.getNextTripState(
        currentState,
        Waypoint(waypointType = TripUtils.INTERMEDIATE_DESTINATION_WAYPOINT_TYPE)
      )

    assertThat(nextState)
      .isEqualTo(TripState(currentState.tripId, TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION, 0))
  }

  @Test
  fun getNextTripState_returnsEnrouteToDropoffForArrivedAtPickup() {
    val currentState =
      TripState(
        tripId = "testTrip",
        tripStatus = TripStatus.ARRIVED_AT_PICKUP,
      )

    val nextState =
      TripUtils.getNextTripState(
        currentState,
        Waypoint(waypointType = TripUtils.DROP_OFF_WAYPOINT_TYPE)
      )

    assertThat(nextState).isEqualTo(TripState(currentState.tripId, TripStatus.ENROUTE_TO_DROPOFF))
  }

  @Test
  fun getNextTripState_returnsArrivedForEnrouteToIntermediate() {
    val currentState =
      TripState(
        tripId = "testTrip",
        tripStatus = TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION,
      )

    val nextState =
      TripUtils.getNextTripState(
        currentState,
        Waypoint(waypointType = TripUtils.DROP_OFF_WAYPOINT_TYPE)
      )

    assertThat(nextState)
      .isEqualTo(TripState(currentState.tripId, TripStatus.ARRIVED_AT_INTERMEDIATE_DESTINATION))
  }

  @Test
  fun getNextTripState_preservesIntermediateDestinationIndex() {
    val intermediateDestinationIndex = 1

    val currentState =
      TripState(
        tripId = "testTrip",
        tripStatus = TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION,
        intermediateDestinationIndex = intermediateDestinationIndex
      )

    val nextState =
      TripUtils.getNextTripState(
        currentState,
        Waypoint(waypointType = TripUtils.DROP_OFF_WAYPOINT_TYPE)
      )

    assertThat(nextState)
      .isEqualTo(
        TripState(
          currentState.tripId,
          TripStatus.ARRIVED_AT_INTERMEDIATE_DESTINATION,
          intermediateDestinationIndex
        )
      )
  }

  @Test
  fun getNextTripState_returnsEnrouteToDropoffForArrivedAtIntermediate() {
    val currentState =
      TripState(
        tripId = "testTrip",
        tripStatus = TripStatus.ARRIVED_AT_INTERMEDIATE_DESTINATION,
      )

    val nextState =
      TripUtils.getNextTripState(
        currentState,
        Waypoint(waypointType = TripUtils.DROP_OFF_WAYPOINT_TYPE)
      )

    assertThat(nextState).isEqualTo(TripState(currentState.tripId, TripStatus.ENROUTE_TO_DROPOFF))
  }

  @Test
  fun getNextTripState_returnsEnrouteToIntermediateForArrivedAtIntermediate() {
    val currentState =
      TripState(
        tripId = "testTrip",
        tripStatus = TripStatus.ARRIVED_AT_INTERMEDIATE_DESTINATION,
        intermediateDestinationIndex = 3,
      )

    val nextState =
      TripUtils.getNextTripState(
        currentState,
        Waypoint(waypointType = TripUtils.INTERMEDIATE_DESTINATION_WAYPOINT_TYPE)
      )

    assertThat(nextState)
      .isEqualTo(
        TripState(
          currentState.tripId,
          TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION,
          currentState.intermediateDestinationIndex + 1
        )
      )
  }

  @Test
  fun getNextTripState_returnsCompleteForEnrouteToDropoff() {
    val currentState =
      TripState(
        tripId = "testTrip",
        tripStatus = TripStatus.ENROUTE_TO_DROPOFF,
      )

    val nextState =
      TripUtils.getNextTripState(
        currentState,
        Waypoint(waypointType = TripUtils.DROP_OFF_WAYPOINT_TYPE)
      )

    assertThat(nextState).isEqualTo(TripState(currentState.tripId, TripStatus.COMPLETE))
  }

  @Test
  fun getNextTripState_returnsUnknownForUnknownStatus() {
    val currentState =
      TripState(
        tripId = "testTrip",
        tripStatus = TripStatus.UNKNOWN_TRIP_STATUS,
      )

    val nextState =
      TripUtils.getNextTripState(
        currentState,
        Waypoint(waypointType = TripUtils.DROP_OFF_WAYPOINT_TYPE)
      )

    assertThat(nextState).isEqualTo(TripState(currentState.tripId, TripStatus.UNKNOWN_TRIP_STATUS))
  }
}
