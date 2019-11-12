package com.learn.sporel.sleeptimer;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    LocationManager mLocationManager;
    EditText meditText;
    MapFragment mMapFragment;
    GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    List<Geofence> mGeofenceList;
    PendingIntent mGeofencePendingIntent;
    Marker destMarker;
    Circle circle;
    Context context;
    Polyline polyline;
    LatLng srcLatLng;
    LatLng destLatLng;
    FloatingActionButton currentLocationButton;
    FloatingActionButton getDirectionsButton;
    PlaceAutocompleteFragment placeAutocompleteFragment;
    private DrawerLayout mdrawerLayout;
    private ListView mdrawerList;
    private ImageButton mHamburgerBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = this.getWindow();

            // clear FLAG_TRANSLUCENT_STATUS flag:
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

            // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

            // finally change the color
            window.setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_main);



        getDirectionsButton = (FloatingActionButton)findViewById(R.id.fab2);
        currentLocationButton = (FloatingActionButton)findViewById(R.id.fab1);
        mHamburgerBtn = (ImageButton)findViewById(R.id.hamburger);

        context = MainActivity.this;
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        placeAutocompleteFragment = (PlaceAutocompleteFragment)getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        meditText = (EditText) findViewById(R.id.text);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        currentLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(srcLatLng)      // Sets the center of the map to Mountain View
                        .zoom(14)                   // Sets the zoom
                        // Sets the orientation of the camera to east
                        // Sets the tilt of the camera to 30 degrees
                        .build();
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        });
        getDirectionsButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.directionBlue)));

        if (getDirectionsButton != null) {
            getDirectionsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getDirections(destLatLng, context);
                }
            });
        }
        mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

        
        placeAutocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                Log.d("Soumen", place.getName().toString());
                if (destMarker != null)
                    destMarker.remove();
                if (circle != null) {
                    circle.remove();
                }

                destLatLng = place.getLatLng();
                destMarker = mMap.addMarker(new MarkerOptions().position(destLatLng).title("Destination"));
                if (getDirectionsButton != null) {
                    getDirectionsButton.setVisibility(View.VISIBLE);
                }

                ArrayList<LatLng> latLngs = new ArrayList<>();
                latLngs.add(destLatLng);
                latLngs.add(srcLatLng);
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (LatLng latLng : latLngs) {
                    builder.include(latLng);
                }

                LatLngBounds bounds = builder.build();
                int padding = 200;
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

                mMap.moveCamera(cu);
                mMap.animateCamera(cu);
            }

            @Override
            public void onError(Status status) {
                Log.d("Soumen", "Error : " + status);
            }
        });

        ArrayList<String> planets = new ArrayList<>();
        planets.add("Mercury");
        planets.add("Venus");
        planets.add("Earth");
        planets.add("Mars");
        planets.add("Jupiter");
        planets.add("Uranus");
        planets.add("Saturn");
        planets.add("Pluto");
        planets.add("Neptune");

        mdrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mdrawerList = (ListView)findViewById(R.id.left_drawer);

        mdrawerList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, planets));
        mdrawerList.setOnItemClickListener(new DrawerItemClickListener());

        mHamburgerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mdrawerLayout.isDrawerOpen(Gravity.LEFT)){
                    mdrawerLayout.closeDrawer(Gravity.LEFT);
                }
                else
                {
                    mdrawerLayout.openDrawer(Gravity.LEFT);
                }
            }
        });

    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener{

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItem(int position){
        Fragment fragment = new PlanetFragment();
        Bundle args = new Bundle();

        args.putInt(PlanetFragment.ARG_PLANET_NUMBER, position);
        fragment.setArguments(args);

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.main_content, fragment).commit();

        mdrawerList.setItemChecked(position, true);
        mdrawerLayout.closeDrawer(mdrawerList);
    }

    public static class PlanetFragment extends Fragment {
        public static final String ARG_PLANET_NUMBER = "planet_number";

        public PlanetFragment(){}

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState){
            ArrayList<String> planets = new ArrayList<>();
            planets.add("Mercury");
            planets.add("Venus");
            planets.add("Earth");
            planets.add("Mars");
            planets.add("Jupiter");
            planets.add("Uranus");
            planets.add("Saturn");
            planets.add("Pluto");
            planets.add("Neptune");
            View rootView = inflater.inflate(R.layout.fragment_planet, container, false);
            int i = getArguments().getInt(ARG_PLANET_NUMBER);
            String planet = planets.get(i);

            int imageId = getResources().getIdentifier(planet.toLowerCase(Locale.getDefault()),
                    "drawable", getActivity().getPackageName());
            ((ImageView) rootView.findViewById(R.id.image)).setImageResource(imageId);
            getActivity().setTitle(planet);
            return rootView;
        }

    }
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onBackPressed(){
        if (getDirectionsButton.getVisibility() == View.GONE){
            super.onBackPressed();
        }
        else{
            destMarker.remove();
            getDirectionsButton.setVisibility(View.GONE);
            placeAutocompleteFragment.setText("");
            polyline.remove();
            circle.remove();
        }
    }

    private void getDirections(LatLng destlatLng, final Context context) {

        if (destlatLng == null || context == null)
            return;
        CircleOptions circleOptions = new CircleOptions()
                .center(destLatLng)
                .fillColor(0x110000FF)
                .strokeColor(0xFF0000FF)
                .strokeWidth(0.5f)
                .radius(500);


        circle = mMap.addCircle(circleOptions);

        mGeofenceList = new ArrayList<Geofence>();
        mGeofenceList.add(new Geofence.Builder().setRequestId("Destination")
                .setCircularRegion(destlatLng.latitude, destlatLng.longitude, 1000)
                .setExpirationDuration(999999999)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER).build());

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, getGeofencingRequest(), geofencePendingIntent());

        new connectAsyncTask(makeUrl(srcLatLng.latitude, srcLatLng.longitude, destlatLng.latitude, destlatLng.longitude)).execute();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        try {
            mMap.setMyLocationEnabled(true);
            mMap.setBuildingsEnabled(true);
            mMap.setIndoorEnabled(true);
        } catch (SecurityException se) {
        }

    }

    protected void createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        final PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();

                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            status.startResolutionForResult(MainActivity.this, 1);
                        } catch (IntentSender.SendIntentException e) {
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
    }


    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }


    private PendingIntent geofencePendingIntent() {
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public String makeUrl(Double srcLat, Double srcLong, Double destLat, Double destLong) {
        StringBuilder sb = new StringBuilder();
        sb.append("http://maps.googleapis.com/maps/api/directions/json");
        sb.append("?origin=");
        sb.append(Double.toString(srcLat));
        sb.append(",");
        sb.append(Double.toString(srcLong));
        sb.append("&destination=");
        sb.append(Double.toString(destLat));
        sb.append(",");
        sb.append(Double.toString(destLong));
        sb.append("&sensor=false&mode=driving&alternatives=true");
        sb.append("&ey=AIzaSyBLh66HLokYIRRV1ssIVcEBtmfZEyBqMUU");

        return sb.toString();
    }

    public String getJSONFromUrl(String url) {

        String jsonStr = null;
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = null;

        try {
            response = client.newCall(request).execute();
            jsonStr = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonStr;
    }

    public void drawPath(String result) {

        //if (polyline != null)
            //polyline.remove();
        try {
            final JSONObject json = new JSONObject(result);
            JSONArray routeArray = json.getJSONArray("routes");
            JSONObject routes = routeArray.getJSONObject(0);
            JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
            String encodedString = overviewPolylines.getString("points");
            List<LatLng> list = decodePoly(encodedString);
            polyline = mMap.addPolyline(new PolylineOptions().addAll(list).width(12).color(Color.parseColor("#05b1fb")).geodesic(true));

        } catch (JSONException je) {
        }
    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0;
        int length = encoded.length();
        int lat = 0, lng = 0;

        while (index < length) {
            int b, shift = 0, result = 0;

            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            srcLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(srcLatLng)      // Sets the center of the map to Mountain View
                    .zoom(14)                   // Sets the zoom
                    // Sets the orientation of the camera to east
                    // Sets the tilt of the camera to 30 degrees
                    .build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private class connectAsyncTask extends AsyncTask<Void, Void, String>{
        private ProgressDialog progressDialog;
        String url;
        connectAsyncTask(String urlpass){
            url = urlpass;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Fetching route, Please wait...");
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(Void... params) {
            JSONParser jsonParser = new JSONParser();
            String json = jsonParser.getJSONFromUrl(url);
            return json;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.hide();
            if(result!=null){
                drawPath(result);
            }
        }

    }
}
