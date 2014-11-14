package find.service.gcm;

import java.util.GregorianCalendar;

import find.service.R;
import find.service.net.diogomarques.wifioppish.MessagesProvider;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class PopUpActivity extends Activity {
	private Context c;
	private final int threshold = (60 * 2 * 1000);
	private final static String TAG = "gcm";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		c = this;
		Intent intent = getIntent();

		final String name = intent.getExtras().getString("name");
		String location = intent.getExtras().getString("location");
		final String date = intent.getExtras().getString("date");
		String duration = intent.getExtras().getString("duration");

		final double latS = intent.getExtras().getDouble("latS");
		final double lonS = intent.getExtras().getDouble("lonS");
		final double latE = intent.getExtras().getDouble("latE");
		final double lonE = intent.getExtras().getDouble("lonE");

		// set timer for retriving location
		long timeleft = DateFunctions.timeToDate(date);
		long handlerTimer = timeleft - threshold;
		if (handlerTimer < 0)
			handlerTimer = 0;
		
		Log.d(TAG, "pop up timer:" + handlerTimer);
		final AlertDialog alert = new AlertDialog.Builder(this)
				.setIcon(R.drawable.service_logo)
				.setTitle("Associate to simulation")
				.setMessage(
						"Do you want to join " + name + " simulation in "
								+ location + " at " + date + " for " + duration
								+ " minutes?")
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								regSimulationContentProvider(name);
								Intent intent = new Intent().setClass(c,
										DemoActivity.class);
								intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								intent.setAction("registerParticipant");
								intent.putExtra("name", name);
								startActivity(intent);
								Simulation.preDownloadTiles(latS, lonS, latE,
										lonE, c);
								ScheduleService.setAlarm(date, c);

								Notifications.generateNotification(c,
										"You have been associate to " + name
												+ ". Details in FIND Service.");
								finish();

							}

						}).setNegativeButton("No", null).show();

		// Hide after some seconds
		final Handler handler = new Handler();
		final Runnable runnable = new Runnable() {
			@Override
			public void run() {
				if (alert.isShowing()) {
					regSimulationContentProvider(name);
					Intent intent = new Intent()
							.setClass(c, DemoActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					intent.setAction("registerParticipant");
					intent.putExtra("name", name);
					startActivity(intent);
					Simulation.preDownloadTiles(latS, lonS, latE, lonE, c);
					Notifications.generateNotification(c, "You have been associate to "
							+ name + ". Details in FIND Service.");
					ScheduleService.setAlarm(date, c);

					alert.dismiss();
					finish();
				}	
			}
		};

		alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				handler.removeCallbacks(runnable);
				finish();

			}
		});

		handler.postDelayed(runnable, handlerTimer);
	}

	private void regSimulationContentProvider(String value) {
		ContentValues cv = new ContentValues();
		cv.put(MessagesProvider.COL_SIMUKEY, "simulation");
		cv.put(MessagesProvider.COL_SIMUVALUE, value);
		c.getContentResolver().insert(MessagesProvider.URI_SIMULATION, cv);
	}


}
