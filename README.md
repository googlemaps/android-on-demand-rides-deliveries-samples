# On-Demand Rides and Deliveries Android Samples

This repository contains the source code of the following samples

1. Driver SDK sample (written in Java) in `/java/driver` directory.
2. Consumer SDK sample (written in Java) in `/java/consumer` directory.

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
4. This project follows Google's Java code style guide (https://google.github.io/styleguide/javaguide.html), please consider integrating into your IDE a plugin to automatically format your code as per guidelines. Example: https://github.com/google/google-java-format#intellij-android-studio-and-other-jetbrains-ides

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

#### Use Android Studio
The project can also be imported into Android Studio. The binary targets are
`java.driver` and `java.consumer`, they can be deployed and launched by clicking
Android Studio's "run" button.

## Important references

- [Installing Cloud SDK](https://cloud.google.com/sdk/docs/install)
- [Transportation SDKs - Android SDK Setup](https://developers.google.com/maps/documentation/transportation-logistics/android_sdk_setup)
- [Option 1: Using the credentials helper plugin (recommended)](https://developers.google.com/maps/documentation/transportation-logistics/android_sdk_setup#option_1_using_the_credentials_helper_plugin_recommended)
