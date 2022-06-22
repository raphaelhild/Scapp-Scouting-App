package com.example.scapp_scouting.fragments

import android.content.Intent
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.scapp_scouting.CreateMarker
import com.example.scapp_scouting.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_maps.*

class MapsFragment : Fragment() {

    //Variablen für die Karte
    private lateinit var currentMapLocation: LatLng
    private var currentCircleRadius: Double = 1000.0
    private lateinit var gMap: GoogleMap

    //Variablen für das InfoWindow
    private lateinit var infoWindow : View
    private var markerSelected = ""
    private var infoWindowStatus = false

    //Variablen für  LocationAdd
    private var addLocationStatus = false

    //Variablen für die Datenbank
    private val db = Firebase.firestore

    private val callback = OnMapReadyCallback { googleMap ->
        //Initialisierung
        gMap = googleMap
        val geocoder = Geocoder(this.context)

        // Map-Settings
        gMap.uiSettings.isMapToolbarEnabled = false // Toolbar ("Open Maps and Route to - Symbols")
        gMap.mapType = GoogleMap.MAP_TYPE_SATELLITE // Map-Style

        // Geocoding and Marker
        val coordinates = geocoder.getFromLocationName("Furtwangen,I-Bau", 1)  // add these two lines
        currentMapLocation = LatLng(coordinates[0].latitude, coordinates[0].longitude)

        //Anfangsradius für die Suche (aktuell 1km)
        showCircleWithRadius(1000.0)

        //Lade alle Marker im Anfangsradius
        showMarkerInRadius()

        //Kamerafahrt zum Startpunkt der Karte (currentMapLocation)
        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentMapLocation, 14.0f))

        //Listener zur Erstellung einer neuen Location
        gMap.setOnMapClickListener {
            if(addLocationStatus){
                addLocationStatus = false
                slideUp(btnAddLocation)
                openCreateMarker(latLng = it)
            }
        }

        //InfoWindow (Wird angezeigt bei Klick auf Marker)
        val infoWindowTitle = view?.findViewById<TextView>(R.id.markerInfoTitle)
        val infoWindowSnippet = view?.findViewById<TextView>(R.id.markerInfoSnippet)
        gMap.setOnMarkerClickListener { marker ->
            if(!infoWindowStatus) {
                infoWindowStatus = true
                markerSelected = marker.snippet.toString()
                slideUp(infoWindow)
                if (infoWindowTitle != null) {
                    infoWindowTitle.text = marker.title.toString()
                }
                if (infoWindowSnippet != null) {
                    infoWindowSnippet.text = marker.snippet.toString()
                }
            }
            else if (markerSelected != marker.snippet.toString()){
                infoWindowStatus = true
                markerSelected = marker.snippet.toString()
                slideUp(infoWindow)
                if (infoWindowTitle != null) {
                    infoWindowTitle.text = marker.title.toString()
                }
                if (infoWindowSnippet != null) {
                    infoWindowSnippet.text = marker.snippet.toString()
                }
            }
            else{
                slideDown(infoWindow)
                infoWindowStatus = false
                markerSelected = ""
            }
            true
        }
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

        val btnAddLocation = view.findViewById<View>(R.id.btnAddLocation)
        btnAddLocation.setOnClickListener {
            addLocation()
        }

        infoWindow = view.findViewById(R.id.markerInfoWindow)

        //Aktion nach und während der Eingabe im Suchfeld
        val searchView = view.findViewById<View>(R.id.navigationSearchView)
        checkSearchView(searchView as SearchView)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    //Animationen für das InfoWindow
    fun slideDown(view: View) {
        view.animate()
            .translationY(view.height.toFloat())
            .alpha(0f)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    view.alpha = 1f
                    view.translationY = 0f
                }
            })
    }
    fun slideUp(view: View) {
        view.visibility = View.VISIBLE
        view.alpha = 0f
        if (view.height > 0) {
            slideUpNow(view)
        } else {
            // wait till height is measured
            view.post { slideUpNow(view) }
        }
    }
    private fun slideUpNow(view: View) {
        view.translationY = view.height.toFloat()
        view.animate()
            .translationY(0f)
            .alpha(1f)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.VISIBLE
                    view.alpha = 1f
                }
            })
    }

    //Funktionen für die Location-Suche
    private fun checkSearchView(search: SearchView) {
        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                setNewLocation(query)
                return false
            }
            override fun onQueryTextChange(newText: String): Boolean {
                return true
            }
        })
    }
    fun setNewLocation(newLocation: String){
        val geocoder = Geocoder(this.context)
        try {
            val coordinates = geocoder.getFromLocationName(newLocation, 1)
            currentMapLocation = LatLng(coordinates[0].latitude, coordinates[0].longitude)
            currentCircleRadius = 1000.0                    // Zurücksetzen des Circle-Radius
            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentMapLocation, 14.0f))
            gMap.clear()                                    // Bestehende Kreise und Marker entfernen

            infoWindowStatus = false                        // Clear infoWindow-Status bei Location-Wechsel der Suche
            slideDown(infoWindow)

            showCircleWithRadius(currentCircleRadius)       // Kreis anzeigen nach Zurücksetzen des Radius
            showMarkerInRadius()                            // Marker im Radius neu setzen

        } catch (e: Exception) {
            Log.e("MapErrors", "Keine Koordinaten zur Zieleingabe gefunden. Fehlermeldung: $e")
        }
    }

    //Auswahl und Anzeigen der Marker im Radius
    private fun showMarkerInRadius(){
        db.collection("Posts")
            //.whereEqualTo("capital", true)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val tempCoordinates = document.data.getValue("Coordinates") as HashMap<Double, Double>
                    val tempLatitude = tempCoordinates.getValue(tempCoordinates.keys.first())
                    val tempLongitude = tempCoordinates.getValue(tempCoordinates.keys.last())
                    val tempMarkerPosition = LatLng(tempLatitude, tempLongitude)

                    if(getDistanceBetweenTwoPoints(currentMapLocation, tempMarkerPosition) <= currentCircleRadius) {
                        addMarker(tempMarkerPosition, document.data["Title"] as String, document.id)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.w("FirebaseGet", "Error getting documents.", exception)
            }
    }
    private fun addMarker(position: LatLng, title: String, snippet: String){
        gMap.addMarker(
            MarkerOptions()
                .position(position)
                .title(title)
                //.title(document.data["Title"] as String)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_icon_bounded)) // alternatively: marker_icon
                .snippet(snippet)
        )
    }
    private fun getDistanceBetweenTwoPoints(start: LatLng, end: LatLng) : Float {
        val startPoint = Location("locationA")
        startPoint.latitude = start.latitude
        startPoint.longitude = start.longitude

        val endPoint = Location("locationA")
        endPoint.latitude = end.latitude
        endPoint.longitude = end.longitude

        return startPoint.distanceTo(endPoint)
    }

    //Neue Location mit Marker hinzufügen
    private fun addLocation(){
        if(!addLocationStatus){
            addLocationStatus = true
            slideDown(btnAddLocation)
            Toast.makeText(this.context,"Click on map to create new Location",Toast.LENGTH_LONG).show()
        }
    }

    //Anzeige und Änderungen am Suchradius
    private fun showCircleWithRadius(radius: Double){
        gMap.addCircle(             // Generierung des ersten Kreises, bzw. Radius für die Anzeige von Markern
            CircleOptions()
                .center(currentMapLocation)
                .radius(radius)
                .strokeColor(Color.RED)
                .fillColor(0x22ff0000)
        )
    }
    private fun increaseCircleRadius(){
        currentCircleRadius += 200               // Radius vergrößern
        updateCircleRadius(currentCircleRadius)
    }
    private fun decreaseCircleRadius(){
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
        showMarkerInRadius()                     // Marker im Radius neu setzen
    }

    //Weitere Funktionen
    private fun changeMapType(){
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
    private fun openCreateMarker(latLng: LatLng){

        val intent = Intent(this.context, CreateMarker::class.java)

        val lat = latLng.latitude.toString()
        val lng = latLng.longitude.toString()

        intent.putExtra("latetude", lat)
        intent.putExtra("longetude", lng)
        startActivity(intent)
    }


}