package com.leandrocesar.routemaps

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.GoogleMap.OnMyLocationClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.leandrocesar.routemaps.util.PermissionUtils.RationaleDialog.Companion.newInstance
import com.leandrocesar.routemaps.util.PermissionUtils.isPermissionGranted
import com.leandrocesar.routemaps.util.GeocoderUtil
import com.leandrocesar.routemaps.util.PermissionUtils

class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
        OnMyLocationButtonClickListener,
        OnMyLocationClickListener, OnRequestPermissionsResultCallback{

    private lateinit var mMap: GoogleMap
    private lateinit var geocoderUtil: GeocoderUtil
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private val DEFAULT_ZOOM = 17.5f
    private var permissionDenied: Boolean = false
    private val REQUEST_CODE = 1
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var toggleBottomSheetButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        initMaps()
        // Defina a StatusBar transparente
        setTransparentStatusBar(window)
        configBottomSheet()
        initGeocode()
    }

    private fun initMaps() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setPadding(0,80,0,150)
        googleMap.setOnMyLocationButtonClickListener(this)
        googleMap.setOnMyLocationClickListener(this)
        enableMyLocation()
        setupMapListeners()
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun setupMapListeners() {
        mMap.setOnCameraIdleListener {
            val cameraPosition = mMap.cameraPosition
            Log.d("MapCameraPosition", "Latitude: ${cameraPosition.target.latitude}, Longitude: ${cameraPosition.target.longitude}")
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(this, getString(R.string.my_location_button_clicked), Toast.LENGTH_SHORT).show()
        startLocationUpdates()

        return false
    }

    override fun onMyLocationClick(location: Location) {
        val addressText = geocoderUtil.getAddressFromLocation(location)
        Toast.makeText(this, addressText, Toast.LENGTH_LONG)
            .show()
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private fun enableMyLocation() {
        // 1. Check if permissions are granted, if so, enable the my location layer
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            // Get the last known location
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val latLng = LatLng(location.latitude, location.longitude)
                    val animateCamera = (CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM))
                    mMap.moveCamera(animateCamera)
                }
            }
            return
        }

        // 2. If if a permission rationale dialog should be shown
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            PermissionUtils.RationaleDialog.newInstance(
                LOCATION_PERMISSION_REQUEST_CODE, true
            ).show(supportFragmentManager, "dialog")
            return
        }

        // 3. Otherwise, request permission
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun initGeocode() {
        // Inicialize a instância de GeocoderUtil com o contexto
        geocoderUtil = GeocoderUtil(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // Configurar a solicitação de localização
        locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(0)
            .setFastestInterval(0)

        // Configurar o retorno de chamada de localização
        locationCallback = object : LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation.let { location ->
                    // Atualizar a posição do mapa para a localização atual do usuário
                    val userLatLng = LatLng(location!!.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(userLatLng))
                }
            }
        }
    }

    private fun configBottomSheet() {
        // Configurar o BottomSheetBehavior
        val bottomSheetContent = findViewById<FrameLayout>(R.id.bottomSheetContent)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetContent)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.isFitToContents = false // Impede que o BottomSheet se ajuste completamente à tela
        bottomSheetBehavior.isHideable = false // Impede que o BottomSheet seja totalmente escondido

        // Encontre o botão de alternância (ImageView) e configure o OnClickListener
        toggleBottomSheetButton = findViewById(R.id.arrow_top)
        toggleBottomSheetButton.setOnClickListener {
            // Verifique o estado atual do BottomSheet e alterne entre EXPANDED e COLLAPSED
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    private fun setTransparentStatusBar(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.parseColor("#46282828")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
            )
            return
        }

        // Dentro de onRequestPermissionsResult
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION)
                || isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_COARSE_LOCATION)
            ) {
                enableMyLocation()
            } else {
                permissionDenied = true
            }
        }

    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (permissionDenied){
            // Permission was not granted, display error dialog.
            showMissingPermissionError()
            permissionDenied = false
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private fun showMissingPermissionError() {
        newInstance(REQUEST_CODE,true).show(supportFragmentManager, "dialog")
    }

    companion object {
        /**
         * Request code for location permission request.
         *
         * @see .onRequestPermissionsResult
         */
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}