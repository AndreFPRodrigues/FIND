package find.service.gcm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.model.LatLng;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

public class RequestServer {

	private static String postCoordinates = "http://accessible-serv.lasige.di.fc.ul.pt/~lost/index.php/rest/victims";
	private static String TAG = "gcm";

	/**
	 * Check if there is wifi connection
	 * 
	 * @return
	 */
	public static boolean netCheckin(Context c) {
		try {
			ConnectivityManager connManager = (ConnectivityManager) c
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo mWifi = connManager
					.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

			if (mWifi != null && mWifi.isConnectedOrConnecting()) {
				return true;

			} else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();

			return false;
		}
	}



	// Register the gcm user
	public static String post(String endpoint, Map<String, String> params)
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

	// Register this account with the server.
	public static void register(final String mac, final String regId,
			final String email) {

		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params1) {
				String serverUrl = "http://accessible-serv.lasige.di.fc.ul.pt/~lost/gcm/register.php";

				Map<String, String> params = new HashMap<String, String>();
				params.put("regId", regId);
				params.put("mac", mac);
				params.put("email", email);
				// Post registration values to web server
				try {
					RequestServer.post(serverUrl, params);

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return serverUrl;
			}
		}.execute(null, null, null);

	}

	/**
	 * Update association method (manual/pop_up) in the server
	 */
	public static void savePreferences(final int associationState,
			final int allowStorage, final String regid) {
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

	/**
	 * Registers user in a simulation
	 * 
	 * @param name
	 */
	public static void registerForSimulation(final String name,
			final String regid, final String address) {

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

					Log.d(TAG, "Associating: " + name + " " + regid + " "
							+ address);

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
						Log.d("gcm", "response " + builder.toString());
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

	public static void sendCoordinates(final String macAddress,
			final LatLng local, final float batery) {

		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				JSONArray jsonArray = new JSONArray();

				JSONObject json = new JSONObject();
				try {
					json.put("nodeid", macAddress);
					json.put("timestamp", System.currentTimeMillis());
					json.put("msg", "");
					json.put("latitude", local.latitude);
					json.put("longitude", local.longitude);
					json.put("llconf", 10);
					json.put("battery", batery);
					json.put("steps", 0);
					json.put("screen", 0);
					json.put("distance", -1);
					json.put("safe", 0);
					jsonArray.put(json);
					String contents = jsonArray.toString();
					HttpClient httpclient = new DefaultHttpClient();
					HttpPost httppost = new HttpPost(postCoordinates);
					List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
							2);
					nameValuePairs
							.add(new BasicNameValuePair("data", contents));
					httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
					HttpResponse response = httpclient.execute(httppost);
					HttpEntity entity = response.getEntity();
				} catch (JSONException e) {
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}
		}.execute(null, null, null);

	}

	protected static void deletePoints(final String regid) {
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

}
