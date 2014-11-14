package find.service.gcm;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import find.service.R;
import find.service.gcm.map.DownloadFile;
import find.service.net.diogomarques.wifioppish.NodeIdentification;
import find.service.net.diogomarques.wifioppish.service.LOSTService;
import find.service.org.json.JSONArray;
import find.service.org.json.JSONObject;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * Main UI for the demo app.
 */
public class DemoActivity extends Activity {

	public static final String EXTRA_MESSAGE = "message";
	public static final String PROPERTY_REG_ID = "registration_id";
	private static final String PROPERTY_APP_VERSION = "appVersion";
	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

	/**
	 * Substitute you own sender ID here. This is the project number you got
	 * from the API Console, as described in "Getting Started."
	 */
	String SENDER_ID = "253078140647";

	/**
	 * Tag used on log messages.
	 */
	static final String TAG = "gcm";

	private GoogleCloudMessaging gcm;
	private AtomicInteger msgId = new AtomicInteger();
	private Context context;

	private String regid;
	private Simulation[] activeSimulations;
	private int indexSimu;
	private String address;
	private String registeredSimulation;
	private String location;
	private String date;
	private String duration;

	private Handler ui;
	private Button associate;
	private boolean state_associated;
	private Button serviceActivate;
	private TextView test;
	// private CheckBox storage;
	private RadioGroup associationStatus;

	int associationState;
	int allowStorage;
	
	private final int MANUAL=0;
	private final int AUTO=1;
	private final int POP_UP=2;

	final static String PATH = Environment.getExternalStorageDirectory()
			+ "/mapapp/world.sqlitedb";;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = getApplicationContext();
		setContentView(R.layout.service_main);
		associate = (Button) findViewById(R.id.associate);
		associationStatus = (RadioGroup) findViewById(R.id.radioGroup1);
		state_associated = false;
		test = (TextView) findViewById(R.id.with);
		serviceActivate = (Button) findViewById(R.id.sservice);
		indexSimu=-1;

		//Check if the service is stopping and blocks interface
		if (LOSTService.toStop) {
			test.setText("Waiting for internet connection to sync files");
			associate.setEnabled(false);
			associationStatus.setEnabled(false);
			((RadioButton) findViewById(R.id.manual)).setEnabled(false);
			((RadioButton) findViewById(R.id.pop)).setEnabled(false);
			serviceActivate.setText("Stopping Service");
			serviceActivate.setEnabled(false);
			return;
		}
		
		//Check service state and changes the button text
		if (LOSTService.serviceActive) {
			serviceActivate.setText("Stop Service");
		}

		//checks if there is internet connection
		if (!RequestServer.netCheckin(context)) {
			
			//Blocks all the association and service preferences, only allows the user to start/stop the service
			test.setText("FIND Service requires internet connection to alter preferences "
					+ "please connect via WIFI and restart the application");
			Toast.makeText(getApplicationContext(),
					"FIND Service Preferences requires internet connection",
					Toast.LENGTH_LONG).show();
			Toast.makeText(getApplicationContext(),
					"Connect via WIFI and restart the application",
					Toast.LENGTH_LONG).show();
			associate.setEnabled(false);
			associationStatus.setEnabled(false);
			((RadioButton) findViewById(R.id.manual)).setEnabled(false);
			((RadioButton) findViewById(R.id.pop)).setEnabled(false);

		} else {
			
			//Checks if the BD responsible for the tiles exits, if not download the file from the server
			// set the tile provider and database
			File bd = new File(Environment.getExternalStorageDirectory()
					.toString() + "/mapapp/world.sqlitedb");
			
			if (!bd.exists()) {
				DownloadFile.downloadTileDB();
			}


			final SharedPreferences preferences = getApplicationContext()
					.getSharedPreferences("Lost",
							android.content.Context.MODE_PRIVATE);
			int idRadioButton = preferences.getInt("associationState", 2);
			ui = new Handler();
			
			
			WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			WifiInfo info = manager.getConnectionInfo();
			
			//gets mac_address (user identification)
			address = info.getMacAddress();
			address = NodeIdentification.getNodeId(address);

			setAssociationStatus(idRadioButton);

			//Service preferences listener
			associationStatus
					.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

						@Override
						public void onCheckedChanged(RadioGroup group,
								int checkedId) {
							if (R.id.manual == checkedId) {
								associationState = MANUAL;
							} else {
								associationState = POP_UP;
							}
							
							SharedPreferences.Editor editor = preferences
									.edit();
							editor.putInt("associationState", associationState);
							editor.commit();
							RequestServer.savePreferences(associationState, allowStorage, regid);

						}
					});

			//registers user for simulation if intent equals "registerParticipant"
			Intent intent = getIntent();
			String action = intent.getAction();
			if (action != null && action.equals("registerParticipant")) {
				RequestServer.registerForSimulation(intent.getStringExtra("name"),  regid, address);
				Log.d("debugg", "Register for simulation 0");
			}
			
			//populates the active simulations window
			getActiveSimulations();
			
	
		}
	}
	
	/**
	 * Toggle the stored preference
	 * @param idRadioButton
	 */
	private void setAssociationStatus(int idRadioButton) {
		RadioButton rt = null;

		switch (idRadioButton) {
		case MANUAL:
			rt = (RadioButton) findViewById(R.id.manual);
			break;		
		case POP_UP: case AUTO:
			rt = (RadioButton) findViewById(R.id.pop);
			break;
		} 
		rt.toggle();
	}

	/**
	 * Check if there the user is associated with a simulation
	 */
	private void checkAssociation() {
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				if (activeSimulations.length == 0) {
					ui.post(new Runnable() {
						public void run() {
							test.setText("No simulations available");
							associate.setEnabled(false);
							state_associated = false;
						}
					});
					return "";
				}

				StringBuilder builder = new StringBuilder();
				HttpClient client = new DefaultHttpClient();
				HttpGet httpGet;

				httpGet = new HttpGet(
						"http://accessible-serv.lasige.di.fc.ul.pt/~lost/index.php/rest/simulations/user/"
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
					} else {
						// Log.e(ParseJSON.class.toString(),
						// "Failed to download file");
					}
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				String simulations = builder.toString();
				JSONArray jsonArray = new JSONArray(simulations);
				if (jsonArray.length() > 0) {
					JSONObject jsonObject = jsonArray.getJSONObject(0);
					registeredSimulation = jsonObject.getString("name");
					location = jsonObject.getString("location");
					date = jsonObject.getString("start_date");
					duration = jsonObject.getString("duration_m");

					// simulation value in the content provider
					Simulation.regSimulationContentProvider(registeredSimulation, context);

					if (registeredSimulation != null
							&& registeredSimulation.length() > 0) {

						ui.post(new Runnable() {
							public void run() {
								Log.d("gcm", "Associado a"+ registeredSimulation);
								test.setText(registeredSimulation + ", "
										+ location + " at " + date + " for "
										+ duration + "min");
								associate
										.setText("Disassociate from Simulation");
								state_associated = true;
							}
						});
					}
				}
				return simulations;
			}
		}.execute(null, null, null);

	}

	/**
	 * Populate the list of active simulations
	 */
	private void getActiveSimulations() {
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				StringBuilder builder = new StringBuilder();
				HttpClient client = new DefaultHttpClient();
				HttpGet httpGet;

				httpGet = new HttpGet(
						"http://accessible-serv.lasige.di.fc.ul.pt/~lost/index.php/rest/simulations");

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
					} else {
						// Log.e(ParseJSON.class.toString(),
						// "Failed to download file");
					}
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				String simulations = builder.toString();
				JSONArray jsonArray = new JSONArray(simulations);

				activeSimulations = new Simulation[jsonArray.length()];
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject jsonObject = jsonArray.getJSONObject(i);
					activeSimulations[i] = new Simulation(jsonObject);
				}

				checkAssociation();

				return simulations;
			}
		}.execute(null, null, null);

	}

	
	/**
	 * Starts/Stops the service 
	 * Handles the onclick of the Start/Stop Button
	 * @param view
	 */
	public void activateService(final View view) {
		if (LOSTService.serviceActive) {
			stop();
		} else {
			Intent svcIntent = new Intent(
					"find.service.net.diogomarques.wifioppish.service.LOSTService.START_SERVICE");
			context.startService(svcIntent);
			serviceActivate.setText("Stop Service");
		}
	}

	/**
	 * Initiates the stopping mechanism
	 */
	private void stop() {
		serviceActivate.setText("Stopping service");
		test.setText("Waiting for internet connection to sync files");
		associate.setEnabled(false);
		state_associated = false;
		associationStatus.setEnabled(false);
		((RadioButton) findViewById(R.id.manual)).setEnabled(false);
		((RadioButton) findViewById(R.id.pop)).setEnabled(false);
		serviceActivate.setEnabled(false);
		Simulation.regSimulationContentProvider("",context); 
		LOSTService.stop(context);

	}

	/**
	 * Handles the on click of the Associate/dissociate Button  
	 * @param view
	 */
	public void associate(final View view) {
		if (state_associated) {
			disassociate();
			return;
		}
		state_associated = true;
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(
				DemoActivity.this);
		LayoutInflater inflater = getLayoutInflater();
		View convertView = (View) inflater.inflate(R.layout.custom, null);
		alertDialog.setView(convertView);
		alertDialog.setTitle("Simulations");

		final ListView lv = (ListView) convertView.findViewById(R.id.listView1);
		lv.setBackgroundColor(Color.WHITE);
		String[] simu = new String[activeSimulations.length];
		

		for (int i = 0; i < simu.length; i++) {
			simu[i] = activeSimulations[i].getName() + ", "
					+ activeSimulations[i].getLocation();
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, simu);
		lv.setAdapter(adapter);
		final AlertDialog al = alertDialog.show();

		lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				final int p = position;
				view.invalidate();
				AlertDialog.Builder alert = new AlertDialog.Builder(
						DemoActivity.this);

				RequestServer.registerForSimulation(activeSimulations[position].getName(), regid, address);

				registeredSimulation = activeSimulations[p].getName();
				Simulation.regSimulationContentProvider(registeredSimulation, context);
				final String  start_date = activeSimulations[position].date;

				ui.post(new Runnable() {
					public void run() {
						Log.d("debugg", "associate to" + registeredSimulation );
						ScheduleService.setAlarm(start_date, context);
						test.setText(activeSimulations[p].toString());
						associate.setText("Disassociate from Simulation");

						al.cancel();
						activeSimulations[p].activate(context);

					}
				});
			}
		});
		al.setCanceledOnTouchOutside(true);
	}

	/**
	 * Dissassociate from the current simulation/alert
	 */
	public void disassociate() {
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				HttpClient client = new DefaultHttpClient();
				HttpGet httpGet;

				httpGet = new HttpGet(
						"http://accessible-serv.lasige.di.fc.ul.pt/~lost/index.php/rest/simulations/unregister/"
								+ address);

				try {
					HttpResponse response = client.execute(httpGet);
					StatusLine statusLine = response.getStatusLine();
					int statusCode = statusLine.getStatusCode();
					if (statusCode == 200) {
						registeredSimulation = "";
						Simulation.regSimulationContentProvider(registeredSimulation, context);

						ui.post(new Runnable() {
							public void run() {
								// Log.d("gcm", registeredSimulation);
								test.setText("No simulation associated");
								associate.setText("Associate to Simulation");
								ScheduleService.cancelAlarm(context);
							}
						});
					} else {
						// Log.e(ParseJSON.class.toString(),
						// "Failed to download file");
					}
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				state_associated = false;
				return "";
			}
		}.execute(null, null, null);
	}


	@Override
	protected void onResume() {
		super.onResume();
		// Check device for Play Services APK.
		checkPlayServices();
	}

	/**
	 * Check the device to make sure it has the Google Play Services APK. If it
	 * doesn't, display a dialog that allows users to download the APK from the
	 * Google Play Store or enable it in the device's system settings.
	 */
	private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, this,
						PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Log.i(TAG, "This device is not supported.");
				finish();
			}
			return false;
		}
		return true;
	}




	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

}