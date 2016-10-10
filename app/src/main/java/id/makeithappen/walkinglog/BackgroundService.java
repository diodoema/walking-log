package id.makeithappen.walkinglog;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class BackgroundService extends Service implements SensorEventListener, GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener, LocationListener {

	private static final String TAG = "BackgroundService";
	public static final String BROADCAST_ACTION = "id.makeithappen.walkinglog.broadcast";
	Intent intent = new Intent(BROADCAST_ACTION);

	SensorManager sensorManager;
	DatabaseHandler db = new DatabaseHandler(this);
	Context context = this;

	public int initialStep, step, calorie, frequency;
	public float percentage;
	public double distance;

	int second, minute, hour;
	int perHour = 1;
	private long startTime = 0L;
	private Handler customHandler = new Handler();
	long timeInMillisecond = 0L;

	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

	private Location mLastLocation;

	// Google client to interact with Google API
	private GoogleApiClient mGoogleApiClient;

	private LocationRequest mLocationRequest;

	// Location updates intervals in sec
	private static int UPDATE_INTERVAL = 10000; // 10 sec
	private static int FATEST_INTERVAL = 5000; // 5 sec
	private static int DISPLACEMENT = 10; // 10 meter

	public static List<LatLng> trackPoints = new ArrayList<>();

	public static boolean isRunning = false;
	public static boolean isRestart = false;
	public static boolean call = false;

	@Override
	public IBinder onBind(Intent arg0) {
		// Used only in case if services are bound (Bound Services).
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		showNotification();
		// We want this service to continue running until it is explicitly stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onCreate() {
		Log.d(TAG, "Start");

		isRunning = true;

		// Initialization values
		initialStep = 0;
		step = 0;
		distance = 0;
		calorie = 0;
		frequency = 0;

		// Get current time
		startTime = SystemClock.uptimeMillis();

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		if(pref.getBoolean("pref_key_track", true)) {
			enableShowTrack();
		}

		// Register sensor listener for STEP COUNTER sensor
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER),
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "Stop");

		stopForeground(true);
		isRunning = false;

		String duration = String.valueOf(String.format("%02d", hour)+":"+String.format("%02d", minute)+":"+String.format("%02d", second));

		// If not restart
		if(!isRestart) {
			// Insert new history record into database
			db.addHistory(new History(step, duration, round(distance, 2), calorie, frequency));
		}

		isRestart = false;
		customHandler.removeCallbacks(updateTimerThread);
		trackPoints.clear();

		// Unregister sensor listener for STEP COUNTER sensor
		sensorManager.unregisterListener(this);

		// Stop location update
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		if(pref.getBoolean("pref_key_track", true)){
			disableShowTrack();
		}

		intent.putExtra("step", String.valueOf(0));
		intent.putExtra("percentage", String.valueOf(String.format("%.2f", 0.00)));
		intent.putExtra("duration_sec", String.valueOf(String.format("%02d", 0)));
		intent.putExtra("duration_min", String.valueOf(String.format("%02d", 0)));
		intent.putExtra("duration_hour", String.valueOf(String.format("%02d", 0)));
		intent.putExtra("distance", String.valueOf(String.format("%.2f", 0.00)));
		intent.putExtra("calorie", String.valueOf(0));
		intent.putExtra("frequency", String.valueOf(0));

		sendBroadcast(intent);
	}

	/**
	 * Method to round decimal
	 * */
	public double round(double value, int places) {
		if (places < 0) throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	/**
	 * Calculate duration time
	 * */
	private Runnable updateTimerThread = new Runnable() {
		public void run() {
			timeInMillisecond = SystemClock.uptimeMillis() - startTime;

			second = (int) (timeInMillisecond / 1000);
			minute = second / 60;
			hour = minute / 60;
			second = second % 60;
			minute = minute % 60;

			if(hour==perHour){
				frequency = step/perHour;
				perHour++;
			}

			intent.putExtra("duration_sec", String.valueOf(String.format("%02d", second)));
			intent.putExtra("duration_min", String.valueOf(String.format("%02d", minute)));
			intent.putExtra("duration_hour", String.valueOf(String.format("%02d", hour)));
			intent.putExtra("frequency", String.valueOf(frequency));

			customHandler.postDelayed(this, 1000);

			//Send Broadcast
			sendBroadcast(intent);
		}
	};

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		Log.d(TAG, "Accuracy has changed: "+accuracy);
	}

	/**
	 * Every sensor changed detect, this method automatically trigger
	 * */
	@Override
	public void onSensorChanged(SensorEvent event) {
		customHandler.postDelayed(updateTimerThread, 1000);

		// Get value from setting menu
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		String prefGoal = pref.getString("pref_key_goal","");
		String prefGender = pref.getString("pref_key_gender", "");
		String prefHeight = pref.getString("pref_key_height", "");
		String prefWeight = pref.getString("pref_key_weight", "");
		boolean allowShowTrack = pref.getBoolean("pref_key_track", true);

		float goal = Float.valueOf(prefGoal);
		float gender = Float.valueOf(prefGender);
		float height = Float.valueOf(prefHeight);
		float weight = Float.valueOf(prefWeight);

		// Check if 'Show Track' enable on setting menu
		if(allowShowTrack && !call){
			enableShowTrack();
		}else if(!allowShowTrack && call){
			disableShowTrack();
		}

		// It will return the total number since we registered, so we need to subtract the initial amount for the current steps
		if (initialStep < 1) {
			// initial step value
			initialStep = (int) event.values[0];
		}

		// Calculate steps taken based on first counter value received.
		step = (int) event.values[0] - initialStep;
		// Calculate steps goal percentage
		percentage = step / goal * 100;

		intent.putExtra("step", String.valueOf(step));
		intent.putExtra("percentage", String.valueOf(String.format("%.2f", percentage)));

		// Check user height for calculate distance
		if (height != 0) {
			countDistance(height, gender, step);
		} else {
			intent.putExtra("distance", String.valueOf(String.format("%.2f", 0.00)));
		}

		// Check use weight for calculate calorie burn
		if (weight != 0) {
			calCalorie(weight, distance);
		} else {
			intent.putExtra("calorie", String.valueOf(0));
		}

		// Update notification value
		showNotification();
	}

	/**
	 * Method to calculate calorie burn
	 * */
	private void calCalorie(float weight, double distance) {
		calorie = (int) ((weight / 0.453592) * (0.30) * (distance / 1.60934));
		intent.putExtra("calorie", String.valueOf(calorie));
	}

	/**
	 * Method to count distance
	 * */
	private void countDistance(float height, float gender, int step) {
		distance = gender * height * step / 100000;
		intent.putExtra("distance", String.valueOf(String.format("%.2f", distance)));
	}

	/**
	 * Call if show track enable on setting
	 * */
	private void enableShowTrack(){
		// First we need to check availability of play services
		if (checkPlayServices()) {
			// Building the GoogleApi client
			buildGoogleApiClient();

			createLocationRequest();

			if (mGoogleApiClient != null) {
				mGoogleApiClient.connect();
			}
		}
		call = true;
		Log.d(TAG, "Enable Show Track");
	}

	/**
	 * Call if show track disable on setting
	 * */
	private void disableShowTrack(){
		if(mGoogleApiClient != null){
			if (mGoogleApiClient.isConnected()) {
				stopLocationUpdates();
				call = false;
				Log.d(TAG, "Disable Show Track");
			}
		}
	}

	/**
	 * Method to display the location on UI
	 * */
	private void setTrack() {
		mLastLocation = LocationServices.FusedLocationApi
				.getLastLocation(mGoogleApiClient);

		if (mLastLocation != null) {
			double latitude = mLastLocation.getLatitude();
			double longitude = mLastLocation.getLongitude();

			LatLng point = new LatLng(latitude, longitude);
			trackPoints.add(point);

		} else {
			Toast.makeText(getApplicationContext(),
					"Couldn't get the location. Make sure location is enabled on the device", Toast.LENGTH_LONG)
					.show();
		}
	}

	/**
	 * Creating google api client object
	 * */
	protected synchronized void buildGoogleApiClient() {
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(LocationServices.API).build();
	}

	/**
	 * Creating location request object
	 * */
	protected void createLocationRequest() {
		mLocationRequest = new LocationRequest();
		mLocationRequest.setInterval(UPDATE_INTERVAL);
		mLocationRequest.setFastestInterval(FATEST_INTERVAL);
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
	}

	/**
	 * Method to verify google play services on the device
	 * */
	private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, (Activity) context,
						PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Toast.makeText(getApplicationContext(),
						"This device is not supported.", Toast.LENGTH_LONG)
						.show();
			}
			return false;
		}
		return true;
	}

	/**
	 * Starting the location updates
	 * */
	protected void startLocationUpdates() {
		LocationServices.FusedLocationApi.requestLocationUpdates(
				mGoogleApiClient, mLocationRequest, this);
	}

	/**
	 * Stopping location updates
	 */
	protected void stopLocationUpdates() {
		LocationServices.FusedLocationApi.removeLocationUpdates(
				mGoogleApiClient, this);
	}

	@Override
	public void onConnected(Bundle bundle) {
		// Once connected with google api, get the location
		setTrack();
		startLocationUpdates();
	}

	@Override
	public void onConnectionSuspended(int i) {
		mGoogleApiClient.connect();
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.d(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
				+ connectionResult.getErrorCode());
	}

	@Override
	public void onLocationChanged(Location location) {
		// Assign the new location
		mLastLocation = location;
		// Displaying the new location on UI
		setTrack();
	}

	/**
	 * Method to show notification
	 */
	private void showNotification() {
		Intent notificationIntent = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		Notification notification = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.ic_step)
				.setContentTitle("Walking Log")
				.setContentText("Step: "+step)
				.setOngoing(true)
				.setContentIntent(pendingIntent)
				.setOngoing(true).build();
		startForeground(101, notification);
	}
}
