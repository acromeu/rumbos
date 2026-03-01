package com.example.rumbos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.OSRMRoadManager


class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var searchBox: EditText
    private lateinit var searchButton: Button
    
    private var originPoint: GeoPoint? = null
    private var destinationPoint: GeoPoint? = null
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize OSMDroid configuration
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        
        setContentView(R.layout.activity_main)
        
        mapView = findViewById(R.id.mapView)
        searchBox = findViewById(R.id.searchBox)
        searchButton = findViewById(R.id.searchButton)
        
        // Set up the map
        setupMap()
        
        // Request location permissions
        requestLocationPermissions()
        
        // Set up search functionality
        setupSearch()
    }
    
    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setBuiltInZoomControls(true)
        mapView.setMultiTouchControls(true)
        
        // Center on Havana as default
        val startPoint = GeoPoint(23.1136, -82.3667) // Havana coordinates
        mapView.controller.setCenter(startPoint)
        mapView.controller.setZoom(12.0)
        
        // Try to center on user's location if available
        centerOnUserLocation()
    }
    
    private fun centerOnUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val locationManager = getSystemService(android.location.LocationManager::class.java)
            
            // Get last known location
            try {
                val location = getLastKnownLocation(locationManager)
                if (location != null) {
                    val userPoint = GeoPoint(location.latitude, location.longitude)
                    mapView.controller.animateTo(userPoint)
                    mapView.controller.setZoom(15.0)
                } else {
                    // If we can't get location, stay centered on Havana
                    Toast.makeText(this, "Unable to determine location, showing Havana", Toast.LENGTH_SHORT).show()
                }
            } catch (ex: SecurityException) {
                Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Permission not granted, stay on Havana
            Toast.makeText(this, "Location permission needed for current location", Toast.LENGTH_LONG).show()
        }
    }
    
    @Suppress("DEPRECATION")
    private fun getLastKnownLocation(locationManager: android.location.LocationManager): Location? {
        var location: Location? = null
        
        val providers = locationManager.getProviders(true)
        for (provider in providers) {
            val lastKnownLocation = locationManager.getLastKnownLocation(provider) ?: continue
            
            if (location == null || lastKnownLocation.time > location.time) {
                location = lastKnownLocation
            }
        }
        
        return location
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
    
    private fun setupSearch() {
        searchButton.setOnClickListener {
            val query = searchBox.text.toString().trim()
            if (query.isNotEmpty()) {
                performSearch(query)
            }
        }
        
        // Allow pressing Enter in search box to trigger search
        searchBox.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN && keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                val query = searchBox.text.toString().trim()
                if (query.isNotEmpty()) {
                    performSearch(query)
                }
                true
            } else {
                false
            }
        }
    }
    
    private fun performSearch(query: String) {
        // For now, just show a toast since we don't have real geocoding implemented yet
        // In a real implementation, we would use Nominatim API or similar to geocode the address
        Toast.makeText(this, "Searching for: $query (geocoding not implemented in this demo)", Toast.LENGTH_LONG).show()
        
        // For demonstration purposes, we'll add a marker at a hardcoded location
        // In a real app, this would be the result of geocoding the query
        val searchResultPoint = GeoPoint(23.133 + Math.random() * 0.1, -82.333 + Math.random() * 0.1) // Random location near Havana
        
        // Add marker to map
        val marker = Marker(mapView)
        marker.position = searchResultPoint
        marker.title = query
        mapView.overlays.add(marker)
        mapView.invalidate()
        
        // Set as origin or destination alternately
        if (originPoint == null) {
            originPoint = searchResultPoint
            marker.snippet = "Origin"
            Toast.makeText(this, "Set as Origin", Toast.LENGTH_SHORT).show()
        } else if (destinationPoint == null) {
            destinationPoint = searchResultPoint
            marker.snippet = "Destination"
            Toast.makeText(this, "Set as Destination", Toast.LENGTH_SHORT).show()
            
            // Draw route between origin and destination
            drawRoute()
        } else {
            // Reset and set as new origin
            originPoint = searchResultPoint
            destinationPoint = null
            marker.snippet = "Origin (reset)"
            mapView.overlays.clear() // Clear previous markers
            mapView.overlays.add(marker)
            Toast.makeText(this, "Set as New Origin, please select destination", Toast.LENGTH_SHORT).show()
        }
        
        mapView.controller.animateTo(searchResultPoint)
    }
    
    private fun drawRoute() {
        if (originPoint != null && destinationPoint != null) {
            // Using OSRM Road Manager for routing
            val roadManager: RoadManager = OSRMRoadManager(this)
            val waypoints = ArrayList<GeoPoint>()
            waypoints.add(originPoint!!)
            waypoints.add(destinationPoint!!)
            
            // Get the road asynchronously
            val road = roadManager.getRoad(waypoints)
            
            // Create a polyline to display the road
            val roadOverlay = RoadManager.buildRoadOverlay(road, mapView.resources.getColor(android.R.color.holo_blue_dark), 8, mapView.resources)
            mapView.overlays.add(roadOverlay)
            
            // Animate to show the route
            val midPoint = GeoPoint(
                (originPoint!!.latitude + destinationPoint!!.latitude) / 2,
                (originPoint!!.longitude + destinationPoint!!.longitude) / 2
            )
            mapView.controller.animateTo(midPoint)
            
            // Start navigation activity when route is drawn
            val intent = Intent(this, NavigationActivity::class.java)
            intent.putExtra("origin_lat", originPoint!!.latitude)
            intent.putExtra("origin_lon", originPoint!!.longitude)
            intent.putExtra("destination_lat", destinationPoint!!.latitude)
            intent.putExtra("destination_lon", destinationPoint!!.longitude)
            startActivity(intent)
        }
    }
    
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }
    
    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}