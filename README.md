# On-Demand Rides and Deliveries Android Samples

This repository contains the source code of the following samples

1. Driver SDK sample (written in Java) in `/java/driver` directory.
2. Consumer SDK sample (written in Java) in `/java/consumer` directory.
3. Driver SDK sample (written in Kotlin) in `/kotlin/kotlin-driver` directory.
4. Consumer SDK sample (written in Kotlin) in `/kotlin/kotlin-consumer` directory.

Note: compared to the Java source, the Kotlin source will have
- The same project structure
- The same arrangement of classes
- Within a class, the same arrangement of variables and methods

Idiomatic Kotlin implementations are applied in the method level so overall
the projects of two languages are consistent.

## Architecture

In order to run the full end-to-end **journey sharing** use case, you need 3
components:

- a [provider backend](https://github.com/googlemaps/java-on-demand-rides-deliveries-stub-provider)
  as it can be utilized by both Android and iOS client samples.
- a driver app (included in this repository)
- a consumer app (included in this repository)

![diagram](documentation/samples_components.png)

As depicted in the figure:

- The consumer app and driver app communicate with the provider backend using
  REST service calls.
- The provider backend also communicates with the Fleet Engine.

The end result should be as follows:

![demo](documentation/journey_sharing.gif)

## Prerequisites
1. Please fully complete [Getting Started with Fleet Engine](https://developers.google.com/maps/documentation/transportation-logistics/on-demand-rides-deliveries-solution/trip-order-progress/fleet-engine)
2. Please make sure the [provider backend](https://github.com/googlemaps/java-on-demand-rides-deliveries-stub-provider)
is up and running.
3. Please make sure two Android emulators are up and running by following
[Create and manage virtual devices](https://developer.android.com/studio/run/managing-avds)
4. This project follows Google's Java code style guide
   (https://google.github.io/styleguide/javaguide.html), please consider
   integrating into your IDE a plugin to automatically format your code as per
   guidelines. Example: https://github.com/google/google-java-format#intellij-android-studio-and-other-jetbrains-ides

## Getting started

### Step 1 - Add API key and other metadata (required)
In project root directory, create a `local.properties` file and add the
following content:

```
MAPS_API_KEY=YOUR_API_KEY
PROVIDER_ID=YOUR_PROVIDER_ID
PROVIDER_URL=http://10.0.2.2:8080
```


### Step 2 - Build and run

#### Use command line

```bash
./gradlew :java:driver:assembleDebug &&\
adb -s YOUR_EMULATOR_ID shell am start -n com.google.mapsplatform.transportation.sample.driver/.SplashScreenActivity
```

```bash
./gradlew :java:consumer:assembleDebug &&\
adb -s YOUR_EMULATOR_ID shell am start -n com.google.mapsplatform.transportation.sample.consumer/.SplashScreenActivity
```

```bash
./gradlew :kotlin:kotlin-driver:assembleDebug &&\
adb -s YOUR_EMULATOR_ID shell am start -n com.google.mapsplatform.transportation.sample.kotlindriver/.SplashScreenActivity
```

```bash
./gradlew :kotlin:kotlin-consumer:assembleDebug &&\
adb -s YOUR_EMULATOR_ID shell am start -n com.google.mapsplatform.transportation.sample.kotlinconsumer/.SplashScreenActivity
```

#### Use Android Studio
The project can also be imported into Android Studio. The binary targets are
`java.driver` and `java.consumer`, they can be deployed and launched by clicking
Android Studio's "run" button.

## Important references

- [Installing Cloud SDK](https://cloud.google.com/sdk/docs/install)
- [Transportation SDKs - Android SDK Setup](https://developers.google.com/maps/documentation/transportation-logistics/android_sdk_setup)
- [Option 1: Using the credentials helper plugin (recommended)](https://developers.google.com/maps/documentation/transportation-logistics/android_sdk_setup#option_1_using_the_credentials_helper_plugin_recommended)

## License

```
Copyright 2022 Google, Inc.

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements.  See the NOTICE file distributed with this work for
additional information regarding copyright ownership.  The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License.  You may obtain a copy of
the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations under
the License.
```
