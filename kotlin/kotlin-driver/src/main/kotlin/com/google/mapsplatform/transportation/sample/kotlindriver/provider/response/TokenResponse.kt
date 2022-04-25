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
package com.google.mapsplatform.transportation.sample.kotlindriver.provider.response

import com.google.gson.annotations.SerializedName
import org.joda.time.Instant

/**
 * Non-extensive POJO representation of a Token response object.
 *
 * Note: This class is implemented with standard setters to allow easier integration with Retrofit
 * framework.
 */
class TokenResponse(
  /**
   * Returns a signed JWT valid for Fleet Engine usage on DriverSDK.
   *
   * @return signed JWT.
   */
  /** String representation of a signed JWT. */
  @SerializedName("jwt") val token: String? = null,
  @SerializedName("creationTimestamp") private val creationTimestampMs: Long = 0,
  @SerializedName("expirationTimestamp") private val expirationTimestampMs: Long = 0
) {
  val creationTimestamp: Instant
    get() = Instant(creationTimestampMs)
  val expirationTimestamp: Instant
    get() = Instant(expirationTimestampMs)
}
