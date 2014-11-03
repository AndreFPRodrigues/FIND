package gcm;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.internal.ck;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import net.diogomarques.wifioppish.MessagesProvider;
import net.diogomarques.wifioppish.NodeIdentification;
import net.diogomarques.wifioppish.R;
import net.diogomarques.wifioppish.service.LOSTService;

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
	static final String TAG = "GCM Demo";

	private GoogleCloudMessaging gcm;
	private AtomicInteger msgId = new AtomicInteger();
	private Context context;

	private String regid;
	private Simulation[] activeSimulations;
	private String address;
	private String registeredSimulation;
	private String location;
	private String date;
	private String duration;

	private Handler ui;
	private Button associate;
	private Button unregister;
	private Button serviceActivate;
	private TextView test;
	//private CheckBox storage;
	private RadioGroup associationStatus;

	int associationState;
	int allowStorage;

	final static String PATH = Environment.getExternalStorageDirectory()
			+ "/mapapp/world.sqlitedb";;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = getApplicationContext();

		setContentView(R.layout.main);
		associate = (Button) findViewById(R.id.associate);
		unregister = (Button) findViewById(R.id.dissassociate);
		associationStatus = (RadioGroup) findViewById(R.id.radioGroup1);
		test = (TextView) findViewById(R.id.with);
		
		serviceActivate = (Button) findViewById(R.id.sservice);
		if(LOSTService.serviceActive){
			serviceActivate.setText("Stop Service");	
		}
		
		if (!netCheckin()) {
			test.setText("FIND Service requires internet connection to alter preferences " +
					"please connect via WIFI and restart the application");
			Toast.makeText(getApplicationContext(),
					"FIND Service Preferences requires internet connection",
					Toast.LENGTH_LONG).show();
			Toast.makeText(getApplicationContext(),
					"Connect via WIFI and restart the application",
					Toast.LENGTH_LONG).show();
			associate.setEnabled(false);
			unregister.setEnabled(false);
			associationStatus.setEnabled(false);
			((RadioButton) findViewById(R.id.manual)).setEnabled(false);
			((RadioButton) findViewById(R.id.pop)).setEnabled(false);

		} else {

			File bd = new File(Environment.getExternalStorageDirectory()
					.toString() + "/mapapp/world.sqlitedb");
			gcm.map.DownloadFile d;
			if (!bd.exists()) {
				d = new gcm.map.DownloadFile();
			}

			// set the tile provider and database

			final SharedPreferences preferences = getApplicationContext()
					.getSharedPreferences("Lost",
							android.content.Context.MODE_PRIVATE);
			//boolean chckValue = preferences.getBoolean("storage", true);
			int idRadioButton = preferences.getInt("associationState", 2);

			ui = new Handler();
			WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			WifiInfo info = manager.getConnectionInfo();
			address = info.getMacAddress();
			address = NodeIdentification.getNodeId(address);
			
			
			/*storage = (CheckBox) findViewById(R.id.checkBox1);
			storage.setChecked(chckValue);
			storage.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					SharedPreferences.Editor editor = preferences.edit();
					editor.putBoolean("storage", isChecked);
					editor.commit();
					allowStorage = isChecked ? 1 : 0;
					savePreferences();

				}
			});*/

			setAssociationStatus(idRadioButton);

			associationStatus
					.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

						@Override
						public void onCheckedChanged(RadioGroup group,
								int checkedId) {

							switch (checkedId) {
							case R.id.manual:
								associationState = 0;
								break;
							/*
							 * case R.id.auto: associationState = 1; break;
							 */
							case R.id.pop:
								associationState = 2;
								break;

							}
							SharedPreferences.Editor editor = preferences
									.edit();
							editor.putInt("associationState", associationState);
							editor.commit();
							savePreferences();

						}
					});

			
			// Check device for Play Services APK. If check succeeds, proceed
			// with
			// GCM registration.
			if (checkPlayServices()) {
				gcm = GoogleCloudMessaging.getInstance(this);
				regid = getRegistrationId(context);

				if (regid.isEmpty()) {
					registerInBackground();
				} else {
					register(address, regid);

				}

				Intent intent = getIntent();
				String action = intent.getAction();
				if (action != null && action.equals("registerParticipant")) {
					registerForSimulation(intent.getStringExtra("name"));

				}

				checkAssociation();

				getActiveSimulations();
				Log.d(TAG, regid);

			} else {
				Log.i(TAG, "No valid Google Play Services APK found.");
			}
		}
	}

	private void setAssociationStatus(int idRadioButton) {
		RadioButton rt = null;

		switch (idRadioButton) {
		case 0:
			rt = (RadioButton) findViewById(R.id.manual);
			break;
		/*
		 * case 1: rt = (RadioButton) findViewById(R.id.auto); break;
		 */
		case 2:
			rt = (RadioButton) findViewById(R.id.pop);
			break;

		}
		rt.toggle();
	}

	protected void savePreferences() {
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				StringBuilder builder = new StringBuilder();
				HttpClient client = new DefaultHttpClient();
				HttpGet httpGet;

				httpGet = new HttpGet(
						"http://accessible-serv.lasige.di.fc.ul.pt/~lost/index.php/rest/simulations/savePreferences/"
								+ associationState
								+ ","
								+ allowStorage
								+ ","
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

	private boolean netCheckin() {
		try {
			ConnectivityManager nInfo = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			nInfo.getActiveNetworkInfo().isConnectedOrConnecting();
			ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo netInfo = cm.getActiveNetworkInfo();
			if (netInfo != null && netInfo.isConnectedOrConnecting()) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	private void checkAssociation() {
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
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
					activeSimulations = new Simulation[jsonArray.length()];
					JSONObject jsonObject = jsonArray.getJSONObject(0);
					registeredSimulation = jsonObject.getString("name");
					location = jsonObject.getString("location");
					date = jsonObject.getString("start_date");
					duration = jsonObject.getString("duration_m");

					// simulation value in the content provider
					regSimulationContentProvider(registeredSimulation);

					if (registeredSimulation != null
							&& registeredSimulation.length() > 0) {

						ui.post(new Runnable() {
							public void run() {
								Log.d("gcm", registeredSimulation);
								test.setText(registeredSimulation + ", "
										+ location + " at " + date + " for "
										+ duration + "min");
								associate.setEnabled(false);
								unregister.setEnabled(true);
							}
						});
					}
				}
				return simulations;
			}
		}.execute(null, null, null);

	}

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
				return simulations;
			}
		}.execute(null, null, null);

	}

	/**
	 * Registers the application with GCM servers asynchronously.
	 * <p>
	 * Stores the registration ID and the app versionCode in the application's
	 * shared preferences.
	 */
	private void registerInBackground() {
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				String msg = "";
				try {
					if (gcm == null) {
						gcm = GoogleCloudMessaging.getInstance(context);
					}
					if (regid.isEmpty())
						regid = gcm.register(SENDER_ID);

					msg = "Device registered, registration ID=" + regid;

					register(address, regid);

					// For this demo: we don't need to send it because the
					// device will send
					// upstream messages to a server that echo back the message
					// using the
					// 'from' address in the message.

					// Persist the regID - no need to register again.
					storeRegistrationId(context, regid);
				} catch (IOException ex) {
					msg = "Error :" + ex.getMessage();
					// If there is an error, don't just keep trying to register.
					// Require the user to click a button again, or perform
					// exponential back-off.
				}
				return msg;
			}

		}.execute(null, null, null);
	}
	
	public void activateService(final View view) {
		if(LOSTService.serviceActive){
			stop();
			LOSTService.serviceActive=false;
		}else{
			Intent svcIntent = new Intent(
					"net.diogomarques.wifioppish.service.LOSTService.START_SERVICE");
			context.startService(svcIntent);
			serviceActivate.setText("Stop Service");
		}
	}
	
	private void stop() {
		serviceActivate.setText("Stopping service in next internet connection...");
		associate.setEnabled(false);
		unregister.setEnabled(false);
		associationStatus.setEnabled(false);
		((RadioButton) findViewById(R.id.manual)).setEnabled(false);
		((RadioButton) findViewById(R.id.pop)).setEnabled(false);
		serviceActivate.setEnabled(false);
		regSimulationContentProvider("");

	}

	// Select Simulation
	public void associate(final View view) {

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

				registerForSimulation(activeSimulations[position].getName());

				registeredSimulation = activeSimulations[p].getName();
				regSimulationContentProvider(registeredSimulation);

				ui.post(new Runnable() {
					public void run() {
						// Log.d("gcm", registeredSimulation);
						test.setText(activeSimulations[p].toString());
						associate.setEnabled(false);
						unregister.setEnabled(true);

						al.cancel();
						activeSimulations[p].activate(context);

					}
				});
				/*
				 * alert.setTitle("Simulation: " + name);
				 * alert.setMessage("Password");
				 * 
				 * // Set an EditText view to get user input final EditText
				 * input = new EditText(DemoActivity.this);
				 * alert.setView(input);
				 * 
				 * alert.setPositiveButton("Ok", new
				 * DialogInterface.OnClickListener() { public void
				 * onClick(DialogInterface dialog, int whichButton) {
				 * al.cancel();
				 * 
				 * }
				 * 
				 * });
				 * 
				 * alert.setNegativeButton("Cancel", new
				 * DialogInterface.OnClickListener() { public void
				 * onClick(DialogInterface dialog, int whichButton) { //
				 * Canceled. } });
				 * 
				 * alert.show();
				 */
			}
		});
		al.setCanceledOnTouchOutside(true);
	}

	public void unregister(final View view) {
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
						regSimulationContentProvider(registeredSimulation);

						ui.post(new Runnable() {
							public void run() {
								// Log.d("gcm", registeredSimulation);
								test.setText("No simulation associated");
								associate.setEnabled(true);
								unregister.setEnabled(false);
								PopUpActivity.cancelAlarm(context);
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

				return "";
			}
		}.execute(null, null, null);
	}

	private void regSimulationContentProvider(String value) {
		ContentValues cv = new ContentValues();
		cv.put(MessagesProvider.COL_SIMUKEY, "simulation");
		cv.put(MessagesProvider.COL_SIMUVALUE, value);
		context.getContentResolver()
				.insert(MessagesProvider.URI_SIMULATION, cv);

	}

	private void registerForSimulation(final String name/*
														 * , final String
														 * password
														 */) {

		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				StringBuilder builder = new StringBuilder();
				HttpClient client = new DefaultHttpClient();
				HttpPost httpPost;

				httpPost = new HttpPost(
						"http://accessible-serv.lasige.di.fc.ul.pt/~lost/index.php/rest/simulations");

				try {
					List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
							3);
					nameValuePairs.add(new BasicNameValuePair("name", name));
					nameValuePairs.add(new BasicNameValuePair("regid", regid));
					nameValuePairs.add(new BasicNameValuePair("mac_address",
							address));

					httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

					// Execute HTTP Post Request
					HttpResponse response = client.execute(httpPost);
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
						// Log.d("gcm", "response " + builder.toString());
					} else {
						// Log.e(ParseJSON.class.toString(),
						// "Failed to download file");
					}
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				return builder.toString();
			}
		}.execute(null, null, null);

	}

	// Register this account with the server.
	void register(final String mac, final String regId) {

		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params1) {
				String serverUrl = "http://accessible-serv.lasige.di.fc.ul.pt/~lost/gcm/register.php";

				Map<String, String> params = new HashMap<String, String>();
				params.put("regId", regId);
				params.put("mac", mac);

				// Post registration values to web server
				try {
					post(serverUrl, params);

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return serverUrl;
			}
		}.execute(null, null, null);

	}

	// Register the gcm user
	private static String post(String endpoint, Map<String, String> params)
			throws IOException {

		StringBuilder sb = new StringBuilder();

		URL url;
		try {

			url = new URL(endpoint);

		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("invalid url: " + endpoint);
		}

		StringBuilder bodyBuilder = new StringBuilder();
		Iterator<Entry<String, String>> iterator = params.entrySet().iterator();

		// constructs the POST body using the parameters
		while (iterator.hasNext()) {
			Entry<String, String> param = iterator.next();
			bodyBuilder.append(param.getKey()).append('=')
					.append(param.getValue());
			if (iterator.hasNext()) {
				bodyBuilder.append('&');
			}
		}

		String body = bodyBuilder.toString();

		byte[] bytes = body.getBytes();

		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			conn.setFixedLengthStreamingMode(bytes.length);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded;charset=UTF-8");
			// post the request
			OutputStream out = conn.getOutputStream();
			out.write(bytes);
			out.flush();
			// Get the server response
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			String line = null;
			// Read Server Response
			while ((line = reader.readLine()) != null) {
				// Append server response in string
				sb.append(line + "\n");
			}
			reader.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (conn != null) {
				conn.disconnect();
				return sb.toString();
			}
		}
		return null;
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

	/**
	 * Stores the registration ID and the app versionCode in the application's
	 * {@code SharedPreferences}.
	 * 
	 * @param context
	 *            application's context.
	 * @param regId
	 *            registration ID
	 */
	private void storeRegistrationId(Context context, String regId) {
		final SharedPreferences prefs = getGcmPreferences(context);
		int appVersion = getAppVersion(context);
		Log.i(TAG, "Saving regId on app version " + appVersion);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PROPERTY_REG_ID, regId);
		editor.putInt(PROPERTY_APP_VERSION, appVersion);
		editor.commit();
	}

	/**
	 * Gets the current registration ID for application on GCM service, if there
	 * is one.
	 * <p>
	 * If result is empty, the app needs to register.
	 * 
	 * @return registration ID, or empty string if there is no existing
	 *         registration ID.
	 */
	private String getRegistrationId(Context context) {
		final SharedPreferences prefs = getGcmPreferences(context);
		String registrationId = prefs.getString(PROPERTY_REG_ID, "");
		if (registrationId.isEmpty()) {
			Log.i(TAG, "Registration not found.");
			return "";
		}
		// Check if app was updated; if so, it must clear the registration ID
		// since the existing regID is not guaranteed to work with the new
		// app version.
		int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION,
				Integer.MIN_VALUE);
		int currentVersion = getAppVersion(context);
		if (registeredVersion != currentVersion) {
			Log.i(TAG, "App version changed.");
			return "";
		}
		return registrationId;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	private static int getAppVersion(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}

	/**
	 * @return Application's {@code SharedPreferences}.
	 */
	private SharedPreferences getGcmPreferences(Context context) {
		// This sample app persists the registration ID in shared preferences,
		// but
		// how you store the regID in your app is up to you.
		return getSharedPreferences(DemoActivity.class.getSimpleName(),
				Context.MODE_PRIVATE);
	}
}