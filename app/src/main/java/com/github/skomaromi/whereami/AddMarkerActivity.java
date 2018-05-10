package com.github.skomaromi.whereami;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AddMarkerActivity extends AppCompatActivity implements OnMapReadyCallback {
    public static final String KEY_MARKERICONIDX = "markericonidx";
    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_LONGITUDE = "longitude";

    @BindView(R.id.rb_addmarker_type1) RadioButton rb_addmarker_type1;
    @BindView(R.id.rg_addmarker_markericon) RadioGroup rg_addmarker_markericon;

    GoogleMap mGoogleMap;
    MapFragment mMapFragment;
    Marker mMarker;

    LatLng mCurrentLatLng;
    Integer mCurrentIconIdx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_marker);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mMapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.f_addmarker_googlemap);
        mMapFragment.getMapAsync(this);

        ButterKnife.bind(this);

        rg_addmarker_markericon.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch(checkedId) {
                    case R.id.rb_addmarker_type1:
                        mCurrentIconIdx = 0;
                        break;
                    case R.id.rb_addmarker_type2:
                        mCurrentIconIdx = 1;
                        break;
                    default:
                        mCurrentIconIdx = 2;
                        break;
                }
            }
        });
        rb_addmarker_type1.setChecked(true);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_addmarker_add:
                int iconIdx;
                double latitude, longitude;

                iconIdx = mCurrentIconIdx;
                latitude = mCurrentLatLng.latitude;
                longitude = mCurrentLatLng.longitude;

                Intent ret = new Intent();
                ret.putExtra(KEY_MARKERICONIDX, iconIdx);
                ret.putExtra(KEY_LATITUDE, latitude);
                ret.putExtra(KEY_LONGITUDE, longitude);

                setResult(Activity.RESULT_OK, ret);
                finish();

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_add_marker_menu, menu);
        return true;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        UiSettings uiSettings = mGoogleMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setMyLocationButtonEnabled(true);
        uiSettings.setZoomGesturesEnabled(true);

        LatLngBounds bounds = mGoogleMap.getProjection().getVisibleRegion().latLngBounds;
        mCurrentLatLng = bounds.getCenter();

        mMarker = mGoogleMap.addMarker(new MarkerOptions()
            .position(mCurrentLatLng));

        mGoogleMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                LatLngBounds b = mGoogleMap.getProjection().getVisibleRegion().latLngBounds;
                mCurrentLatLng = b.getCenter();
                mMarker.setPosition(mCurrentLatLng);
            }
        });
    }
}
