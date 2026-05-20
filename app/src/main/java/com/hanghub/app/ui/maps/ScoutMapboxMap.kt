package com.hanghub.app.ui.maps

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.util.Log
import com.hanghub.app.data.HHPlace
import com.hanghub.app.data.HHUser
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.viewannotation.ViewAnnotationManager
import com.mapbox.maps.viewannotation.ViewAnnotationUpdateMode
import com.mapbox.maps.viewannotation.annotationAnchor
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import kotlin.math.cos

data class ScoutUserPin(
    val id: String,
    val user: HHUser,
    val xFrac: Float,
    val yFrac: Float,
)

data class ScoutPlacePin(
    val id: String,
    val place: HHPlace,
    val xFrac: Float,
    val yFrac: Float,
)

@Composable
fun ScoutMapboxMap(
    userPins: List<ScoutUserPin>,
    placePins: List<ScoutPlacePin>,
    onUserTap: (HHUser) -> Unit,
    onPlaceTap: (HHPlace) -> Unit,
    modifier: Modifier = Modifier,
    centerLat: Double = 37.7599,
    centerLon: Double = -122.4148,
    zoom: Double = 14.5,
    pitch: Double = 30.0,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isDark = isSystemInDarkTheme()

    val styleUri = remember(isDark) {
        // Prefer bundled style JSON; fall back to a built-in style if missing.
        val asset = if (isDark) "asset://scout-mapbox-style-dark.json" else "asset://scout-mapbox-style-light.json"
        asset
    }

    val latestOnUserTap = rememberUpdatedState(onUserTap)
    val latestOnPlaceTap = rememberUpdatedState(onPlaceTap)

    val mapViewState = remember {
        val camera = CameraOptions.Builder()
            .center(Point.fromLngLat(centerLon, centerLat))
            .zoom(zoom)
            .pitch(pitch)
            .bearing(0.0)
            .build()

        val initOptions = MapInitOptions(
            context = context,
            cameraOptions = camera,
            styleUri = Style.MAPBOX_STREETS,
        )

        val mapView = MapView(context, initOptions)
        mapView.viewAnnotationManager.setViewAnnotationUpdateMode(ViewAnnotationUpdateMode.MAP_SYNCHRONIZED)
        MapViewHolder(mapView = mapView, viewAnnotationManager = mapView.viewAnnotationManager)
    }

    val mapView = mapViewState.mapView
    val viewAnnotationManager = mapViewState.viewAnnotationManager

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Track which style we've loaded to avoid reloading every recomposition.
    val loadedStyle = remember { mutableStateOf<String?>(null) }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { mv ->
            // Gestures
            mv.gestures.apply {
                rotateEnabled = true
                pitchEnabled = true
                pinchToZoomEnabled = true
                doubleTapToZoomInEnabled = true
                doubleTouchToZoomOutEnabled = true
            }

            if (loadedStyle.value != styleUri) {
                loadedStyle.value = styleUri
                mapViewState.pinViews.clear()
                mv.mapboxMap.loadStyle(styleUri) {
                    // Ensure we add annotations on the UI thread.
                    mv.post {
                        syncPins(
                            context = context,
                            holder = mapViewState,
                            userPins = userPins,
                            placePins = placePins,
                            centerLat = centerLat,
                            centerLon = centerLon,
                            onUserTap = { latestOnUserTap.value(it) },
                            onPlaceTap = { latestOnPlaceTap.value(it) },
                        )
                    }
                }
            } else if (mv.mapboxMap.isStyleLoaded()) {
                mv.post {
                    syncPins(
                        context = context,
                        holder = mapViewState,
                        userPins = userPins,
                        placePins = placePins,
                        centerLat = centerLat,
                        centerLon = centerLon,
                        onUserTap = { latestOnUserTap.value(it) },
                        onPlaceTap = { latestOnPlaceTap.value(it) },
                    )
                }
            }
        }
    )
}

private class MapViewHolder(
    val mapView: MapView,
    val viewAnnotationManager: ViewAnnotationManager,
){
    val pinViews: MutableMap<String, android.view.View> = mutableMapOf()
}

private fun syncPins(
    context: Context,
    holder: MapViewHolder,
    userPins: List<ScoutUserPin>,
    placePins: List<ScoutPlacePin>,
    centerLat: Double,
    centerLon: Double,
    onUserTap: (HHUser) -> Unit,
    onPlaceTap: (HHPlace) -> Unit,
) {
    val viewAnnotationManager = holder.viewAnnotationManager
    val desiredIds = buildSet {
        placePins.forEach { add(it.id) }
        userPins.forEach { add(it.id) }
    }

    // Remove stale
    val iterator = holder.pinViews.iterator()
    while (iterator.hasNext()) {
        val (id, view) = iterator.next()
        if (!desiredIds.contains(id)) {
            runCatching { viewAnnotationManager.removeViewAnnotation(view) }
            iterator.remove()
        }
    }

    placePins.forEach { pin ->
        val point = pointFromFrac(pin.xFrac, pin.yFrac, centerLat, centerLon)
        if (holder.pinViews.containsKey(pin.id)) return@forEach
        val view = ScoutPinViews.place(context, pin.place) { onPlaceTap(pin.place) }
        view.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED),
            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED),
        )
        val w = view.measuredWidth.coerceAtLeast(1).toDouble()
        val h = view.measuredHeight.coerceAtLeast(1).toDouble()
        val added = runCatching {
            viewAnnotationManager.addViewAnnotation(
                view,
                viewAnnotationOptions {
                    geometry(point)
                    allowOverlap(true)
                    allowOverlapWithPuck(true)
                    allowZElevate(true)
                    ignoreCameraPadding(true)
                    visible(true)
                    width(w)
                    height(h)
                    annotationAnchor {
                        anchor(ViewAnnotationAnchor.BOTTOM)
                        offsetY(6.0)
                    }
                }
            )
        }.onFailure {
            Log.e("ScoutMapboxMap", "Failed to add place pin ${pin.id}", it)
        }.getOrNull()
        if (added != null) holder.pinViews[pin.id] = view
    }

    userPins.forEach { pin ->
        val point = pointFromFrac(pin.xFrac, pin.yFrac, centerLat, centerLon)
        if (holder.pinViews.containsKey(pin.id)) return@forEach
        val view = ScoutPinViews.user(context, pin.user) { onUserTap(pin.user) }
        view.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED),
            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED),
        )
        val w = view.measuredWidth.coerceAtLeast(1).toDouble()
        val h = view.measuredHeight.coerceAtLeast(1).toDouble()
        val added = runCatching {
            viewAnnotationManager.addViewAnnotation(
                view,
                viewAnnotationOptions {
                    geometry(point)
                    allowOverlap(true)
                    allowOverlapWithPuck(true)
                    allowZElevate(true)
                    ignoreCameraPadding(true)
                    visible(true)
                    width(w)
                    height(h)
                    annotationAnchor {
                        anchor(ViewAnnotationAnchor.CENTER)
                    }
                }
            )
        }.onFailure {
            Log.e("ScoutMapboxMap", "Failed to add user pin ${pin.id}", it)
        }.getOrNull()
        if (added != null) holder.pinViews[pin.id] = view
    }
}

private fun pointFromFrac(
    xFrac: Float,
    yFrac: Float,
    centerLat: Double,
    centerLon: Double,
    spanMeters: Double = 1400.0,
): Point {
    // Convert a normalized screen fraction into a fake coordinate offset around a center point.
    val metersX = (xFrac - 0.5) * spanMeters
    val metersY = (yFrac - 0.5) * spanMeters

    val metersPerDegLat = 111_320.0
    val metersPerDegLon = 111_320.0 * cos(Math.toRadians(centerLat))

    val dLat = -(metersY / metersPerDegLat)
    val dLon = (metersX / metersPerDegLon)

    return Point.fromLngLat(centerLon + dLon, centerLat + dLat)
}
