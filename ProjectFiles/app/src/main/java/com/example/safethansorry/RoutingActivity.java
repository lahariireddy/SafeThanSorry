package com.example.safethansorry;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import com.example.safethansorry.RoutingHelpers.FetchURL;
import com.example.safethansorry.RoutingHelpers.FetchURL;
import com.example.safethansorry.RoutingHelpers.TaskLoadedCallback;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;


import com.example.safethansorry.RoutingHelpers.TaskLoadedCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


import android.widget.AdapterView.OnItemClickListener;




public class RoutingActivity extends AppCompatActivity implements OnItemClickListener, OnMapReadyCallback, TaskLoadedCallback,GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    LocationRequest mLocationRequest;
    double latitude=0, longitude=0;
    GoogleMap mMap;
    Marker sourceMarker = null, destMarker = null;
    Button getDirection;
    private Polyline currentPolyline;
    HashMap<String,MarkerOptions> hashMapMarker;
    AutoCompleteTextView sourceAutoCompView, destAutoCompView;
    String currLoc = "";
    //ListView nearbyHospList, nearbyPoliceList;
    //ArrayList<String> nearbyHospArray, nearbyPoliceArray;
    //HashMap<String, LatLng> hospitalNameToLatLngMap, policeNameToLatLngMap;

    private static final String LOG_TAG = "Autocomplete";
    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
    private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
    private static final String OUT_JSON = "/json";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routing);
        getDirection = findViewById(R.id.btnGetDirection);
        hashMapMarker = new HashMap<>();



        sourceAutoCompView = findViewById(R.id.sourceAutoCompleteTextView);
        sourceAutoCompView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sourceAutoCompView.setText("");
            }
        });

        sourceAutoCompView.setAdapter(new GooglePlacesAutocompleteAdapter(this, R.layout.list_item));
        sourceAutoCompView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView adapterView, View view, int position, long id) {
                String str = (String) adapterView.getItemAtPosition(position);
                Toast.makeText(RoutingActivity.this,"Source: "+str , Toast.LENGTH_LONG).show();
                getDirection.setVisibility(View.VISIBLE);

                LatLng latLng = null;
                try {
                    if(str!="") {
                        latLng = getLatLng(str);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(latLng!=null) {

                    if(sourceMarker != null){
                        mMap.clear();
                        hashMapMarker.remove("source");
                        addAllMarkers();
                        sourceMarker = null;

                    }

                    if(sourceMarker == null) {

                        MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.position(latLng);
                        markerOptions.draggable(true);
                        try {
                            markerOptions.title(getAddress(latLng.latitude, latLng.longitude));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                        sourceMarker = mMap.addMarker(markerOptions);
                        hashMapMarker.put("source", markerOptions);
                        mMap.addMarker(markerOptions);
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                    }
                }

                //Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
            }
        });

        destAutoCompView = findViewById(R.id.destinationAutoCompleteTextView);
        destAutoCompView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                destAutoCompView.setText("");
            }
        });

        destAutoCompView.setAdapter(new GooglePlacesAutocompleteAdapter(this, R.layout.list_item));
        destAutoCompView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView adapterView, View view, int position, long id) {
                String str = (String) adapterView.getItemAtPosition(position);
                Toast.makeText(RoutingActivity.this,"Destination: "+str , Toast.LENGTH_LONG).show();

                //Making "Get route" button visible
                getDirection.setVisibility(View.VISIBLE);

                LatLng latLng = null;
                try {
                    if(str!="") {
                        latLng = getLatLng(str);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(latLng!=null) {

                    if(destMarker != null){
                        mMap.clear();
                        hashMapMarker.remove("destination");
                        addAllMarkers();
                        destMarker = null;

                    }

                    if(destMarker == null) {

                        MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.position(latLng);
                        markerOptions.draggable(true);
                        try {
                            markerOptions.title(getAddress(latLng.latitude, latLng.longitude));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                        destMarker = mMap.addMarker(markerOptions);
                        hashMapMarker.put("destination", markerOptions);
                        mMap.addMarker(markerOptions);
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                    }
                }
                //Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
            }
        });


        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        //Check if Google Play Services Available or not
        if (!CheckGooglePlayServices()) {
            Log.d("onCreate", "Finishing test case since Google Play Services are not available");
            finish();
        }
        else {
            Log.d("onCreate","Google Play Services available.");
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.mapNearBy);
        mapFragment.getMapAsync(this);


        //OnClick 'Get Directions' button
        getDirection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (currentPolyline != null) {
                    currentPolyline.remove();
                }
                mMap.clear();

                addAllMarkers();

                if (sourceMarker != null && destMarker != null) {

                    new FetchURL(RoutingActivity.this).execute(getUrl(sourceMarker.getPosition(), destMarker.getPosition(), "driving"), "driving");
                    LatLng latLng = new LatLng(sourceMarker.getPosition().latitude, sourceMarker.getPosition().longitude);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
                }
                else if(sourceMarker == null && destMarker!=null){

                    new FetchURL(RoutingActivity.this).execute(getUrl(mCurrLocationMarker.getPosition(), destMarker.getPosition(), "driving"), "driving");
                    LatLng latLng = new LatLng(latitude, longitude);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
                }
                else{
                    Toast.makeText(RoutingActivity.this,"Enter destination", Toast.LENGTH_LONG).show();
                }


            }
        });

        RelativeLayout nearbyHospLayout = findViewById(R.id.layout_hospital);
        nearbyHospLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //nearbyHospArray = new ArrayList<String>();
                //hospitalNameToLatLngMap = new HashMap<String, LatLng>();
                Toast.makeText(RoutingActivity.this,"Hospitals", Toast.LENGTH_LONG).show();

                getNearby("hospital");


            }
        });

        RelativeLayout nearbyPoliceLayout = findViewById(R.id.layout_police);
        nearbyPoliceLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //nearbyPoliceArray = new ArrayList<String>();
                //policeNameToLatLngMap = new HashMap<String, LatLng>();

                getNearby("police");


            }
        });




    }



    /**
     * Start: Methods and classes for getting nearby locations
     */

    void getNearby( String placeType) {
        getDirection.setVisibility(View.INVISIBLE);

        mMap.clear();

        if(hashMapMarker.get("current")!=null){
            mMap.addMarker(hashMapMarker.get("current"));
        }
        LatLng latLng = new LatLng(latitude, longitude);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));


        StringBuilder sb = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        sb.append("location=" + latitude + "," + longitude);
        if(placeType == "hospital"){
            sb.append("&radius=2000");
        }
        if(placeType == "police") {
            sb.append("&radius=5000");
        }
        sb.append("&types=" + placeType);
        sb.append("&sensor=true");
        sb.append("&key=" + getString(R.string.google_maps_key));                           /** API KEY **/
        sb.append("&opennow=true");

        // Creating a new non-ui thread task to download json data
        PlacesTask placesTask = new PlacesTask();

        // Invokes the "doInBackground()" method of the class PlaceTask
        placesTask.execute(sb.toString());

    }

    /** A method to download json data from url */
    private String downloadUrl(String strUrl) throws IOException{
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb  = new StringBuffer();

            String line = "";
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d("Exception dwnloadng url", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }

        return data;
    }

    /** A class, to download Google Places */
    private class PlacesTask extends AsyncTask<String, Integer, String> {

        String data = null;

        // Invoked by execute() method of this object
        @Override
        protected String doInBackground(String... url) {
            try{
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(String result){
            ParserTask parserTask = new ParserTask();

            // Start parsing the Google places in JSON format
            // Invokes the "doInBackground()" method of the class ParseTask
            parserTask.execute(result);
        }

    }

    /** A class to parse the Google Places in JSON format */
    private class ParserTask extends AsyncTask<String, Integer, List<HashMap<String,String>>>{

        JSONObject jObject;

        // Invoked by execute() method of this object
        @Override
        protected List<HashMap<String,String>> doInBackground(String... jsonData) {

            List<HashMap<String, String>> places = null;
            PlaceJSONParser placeJsonParser = new PlaceJSONParser();

            try{
                jObject = new JSONObject(jsonData[0]);

                /** Getting the parsed data as a List construct */
                places = placeJsonParser.parse(jObject);

            }catch(Exception e){
                Log.d("Exception",e.toString());
            }
            return places;
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(List<HashMap<String,String>> list){

            // Clears all the existing markers
            // mMap.clear();
            //count=count+list.size();
            for(int i=0;i<list.size();i++){

                // Creating a marker
                MarkerOptions markerOptions = new MarkerOptions();

                // Getting a place from the places list
                HashMap<String, String> hmPlace = list.get(i);

                // Getting latitude of the place
                double lat = Double.parseDouble(hmPlace.get("lat"));

                // Getting longitude of the place
                double lng = Double.parseDouble(hmPlace.get("lng"));

                // Getting name
                String name = hmPlace.get("place_name");

                // Getting vicinity
                String vicinity = hmPlace.get("vicinity");

                LatLng latLng = new LatLng(lat, lng);

                // Setting the position for the marker
                markerOptions.position(latLng);

                // Setting the title for the marker.
                //This will be displayed on taping the marker
                markerOptions.title(name + " : " + vicinity);

                // Placing a marker on the touched position
                mMap.addMarker(markerOptions);
            }
        }
    }

    /**
     * End : Methods and classes to get nearby locations
     */




    /**
     * Begin : Methods for autocomplete
     **/
    public void onItemClick(AdapterView adapterView, View view, int position, long id) {
        String str = (String) adapterView.getItemAtPosition(position);
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    public ArrayList autocomplete(String input) {
        ArrayList resultList = null;

        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON);
            sb.append("?key=" + "AIzaSyDu-CcNOu9R3RvWPVshJFDa7GHE0ezf1w4");
            sb.append("&components=country:in");
            sb.append("&input=" + URLEncoder.encode(input, "utf8"));

            URL url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error processing Places API URL", e);
            return resultList;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error connecting to Places API", e);
            return resultList;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");

            // Extract the Place descriptions from the results
            resultList = new ArrayList(predsJsonArray.length());
            for (int i = 0; i < predsJsonArray.length(); i++) {
                System.out.println(predsJsonArray.getJSONObject(i).getString("description"));
                System.out.println("============================================================");
                resultList.add(predsJsonArray.getJSONObject(i).getString("description"));
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Cannot process JSON results", e);
        }

        if(currLoc!=""){

            resultList.add(0,currLoc);
        }

        return resultList;
    }

    class GooglePlacesAutocompleteAdapter extends ArrayAdapter implements Filterable {
        private ArrayList resultList;

        public GooglePlacesAutocompleteAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        @Override
        public int getCount() {
            return resultList.size();
        }

        @Override
        public String getItem(int index) {
            return resultList.get(index).toString();
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    if (constraint != null) {
                        // Retrieve the autocomplete results.
                        resultList = autocomplete(constraint.toString());

                        // Assign the data to the FilterResults
                        filterResults.values = resultList;
                        filterResults.count = resultList.size();
                    }
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count > 0) {
                        notifyDataSetChanged();
                    } else {
                        notifyDataSetInvalidated();
                    }
                }
            };
            return filter;
        }
    }

    /**
     * End : Methods for autocomplete
     **/



    /**
     * Method for getting source and dest after getDirections is clicked
     **/
    public void getSourceDestMarker() {
        //Get Source Marker
        String location = sourceAutoCompView.getText().toString();

        LatLng latLng = null;
        try {
            if (location != "") {
                latLng = getLatLng(location);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (latLng != null) {

            if (sourceMarker != null) {
                mMap.clear();
                hashMapMarker.remove("source");
                addAllMarkers();
                sourceMarker = null;

            }

            if (sourceMarker == null) {

                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.draggable(true);
                try {
                    markerOptions.title(getAddress(latLng.latitude, latLng.longitude));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                sourceMarker = mMap.addMarker(markerOptions);
                hashMapMarker.put("source", markerOptions);
                mMap.addMarker(markerOptions);
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));


            }
        }

        //get dest marker
        String location2 = destAutoCompView.getText().toString();

        LatLng latLng2 = null;
        try {
            if(location2!="") {
                latLng2 = getLatLng(location2);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(latLng2!=null) {

            if(destMarker != null){
                mMap.clear();
                hashMapMarker.remove("destination");
                addAllMarkers();
                destMarker = null;

            }

            if(destMarker == null) {

                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng2);
                markerOptions.draggable(true);
                try {
                    markerOptions.title(getAddress(latLng2.latitude, latLng2.longitude));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                destMarker = mMap.addMarker(markerOptions);
                hashMapMarker.put("destination", markerOptions);
                mMap.addMarker(markerOptions);
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng2));
            }
        }


    }



    /**
     * Method for adding current location, source and destination markers to map
     **/
    public void addAllMarkers(){
        if(hashMapMarker.get("source")!=null){
            mMap.addMarker(hashMapMarker.get("source"));
        }
        if(hashMapMarker.get("destination")!=null){
            mMap.addMarker(hashMapMarker.get("destination"));
        }
        if(hashMapMarker.get("current")!=null){
            mMap.addMarker(hashMapMarker.get("current"));
        }

    }


    /**
     * Method to convert a String to LatLng
     **/
    public LatLng getLatLng(String location) throws IOException {
        Geocoder geocoder = new Geocoder(this);
        List<Address> addresses;
        addresses = geocoder.getFromLocationName(location, 1);
        if(addresses.size() > 0) {
            double resLat = addresses.get(0).getLatitude();
            double reslng = addresses.get(0).getLongitude();

            LatLng resLatLng = new LatLng(resLat, reslng);


            return resLatLng;
        }
        return null;

    }


    /**
     * Method to convert LatLng to address
     **/
    public String getAddress(double lat, double lng) throws IOException {

        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());

        addresses = geocoder.getFromLocation(lat, lng, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5

        String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
        String city = addresses.get(0).getLocality();
        String state = addresses.get(0).getAdminArea();
        String country = addresses.get(0).getCountryName();
        String postalCode = addresses.get(0).getPostalCode();
        String knownName = addresses.get(0).getFeatureName(); // Only if available else return NULL

        return address;

    }



    private boolean CheckGooglePlayServices() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if(result != ConnectionResult.SUCCESS) {
            if(googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result,
                        0).show();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Log.d("mylog", "Added Markers");

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        } else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }
    }


    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }


    /**
     * Get URL for getting data from Directions API
     **/
    private String getUrl(LatLng origin, LatLng dest, String directionMode) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Mode
        String mode = "mode=" + directionMode;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest +"&alternatives=true"+ "&" + mode;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getString(R.string.google_maps_key);
        return url;
    }

    @Override
    public void onTaskDone(int count, List<PolylineOptions> poly) {
        int i, maxIndex=0;
        int[] estCount =  new int[count];
        for(i=0;i<count;i++){
            if(poly.get(i)!=null){
                estCount[i] = (getNumberOfEstablishmentsForRoute((PolylineOptions)poly.get(i)));
                Log.d("MYINT", "value: " + estCount[i]);
                if(estCount[maxIndex]<estCount[i])
                    maxIndex = i;
                //mMap.addPolyline((PolylineOptions)poly.get(i));
            }
        }
        for(i=0;i<count;i++){
            if(maxIndex != i) {
                mMap.addPolyline((PolylineOptions) poly.get(i).color(Color.BLUE));
            }
            else{
                mMap.addPolyline((PolylineOptions)poly.get(i).color(Color.MAGENTA));
            }

        }
        mMap.addPolyline((PolylineOptions)poly.get(maxIndex).color(Color.MAGENTA));

    }


    /**
     * Start: Methods and classes for getting nearby locations along a route
     */
    int masterCount = 0, localCount=0;

    int getNumberOfEstablishmentsForRoute(PolylineOptions polylineOptions) {
        getDirection.setVisibility(View.INVISIBLE);
        int i;
        masterCount = 0;
        LatLng currentPoint;
        List<LatLng> points = polylineOptions.getPoints();

        for(i=0;i<points.size();i=i+5) {
            localCount = 0;
            currentPoint = (LatLng) points.get(i);
            StringBuilder sb = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
            sb.append("location=" + currentPoint.latitude + "," + currentPoint.longitude);
            sb.append("&radius=100");
            sb.append("&types=" + "hospitals");
            sb.append("&sensor=true");
            sb.append("&key=AIzaSyDu-CcNOu9R3RvWPVshJFDa7GHE0ezf1w4");
            sb.append("&opennow=true");

            // Creating a new non-ui thread task to download json data
            PlacesTaskNonUI placesTaskNonUI = new PlacesTaskNonUI();

            // Invokes the "doInBackground()" method of the class PlaceTask
            placesTaskNonUI.execute(sb.toString());
            masterCount+=localCount;
            Log.d("mastercount", "value: " + masterCount);
        }
        Log.d("master count final", "value: " + masterCount);
        return masterCount;

    }


    /** A method to download json data from url */
    private String downloadUrlNonUI (String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception dwnloadng url", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }

        return data;
    }

    /** A class, to download Google Places */
    private class PlacesTaskNonUI extends AsyncTask<String, Integer, String> {


        String data = null;

        // Invoked by execute() method of this object
        @Override
        protected String doInBackground(String... url) {
            try {
                data = downloadUrlNonUI(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(String result) {
            ParserTaskNonUI parserTaskNonUI = new ParserTaskNonUI();

            // Start parsing the Google places in JSON format
            // Invokes the "doInBackground()" method of the class ParseTask
            parserTaskNonUI.execute(result);
        }


    }

    /** A class to parse the Google Places in JSON format */
    private class ParserTaskNonUI extends AsyncTask<String, Integer, List<HashMap<String, String>>> {

        JSONObject jObject;

        // Invoked by execute() method of this object
        @Override
        protected List<HashMap<String, String>> doInBackground(String... jsonData) {

            List<HashMap<String, String>> places = null;
            PlaceJSONParser placeJsonParser = new PlaceJSONParser();

            try {
                jObject = new JSONObject(jsonData[0]);

                /** Getting the parsed data as a List construct */
                places = placeJsonParser.parse(jObject);

            } catch (Exception e) {
                Log.d("Exception", e.toString());
            }

            return places;
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        public void onPostExecute(List<HashMap<String, String>> list) {


            localCount= list.size();
            Log.d("InParserNonUI", "value: " + localCount);

        }

    }

    /**
     * End : Methods and classes to get nearby locations along the route
     */


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("onLocationChanged", "entered");

        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        latitude = location.getLatitude();
        longitude = location.getLongitude();


        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.draggable(true);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        mCurrLocationMarker = mMap.addMarker(markerOptions);
        hashMapMarker.put("current",markerOptions);
        //sourceMarker = mCurrLocationMarker;

        //Set source_textBox to current location address
        try {
            sourceAutoCompView.setText(getAddress(latitude, longitude));
            currLoc = getAddress(latitude, longitude);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));


        Toast.makeText(RoutingActivity.this,"Your Current Location", Toast.LENGTH_LONG).show();


        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            Log.d("onLocationChanged", "Removing Location Updates");
        }

    }


    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted. Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other permissions this app might request.
            // You can add here other case statements according to your requirement.
        }
    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
