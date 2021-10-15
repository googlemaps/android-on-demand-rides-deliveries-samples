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
package com.google.mapsplatform.transportation.sample.driver.provider.response;

import com.google.gson.annotations.SerializedName;

/** Non-extensive POJO representation of a Trip response object. */
public class GetTripResponse {

  @SerializedName("trip")
  private TripData trip;

  @SerializedName("routeToken")
  private String routeToken;

  public TripData getTripData() {
    return trip;
  }

  public void setTripData(TripData trip) {
    this.trip = trip;
  }

  public String getRouteToken() {
    return routeToken;
  }

  public void setRouteToken(String routeToken) {
    this.routeToken = routeToken;
  }
}
