package find.service.net.diogomarques.wifioppish.service;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

import find.service.R;
import find.service.gcm.DemoActivity;
import find.service.net.diogomarques.wifioppish.AndroidEnvironment;
import find.service.net.diogomarques.wifioppish.AndroidPreferences;
import find.service.net.diogomarques.wifioppish.IEnvironment;
import find.service.net.diogomarques.wifioppish.MessagesProvider;
import find.service.net.diogomarques.wifioppish.IEnvironment.State;
import find.service.net.diogomarques.wifioppish.MyPreferenceActivity;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Represents the service that runs on foreground. It uses the LOST-OppNet
 * business logic to create an opportunistic network and exchange messages. To
 * start the service, an {@link Intent} must be created with the action
 * <tt>net.diogomarques.wifioppish.service.LOSTService.START_SERVICE</tt>,
 * followed by a call to {@link Activity#startService(Intent)}. Example:
 * 
 * <pre>
 * {
 * 	&#064;code
 * 	Intent i = new Intent(
 * 			&quot;net.diogomarques.wifioppish.service.LOSTService.START_SERVICE&quot;);
 * 	startService(i);
 * }
 * </pre>
 * 
 * This service also creates a {@link Notification} to ensure the service
 * remains active event the system is low on resources.
 * 
 * @author André Silva <asilva@lasige.di.fc.ul.pt>
 */
public class LOSTService extends Service {

	private final int NOTIFICATION_STICKY = 1;
	private final static String TAG = "LOST Service";
	private NotificationManager notificationManager;
	private static IEnvironment environment;

	public static boolean serviceActive = false;
	public static boolean toStop = false;
	public static boolean synced = false;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "Service created");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (environment != null) {
			Log.i(TAG, "Stopped looped");

			// environment.stopStateLoop();
			environment = null;
			serviceActive = false;
		}
		stopSelf();
		Log.i(TAG, "Service destroyed");

	}

	public static void stop(Context context) {

		if (!toStop) {
			LOSTService.toStop = true;
			saveLogCat();

			if (environment != null) {
				environment.stopStateLoop();
				// environment=null;
				serviceActive = false;
			}
			// indicate that service is now stopped connected
			ContentValues cv = new ContentValues();
			cv.put(MessagesProvider.COL_STATUSKEY, "service");
			cv.put(MessagesProvider.COL_STATUSVALUE, "Stopping");
			context.getContentResolver().insert(MessagesProvider.URI_STATUS, cv);
			
			Intent intent = new Intent(context, LOSTService.class);
			PendingIntent pintent = PendingIntent.getService(context, 0,
					intent, 0);
			AlarmManager alarm = (AlarmManager) context
					.getSystemService(Context.ALARM_SERVICE);
			alarm.set(AlarmManager.RTC_WAKEUP,
					System.currentTimeMillis() + 100, pintent);
			System.exit(0);
		}
		return;
	}

	public static void terminate(Context context) {
		LOSTService.toStop = false;
		LOSTService.serviceActive = false;
		LOSTService.synced = false;

		Intent svcIntent = new Intent(
				"find.service.net.diogomarques.wifioppish.service.LOSTService.START_SERVICE");

		//context.deleteDatabase("LOSTMessages");
		ContentResolver cr = environment.getAndroidContext()
				.getContentResolver();
		cr.query(Uri
				.parse("content://find.service.net.diogomarques.wifioppish.MessagesProvider/customsend"),
				null, "", null, "");


		ContentValues cv = new ContentValues();
		cv.put(MessagesProvider.COL_STATUSKEY, "service");
		cv.put(MessagesProvider.COL_STATUSVALUE, "Disabled");
		context.getContentResolver().insert(MessagesProvider.URI_STATUS, cv);

		context.stopService(svcIntent);
		System.exit(0);

	}

	/**
	 * Starts the business logic to create an opportunistic network
	 */
	private void processStart() {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {

				if (LOSTService.toStop) {
					environment.startStateLoop(State.Stopped);

				} else {
					environment.startStateLoop(State.Scanning);
				}
				return null;
			}

		}.execute();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "About to start service");

		if (environment == null) {
			Log.i(TAG, "Creating new instance");
			serviceActive = true;

			// populate default preferences that may be missing
			PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

			environment = AndroidEnvironment.createInstance(this);

			// listener for state
			ContentResolver cr = getContentResolver();
			String URL = "content://find.service.net.diogomarques.wifioppish.MessagesProvider/status";
			Uri uri = Uri.parse(URL);
			Cursor c = getContentResolver().query(uri, null, "", null, "");
			String serviceState = "";
			if (c.moveToFirst()) {
				do {
					if (c.getString(c.getColumnIndex("statuskey")).equals(
							"service")) {
						serviceState = c.getString(c
								.getColumnIndex("statusvalue"));
					}
				} while (c.moveToNext());
			}
			c.close();
			if (serviceState.equals("Stopping")) {
				LOSTService.toStop = true;
				startForeground(NOTIFICATION_STICKY,
						getNotification("FIND Service is syncing"));
			} else {
				startForeground(NOTIFICATION_STICKY,
						getNotification("The FIND Service is now running"));

			}

			processStart();
		}

		return Service.START_STICKY;
	}

	/**
	 * Creates a notification telling that the LOST Service is running. This
	 * notification is important to ensure the service keeps running and doesn't
	 * killed by Android system when the system is low on resources.
	 * 
	 * @param contentText
	 * 
	 * @return {@link Notification} instance, with default values to tell
	 *         service is running
	 */
	@SuppressWarnings("deprecation")
	private Notification getNotification(String contentText) {
		Log.i(TAG, "Get notification");

		if (notificationManager == null)
			notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		CharSequence contentTitle = "FIND Service";
		notificationManager.cancelAll();
		// Although deprecated, this code ensures compatibility with older
		// Android versions
		Notification note = new Notification(R.drawable.service_logo,
				contentTitle, 0);
		note.flags |= Notification.FLAG_NO_CLEAR;
		note.flags |= Notification.FLAG_FOREGROUND_SERVICE;

		PendingIntent intent = PendingIntent.getActivity(this, 0, new Intent(
				this, MyPreferenceActivity.class), 0);

		note.setLatestEventInfo(this, contentTitle, contentText, intent);
		return note;
	}

	private static void saveLogCat() {
		String filePath = Environment.getExternalStorageDirectory() + "/logcat";

		try {

			Runtime.getRuntime().exec(
					new String[] { "logcat", "-f", filePath + "_FIND.txt",
							"-v", "time", "dalvikvm:S *:V" });
			Runtime.getRuntime().exec(new String[] { "logcat", "-c" });

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
