/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package find.service.gcm;

import java.io.BufferedReader;
import java.io.IOException; 
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import com.google.android.gms.maps.model.LatLng;
import find.service.net.diogomarques.wifioppish.MessagesProvider;
import find.service.net.diogomarques.wifioppish.NodeIdentification;
import find.service.net.diogomarques.wifioppish.sensors.LocationSensor;
import find.service.net.diogomarques.wifioppish.service.LOSTService;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Handler;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

/**
 * This {@code WakefulBroadcastReceiver} takes care of creating and managing a
 * partial wake lock for your app. It passes off the work of processing the GCM
 * message to an {@code IntentService}, while ensuring that the device does not
 * go back to sleep in the transition. The {@code IntentService} calls
 * {@code GcmBroadcastReceiver.completeWakefulIntent()} when it is ready to
 * release the wake lock.
 */

public class GcmBroadcastReceiver extends WakefulBroadcastReceiver {
	private final String TAG = "GCM_Receiver";
	private final int STOP = 3;
	private final int RADIUS_DOWNLOAD = 1;
	private Context c;
 
	private long locationTimeout;
	private long locationTimer;  
	private int number_attempts = 5; 
	private int attempts = 1;
	private Location currentLoc = null;

	String type;
	String name;
	String date;
	double latS;
	double lonS;
	double latE;
	double lonE;
	Intent intent;

	private LocationSensor ls;

	@Override
	public void onReceive(Context context, Intent intent) {
		c = context;
		Log.d(TAG, "received notification: " + intent.getAction());
		setResultCode(Activity.RESULT_OK);
		this.intent = intent;

		// check if its alarm to start the service and enables it
		if (intent.getAction().equals("startAlarm")) {
			Log.d(TAG, "Handling alarm");

			handleAlarm();
			return;
		}

		// get data from push notification
		type = intent.getExtras().getString("type");
		Log.d(TAG, "Type:" + type);

		// if received stop notification start the stopping service
		try {
			int tp = Integer.parseInt(type);

			if (tp == STOP) {
				Log.d(TAG, "Stopping service");
				Notifications
						.generateNotification(c, "terminating the service");

				regSimulationContentProvider("");
				LOSTService.stop(c);
				return;
			}
		} catch (NumberFormatException e) {
			Log.d(TAG, "Converted type is not a number");

		}

		Log.d(TAG, "Checking received new notification");
		// received new simulation notification, getting parameters
		name = intent.getExtras().getString("name");
		date = intent.getExtras().getString("date");
		latS = Double.parseDouble(intent.getExtras().getString("latS"));
		lonS = Double.parseDouble(intent.getExtras().getString("lonS"));
		latE = Double.parseDouble(intent.getExtras().getString("latE"));
		lonE = Double.parseDouble(intent.getExtras().getString("lonE"));

		Log.d(TAG, "date: " + date);

		// set timer for retriving location
		long timeleft = DateFunctions.timeToDate(date);
		locationTimer = timeleft / 2;
		locationTimeout = locationTimer / number_attempts;

		Log.d(TAG, "timeleft: " + timeleft);

		// retriving last best location
		Location l = LocationFunctions.getBestLocation(context);
		if (l == null || LocationFunctions.oldLocation(l)) {
			Log.d(TAG, "old or null location");

			// if old location then try to get new location for half the time
			// left until the starting date
			ls = new LocationSensor(c);
			ls.startSensor();
			getLocation();

		} else {

			// prompt pop up window
			currentLoc = l;
			startPopUp(new double[] { currentLoc.getLatitude(),
					currentLoc.getLongitude() });
		}

	}

	/**
	 * Handles the start service alarm
	 */
	private void handleAlarm() {
		boolean reset = intent.getBooleanExtra("reset", false);
		if (reset) {
			Log.d(TAG, "reset");
			return;
		}
		Log.d(TAG, "received alarm intent starting service");

		Intent svcIntent = new Intent(
				"find.service.net.diogomarques.wifioppish.service.LOSTService.START_SERVICE");
		c.startService(svcIntent);

		final SharedPreferences preferences = c.getApplicationContext()
				.getSharedPreferences("Lost",
						android.content.Context.MODE_PRIVATE);
		boolean checkedLocation = preferences.getBoolean("location", false);
		if (!checkedLocation) {
			Log.d(TAG, "Confirm location is within bounds");

			ls = new LocationSensor(c);
			ls.startSensor();
			isInSimulationLocation(preferences.getFloat("latS", 0),
					preferences.getFloat("lonS", 0),
					preferences.getFloat("latE", 0),
					preferences.getFloat("lonE", 0));
		}
	}

	/**
	 * Prompt timed pop-up asking if the user wishes to associate himself with
	 * if it timeouts it automatically associates the user
	 * 
	 * @param currentLoc
	 */
	private void startPopUp(double[] currentLoc) {

		final SharedPreferences preferences = c.getApplicationContext()
				.getSharedPreferences("Lost",
						android.content.Context.MODE_PRIVATE);

		// if location is defined check if its inside the area of the alert
		boolean isInside;
		LatLng center;
		if (currentLoc != null && currentLoc[0] != 0) {
			Log.d(TAG, "got location:" + currentLoc.toString());
			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean("location", true);
			editor.commit();

			center = new LatLng(currentLoc[0], currentLoc[1]);
			isInside = LocationFunctions.isInLocation(currentLoc, latS, lonS,
					latE, lonE);
			if (!isInside) {
				Log.d(TAG, "Stopping: not inside bounds");
				return;
			}
			RequestServer.sendCoordinates(NodeIdentification.getMyNodeId(c), center, LocationFunctions.getBatteryLevel(c));
			LatLng start = LocationFunctions.adjustCoordinates(center,
					RADIUS_DOWNLOAD, 135);
			intent.putExtra("latS", start.latitude);
			intent.putExtra("lonS", start.longitude);
			LatLng end = LocationFunctions.adjustCoordinates(center,
					RADIUS_DOWNLOAD, 315);
			intent.putExtra("latE", end.latitude);
			intent.putExtra("lonE", end.longitude);

		} else {
			Log.d(TAG, "Location undefined: calculating center");

			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean("location", false);
			editor.putFloat("latS", (float) latS);
			editor.putFloat("lonS", (float) lonS);
			editor.putFloat("latE", (float) latE);
			editor.putFloat("lonE", (float) lonE);
			editor.commit();

			center = LocationFunctions.findCenter(latS, lonS, latE, lonE);
			// get top left coordinate
			LatLng start = LocationFunctions.adjustCoordinates(center,
					RADIUS_DOWNLOAD, 135);
			intent.putExtra("latS", start.latitude);
			intent.putExtra("lonS", start.longitude);
			// get bottom right coordinate
			LatLng end = LocationFunctions.adjustCoordinates(center,
					RADIUS_DOWNLOAD, 315);
			intent.putExtra("latE", end.latitude);
			intent.putExtra("lonE", end.longitude);
		}

		// timed popup dialog
		Log.d(TAG, "Starting pop up");
		intent.setClass(c, PopUpActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		c.startActivity(intent);

	}

	protected void deletePoints(final String regid) {
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				StringBuilder builder = new StringBuilder();
				HttpClient client = new DefaultHttpClient();
				HttpGet httpGet;

				httpGet = new HttpGet(
						"http://accessible-serv.lasige.di.fc.ul.pt/~lost/index.php/rest/simulations/deletePoints/"
								+ regid);

				try {
					HttpResponse response = client.execute(httpGet);
					StatusLine statusLine = response.getStatusLine();
					int statusCode = statusLine.getStatusCode();
					if (statusCode == 200) {
						HttpEntity entity = response.getEntity();
						InputStream content = entity.getContent();
						BufferedReader reader = new BufferedReader(
								new InputStreamReader(content));
						String line;
						while ((line = reader.readLine()) != null) {
							builder.append(line);
						}
					}
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				return null;
			}
		}.execute(null, null, null);
	}

	private void regSimulationContentProvider(String value) {
		ContentValues cv = new ContentValues();
		cv.put(MessagesProvider.COL_SIMUKEY, "simulation");
		cv.put(MessagesProvider.COL_SIMUVALUE, value);
		c.getContentResolver().insert(MessagesProvider.URI_SIMULATION, cv);
	}

	// On service start verify if the user is in a affected area
	Handler verifyLoc = new Handler();

	public class MyRunnable implements Runnable {
		private float lat;
		private float lat2;
		private float lon;
		private float lon2;

		public MyRunnable(float lat, float lat2, float lon, float lon2) {
			this.lat = lat;
			this.lat2 = lat2;
			this.lon = lon;
			this.lon2 = lon2;
		}

		public void run() {
			double[] value = (double[]) ls.getCurrentValue();
			if (value[0] != 0) {
				if (!LocationFunctions
						.isInLocation(value, lat, lon, lat2, lon2)) {
					stop();
					Log.d(TAG, "Not in location");
				}
				Log.d(TAG, "In location");

			} else {
				Log.d(TAG, "Undefined location");
				isInSimulationLocation(lat, lon, lat2, lon2);

			}
		}
	}

	/**
	 * Stops the service the user is not in location and deletes all points
	 */
	private void stop() {
		Log.d(TAG, "Stopping service");
		Notifications.generateNotification(c, "The simulation has stopped");

		Intent svcIntent = new Intent(
				"find.service.net.diogomarques.wifioppish.service.LOSTService.START_SERVICE");
		final SharedPreferences prefs = c.getSharedPreferences(
				DemoActivity.class.getSimpleName(), Context.MODE_PRIVATE);
		c.stopService(svcIntent);
		regSimulationContentProvider("");
		String regid = prefs.getString(DemoActivity.PROPERTY_REG_ID, "");

		deletePoints(regid);
		c.deleteDatabase("LOSTMessages");

		Intent mStartActivity = new Intent(c, DemoActivity.class);
		int mPendingIntentId = 123456;
		PendingIntent mPendingIntent = PendingIntent.getActivity(c,
				mPendingIntentId, mStartActivity,
				PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager mgr = (AlarmManager) c
				.getSystemService(Context.ALARM_SERVICE);
		mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100,
				mPendingIntent);
		System.exit(0);
		return;

	}

	/**
	 * Try to gather location from gps start popup activity if it gets it
	 */
	Handler handler = new Handler();
	final Runnable runnable = new Runnable() {
		@Override
		public void run() {
			Log.d(TAG, "Trying to get  location");
			double[] value = (double[]) ls.getCurrentValue();
			if (value[0] != 0) {
				ls.stopSensor();
				startPopUp(value);
			} else {

				getLocation();
			}

		}
	};

	/**
	 * Try to get gps location "number_attemps" times, if it fails prompt the
	 * pop up
	 */
	private void getLocation() {
		if (attempts < number_attempts) {
			handler.postDelayed(runnable, locationTimeout);
			attempts++;
		} else {
			ls.stopSensor();
			startPopUp(null);
		}
	}

	/**
	 * Verify if
	 * 
	 * @param lat
	 *            - top left latitude
	 * @param lon
	 *            - top left longitude
	 * @param lat2
	 *            - bottom right latitude
	 * @param lon2
	 *            - bottom right longitude
	 */
	private void isInSimulationLocation(float lat, float lon, float lat2,
			float lon2) {

		verifyLoc.postDelayed(new MyRunnable(lat, lon, lat2, lon2), 30000);

	}


}
