package com.example.placepickerv2

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import com.example.placepickerv2.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnCameraIdleListener, GoogleMap.OnCameraMoveStartedListener {

    private lateinit var mMap: GoogleMap
    private lateinit var mBind: ActivityMapsBinding
    private lateinit var mapFragment: SupportMapFragment

    private lateinit var mFusedLocation: FusedLocationProviderClient
    private var mLocationPermissionGranted = false
    private val DEFAULT_ZOOM = 15f
    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 108

    private var oldLatLng: LatLng = LatLng(-6.988608203410685, 110.41835728145327)
    private lateinit var newLatLng: LatLng
    private var newAddress = ""

    companion object {
        val KOORDINAT = "maps_location"
        val ADDRESS = "maps_address"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBind = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(mBind.root)

        mBind.btnConf.setOnClickListener {
            val intn = Intent()
            val bund = Bundle()
            val koordinat = "" + newLatLng.latitude + "," + newLatLng.longitude
            bund.putString(KOORDINAT, koordinat)
            bund.putString(ADDRESS, newAddress)
            intn.putExtras(bund)
            setResult(RESULT_OK, intn)
            finish()
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        getLocationPermission()
        mFusedLocation = LocationServices.getFusedLocationProviderClient(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        googleMap.let {
            mMap = googleMap
            mMap.clear()
            mMap.uiSettings.isZoomControlsEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
            mMap.uiSettings.isCompassEnabled = true
            // membuat peta tidak bisa digeser
//            mMap.uiSettings.isScrollGesturesEnabled = false

            getCurretLocation()
            mMap.setOnCameraIdleListener(this)
            mMap.setOnCameraMoveStartedListener(this)

            setMyLocation()
            oldLatLng = mMap.cameraPosition.target
        }
    }

    override fun onCameraIdle() {
        mMap.let {
            it.cameraPosition.let { position ->
                newLatLng = mMap.cameraPosition.target
                if (oldLatLng != newLatLng) {
                    oldLatLng = newLatLng
                    mBind.iconMarker.animate().translationY(0f).start()
                    mBind.iconMarkerShadow.animate().withStartAction {
                        mBind.iconMarkerShadow.setPadding(0)
                    }.start()
                }
                newAddress = getGeocoderAddress(this, newLatLng)
                mBind.tvPlace.text = newAddress
            }

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        mLocationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true
                    getCurretLocation()
                    setMyLocation()
                }
            }
        }
    }

    private fun setMyLocation() {
        try {
            if (mLocationPermissionGranted)
                mMap.isMyLocationEnabled = true
            else
                getLocationPermission()
        } catch (e: SecurityException) {

        }
    }

    private fun getLocationPermission() {
        mLocationPermissionGranted = false
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }

    private fun getCurretLocation() {
        try {
            if (mLocationPermissionGranted) {
                val locationResult: Task<Location> = mFusedLocation.getLastLocation()
                locationResult.addOnCompleteListener {
                    if (it.isComplete && it.result != null) {
                        val locRslt = it.result
                        oldLatLng = LatLng(locRslt.latitude, locRslt.longitude)
                    }
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(oldLatLng, DEFAULT_ZOOM))
                }
            } else {
                getLocationPermission()
            }
        } catch (e: SecurityException) {

        }

    }


    private fun animateCamera() {
        mMap.animateCamera(CameraUpdateFactory.newLatLng(oldLatLng))
    }

    override fun onCameraMoveStarted(p0: Int) {
        mBind.iconMarker.animate().translationY(-50f).start()
        mBind.iconMarkerShadow.animate().withStartAction {
            mBind.iconMarkerShadow.setPadding(10)
        }.start()
    }

    private fun getGeocoderAddress(context: Context, latLng: LatLng): String {
        var sAddress = ""
        if (latLng != null) {
            try {
                var lisAddress: List<Address>
                val geoCode = Geocoder(context)
                lisAddress = geoCode.getFromLocation(latLng.latitude, latLng.longitude, 2)
                if (lisAddress.size > 0) {
                    sAddress += if (lisAddress[0].locality.isNullOrEmpty()) "" else lisAddress[0].locality + ", "
                    sAddress += if (lisAddress[0].subAdminArea.isNullOrEmpty()) "" else lisAddress[0].subAdminArea + ", "
                    sAddress += lisAddress[0].adminArea
                }
            } catch (e: Exception) {
            }
        }
        return sAddress
    }

}