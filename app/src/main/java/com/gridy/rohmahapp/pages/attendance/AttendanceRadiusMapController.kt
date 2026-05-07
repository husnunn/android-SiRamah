package com.gridy.rohmahapp.pages.attendance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.gridy.rohmahapp.R
import com.gridy.rohmahapp.data.model.AttendanceMapUi
import timber.log.Timber

/**
 * Meng-handle [SupportMapFragment], konfigurasi [GoogleMap], sinkron pending [AttendanceMapUi],
 * serta overlay marker / lingkaran / kamera area radius absensi.
 *
 * [AttendanceFragment] hanya menghubung ViewBinding kartu/teks dengan [attachMapFragment]/[present]/[clearOverlayState].
 */
class AttendanceRadiusMapController(
    private val host: Fragment,
    @IdRes private val mapContainerViewId: Int,
) {

    private var googleMap: GoogleMap? = null
    private var pendingMapUi: AttendanceMapUi? = null

    private fun bitmapDescriptorFromVector(
        context: Context,
        @DrawableRes vectorResId: Int
    ): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
            ?: return BitmapDescriptorFactory.defaultMarker()

        val width = if (vectorDrawable.intrinsicWidth > 0) {
            vectorDrawable.intrinsicWidth
        } else {
            (40 * context.resources.displayMetrics.density).toInt()
        }

        val height = if (vectorDrawable.intrinsicHeight > 0) {
            vectorDrawable.intrinsicHeight
        } else {
            (40 * context.resources.displayMetrics.density).toInt()
        }

        vectorDrawable.setBounds(0, 0, width, height)

        val bitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    fun attachMapFragment(
        fragmentManager: FragmentManager,
        /** Dipakai `post { … }` agar ukuran kartu/host terukur sama seperti perilaku lama. */
        layoutAnchorCard: View,
        mapOuter: View,
    ) {
        var mapFragment =
            fragmentManager.findFragmentById(mapContainerViewId) as? SupportMapFragment
        if (mapFragment == null) {
            mapFragment = SupportMapFragment()
            fragmentManager.beginTransaction()
                .replace(mapContainerViewId, mapFragment)
                .commitNow()
        }

        mapFragment.getMapAsync { map ->
            Timber.tag(LOG_TAG).d("onMapReady called")
            googleMap = map
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            map.setMapStyle(null)
            map.uiSettings.apply {
                isZoomControlsEnabled = true
                isMapToolbarEnabled = false
            }
            map.setOnMapLoadedCallback {
                Timber.tag(LOG_TAG).d("onMapLoadedCallback — layer basemap siap ditampilkan")
            }
            tryRender(reason = "onMapReady")
            scheduleRetryAfterLayout(
                layoutAnchorCard,
                mapOuter,
                reasonPrefix = "onMapReady"
            )
        }
    }

    fun installNestedScrollPassthrough(mapFrame: View, nestedScrollOuter: ViewGroup) {
        mapFrame.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN ->
                    nestedScrollOuter.requestDisallowInterceptTouchEvent(true)

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    nestedScrollOuter.requestDisallowInterceptTouchEvent(false)
            }
            false
        }
    }

    fun onDestroyView() {
        googleMap = null
        pendingMapUi = null
    }

    /** Data peta baru + pastikan ada pass layout seperti saat kartu baru `VISIBLE`. */
    fun present(mapUi: AttendanceMapUi, layoutAnchorCard: View, mapOuter: View) {
        pendingMapUi = mapUi
        Timber.tag(LOG_TAG).d(
            "map UI queued: showMapSection=true site=%s,%s user=%s,%s radius_m=%s",
            mapUi.siteLatitude,
            mapUi.siteLongitude,
            mapUi.userLatitude,
            mapUi.userLongitude,
            mapUi.radiusMeter
        )
        scheduleRetryAfterLayout(
            layoutAnchorCard,
            mapOuter,
            reasonPrefix = "applyAttendanceMapPresentation"
        )
    }

    fun clearOverlayState() {
        pendingMapUi = null
        Timber.tag(LOG_TAG).d("map section hidden: showMapSection=false")
        googleMap?.clear()
    }

    private fun scheduleRetryAfterLayout(
        layoutAnchorCard: View,
        mapOuter: View,
        reasonPrefix: String,
    ) {
        layoutAnchorCard.post {
            if (!isHostViewReady()) return@post
            mapOuter.requestLayout()
            logMapContainer(stage = "$reasonPrefix/post-layout", layoutAnchorCard, mapOuter)
            tryRender(reason = "$reasonPrefix/post-layout")
        }
    }

    private fun logMapContainer(stage: String, layoutAnchorCard: View, mapOuter: View) {
        fun visLbl(x: Int) =
            when (x) {
                View.VISIBLE -> "VISIBLE"
                View.GONE -> "GONE"
                View.INVISIBLE -> "INVISIBLE"
                else -> x.toString()
            }
        Timber.tag(LOG_TAG).d(
            "map container [%s] card.vis=%s map.vis=%s mapWxH=%sx%s measuredWxH=%sx%s alpha=%s",
            stage,
            visLbl(layoutAnchorCard.visibility),
            visLbl(mapOuter.visibility),
            mapOuter.width,
            mapOuter.height,
            mapOuter.measuredWidth,
            mapOuter.measuredHeight,
            mapOuter.alpha
        )
    }

    private fun isHostViewReady(): Boolean = host.view != null && host.isAdded

    private fun tryRender(reason: String) {
        Timber.tag(LOG_TAG).d(
            "tryRenderAttendanceMap(reason=%s) googleMapReady=%s pendingShowSection=%s",
            reason,
            googleMap != null,
            pendingMapUi?.showMapSection == true
        )
        val map = googleMap ?: run {
            Timber.tag(LOG_TAG).d(
                "skip render: GoogleMap belum siap — data pending akan di-render dari onMapReady"
            )
            return
        }
        val ui = pendingMapUi
        when {
            ui == null -> {
                Timber.tag(LOG_TAG).d("skip render: pending map UI kosong")
                map.clear()
            }
            !ui.showMapSection -> {
                Timber.tag(LOG_TAG).d("skip render: showMapSection=false")
                map.clear()
            }
            else -> {
                Timber.tag(LOG_TAG).d("BOTH READY (map + mapUi) → renderMap")
                render(map, ui)
            }
        }
    }

    private fun render(map: GoogleMap, ui: AttendanceMapUi) {
        if (!isHostViewReady()) {
            Timber.tag(LOG_TAG).w("renderMap aborted: host view tidak siap")
            return
        }
        Timber.tag(LOG_TAG).d(
            "renderMap called mapType=%s (expect NORMAL=%s) showMapSection=%s",
            map.mapType,
            GoogleMap.MAP_TYPE_NORMAL,
            ui.showMapSection
        )

        map.clear()

        val school = ui.toSchoolLatLng()
        if (school == null) {
            Timber.tag(LOG_TAG).e(
                "renderMap FAILED: school LatLng null siteLat=%s siteLng=%s showSection=%s",
                ui.siteLatitude,
                ui.siteLongitude,
                ui.showMapSection
            )
            return
        }

        Timber.tag(LOG_TAG).d(
            "site lat/lng/radius_m = %s / %s / %s",
            ui.siteLatitude,
            ui.siteLongitude,
            ui.radiusMeter
        )
        Timber.tag(LOG_TAG).d(
            "user lat/lng = %s / %s",
            ui.userLatitude,
            ui.userLongitude
        )

        val ctx = host.requireContext()
        val density = ctx.resources.displayMetrics.density

        map.addMarker(
            MarkerOptions()
                .position(school)
                .title(ui.siteName ?: ctx.getString(R.string.attendance_map_marker_school_title))
                .snippet(ctx.getString(R.string.attendance_map_marker_school_snippet))
                .icon(
                    bitmapDescriptorFromVector(
                        ctx,
                        R.drawable.ic_school_building_marker
                    )
                )
        )
        Timber.tag(LOG_TAG).d("markers added: school=yes")

        var circleAdded = false
        ui.radiusMeter?.takeIf { it > 0 }?.let { r ->
            map.addCircle(
                CircleOptions()
                    .center(school)
                    .radius(r)
                    .strokeWidth(density * 2f)
                    .strokeColor(ContextCompat.getColor(ctx, R.color.error_red_stroke_map))
                    .fillColor(ContextCompat.getColor(ctx, R.color.error_red_fill_map))
            )
            circleAdded = true
            Timber.tag(LOG_TAG).d("circle added radius_m=%s center=%s", r, school)
        }
        if (!circleAdded) {
            Timber.tag(LOG_TAG).d("circle skipped radius_m=%s", ui.radiusMeter)
        }

        val user = ui.toUserLatLng()
        if (user != null) {
            map.addMarker(
                MarkerOptions()
                    .position(user)
                    .title(ctx.getString(R.string.attendance_map_marker_user_title))
            )
            Timber.tag(LOG_TAG).d("markers added: user=yes position=%s", user)
        } else {
            Timber.tag(LOG_TAG).d("markers added: user=no (coords tidak tersedia)")
        }

        val mapOuterView = host.requireView().findViewById<View>(mapContainerViewId)
        mapOuterView.post {
            if (!isHostViewReady()) return@post
            fitViewport(map, school, user, ui.radiusMeter)
            mapOuterView.invalidate()
            Timber.tag(LOG_TAG).d("invalidate map host setelah camera")
        }
    }

    private fun fitViewport(map: GoogleMap, school: LatLng, user: LatLng?, radiusMeters: Double?) {
        val padding = (120 * host.resources.displayMetrics.density).toInt()
        if (user != null) {
            try {
                val bounds = LatLngBounds.Builder().include(school).include(user).build()
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                capZoom(map)
                Timber.tag(LOG_TAG).d(
                    "camera moved: LatLngBounds pad=%s zoom=%s school=%s user=%s",
                    padding,
                    map.cameraPosition.zoom,
                    school,
                    user
                )
                return
            } catch (e: IllegalStateException) {
                Timber.tag(LOG_TAG).w(e, "LatLng bounds tidak valid school=%s user=%s", school, user)
            } catch (e: Exception) {
                Timber.tag(LOG_TAG).w(e, "moveCamera(bounds) gagal school=%s user=%s", school, user)
            }
        }
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(school, zoomForRadius(radiusMeters)))
        capZoom(map)
        Timber.tag(LOG_TAG).d(
            "camera moved: newLatLngZoom school=%s zoom=%s",
            school,
            map.cameraPosition.zoom
        )
    }

    private fun capZoom(map: GoogleMap) {
        if (map.cameraPosition.zoom > MAX_DETAIL_ZOOM) {
            map.moveCamera(CameraUpdateFactory.zoomTo(MAX_DETAIL_ZOOM))
        }
    }

    private fun zoomForRadius(@Suppress("UNUSED_PARAMETER") radiusMeters: Double?): Float =
        KECAMATAN_AREA_ZOOM

    companion object {
        const val LOG_TAG = "AttendanceMap"

        /** Skala area (nilai lebih besar = lebih dekat). */
        private const val KECAMATAN_AREA_ZOOM = 17f
        private const val MAX_DETAIL_ZOOM = KECAMATAN_AREA_ZOOM
    }
}

private fun AttendanceMapUi.toSchoolLatLng(): LatLng? {
    val la = siteLatitude ?: return null
    val lo = siteLongitude ?: return null
    if (la.isNaN() || lo.isNaN()) return null
    return LatLng(la, lo)
}

private fun AttendanceMapUi.toUserLatLng(): LatLng? {
    val la = userLatitude ?: return null
    val lo = userLongitude ?: return null
    if (la.isNaN() || lo.isNaN()) return null
    return LatLng(la, lo)
}
