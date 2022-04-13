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
package com.google.mapsplatform.transportation.sample.consumer;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A lifecycle-aware observable that sends only new updates after subscription, used for events like
 * navigation and Snackbar messages.
 *
 * <p>This avoids a common problem with events: on configuration change (like rotation) an update
 * can be emitted if the observer is active. This LiveData only calls the observable if there's an
 * explicit call to setValue().
 *
 * <p>Note that only one observer is going to be notified of changes.
 *
 * <p>Copied from googlesamples:
 * https://github.com/googlesamples/android-architecture/blob/dev-todo-mvvm-live/todoapp/app/src/main/java/com/example/android/architecture/blueprints/todoapp/SingleLiveEvent.java
 */
public final class SingleLiveEvent<T> extends MutableLiveData<T> {
  private final AtomicBoolean pending = new AtomicBoolean(false);

  @MainThread
  @Override
  public void observe(LifecycleOwner owner, final Observer<? super T> observer) {
    // Observe the internal MutableLiveData
    super.observe(owner, t -> {
      if (pending.compareAndSet(true, false)) {
        observer.onChanged(t);
      }
    });
  }

  @MainThread
  @Override
  public void setValue(@Nullable T t) {
    pending.set(true);
    super.setValue(t);
  }
}
