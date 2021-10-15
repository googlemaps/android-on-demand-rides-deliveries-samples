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
package com.google.mapsplatform.transportation.sample.provider.response;

import org.joda.time.Instant;

/** Non-extensive POJO representation of a Token response object.
 *
 * <p>Note: This class is implemented with standard setters to allow easier integration with
 * Retrofit framework.</p>
 */
public final class TokenResponse {

  /** String representation of a signed JWT. */
  private String token;
  private long creationTimestampMs;
  private long expirationTimestampMs;

  /**
   * Returns a signed JWT valid for Fleet Engine usage on ConsumerSDK.
   *
   * @return signed JWT.
   */
  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public Instant getCreationTimestamp() {
    return new Instant(creationTimestampMs);
  }

  public Instant getExpirationTimestamp() {
    return new Instant(expirationTimestampMs);
  }
}

