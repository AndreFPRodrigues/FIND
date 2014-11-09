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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity; 
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.android.gms.maps.model.LatLng;

import find.service.R;
import find.service.net.diogomarques.wifioppish.MessagesProvider;
import find.service.net.diogomarques.wifioppish.sensors.LocationSensor;
import find.service.net.diogomarques.wifioppish.service.LOSTService;
import android.R.integer;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log; 
import android.widget.Toast; 

/**
 * This {@code WakefulBroadcastReceiver} takes care of creating and managing a
 * partial wake lock for your app. It passes off the work of processing the GCM
 * message to an {@code IntentService}, while ensuring that the device does not
 * go back to sleep in the transition. The {@code IntentService} calls
 * {@code GcmBroadcastReceiver.completeWakefulIntent()} when it is ready to
 * release the wake lock.
 */

public class GcmBroadcastReceiver extends WakefulBroadcastReceiver {
	private final String TAG = "gcm";
	private final int CREATE_AUTO = 0;
	private final int CREATE_POP = 1;
	private final int START = 2;
	private final int STOP = 3;
	private final int LAST_UPDATE_THRESHOLD = 1000 * 60 * 120;
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
		Intent svcIntent = null;
		Log.d(TAG, "received notification");

		setResultCode(Activity.RESULT_OK);
		if (intent.getAction().equals("startAlarm")) {
			boolean reset = intent.getBooleanExtra("reset", false);
			if(reset){
				Log.d("gcm", "reset");
				return;
			}
			Log.d(TAG, "starting service");
			svcIntent = new Intent(
					"find.service.net.diogomarques.wifioppish.service.LOSTService.START_SERVICE");
			context.startService(svcIntent);

			final SharedPreferences preferences = c.getApplicationContext()
					.getSharedPreferences("Lost",
							android.content.Context.MODE_PRIVATE);
			boolean checkedLocation = preferences.getBoolean("location", false);
			if (!checkedLocation) {
				ls = new LocationSensor(c);
				ls.startSensor();
				isInSimulationLocation(preferences.getFloat("latS", 0),
						preferences.getFloat("lonS", 0),
						preferences.getFloat("latE", 0),
						preferences.getFloat("lonE", 0));
			}
			return;
		}
		this.intent = intent;
		
	
		// get data from push notification
		type = intent.getExtras().getString("type");
		

		try { 
			int tp = Integer.parseInt(type); 
			if (tp == STOP) {
				Log.d(TAG, "received notification to STOP1 ");

				regSimulationContentProvider("");
				Log.d(TAG, "received notification to STOP 2");

				LOSTService.serviceActive=false;
				Log.d(TAG, "received notification to STOP 3");

				return ;
			}
		} catch (NumberFormatException e) {
			Log.d("gcm", "not int " + type);
		}
		
		name = intent.getExtras().getString("name");
		date = intent.getExtras().getString("date");
		latS = Double.parseDouble(intent.getExtras().getString("latS"));
		lonS = Double.parseDouble(intent.getExtras().getString("lonS"));
		latE = Double.parseDouble(intent.getExtras().getString("latE"));
		lonE = Double.parseDouble(intent.getExtras().getString("lonE"));
		
		
		// set timer for retriving location
		long timeleft = timeToDate(date);
		locationTimer = timeleft / 2;
		locationTimeout = locationTimer / number_attempts;

		Log.d(TAG, "ttimeleft:" + timeleft);

		// retriving last best location
		Location l = getBestLocation();
		int aux = 0;
		if (l == null || oldLocation(l)) {
			Log.d(TAG, "old location");

			// if old location then try to get new location for half the time
			// left until the starting date
			ls = new LocationSensor(c);
			ls.startSensor();
			getLocation();

			Log.d(TAG, "old location out");

		} else {
			currentLoc = l;
			startPopUp(new double[] { currentLoc.getLatitude(),
					currentLoc.getLongitude() });
		}

		/*
		 * try { int tp = Integer.parseInt(type);
		 * 
		 * switch (tp) { case CREATE_AUTO:
		 * 
		 * generateNotification(context, "You have been associate to " + name +
		 * ". Details in FIND Service."); regSimulationContentProvider(name);
		 * Simulation.preDownloadTiles(latS, lonS, latE, lonE, c); break; case
		 * CREATE_POP: intent.setClass(context, TransparentActivity.class);
		 * intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		 * context.startActivity(intent); break; case START: // start the
		 * service, if not instanciated already
		 * 
		 * svcIntent = new Intent(
		 * "net.diogomarques.wifioppish.service.LOSTService.START_SERVICE");
		 * context.startService(svcIntent);
		 * 
		 * break;
		 * 
		 * } }
		 * 
		 * }
		 */

	}

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
			isInside = isInLocation(currentLoc, latS, lonS, latE, lonE);
			if (!isInside) {
				Log.d(TAG, "not inside");

				return;
			}
			LatLng start = adjustCoordinates(center, RADIUS_DOWNLOAD, 135);
			intent.putExtra("latS", start.latitude);
			intent.putExtra("lonS", start.longitude);
			LatLng end = adjustCoordinates(center, RADIUS_DOWNLOAD, 315);
			intent.putExtra("latE", end.latitude);
			intent.putExtra("lonE", end.longitude);

		} else {
			Log.d(TAG, "location undefined calculate center");

			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean("location", false);
			editor.putFloat("latS", (float) latS);
			editor.putFloat("lonS", (float) lonS);
			editor.putFloat("latE", (float) latE);
			editor.putFloat("lonE", (float) lonE);
			editor.commit();

			center = findCenter(latS, lonS, latE, lonE);
			LatLng start = adjustCoordinates(center, RADIUS_DOWNLOAD, 135);
			intent.putExtra("latS", start.latitude);
			intent.putExtra("lonS", start.longitude);
			LatLng end = adjustCoordinates(center, RADIUS_DOWNLOAD, 315);
			intent.putExtra("latE", end.latitude);
			intent.putExtra("lonE", end.longitude);
		}

		// timed popup dialog
		Log.d(TAG, "call pop up");
		intent.setClass(c, PopUpActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		c.startActivity(intent);
	}

	private LatLng findCenter(double f_latS, double f_lonS, double f_latE,
			double f_lonE) {
		double diffLat = Math.abs(f_latS - f_latE) / 2;
		double diffLon = Math.abs(f_lonE - f_lonS) / 2;

		// tp.downloadTilesInBound(f_latS, f_lonE, f_latE, f_lonS , MIN_ZOOM,
		// MAX_ZOOM, c);

		return new LatLng(f_latS + diffLat, f_lonE + diffLon);
	}

	private LatLng adjustCoordinates(LatLng center, int radius, int degrees) {
		double lat = (center.latitude * Math.PI) / 180;
		
		double lon = (center.longitude * Math.PI) / 180;
		
		double d = (float) (((float) radius) / 6378.1);

		double brng = degrees * Math.PI / 180;
		// rad
		double destLat = Math.asin(Math.sin(lat) * Math.cos(d) + Math.cos(lat)
				* Math.sin(d) * Math.cos(brng));
		double destLng = ((lon + Math.atan2(
				Math.sin(brng) * Math.sin(d) * Math.cos(lat), Math.cos(d)
						- Math.sin(lat) * Math.sin(destLat))) * 180)
				/ Math.PI;
		destLat = (destLat * 180) / Math.PI;

		Log.d(TAG, "lat:" + lat + "->" + destLat + " lon:" + lon + "->"
				+ destLng);
		return new LatLng(destLat, destLng);

	}

	private boolean isInLocation(double[] loc, double f_latS, double f_lonS,
			double f_latE, double f_lonE) {

		Log.d("gcm", "values " + f_latS + " " + f_lonS + " " + f_latE + " "
				+ f_lonE);
		double lat = loc[0];
		double lon = loc[1];
		if (lat < f_latS && lat > f_latE && lon > f_lonE && lon < f_lonS) {
			return true;
		}
		return false;
	}

	static long timeToDate(String dtStart) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		try {
			Date date = format.parse(dtStart);
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date c_date = new Date();
			dateFormat.format(c_date);
			return getDateDiff(c_date, date, TimeUnit.MILLISECONDS);

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * Get a diff between two dates
	 * 
	 * @param date1
	 *            the oldest date
	 * @param date2
	 *            the newest date
	 * @param timeUnit
	 *            the unit in which you want the diff
	 * @return the diff value, in the provided unit
	 */
	public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
		long diffInMillies = date2.getTime() - date1.getTime();
		return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
	}

	private static void generateNotification(Context context, String message) {
		int icon = R.drawable.service_logo;
		long when = System.currentTimeMillis();
		NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(icon, message, when);

		String title = context.getString(R.string.app_name);

		Intent notificationIntent = new Intent(context, DemoActivity.class);
		// set intent so it does not start a new activity
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent intent = PendingIntent.getActivity(context, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(context, title, message, intent);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;

		// Play default notification sound
		notification.defaults |= Notification.DEFAULT_SOUND;

		// Vibrate if vibrate is enabled
		notification.defaults |= Notification.DEFAULT_VIBRATE;
		notificationManager.notify(0, notification);
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

	/**
	 * try to get the 'best' location selected from all providers
	 */
	private Location getBestLocation() {
		Location gpslocation = getLocationByProvider(LocationManager.GPS_PROVIDER);
		Location networkLocation = getLocationByProvider(LocationManager.NETWORK_PROVIDER);
		// if we have only one location available, the choice is easy
		if (gpslocation == null) {
			Log.d(TAG,
					"No GPS Location available.");
			if (networkLocation!=null && networkLocation.getAccuracy() < 1500)
				return networkLocation; 
			else
				return null;
		}
		if (networkLocation == null) {
			Log.d(TAG, "No Network Location available");
			return gpslocation;
		}
		// a locationupdate is considered 'old' if its older than the configured
		// update interval. this means, we didn't get a
		// update from this provider since the last check
		boolean gpsIsOld = oldLocation(gpslocation);
		boolean networkIsOld = oldLocation(networkLocation);
		// gps is current and available, gps is better than network
		if (!gpsIsOld ) {
			Log.d(TAG, "Returning current GPS Location");
			return gpslocation;
		}
		// gps is old, we can't trust it. use network location
		if (!networkIsOld) {
			Log.d(TAG, "GPS is old, Network is current, returning network");
			return networkLocation;
		}
		// both are old return the newer of those two
		if (gpslocation.getTime() > networkLocation.getTime()) {
			Log.d(TAG, "Both are old, returning gps(newer)");
			return gpslocation;
		} else {
			Log.d(TAG, "Both are old, returning network(newer)");
			return networkLocation;
		}
	}

	private boolean oldLocation(Location l) {
		long old = System.currentTimeMillis() - LAST_UPDATE_THRESHOLD;
		return (l.getTime() < old);
	}

	/**
	 * get the last known location from a specific provider (network/gps)
	 */
	private Location getLocationByProvider(String provider) {
		Location location = null;

		LocationManager locationManager = (LocationManager) c
				.getApplicationContext().getSystemService(
						Context.LOCATION_SERVICE);
		try {
			if (locationManager.isProviderEnabled(provider)) {
				location = locationManager.getLastKnownLocation(provider);
			}
		} catch (IllegalArgumentException e) {
			Log.d(TAG, "Cannot acces Provider " + provider);
		}
		return location;
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
				if (!isInLocation(value, lat, lon, lat2, lon2)){
					stop();
					Log.d(TAG, "In location");

				} 
			} else {  
				Log.d(TAG, "No location");

				isInSimulationLocation(lat, lon, lat2, lon2);

			}
		}
	}

	private void stop() {
		Log.d(TAG, "Stopping service");
		generateNotification(c, "The simulation has stopped");

		
		Intent svcIntent = new Intent(
				"find.service.net.diogomarques.wifioppish.service.LOSTService.START_SERVICE");
		final SharedPreferences prefs = c.getSharedPreferences(DemoActivity.class.getSimpleName(),
				Context.MODE_PRIVATE);
		c.stopService(svcIntent);
		regSimulationContentProvider("");
		String regid = prefs.getString(DemoActivity.PROPERTY_REG_ID, "");

		deletePoints(regid);
		c.deleteDatabase("LOSTMessages");

		Intent mStartActivity = new Intent(c, DemoActivity.class);
		int mPendingIntentId = 123456;
		PendingIntent mPendingIntent = PendingIntent.getActivity(c, mPendingIntentId,    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager mgr = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
		mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
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
			double[] value = (double[]) ls.getCurrentValue();
			if (value[0] != 0) {
				ls.stopSensor();
				startPopUp(value);
			} else {

				getLocation();
			}

		}
	};

	private void getLocation() {
		if (attempts < number_attempts) {
			handler.postDelayed(runnable, locationTimeout);
			attempts++;
		} else {

			ls.stopSensor();
			startPopUp(null);
		}

	}

	private void isInSimulationLocation(float lat, float lon, float lat2,
			float lon2) {

		verifyLoc.postDelayed(new MyRunnable(lat, lon, lat2, lon2), 30000);

	}

	public boolean doLocationUpdate(Location l, boolean force) {
		if (l == null) {
			Log.d(TAG, "Empty location");
			if (force)
				Toast.makeText(c, "Current location not available",
						Toast.LENGTH_SHORT).show();
			return false;
		}
		if (l.getAccuracy() >= currentLoc.getAccuracy()
				&& l.distanceTo(currentLoc) < l.getAccuracy() && !force) {
			Log.d(TAG, "Accuracy got worse and we are still "
					+ "within the accuracy range.. Not updating");
			return false;
		}
		currentLoc = l;

		return true;
	}
	// upload/store your location here

}
