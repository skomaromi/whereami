package com.github.skomaromi.whereami;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    @BindView(R.id.tv_latitude_value) TextView tv_latitude_value;
    @BindView(R.id.tv_longitude_value) TextView tv_longitude_value;
    @BindView(R.id.tv_address_value) TextView tv_address_value;
    @BindView(R.id.tv_city_value) TextView tv_city_value;
    @BindView(R.id.tv_country_value) TextView tv_country_value;

    private static final int REQUEST_LOCATION_PERMISSION = 10;
    LocationManager mLocationManager;
    LocationListener mLocationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        mLocationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        mLocationListener = new SimpleLocationListener();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!hasLocationPermission()) {
            requestPermission();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(hasLocationPermission()) {
            startTracking();
        }
    }

    private void startTracking() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        long minTime = 1000;
        float minDistance = 10;

        mLocationManager.requestLocationUpdates(
            mLocationManager.getBestProvider(criteria, true),
            minTime,
            minDistance,
            mLocationListener
        );
    }

    private boolean hasLocationPermission() {
        int status = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        );

        return status == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        String[] permissions = new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        ActivityCompat.requestPermissions(
                MainActivity.this,
                permissions,
                REQUEST_LOCATION_PERMISSION
        );
    }

    private void askForPermission() {
        boolean shouldExplain = ActivityCompat
                .shouldShowRequestPermissionRationale(
                        MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION
        );
        if(shouldExplain) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Location permission")
                    .setMessage("To display the location, location permission is necessary.")
                    .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermission();
                            dialog.cancel();
                        }
                    })
                    .show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_LOCATION_PERMISSION) {
            if(grantResults.length <= 0) {
                askForPermission();
            }
        }
    }

    private void updateLocationDisplay(Location location) {
        tv_latitude_value.setText(Double.toString(location.getLatitude()));
        tv_longitude_value.setText(Double.toString(location.getLongitude()));

        if(Geocoder.isPresent()) {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> nearbyAddresses = geocoder.getFromLocation(
                        location.getLatitude(),
                        location.getLongitude(),
                        1
                );

                if(nearbyAddresses.size() > 0) {
                    Address nearestAddress = nearbyAddresses.get(0);
                    tv_address_value.setText(nearestAddress.getAddressLine(
                            0
                    ).split(",")[0]);
                    tv_city_value.setText(nearestAddress.getLocality());
                    tv_country_value.setText(nearestAddress.getCountryName());
                }
            }
            catch (IOException e) { e.printStackTrace(); }
        }
    }

    private class SimpleLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            updateLocationDisplay(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) { }

        @Override
        public void onProviderDisabled(String provider) { }
    }
}
