package com.example.nearbyresto;
//NIM : 10120055
//Nama : Abyan Dhiya Ulhaq
//Kelas : IF-2
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.nearbyresto.databinding.ActivityMapsBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import android.content.Intent;
import android.view.View;
import android.widget.ImageButton;
import com.android.volley.toolbox.JsonObjectRequest;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MapsActivity extends FragmentActivity {

    private ActivityMapsBinding binding;
    private SupportMapFragment mapFragment;
    private FusedLocationProviderClient client;

    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        client = LocationServices.getFusedLocationProviderClient(this);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView);

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapsActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        getCurrentLocation();
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
            return;
        }

        Task<Location> task = client.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    LatLng userLatLng = new LatLng(latitude, longitude);

                    mapFragment.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(GoogleMap googleMap) {
                            googleMap.addMarker(new MarkerOptions().position(userLatLng).title("Lokasi Saya"));
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 12));

                            String apiKey = getString(R.string.google_maps_api_key);
                            String type = "restaurant terfavorit";

                            Uri.Builder builder = new Uri.Builder();
                            builder.scheme("https")
                                    .authority("maps.googleapis.com")
                                    .appendPath("maps")
                                    .appendPath("api")
                                    .appendPath("place")
                                    .appendPath("textsearch")
                                    .appendPath("json")
                                    .appendQueryParameter("query", type)
                                    .appendQueryParameter("location", latitude + "," + longitude)
                                    .appendQueryParameter("key", apiKey);
                            String url = builder.build().toString();

                            Executors.newSingleThreadExecutor().execute(new Runnable() {
                                @Override
                                public void run() {
                                    sendNearbyPlacesRequest(url, googleMap);
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    private void sendNearbyPlacesRequest(String url, final GoogleMap googleMap) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray results = response.getJSONArray("results");
                            List<Place> places = new ArrayList<>();

                            for (int i = 0; i < results.length(); i++) {
                                JSONObject place = results.getJSONObject(i);
                                JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                                double latitude = location.getDouble("lat");
                                double longitude = location.getDouble("lng");
                                String name = place.getString("name");
                                double rating = place.optDouble("rating", 0.0);

                                places.add(new Place(name, latitude, longitude, rating));
                            }

                            Collections.sort(places, new Comparator<Place>() {
                                @Override
                                public int compare(Place p1, Place p2) {
                                    return Double.compare(p2.getRating(), p1.getRating());
                                }
                            });

                            int numOfResults = Math.min(places.size(), 5);

                            for (int i = 0; i < numOfResults; i++) {
                                Place place = places.get(i);
                                LatLng restaurantLatLng = new LatLng(place.getLatitude(), place.getLongitude());
                                MarkerOptions markerOptions = new MarkerOptions()
                                        .position(restaurantLatLng)
                                        .title(place.getName())
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.markercustom));
                                googleMap.addMarker(markerOptions);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                });

        RequestQueue queue = VolleySingleton.getInstance(this).getRequestQueue();
        queue.add(request);
    }

    private class Place {
        private String name;
        private double latitude;
        private double longitude;
        private double rating;

        public Place(String name, double latitude, double longitude, double rating) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
            this.rating = rating;
        }

        public String getName() {
            return name;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public double getRating() {
            return rating;
        }
    }


    public static class VolleySingleton {
        private static VolleySingleton instance;
        private RequestQueue requestQueue;
        private static Context context;

        private VolleySingleton(Context context) {
            VolleySingleton.context = context;
            requestQueue = getRequestQueue();
        }

        public static synchronized VolleySingleton getInstance(Context context) {
            if (instance == null) {
                instance = new VolleySingleton(context);
            }
            return instance;
        }

        public RequestQueue getRequestQueue() {
            if (requestQueue == null) {
                requestQueue = Volley.newRequestQueue(context.getApplicationContext());
            }
            return requestQueue;
        }
    }
}
