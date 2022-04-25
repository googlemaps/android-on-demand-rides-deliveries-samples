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
package com.google.mapsplatform.transportation.sample.kotlinconsumer.provider.service

import com.google.common.base.Predicate
import com.google.common.base.Supplier
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import java.lang.Exception
import java.lang.RuntimeException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/** Retries getting Future if exception is thrown or certain criteria is not met. */
class RetryingFuture(private val executor: ScheduledExecutorService) {
  /**
   * Retries getting Future if exception is thrown or certain criteria is not met.
   *
   * @param futureSupplier entity that can create the target future.
   * @param retries number of retries. Will run forever until success condition is met if set to
   * [.RUN_FOREVER] .
   * @param intervalMillis retry interval in milliseconds.
   * @param successCondition if evaluates to true return future, otherwise retry until condition is
   * met or retry count exhausted.
   * @param <T> return type.
   * @return valid result of type T. </T>
   */
  fun <T> runWithRetries(
    futureSupplier: Supplier<ListenableFuture<T>>,
    retries: Int,
    intervalMillis: Long,
    successCondition: Predicate<T>
  ): ListenableFuture<T> {
    val resultFuture: SettableFuture<T> = SettableFuture.create()
    runWithRetriesInternal(resultFuture, futureSupplier, retries, intervalMillis, successCondition)
    return resultFuture
  }

  private fun <T> runWithRetriesInternal(
    future: SettableFuture<T>,
    futureSupplier: Supplier<ListenableFuture<T>>,
    retries: Int,
    intervalMillis: Long,
    successCondition: Predicate<T>
  ) {
    val immediateFuture: ListenableFuture<T> =
      try {
        futureSupplier.get()
      } catch (e: Exception) {
        handleFailure(future, futureSupplier, retries, intervalMillis, successCondition, e)
        return
      }
    Futures.addCallback(
      immediateFuture,
      object : FutureCallback<T> {
        override fun onSuccess(result: T?) {
          if (successCondition.apply(result)) {
            future.set(result)
          } else {
            val exception = RuntimeException("Success condition not met, retrying.")
            handleFailure(
              future,
              futureSupplier,
              retries,
              intervalMillis,
              successCondition,
              exception
            )
          }
        }

        override fun onFailure(t: Throwable) {
          handleFailure(future, futureSupplier, retries, intervalMillis, successCondition, t)
        }
      },
      MoreExecutors.directExecutor()
    )
  }

  private fun <T> handleFailure(
    future: SettableFuture<T>,
    futureSupplier: Supplier<ListenableFuture<T>>,
    retries: Int,
    delayInMillis: Long,
    successCondition: Predicate<T>,
    t: Throwable
  ) {
    if (retries == RUN_FOREVER || retries > 0) {
      executor.schedule(
        {
          val newRetriesCount = if (retries == RUN_FOREVER) RUN_FOREVER else retries - 1
          runWithRetriesInternal(
            future,
            futureSupplier,
            newRetriesCount,
            delayInMillis,
            successCondition
          )
        },
        delayInMillis,
        TimeUnit.MILLISECONDS
      )
    } else {
      future.setException(t)
    }
  }

  companion object {
    var RUN_FOREVER = Int.MIN_VALUE
  }
}
