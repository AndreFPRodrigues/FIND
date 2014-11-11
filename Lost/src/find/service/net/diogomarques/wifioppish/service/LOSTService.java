package find.service.net.diogomarques.wifioppish.service;

import find.service.R;
import find.service.net.diogomarques.wifioppish.AndroidEnvironment;
import find.service.net.diogomarques.wifioppish.AndroidPreferences;
import find.service.net.diogomarques.wifioppish.IEnvironment;
import find.service.net.diogomarques.wifioppish.IEnvironment.State;
import find.service.net.diogomarques.wifioppish.MyPreferenceActivity;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Represents the service that runs on foreground. It uses the LOST-OppNet
 * business logic to create an opportunistic network and exchange messages. 
 * To start the service, an {@link Intent} 
 * must be created with the action <tt>net.diogomarques.wifioppish.service.LOSTService.START_SERVICE</tt>, 
 * followed by a call to {@link Activity#startService(Intent)}. Example:
 * 
 * <pre>
 * {@code
 * Intent i = new Intent("net.diogomarques.wifioppish.service.LOSTService.START_SERVICE"); 
 * startService(i);
 * }
 * </pre>
 * 
 * This service also creates a {@link Notification} to ensure the service remains
 * active event the system is low on resources. 
 * 
 * @author André Silva <asilva@lasige.di.fc.ul.pt>
 */
public class LOSTService extends Service {

	private final int NOTIFICATION_STICKY = 1;
	private final String TAG = "LOST Service";
	private NotificationManager notificationManager;
	private static IEnvironment environment;
	
	public static boolean serviceActive=false;
	public static boolean toStop=false;

	
	@Override
	public void onCreate() { 
		super.onCreate();
		Log.i(TAG, "Service created");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if(environment != null){
			Log.i(TAG, "Stopped looped");

			environment.stopStateLoop();
			environment=null;
			serviceActive=false;
		}
		stopSelf();
		Log.i(TAG, "Service destroyed");
		
	}

	/**
	 * Starts the business logic to create an opportunistic network
	 */
	private void processStart() {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				environment.startStateLoop(State.Scanning);
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
			serviceActive=true;
			
			// populate default preferences that may be missing
			PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

			environment = AndroidEnvironment.createInstance(this);
			processStart();
			startForeground(NOTIFICATION_STICKY, getNotification());
		}

		return Service.START_STICKY; 
	}

	/**
	 * Creates a notification telling that the LOST Service is running. This
	 * notification is important to ensure the service keeps running and doesn't
	 * killed by Android system when the system is low on resources.
	 * 
	 * @return {@link Notification} instance, with default values to tell
	 *         service is running
	 */
	@SuppressWarnings("deprecation")
	private Notification getNotification() {
		Log.i(TAG, "Get notification");

		if (notificationManager == null)
			notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		CharSequence contentTitle = "FIND Service";
		CharSequence contentText = "The FIND Service is now running";

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

}
