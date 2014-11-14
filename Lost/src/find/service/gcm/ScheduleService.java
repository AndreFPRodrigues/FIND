package find.service.gcm;

import java.util.GregorianCalendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class ScheduleService {
	private final static String TAG = "gcm";

	public static void setAlarm(String date , Context c) {
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
