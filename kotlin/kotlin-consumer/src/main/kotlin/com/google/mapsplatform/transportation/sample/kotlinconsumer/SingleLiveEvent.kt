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
package com.google.mapsplatform.transportation.sample.kotlinconsumer

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A lifecycle-aware observable that sends only new updates after subscription, used for events like
 * navigation and Snackbar messages.
 *
 * This avoids a common problem with events: on configuration change (like rotation) an update can
 * be emitted if the observer is active. This LiveData only calls the observable if there's an
 * explicit call to setValue().
 *
 * Note that only one observer is going to be notified of changes.
 *
 * Copied from googlesamples:
 * https://github.com/googlesamples/android-architecture/blob/dev-todo-mvvm-live/todoapp/app/src/main/java/com/example/android/architecture/blueprints/todoapp/SingleLiveEvent.java
 */
class SingleLiveEvent<T> : MutableLiveData<T?>() {
  private val pending = AtomicBoolean(false)
  @MainThread
  override fun observe(owner: LifecycleOwner, observer: Observer<in T?>) {
    // Observe the internal MutableLiveData
    super.observe(
      owner,
      { t: T? ->
        if (pending.compareAndSet(true, false)) {
          observer.onChanged(t)
        }
      }
    )
  }

  @MainThread
  override fun setValue(t: T?) {
    pending.set(true)
    super.setValue(t)
  }
}
