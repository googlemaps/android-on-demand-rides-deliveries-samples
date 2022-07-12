# On-Demand Rides and Deliveries Android Samples

This repository contains the source code of the following samples

1. Driver SDK sample (written in Java) in `java/driver` directory.
2. Consumer SDK sample (written in Java) in `java/consumer` directory.
3. Driver SDK sample (written in Kotlin) in `kotlin/kotlin-driver` directory.
4. Consumer SDK sample (written in Kotlin) in `kotlin/kotlin-consumer` directory.

Compared to the Java source, the Kotlin source has the same project structure
and the same arrangement of classes and methods.
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
1. Fully complete the Prerequisites section of
   [Getting Started with Fleet Engine](https://developers.google.com/maps/documentation/transportation-logistics/on-demand-rides-deliveries-solution/trip-order-progress/fleet-engine).
2. Make sure the
   [provider backend](https://github.com/googlemaps/java-on-demand-rides-deliveries-stub-provider)
   is up and running.
3. Make sure two Android emulators are up and running by following
   [Create and manage virtual devices](https://developer.android.com/studio/run/managing-avds).
4. Get an API key for the Maps SDK for Android by following
   [Using API Keys](https://developers.google.com/maps/documentation/android-sdk/get-api-key).

## Getting started

### Step 1 - Add API key and other metadata (required)
In project root directory, create a `local.properties` file and add the
following content:

```
MAPS_API_KEY=<YOUR_MAPS_API_KEY>
PROVIDER_ID=<YOUR_PROVIDER_ID>
PROVIDER_URL=http://10.0.2.2:8080
```

`<YOUR_MAPS_API_KEY>` is your API key for the Maps SDK for Android.
`<YOUR_PROVIDER_ID>` is the Project ID of your Google Cloud Project that contains
the service account used to call the Fleet Engine APIs.

### Step 2 - Build and run

#### Use Android Studio
The project can be imported into Android Studio. The binary targets are
`java.driver` and `java.consumer`. They can be deployed and launched by clicking
Android Studio's "run" button.

#### Use command line

```bash
./gradlew :java:driver:assembleDebug &&\
adb -s <YOUR_EMULATOR_ID> shell am start -n com.google.mapsplatform.transportation.sample.driver/.SplashScreenActivity
```

```bash
./gradlew :java:consumer:assembleDebug &&\
adb -s <YOUR_EMULATOR_ID> shell am start -n com.google.mapsplatform.transportation.sample.consumer/.SplashScreenActivity
```

```bash
./gradlew :kotlin:kotlin-driver:assembleDebug &&\
adb -s <YOUR_EMULATOR_ID> shell am start -n com.google.mapsplatform.transportation.sample.kotlindriver/.SplashScreenActivity
```

```bash
./gradlew :kotlin:kotlin-consumer:assembleDebug &&\
adb -s <YOUR_EMULATOR_ID> shell am start -n com.google.mapsplatform.transportation.sample.kotlinconsumer/.SplashScreenActivity
```

## Important references

- [Installing Cloud SDK](https://cloud.google.com/sdk/docs/install)
- [Getting started with the Driver SDK for Android](https://developers.google.com/maps/documentation/transportation-logistics/on-demand-rides-deliveries-solution/trip-order-progress/driver-sdk/driver_sdk_quickstart_android)
- [Getting started with the Consumer SDK for Android](https://developers.google.com/maps/documentation/transportation-logistics/on-demand-rides-deliveries-solution/trip-order-progress/consumer-sdk/consumer_sdk_quickstart_android)
- [Add the API key to your app](https://developers.google.com/maps/documentation/transportation-logistics/on-demand-rides-deliveries-solution/trip-order-progress/driver-sdk/driver_sdk_quickstart_android#add_the_api_key_to_your_app)

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
