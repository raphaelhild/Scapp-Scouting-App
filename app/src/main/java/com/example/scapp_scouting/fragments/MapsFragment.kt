package com.example.scapp_scouting.fragments

import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import com.example.scapp_scouting.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapsFragment : Fragment() {

    private lateinit var currentMapLocation: LatLng
    private var currentCircleRadius: Double = 1000.0
    private lateinit var gMap: GoogleMap

    private val callback = OnMapReadyCallback { googleMap ->
        gMap = googleMap
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        val geocoder = Geocoder(this.context)

        // Map-Settings
        googleMap.uiSettings.isMapToolbarEnabled = false // Toolbar ("Open Maps and Route to - Symbols")
        googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE // Map-Style

        // Geocoding and Marker
        val coordinates = geocoder.getFromLocationName("Furtwangen,I-Bau", 1)  // add these two lines
        currentMapLocation = LatLng(coordinates[0].latitude, coordinates[0].longitude)
        googleMap.addCircle(             // Generierung des ersten Kreises, bzw. Radius für die Anzeige von Markern
            CircleOptions()
                .center(currentMapLocation)
                .radius(1000.0)
                .strokeColor(Color.RED)
                .fillColor(0x22ff0000)
        )
        googleMap.addMarker(             // Test zum Setzen eines Markers
            MarkerOptions()
                .position(currentMapLocation)
                .title(coordinates[0].postalCode.toString() + " " + coordinates[0].locality)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_icon_bounded)) // alternatively: marker_icon
                .snippet(coordinates[0].countryName.toString())
        )
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentMapLocation, 14.0f))

        //TODO: currentMapLocation mit aktuellem Standort definieren?
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.fragment_maps, container, false)

        val btnMapType = view.findViewById<View>(R.id.btnChangeMapType)
        btnMapType.setOnClickListener { changeMapType() }

        val btnIncreaseRadius = view.findViewById<View>(R.id.btnIncreaseRadius)
        btnIncreaseRadius.setOnClickListener { increaseCircleRadius() }

        val btnDecreaseRadius = view.findViewById<View>(R.id.btnDecreaseRadius)
        btnDecreaseRadius.setOnClickListener { decreaseCircleRadius() }

        //Aktion nach und während der Eingabe im Suchfeld
        //TODO: SearchView reparieren
        /*val searchView = view.findViewById<View>(R.id.navigationSearchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }
            override fun onQueryTextChange(newText: String): Boolean {
                return true
            }
        })*/

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    fun setNewLocation(newLocation: String){
        val geocoder = Geocoder(this.context)
        try {
            val coordinates = geocoder.getFromLocationName(newLocation, 1)  // add these two lines
            currentMapLocation = LatLng(coordinates[0].latitude, coordinates[0].longitude)
            currentCircleRadius = 1000.0             // Zurücksetzen des Circle-Radius
            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentMapLocation, 14.0f))
            gMap.clear()                             // Bestehende Kreise und Marker entfernen
            gMap.addCircle(                          // Kreis im Standard-Radius einfügen
                CircleOptions()
                    .center(currentMapLocation)
                    .radius(currentCircleRadius)
                    .strokeColor(Color.RED)
                    .fillColor(0x22ff0000)
            )
            updateMarkerInRadius()                   // Marker im Radius neu setzen

        } catch (e: Exception) {
            Log.e("MapErrors", "Keine Koordinaten zur Zieleingabe gefunden. Fehlermeldung: $e")
        }
    }

    fun increaseCircleRadius(){
        currentCircleRadius += 200               // Radius vergrößern
        updateCircleRadius(currentCircleRadius)
    }

    fun decreaseCircleRadius(){
        currentCircleRadius -= 200               // Radius verkleinern
        updateCircleRadius(currentCircleRadius)
    }

    private fun updateCircleRadius(radius: Double){
        gMap.clear()                             // Bestehende Kreise und Marker entfernen
        gMap.addCircle(                          // Kreis mit vorgegebenem Radius einfügen
            CircleOptions()
                .center(currentMapLocation)
                .radius(radius)
                .strokeColor(Color.RED)
                .fillColor(0x22ff0000)
        )
        updateMarkerInRadius()                   // Marker im Radius neu setzen
    }

    private fun updateMarkerInRadius(){
        // TODO: Hier dann jedes mal alle Marker im Radius laden
        // Überprüfung der Marker innerhalb des Radius mit getDistanceBetweenTwoPoints()
    }
    fun changeMapType(){
        when (gMap.mapType) {
            GoogleMap.MAP_TYPE_SATELLITE -> {
                gMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            }
            GoogleMap.MAP_TYPE_NORMAL -> {
                gMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            }
            else -> {
                gMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            }
        }
    }
    fun getDistanceBetweenTwoPoints(start: LatLng, end: LatLng) : Float {
        val startPoint = Location("locationA")
        startPoint.setLatitude(start.latitude)
        startPoint.setLongitude(start.longitude)

        val endPoint = Location("locationA")
        endPoint.setLatitude(end.latitude)
        endPoint.setLongitude(end.longitude)

        return startPoint.distanceTo(endPoint)
    }
}