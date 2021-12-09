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
package com.google.mapsplatform.transportation.sample.provider.service;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/** Retries getting Future if exception is thrown or certain criteria is not met. */
public final class RetryingFuture {
  static int RUN_FOREVER = Integer.MIN_VALUE;

  private final ScheduledExecutorService executor;

  public RetryingFuture(ScheduledExecutorService executor) {
    this.executor = executor;
  }

  /**
   * Retries getting Future if exception is thrown or certain criteria is not met.
   *
   * @param futureSupplier entity that can create the target future.
   * @param retries number of retries. Will run forever until success condition is met if set to
   *     {@link #RUN_FOREVER} .
   * @param intervalMillis retry interval in milliseconds.
   * @param successCondition if evaluates to true return future, otherwise retry until condition is
   *     met or retry count exhausted.
   * @param <T> return type.
   * @return valid result of type T.
   */
  public <T> ListenableFuture<T> runWithRetries(
      Supplier<ListenableFuture<T>> futureSupplier,
      int retries,
      long intervalMillis,
      Predicate<T> successCondition) {
    SettableFuture<T> resultFuture = SettableFuture.create();
    runWithRetriesInternal(resultFuture, futureSupplier, retries, intervalMillis, successCondition);
    return resultFuture;
  }

  private <T> void runWithRetriesInternal(
      final SettableFuture<T> future,
      final Supplier<ListenableFuture<T>> futureSupplier,
      final int retries,
      final long intervalMillis,
      final Predicate<T> successCondition) {
    ListenableFuture<T> immediateFuture;
    try {
      immediateFuture = futureSupplier.get();
    } catch (Exception e) {
      handleFailure(future, futureSupplier, retries, intervalMillis, successCondition, e);
      return;
    }

    Futures.addCallback(
        immediateFuture,
        new FutureCallback<T>() {
          @Override
          public void onSuccess(T result) {
            if (successCondition.apply(result)) {
              future.set(result);
            } else {
              RuntimeException exception =
                  new RuntimeException("Success condition not met, retrying.");
              handleFailure(
                  future, futureSupplier, retries, intervalMillis, successCondition, exception);
            }
          }

          @Override
          public void onFailure(Throwable t) {
            handleFailure(future, futureSupplier, retries, intervalMillis, successCondition, t);
          }
        },
        MoreExecutors.directExecutor());
  }

  private <T> void handleFailure(
      SettableFuture<T> future,
      Supplier<ListenableFuture<T>> futureSupplier,
      int retries,
      long delayInMillis,
      Predicate<T> successCondition,
      Throwable t) {
    if (retries == RUN_FOREVER || retries > 0) {
      ScheduledFuture<?> unused =
          executor.schedule(
              () -> {
                int newRetriesCount = retries == RUN_FOREVER ? RUN_FOREVER : retries - 1;
                runWithRetriesInternal(
                    future, futureSupplier, newRetriesCount, delayInMillis, successCondition);
              },
              delayInMillis,
              MILLISECONDS);
    } else {
      future.setException(t);
    }
  }
}
