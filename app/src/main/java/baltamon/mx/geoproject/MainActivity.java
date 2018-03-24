package baltamon.mx.geoproject;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mGoogleMap;
    private GoogleApiClient mGoogleApiClient;
    private boolean mLocationPermissionGranted;
    private Location mLastKnownLocation;
    private CameraPosition mCameraPosition;
    private static final float CAMERA_ZOOM = 13;

    private static final int LOCATION_PERMISSION = 100;

    private Realm mRealm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupRealm();

        setupToolbar();
        setupConnection();
        setupRecyclerView();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        checkForLocationPermissions();

        if (mLocationPermissionGranted)
            updateLocationUI();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.tb_empty_list:
                Toast.makeText(this, "Empty list", Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setupToolbar() {
        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        ActionBar mActionBar = getSupportActionBar();
        if (mActionBar != null)
            mActionBar.setTitle("Geo Project");
    }


    private void setupRealm(){
        Realm.init(this);
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                .name("geoprojectdb.realm").build();
        mRealm = Realm.getInstance(realmConfiguration);
    }

    private void setupConnection() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
        mGoogleApiClient.connect();
    }

    public void setupRecyclerView() {
        ArrayList<String> arrayList = new ArrayList<>();
        for (int i = 1; i <= 10; i++)
            arrayList.add(i + " Number of address");

        RecyclerView mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        AddressesRecyclerAdapter mAdapter = new AddressesRecyclerAdapter(arrayList);
        mRecyclerView.setAdapter(mAdapter);
    }

    public void checkForLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            mLocationPermissionGranted = true;
        else
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case LOCATION_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    mLocationPermissionGranted = true;
                break;
        }
        updateLocationUI();
    }

    private void updateLocationUI() {
        if (mGoogleMap == null)
            return;
        try {
            if (mLocationPermissionGranted) {
                mGoogleMap.setMyLocationEnabled(true);
                mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
                getDeviceLocation();
            } else {
                mGoogleMap.setMyLocationEnabled(false);
                mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getDeviceLocation() {
        try {
            mLastKnownLocation = LocationServices.FusedLocationApi.
                    getLastLocation(mGoogleApiClient);
            updateCameraPosition();
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void updateCameraPosition() {
        if (mLastKnownLocation != null) {
            mCameraPosition = new CameraPosition(
                    new LatLng(mLastKnownLocation.getLatitude(),
                            mLastKnownLocation.getLongitude()), CAMERA_ZOOM, 0, 0);
            mGoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));
        } else {
            Log.d("Location Error", "Current location is null. Using defaults.");
            mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        showToast("Connection was suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        showToast("There was a problem in the connection");
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        mRealm.close();
        super.onDestroy();
    }
}
