package com.tigontombstone.goflyakite;

import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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

public class MainActivity extends AppCompatActivity
{

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
	 * Provides access to the Location Settings API.
	 */
	private SettingsClient mSettingsClient;

	/**
	 * Stores parameters for requests to the FusedLocationProviderApi.
	 */
	private LocationRequest mLocationRequest;

	/**
	 * Stores the types of location services the client is interested in using. Used for checking
	 * settings to determine if the device has optimal location settings.
	 */
	private LocationSettingsRequest mLocationSettingsRequest;

	/**
	 * Callback for Location events.
	 */
	private LocationCallback mLocationCallback;

	/**
	 * Represents a geographical location.
	 */
	private Location mCurrentLocation;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
		mSettingsClient = LocationServices.getSettingsClient(this);

		createLocationCallback();
		createLocationRequest();
		buildLocationSettingsRequest();
	}

	@Override
	protected void onStart()
	{
		super.onStart();

		startLocationUpdates();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
	}

	/**
	 * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
	 * runtime permission has been granted.
	 */
	private void startLocationUpdates() {
		// Begin by checking if the device has the necessary location settings.
		mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
				.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
					@Override
					public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
						Log.i(TAG, "All location settings are satisfied.");

						//noinspection MissingPermission
						mFusedLocationClient.requestLocationUpdates(mLocationRequest,
								mLocationCallback, Looper.myLooper());

						//updateUI();
					}
				})
				.addOnFailureListener(this, new OnFailureListener() {
					@Override
					public void onFailure(@NonNull Exception e) {
						int statusCode = ((ApiException) e).getStatusCode();
						switch (statusCode) {
							case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
								Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
										"location settings ");
								try {
									// Show the dialog by calling startResolutionForResult(), and check the
									// result in onActivityResult().
									ResolvableApiException rae = (ResolvableApiException) e;
									rae.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
								} catch (IntentSender.SendIntentException sie) {
									Log.i(TAG, "PendingIntent unable to execute request.");
								}
								break;
							case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
								String errorMessage = "Location settings are inadequate, and cannot be " +
										"fixed here. Fix in Settings.";
								Log.e(TAG, errorMessage);
								Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
								//mRequestingLocationUpdates = false;
						}

					}
				});
	}

	/**
	 * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
	 * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
	 * if a device has the needed location settings.
	 */
	private void buildLocationSettingsRequest() {
		LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
		builder.addLocationRequest(mLocationRequest);
		mLocationSettingsRequest = builder.build();
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

				mCurrentLocation = locationResult.getLastLocation();
				Log.d(TAG, mCurrentLocation.toString());
			}
		};
	}


}