package find.service.gcm;

import java.util.GregorianCalendar;
import find.service.net.diogomarques.wifioppish.sensors.LocationSensor;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class ScheduleService extends BroadcastReceiver {
	private final static String TAG = "gcm";
	private static Context c;
	private LocationSensor ls;
	@Override
	public void onReceive(Context context, Intent intent) {
		c= context;
		if (intent.getAction().equals("startAlarm")) {
			handleAlarm();
		}
	}
	
	/**
	 * Handles the start service alarm
	 */
	private void handleAlarm() {
	
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
					stop(c);
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
	private void stop(Context c) {
		Log.d(TAG, "Stopping service");
		Notifications.generateNotification(c,"FIND Servoce" ,"Stopping service",null);

		Intent svcIntent = new Intent(
				"find.service.net.diogomarques.wifioppish.service.LOSTService.START_SERVICE");
		final SharedPreferences prefs = c.getSharedPreferences(
				DemoActivity.class.getSimpleName(), Context.MODE_PRIVATE);
		c.stopService(svcIntent);
		Simulation.regSimulationContentProvider("","","","",c);
		String regid = prefs.getString(SplashScreen.PROPERTY_REG_ID, "");

		RequestServer.deletePoints(regid);
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
	
	
	
	
	public static void setStartAlarm(String date, Context c) {
		date= date.replaceAll("-", "/");
		long timeleft = DateFunctions.timeToDate(date);
		if (timeleft < 0) {
			timeleft=0;
		
		}
			Long time = new GregorianCalendar().getTimeInMillis() + timeleft;
			Log.d(TAG, "setting alarm " + timeleft + " date:" + date);
			Intent intentAlarm = new Intent("startAlarm");
			PendingIntent startPIntent = PendingIntent.getBroadcast(c, 0,
					intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT);
			
			// create the object
			AlarmManager alarmManager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);

			// set the alarm for particular time
			alarmManager.set(AlarmManager.RTC_WAKEUP, time, startPIntent);
			Toast.makeText(c, "Service will automatically start at " + date,
			Toast.LENGTH_LONG).show();
	}
	
	public static void setStopAlarm(String date,String duration,  Context c) {
		date= date.replaceAll("-", "/");
		long timeleft = DateFunctions.timeToDate(date);
		if (timeleft < 0) {
			timeleft=0;
		
		}
			Long time = new GregorianCalendar().getTimeInMillis() + timeleft;
			Log.d(TAG, "setting alarm " + timeleft + " date:" + date);
			Intent intentAlarm = new Intent("startAlarm");
			PendingIntent startPIntent = PendingIntent.getBroadcast(c, 0,
					intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT);
			
			// create the object
			AlarmManager alarmManager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);

			// set the alarm for particular time
			alarmManager.set(AlarmManager.RTC_WAKEUP, time, startPIntent);
			Toast.makeText(c, "Service will automatically start at " + date,
			Toast.LENGTH_LONG).show();
	}
	

	public static void cancelAlarm(Context c) {
		Log.d(TAG, "canceling alarm");

		Intent intentAlarm = new Intent("startAlarm");
		PendingIntent startPIntent = PendingIntent.getBroadcast(c, 0,
				intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT);

		// create the object
		AlarmManager alarmManager = (AlarmManager) c
				.getSystemService(Context.ALARM_SERVICE);

		// set the alarm for particular time
		alarmManager.cancel(startPIntent);
		// Toast.makeText(this, "Alarm Scheduled for " + timeleft,
		// Toast.LENGTH_LONG).show();

	}

}
