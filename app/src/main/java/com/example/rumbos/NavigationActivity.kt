package com.example.rumbos

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


class NavigationActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var endNavigationButton: Button
    private var originPoint: GeoPoint? = null
    private var destinationPoint: GeoPoint? = null
    private var myLocationOverlay: MyLocationNewOverlay? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize OSMDroid configuration
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        
        setContentView(R.layout.activity_navigation)
        
        mapView = findViewById(R.id.navigationMapView)
        endNavigationButton = findViewById(R.id.endNavigationButton)
        
        // Get origin and destination from intent
        originPoint = GeoPoint(
            intent.getDoubleExtra("origin_lat", 23.1136),
            intent.getDoubleExtra("origin_lon", -82.3667)
        )
        
        destinationPoint = GeoPoint(
            intent.getDoubleExtra("destination_lat", 23.1136),
            intent.getDoubleExtra("destination_lon", -82.3667)
        )
        
        setupMap()
        setupLocationTracking()
        requestLocationPermissions()
        
        // Draw the route
        drawRoute()
        
        // Set up end navigation button
        setupEndNavigationButton()
    }
    
    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setBuiltInZoomControls(true)
        mapView.setMultiTouchControls(true)
        
        // Center on the route initially
        if (originPoint != null) {
            mapView.controller.setCenter(originPoint)
            mapView.controller.setZoom(15.0)
        }
    }
    
    private fun setupLocationTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Initialize location overlay
            myLocationOverlay = MyLocationNewOverlay(mapView)
            mapView.overlays.add(myLocationOverlay)
            myLocationOverlay?.enableMyLocation(enable: true)
            myLocationOverlay?.enableFollowLocation(true)
        }
    }
    
    private fun requestLocationPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        val permissionsToRequest = permissions.filter { 
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED 
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }
    
    private fun drawRoute() {
        if (originPoint != null && destinationPoint != null) {
            // Create a simple line between origin and destination
            // In a real implementation, this would use routing algorithms
            val polyline = Polyline()
            polyline.outlinePaint.color = android.graphics.Color.RED
            polyline.outlinePaint.strokeWidth = 8f
            
            val points = ArrayList<GeoPoint>()
            points.add(originPoint!!)
            points.add(destinationPoint!!)
            
            polyline.setPoints(points)
            mapView.overlays.add(polyline)
            
            // Animate to show the route
            val midPoint = GeoPoint(
                (originPoint!!.latitude + destinationPoint!!.latitude) / 2,
                (originPoint!!.longitude + destinationPoint!!.longitude) / 2
            )
            mapView.controller.animateTo(midPoint)
        }
    }
    
    private fun setupEndNavigationButton() {
        endNavigationButton.setOnClickListener {
            finish() // Close this activity and return to main
        }
    }
    
    override fun onResume() {
        super.onResume()
        mapView.onResume()
        if (myLocationOverlay != null && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            myLocationOverlay?.enableMyLocation(enable: true)
        }
    }
    
    override fun onPause() {
        super.onPause()
        mapView.onPause()
        myLocationOverlay?.disableFollowLocation()
        myLocationOverlay?.disableMyLocation()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        myLocationOverlay?.onDetach(myLocationOverlay?.mapView)
    }
}