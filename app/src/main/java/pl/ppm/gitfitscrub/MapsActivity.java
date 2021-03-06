package pl.ppm.gitfitscrub;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.widget.LinearLayout;
import android.view.ViewGroup;

import com.google.android.gms.maps.model.*;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.floor;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    GoogleMap mGoogleMap;
    SupportMapFragment mapFrag;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation, lStart, lEnd;
    static double distance = 0;
    double speed;
    Marker mCurrLocationMarker;
    FusedLocationProviderClient mFusedLocationClient;
    private Chronometer mChronometer;
    private Button mStartButton;
    private Button mStopButton;
    static TextView distanceText, speedText;
    boolean StartB = false;
    int night = 1;
    long sec;
    int min;
    double maxspeed = 0.0;
    double averagespeed = 0.0;
    double pace;
    double pace1;
    int numberspeed = 0;
    private LinearLayout panel;
    private LinearLayout jasiek;
    int kolor = Color.parseColor("#000080");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);

        mStartButton = (Button) findViewById(R.id.start_button);
        mStopButton = (Button) findViewById (R.id.stop_button);
        mChronometer = (Chronometer) findViewById(R.id.chronometer);


        distanceText = (TextView) findViewById(R.id.distanceText);
        speedText = (TextView) findViewById(R.id.speedText);

        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StartB = true;
                mStartButton.setVisibility(View.GONE);
                mStopButton.setVisibility(View.VISIBLE);
                mStopButton.setBackgroundColor ( Color.parseColor("#800000") );
                mChronometer.setBase(SystemClock.elapsedRealtime());
                mChronometer.start();
            }
        });
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StartB = false;
                mStopButton.setVisibility(View.GONE);
                mStopButton.setBackgroundColor ( Color.parseColor("#800000") );
                averagespeed = averagespeed/(double)numberspeed;
                panel = findViewById(R.id.panel);
                panel.setVisibility(view.GONE);
                mChronometer.stop();
                showElapsedTime();
                buildAlertPodsumowanie();
                jasiek = findViewById(R.id.jasiek);
                jasiek.getLayoutParams().height =  ViewGroup.LayoutParams.MATCH_PARENT;
            }
        });


        SharedPreferences prefs = getSharedPreferences("database", MODE_PRIVATE);
        night = prefs.getInt("night", 1);

    }


    public String summaryValues() {
        @SuppressLint("DefaultLocale") String fDist = String.format("%.3f", distance);
        @SuppressLint("DefaultLocale") String fTime = String.format("%s:%s",
                String.format("%02d", min), String.format("%02d", sec));
        @SuppressLint("DefaultLocale") String fMaxSpeed = String.format("%.2f", maxspeed);
        @SuppressLint("DefaultLocale") String fAvgSpeed = String.format("%.2f", averagespeed);

        String summaryData;
        summaryData = fTime + " " + fDist + " " + fAvgSpeed + " " + fMaxSpeed;

        return summaryData;
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    private void buildAlertMessageNoGps() {
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setMessage("Twój GPS jest wyłączony. Czy chcesz go włączyć?")
                .setCancelable(false)
                .setPositiveButton("Tak", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("Nie", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final android.app.AlertDialog alert = builder.create();
        alert.show();
    }

    private void showElapsedTime() {
        sec = SystemClock.elapsedRealtime() - mChronometer.getBase();
        min = 0;
        sec = sec/1000;
        if ( sec > 60 ) {
            min = (int)sec/60;
            sec = sec-min*60;
        }
    }

    private void buildAlertPodsumowanie() {
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);

        builder .setTitle("Podsumowanie treningu")
                .setMessage("dystans: " + new DecimalFormat ( "#.###" ).format ( distance ) + " km"
                        + "\n" + "czas: " + String.format("%02d", min) + ":"
                        + String.format("%02d", sec) + " min" + "\n"
                        + "maksymalna prędkość: " + new DecimalFormat ( "#.##" ).format (maxspeed) + " km/h"
                        + "\n" + "średnia prędkość: " + new DecimalFormat ( "#.##" ).format (averagespeed) + " km/h")
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        String stats = summaryValues();
                        distance = 0.0;
                        maxspeed = 0.0;
                        averagespeed = 0.0;
                        numberspeed = 0;
                        Intent intent = new Intent(getApplicationContext(), HistoryActivity.class);
                        intent.putExtra("data", stats);
                        startActivity(intent);
                    }
                });

        final android.app.AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mGoogleMap=googleMap;
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        setSelectedStyle();

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) { buildAlertMessageNoGps();}
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mGoogleMap.setMyLocationEnabled(true);
            } else {
                checkLocationPermission();
            }
        }
        else {
            buildGoogleApiClient();
            mGoogleMap.setMyLocationEnabled(true);
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

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(100);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        }
    }

    LocationCallback mLocationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                mLastLocation = location;
                if (mCurrLocationMarker !=null) {
                    mCurrLocationMarker.remove();
                }
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title("Tu jestem!");
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker));
                mCurrLocationMarker = mGoogleMap.addMarker(markerOptions);
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
                if(StartB) {
                    if (lStart == null) {
                        lStart = mLastLocation;
                        lEnd = mLastLocation;
                    } else {
                        lEnd = mLastLocation;
                    }
                    Polyline line = mGoogleMap.addPolyline(new PolylineOptions()
                            .add(new LatLng(lStart.getLatitude(), lStart.getLongitude()), new LatLng(lEnd.getLatitude(), lEnd.getLongitude()))
                            .width(8)
                            .color(kolor));

                    speed = location.getSpeed() * 3.6;
                    pace = 60/speed;
                    pace1 = pace;
                    pace = floor(pace);
                    pace1 = (pace1-pace)*60;
                    if(maxspeed < speed) maxspeed = speed;
                    averagespeed = averagespeed + speed;
                    numberspeed = numberspeed + 1;

                    updateUI();
                }
            }
        }
    };

    private void updateUI() {
        distance = distance + (lStart.distanceTo(lEnd) / 1000.00);
        if (speed > 0.0) {
            speedText.setText ( new DecimalFormat ( "#.##" ).format ( speed ) + " km/h" );
        } else {
            speedText.setText("0 km/h");
        }

        if (distance > 0.0) {
            distanceText.setText ( new DecimalFormat ( "#.###" ).format ( distance ) + " km" );
        }
        else {
            distanceText.setText ( "0 km" );
        }

        lStart = lEnd;
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {}

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("Wymagane są uprawnienia lokalizacji")
                        .setMessage("Aplikacja wymaga uprawnień do lokalizacji aby funkcjonować poprawnie.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(MapsActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mGoogleMap.setMyLocationEnabled(true);
                    }

                } else {
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    public void deepChangeTextColor(ViewGroup parentLayout){
        for (int count=0; count < parentLayout.getChildCount(); count++){
            View view = parentLayout.getChildAt(count);
            if(view instanceof TextView){
                ((TextView)view).setTextColor(Color.parseColor("#dddddd"));
            } else if(view instanceof ViewGroup){
                deepChangeTextColor((ViewGroup)view);
            }
        }
    }

    private void setSelectedStyle() {

        MapStyleOptions style;
        if (night == 2) {
            style = MapStyleOptions.loadRawResourceStyle ( this, R.raw.night_style );
            kolor = Color.parseColor("#00ff00");
            LinearLayout root = findViewById(R.id.main);
            root.setBackgroundColor(Color.parseColor("#222222"));
            deepChangeTextColor ( root );
        } else {
            style = null;
        }
        mGoogleMap.setMapStyle(style);
    }
}
