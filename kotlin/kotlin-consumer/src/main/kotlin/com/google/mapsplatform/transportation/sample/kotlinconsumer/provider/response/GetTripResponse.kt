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
package com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.response

import com.google.gson.annotations.SerializedName

/** Getting trip info after selecting drop-off pick-up locations. */
class GetTripResponse(
  @SerializedName("trip") val trip: TripResponse? = null,
  @SerializedName("routeToken") private val routeToken: String? = null
)
