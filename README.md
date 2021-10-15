On-Demand Rides and Deliveries Android Samples
============

## Included samples

This repository contains binaries of the following samples

1. Driver SDK sample (written in Java) in `/java/driver` directory.
2. Consumer SDK samples (written in Java) in `/java/consumer` directory.

### Architecture

In order to run full end-to-end **journey sharing** use case you need 3
components:

- a driver app,
- a consumer app (both included in this repo)
- a provider backend

![diagram](documentation/samples_components.png)

The **provider backend** will be included in a serparate repo as it can be utlized by both Android and iOS client samples.

As depicted in the figure:

- The consumer app and driver app communicate with provider bckend using REST
service calls.
- The provider backend also communicate with FleetEngine.

The end result should be as follow:

![demo](documentation/journey_sharing.gif)

## Getting started

**NOTE**: The following instructions assume your project has gone through the
Maps Platform onboarding process, a project in Google Cloud console has been set
up, and you have the appropriate API keys.

### Step 1 - Set up Google Cloud CLI (required)

Follow the [official guide](https://cloud.google.com/sdk/docs/install) to
install and setup Google Cloud CLI. This will help you easily pull gated SDK
artifacts into the Gradle projects.

Verify that the
[credentials helper plugin](https://developers.google.com/maps/documentation/transportation-logistics/android_sdk_setup#option_1_using_the_credentials_helper_plugin_recommended)
works on your DEV machine correctly.

### Step 2 - Add API key and other metadata (required)
#### Driver sample
In `java/driver/src/main/AndroidManifest.xml`

Add your API key to `android:value` field

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_API_KEY"/>
```

Add your provider ID to `android:value` field

```xml
<meta-data
    android:name="com.example.driver.sampleapp.provider_id"
    android:value="YOUR_PROVIDER_ID"/>
```

Add your provider backend URL(with port number) to `android:value` field

```xml
<meta-data
    android:name="com.example.driver.sampleapp.provider_url"
    android:value="http://10.0.2.2:8080" />
```

**NOTE**: The logic of reading provider ID and URL is in `ProviderUtils.java`.
When changing `android:name="com.example.driver.sampleapp.provider_url"`, be
sure to update the static fields in it.

### Step 3 - Build and run

Import your project to Android Studio, or use Gradle to build and run the
project.

```bash
./gradlew :java:driver:assembleDebug
```

```bash
./gradlew :java:consumer:assembleDebug
```

The binary targets are `java.driver` and `java.consumer`.

Important references
--------------------
- [Installing Cloud SDK](https://cloud.google.com/sdk/docs/install)
- [Transportation SDKs - Android SDK Setup](https://developers.google.com/maps/documentation/transportation-logistics/android_sdk_setup)
- [Option 1: Using the credentials helper plugin (recommended)](https://developers.google.com/maps/documentation/transportation-logistics/android_sdk_setup#option_1_using_the_credentials_helper_plugin_recommended)

License
-------

```
Copyright 2021 Google, Inc.

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
