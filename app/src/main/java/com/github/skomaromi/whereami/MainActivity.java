package com.github.skomaromi.whereami;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    @BindView(R.id.tv_latitude_value) TextView tv_latitude_value;
    @BindView(R.id.tv_longitude_value) TextView tv_longitude_value;
    @BindView(R.id.tv_address_value) TextView tv_address_value;
    @BindView(R.id.tv_city_value) TextView tv_city_value;
    @BindView(R.id.tv_country_value) TextView tv_country_value;

    @BindView(R.id.btn_addamarker) Button btn_addamarker;
    @BindView(R.id.btn_takeaphoto) Button btn_takeaphoto;

    private static final int REQUEST_LOCATION_PERMISSION = 10;
    LocationManager mLocationManager;
    LocationListener mLocationListener;

    String mCurAddress, mCurCity, mCurCountry;

    GoogleMap mGoogleMap;
    MapFragment mMapFragment;
    Marker mLocationMarker;
    ArrayList<Marker> mCustomMarkers;

    private static final int REQUEST_ADDMARKER = 11;
    private static final int REQUEST_IMAGE_CAPTURE = 12;

    SoundPool mSoundPool;
    Boolean mSoundIsReady;
    Map<Integer, Integer> mSounds = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        btn_takeaphoto.setEnabled(false);

        mLocationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        mLocationListener = new SimpleLocationListener();

        mMapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.f_googlemap);
        mMapFragment.getMapAsync(this);

        mCustomMarkers = new ArrayList<>();

        mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 1);
        mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                mSoundIsReady = true;
            }
        });
        loadSounds();
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        UiSettings uiSettings = mGoogleMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setMyLocationButtonEnabled(false);
        uiSettings.setZoomGesturesEnabled(true);
    }

    private void loadSounds() {
        mSounds.put(R.raw.gong, mSoundPool.load(this, R.raw.gong, 1));
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

                    mCurAddress = nearestAddress.getAddressLine(
                            0
                    ).split(",")[0];
                    tv_address_value.setText(mCurAddress);

                    mCurCity = nearestAddress.getLocality();
                    tv_city_value.setText(mCurCity);

                    mCurCountry = nearestAddress.getCountryName();
                    tv_country_value.setText(mCurCountry);
                }
            }
            catch (IOException e) { e.printStackTrace(); }
        }

        if(mLocationMarker == null) {
            mLocationMarker = mGoogleMap.addMarker(
                new MarkerOptions()
                    .position(new LatLng(
                            location.getLatitude(),
                            location.getLongitude()
                    ))
                    .title("My location")
            );
        }
        else {
            mLocationMarker.setPosition(new LatLng(
                    location.getLatitude(), location.getLongitude()
            ));
        }

        btn_takeaphoto.setEnabled(true);
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

    @OnClick(R.id.btn_addamarker)
    public void showAddMarkerActivity() {
        Intent addMarkerActivity = new Intent(this, AddMarkerActivity.class);
        startActivityForResult(addMarkerActivity, REQUEST_ADDMARKER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ADDMARKER) {
            if(resultCode == Activity.RESULT_OK) {
                MarkerOptions newMarker = new MarkerOptions();

                if(data.hasExtra(AddMarkerActivity.KEY_MARKERICONIDX)) {
                    int idx = data.getIntExtra(AddMarkerActivity.KEY_MARKERICONIDX, -1);
                    switch(idx) {
                        case 0:
                            newMarker.icon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_input_add));
                            break;
                        case 1:
                            newMarker.icon(BitmapDescriptorFactory.fromResource(android.R.drawable.star_big_on));
                            break;
                        default:
                            newMarker.icon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_delete));
                            break;
                    }
                }

                if(data.hasExtra(AddMarkerActivity.KEY_LATITUDE) && data.hasExtra(AddMarkerActivity.KEY_LONGITUDE)) {
                    double latitude, longitude;
                    latitude = data.getDoubleExtra(AddMarkerActivity.KEY_LATITUDE, -1);
                    longitude = data.getDoubleExtra(AddMarkerActivity.KEY_LONGITUDE, -1);
                    newMarker.position(new LatLng(latitude, longitude));
                }

                mCustomMarkers.add(mGoogleMap.addMarker(newMarker));

                if(mSoundIsReady) {
                    mSoundPool.play(mSounds.get(R.raw.gong), 1, 1, 1, 0, 1);
                }
            }
        }
    }

    @OnClick(R.id.btn_takeaphoto)
    public void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;

            File mediaStorageDir = new File(
                    Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES
                    ),
                    "WhereAmI"
            );

            if(!mediaStorageDir.exists()) {
                if(!mediaStorageDir.mkdirs()) {
                    return;
                }
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

            String curCountry = mCurCountry.replace(' ', '-');
            String curCity = mCurCity.replace(' ', '-');
            String curAddress = mCurAddress.replace(' ', '-');

            photoFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + "_" + curCountry + "_" + curCity + "_" + curAddress + ".jpg");
            try{
                photoFile.createNewFile();
            }
            catch (Exception e) { e.printStackTrace();}

            Uri photoURI = FileProvider.getUriForFile(this, "com.github.skomaromi.whereami.fileprovider", photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(photoFile), "image/*");

            PendingIntent notificationIntent = PendingIntent.getActivity(this, 0, intent, 0);

            Notification notification = new NotificationCompat.Builder(this, "V1")
                    .setContentTitle("WhereAmI")
                    .setContentText("Image captured.")
                    .setSmallIcon(android.R.drawable.ic_dialog_email)
                    .setContentIntent(notificationIntent)
                    .build();

            NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

            notificationManager.notify(0, notification);
        }
    }
}
