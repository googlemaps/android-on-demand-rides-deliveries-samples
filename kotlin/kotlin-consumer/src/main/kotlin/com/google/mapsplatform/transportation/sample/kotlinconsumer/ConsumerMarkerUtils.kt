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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.mapsplatform.transportation.consumer.model.MarkerType
import com.google.android.libraries.mapsplatform.transportation.consumer.view.ConsumerController
import java.lang.IllegalArgumentException

/** Marker util class to get the correct image based on the marker types. */
object ConsumerMarkerUtils {
  /** Anchor value for the markers to display on the map. */
  private const val ANCHOR_VALUE = 0.5f

  /**
   * Based on ConsumerMarkerType, get the MarkerOptions to display on the map with the corresponding
   * images.
   */
  fun getConsumerMarkerOptions(
    context: Context?,
    @ConsumerMarkerType consumerMarkerType: Int
  ): MarkerOptions {
    return when (consumerMarkerType) {
      ConsumerMarkerType.PICKUP_POINT ->
        MarkerOptions()
          .anchor(ANCHOR_VALUE, ANCHOR_VALUE)
          .icon(toBitmapDescriptor(context, R.drawable.ic_custom_pickup))
      ConsumerMarkerType.DROPOFF_POINT ->
        MarkerOptions()
          .anchor(ANCHOR_VALUE, ANCHOR_VALUE)
          .icon(toBitmapDescriptor(context, R.drawable.ic_custom_dropoff))
      ConsumerMarkerType.INTERMEDIATE_DESTINATION_POINT ->
        MarkerOptions()
          .anchor(ANCHOR_VALUE, ANCHOR_VALUE)
          .icon(toBitmapDescriptor(context, R.drawable.ic_intermediate_waypoint))
      ConsumerMarkerType.PREVIOUS_TRIP_PENDING_POINT ->
        MarkerOptions()
          .anchor(ANCHOR_VALUE, ANCHOR_VALUE)
          .icon(toBitmapDescriptor(context, R.drawable.ic_intermediate_destination))
      else ->
        throw IllegalArgumentException(
          String.format("Marker type: %d not supported", consumerMarkerType)
        )
    }
  }

  /** Convert a vector drawable to a bitmap descriptor. */
  private fun toBitmapDescriptor(
    context: Context?,
    @DrawableRes resourceId: Int
  ): BitmapDescriptor {
    val vectorDrawable = AppCompatResources.getDrawable(context!!, resourceId)
    val bitmap =
      Bitmap.createBitmap(
        vectorDrawable!!.intrinsicWidth,
        vectorDrawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
      )
    val canvas = Canvas(bitmap)
    vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
    vectorDrawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
  }

  /** Customize the pickup and drop-off markers. */
  fun setCustomMarkers(mConsumerController: ConsumerController?, context: Context?) {
    val mapStyle = mConsumerController!!.consumerMapStyle
    val pickupMarkerStyleOptions =
      MarkerOptions().icon(toBitmapDescriptor(context, R.drawable.ic_custommarker_pickup))
    mapStyle.setMarkerStyleOptions(MarkerType.TRIP_PICKUP_POINT, pickupMarkerStyleOptions)
    val dropoffMarkerStyleOptions =
      MarkerOptions().icon(toBitmapDescriptor(context, R.drawable.ic_custommarker_destination))
    mapStyle.setMarkerStyleOptions(MarkerType.TRIP_DROPOFF_POINT, dropoffMarkerStyleOptions)
    val intermediateMarkerStyleOptions =
      MarkerOptions().icon(toBitmapDescriptor(context, R.drawable.ic_intermediate_waypoint))
    mapStyle.setMarkerStyleOptions(
      MarkerType.TRIP_INTERMEDIATE_DESTINATION,
      intermediateMarkerStyleOptions
    )
  }
}
