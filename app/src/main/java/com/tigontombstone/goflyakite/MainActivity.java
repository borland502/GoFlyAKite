package com.tigontombstone.goflyakite;

import android.Manifest;
import android.app.Application;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.johnhiott.darkskyandroidlib.ForecastApi;
import com.johnhiott.darkskyandroidlib.RequestBuilder;
import com.johnhiott.darkskyandroidlib.models.Request;
import com.johnhiott.darkskyandroidlib.models.WeatherResponse;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MainActivity extends AppCompatActivity {

	private static final String TAG = MainActivity.class.getSimpleName();

	/**
	 * Code used in requesting runtime permissions.
	 */
	private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

	/**
	 * Constant used in the location settings dialog.
	 */
	private static final int REQUEST_CHECK_SETTINGS = 0x1;

	/**
	 * The desired interval for location updates. Inexact. Updates may be more or less frequent.
	 */
	private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

	/**
	 * The fastest rate for active location updates. Exact. Updates will never be more frequent
	 * than this value.
	 */
	private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
			UPDATE_INTERVAL_IN_MILLISECONDS / 2;

	// Keys for storing activity state in the Bundle.
	private final static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
	private final static String KEY_LOCATION = "location";
	private final static String KEY_LAST_UPDATED_TIME_STRING = "last-updated-time-string";

	/**
	 * Provides access to the Fused Location Provider API.
	 */
	private FusedLocationProviderClient mFusedLocationClient;

	/**
	 * Stores parameters for requests to the FusedLocationProviderApi.
	 */
	private LocationRequest mLocationRequest;

	/**
	 * Callback for Location events.
	 */
	private LocationCallback mLocationCallback;

	/**
	 * Tracks the status of the location updates request. Value changes when the user presses the
	 * Start Updates and Stop Updates buttons.
	 */
	private Boolean mRequestingLocationUpdates;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
		mRequestingLocationUpdates = false;

		createLocationCallback();
		createLocationRequest();

		ForecastApi.create("");

	}

	@Override
	protected void onStart() {
		super.onStart();

		startLocationUpdates();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	/**
	 * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
	 * runtime permission has been granted.
	 */
	private void startLocationUpdates() {
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			if(checkPermissions()){
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
			}
			return;
		}
		mFusedLocationClient.requestLocationUpdates(mLocationRequest,
				mLocationCallback, Looper.myLooper());

	}

	/**
	 * Return the current state of the permissions needed.
	 */
	private boolean checkPermissions() {
		//TODO:  Add Internet permission check
		int permissionState = ActivityCompat.checkSelfPermission(this,
				Manifest.permission.ACCESS_FINE_LOCATION);
		return permissionState == PackageManager.PERMISSION_GRANTED;
	}


	/**
	 * Sets up the location request. Android has two location request settings:
	 * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
	 * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
	 * the AndroidManifest.xml.
	 * <p/>
	 * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
	 * interval (5 seconds), the Fused Location Provider API returns location updates that are
	 * accurate to within a few feet.
	 * <p/>
	 * These settings are appropriate for mapping applications that show real-time location
	 * updates.
	 */
	private void createLocationRequest() {
		mLocationRequest = new LocationRequest();

		// Sets the desired interval for active location updates. This interval is
		// inexact. You may not receive updates at all if no location sources are available, or
		// you may receive them slower than requested. You may also receive updates faster than
		// requested if other applications are requesting location at a faster interval.
		mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

		// Sets the fastest rate for active location updates. This interval is exact, and your
		// application will never receive updates faster than this value.
		mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	}

	/**
	 * Creates a callback for receiving location events.
	 */
	private void createLocationCallback() {
		mLocationCallback = new LocationCallback() {
			@Override
			public void onLocationResult(LocationResult locationResult) {
				super.onLocationResult(locationResult);

				//TODO:  Kill location update requests

				final RequestBuilder weatherRequest = new RequestBuilder();
				Request request = new Request();
				request.setLat(Double.toString(locationResult.getLastLocation().getLatitude()));
				request.setLng(Double.toString(locationResult.getLastLocation().getLongitude()));
				request.setUnits(Request.Units.US);
				request.setLanguage(Request.Language.ENGLISH);

				weatherRequest.getWeather(request, new Callback<WeatherResponse>() {
					@Override
					public void success(WeatherResponse weatherResponse, Response response) {
						Log.d(TAG, weatherResponse.getCurrently().getWindSpeed());
					}

					@Override
					public void failure(RetrofitError error) {
						//TODO:  UI notification
						Log.e(TAG, error.getMessage());
					}
				});
			}
		};
	}

	/**
	 * Callback received when a permissions request has been completed.
	 */
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
										   @NonNull int[] grantResults) {
		Log.i(TAG, "onRequestPermissionResult");
		if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
			if (grantResults.length <= 0) {
				// If user interaction was interrupted, the permission request is cancelled and you
				// receive empty arrays.
				Log.i(TAG, "User interaction was cancelled.");
			} else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				if (mRequestingLocationUpdates) {
					Log.i(TAG, "Permission granted, updates requested, starting location updates");
					startLocationUpdates();
				}
			} else {
				// Permission denied.
				Log.e(TAG, "Location permission denied");
			}
		}
	}

}