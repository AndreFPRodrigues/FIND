package oppus.rescue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;

import oppus.rescue.Victim.VictimNode;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Color;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.mapapp.tileManagement.TilesProvider;

public class MapManager implements OnMarkerClickListener, OnMapClickListener {

	private GoogleMap googleMap;
	private HashMap<String, Victim> victims;
	private final static String LT = "RESCUE";

	// timestmap of last update
	private long latestTimestamp = -1;
	// Provides us with Tiles objects, passed to MapView
	private TilesProvider tilesProvider;
	// temporary markers when a victim is selected
	private Stack<Marker> tempMarkers;
	private Polyline tempRoute;

	private LayoutInflater context;

	// timestamp of the last time a point was added
	private long lastUpdate = -1;
	// timestamp of the last time it updated via web
	private long lastWebConnection = -1;
	// timestamp of the last local update
	private long lastLocalUpdate;
	// Total number of points retrived
	private int numberOfPoints = 0;
	// Number of points to display per victim (-1 displays all)
	private int numberPointsPerVictim = -1;
	// Display safe victims
	private boolean showSafe = true;

	private String filepath;
	private final int UPDATE_TIME = 30000;

	/**
	 * Map and listeners initialisation Starts tile provider
	 * 
	 * @param map
	 */
	public MapManager(GoogleMap map, LayoutInflater inflater) {
		this.context = inflater;
		googleMap = map;
		googleMap.setMapType(GoogleMap.MAP_TYPE_NONE);
		googleMap.setMyLocationEnabled(true);
		googleMap.setOnMarkerClickListener(this);
		googleMap.setOnMapClickListener(this);
		victims = new HashMap<String, Victim>();
		tempMarkers = new Stack<Marker>();
		filepath = Environment.getExternalStorageDirectory().toString()
				+ "/oppus/victims";
		startTileProvider();
		setInfoWindow();

	}

	private void setInfoWindow() {
		// Setting a custom info window adapter for the google map
		googleMap.setInfoWindowAdapter(new InfoWindowAdapter() {

			// Use default InfoWindow frame
			@Override
			public View getInfoWindow(Marker arg0) {
				return null;
			}

			// Defines the contents of the InfoWindow
			@Override
			public View getInfoContents(Marker marker) {
				VictimNode victim = victims.get(marker.getTitle()).getNode(
						marker.getSnippet());
				View v;
				if (!victim.getMessage().equals("")) {
					v = context.inflate(R.layout.info_window_message, null);
					// Getting reference to the TextView to set message
					TextView message = (TextView) v.findViewById(R.id.message);
					message.setText(victim.getMessage());
					message.setTextColor(Color.BLACK);
				} else {

					// Getting view from the layout file info_window_layout
					v = context.inflate(R.layout.info_window_layout, null);
				}

				// Getting reference to the TextView to set name
				TextView name = (TextView) v.findViewById(R.id.name);

				// Getting reference to the TextView to set lastTime
				TextView last = (TextView) v.findViewById(R.id.lastTime);
				// Getting reference to the TextView to set battery
				TextView battery = (TextView) v.findViewById(R.id.battery);
				// Getting reference to the TextView to set from
				TextView from = (TextView) v.findViewById(R.id.from);
				// Getting reference to the TextView to set steps
				TextView steps = (TextView) v.findViewById(R.id.steps);
				// Getting reference to the TextView to set distance
				TextView distance = (TextView) v.findViewById(R.id.distance);
				// Getting reference to the TextView to set screen
				TextView screen = (TextView) v.findViewById(R.id.screen);

				name.setText(marker.getTitle());
				name.setTextColor(Color.BLACK);
				last.setText("Here:    " + convertTime(victim.getTimestamp()));
				last.setTextColor(Color.BLACK);
				last.setText("Here:    " + convertTime(victim.getTimestamp()));
				last.setTextColor(Color.BLACK);
				battery.setText("Battery         " + victim.getBatery() + " %");
				battery.setTextColor(Color.BLACK);
				from.setText("From:    " + convertTime(victim.getTimestamp()));
				from.setTextColor(Color.BLACK);
				steps.setText("Steps             " + victim.getSteps());
				steps.setTextColor(Color.BLACK);
				distance.setText("Distance       " + victim.getDistance()
						+ " m");
				distance.setTextColor(Color.BLACK);
				screen.setText("Screen           " + victim.getScreen()
						+ " times");
				screen.setTextColor(Color.BLACK);

				// Returning the view containing InfoWindow contents
				return v;

			}
		});
	}

	void startTileProvider() {
		// Creating our database tilesProvider to pass it to our MapView
		String path = Environment.getExternalStorageDirectory()
				+ "/mapapp/world.sqlitedb";
		tilesProvider = new TilesProvider(path);

		// Create new TileOverlayOptions instance.
		TileOverlayOptions opts = new TileOverlayOptions();
		opts.tileProvider(tilesProvider);
		// Add the tile overlay to the map.
		TileOverlay overlay = googleMap.addTileOverlay(opts);

		updateVictims();
		/*
		 * // Update and draw the map view mapView.refresh();
		 */

		// tilesProvider.downloadTilesInBound(39.415817, -9.163834, 39.395955,
		// -9.113635, 16, 17);

	}

	private void addMarker(double lat, double lon, String message,
			boolean firstPoint, String author, long timestamp, int steps,
			int screen, int distance, int batery, boolean safe) {
		MarkerOptions marker;

		marker = new MarkerOptions().position(new LatLng(lat, lon)).title(
				author);

		Marker m = null;
		if (showSafe || !safe)
			m = googleMap.addMarker(marker);

		if (firstPoint) {

			addFirstVictim(m, message, timestamp, steps, screen, distance,
					batery, safe);
		} else
			addToVictim(m, message, timestamp, steps, screen, distance, batery,
					safe);

	}

	@Override
	public boolean onMarkerClick(final Marker marker) {
		String idNode = marker.getTitle();
		Victim focus = victims.get(idNode);
		clearTempMarkers();

		ArrayList<VictimNode> nodes = focus.getMarkers();
		int length;
		if(numberPointsPerVictim==-1 || nodes.size()<(numberPointsPerVictim+1) )
			length= nodes.size();
		else
			length = numberPointsPerVictim+1;
			
		LatLng[] route = new LatLng[length];
		int i = 0;
		for (VictimNode n : nodes) {
			String message = n.getMessage();
			float color = BitmapDescriptorFactory.HUE_CYAN;
			if (!message.equals(""))
				color = BitmapDescriptorFactory.HUE_ORANGE;
			MarkerOptions marker1 = new MarkerOptions()
					.position(
							new LatLng(n.getCoord().latitude,
									n.getCoord().longitude)).title(idNode)
					.snippet("" + n.getId())
					.icon(BitmapDescriptorFactory.defaultMarker(color));

			route[i] = n.getCoord();
			i++;
			tempMarkers.add(googleMap.addMarker(marker1));
			if(i>numberPointsPerVictim && numberPointsPerVictim!=-1)
				break;
		}
		marker.showInfoWindow();
		tempRoute = googleMap.addPolyline(new PolylineOptions().add(route)
				.width(3).color(Color.RED).zIndex(10));

		return false;
	}

	@Override
	public void onMapClick(LatLng arg0) {
		clearTempMarkers();

	}

	/**
	 * Removes all temporary markers and routes
	 */
	private void clearTempMarkers() {
		if (tempRoute != null)
			tempRoute.remove();
		Marker m;
		while (!tempMarkers.empty()) {
			m = tempMarkers.pop();
			m.remove();
		}
	}

	private void updateVictims() {

		File f = new File(filepath);
		Scanner scanner;
		ArrayList<String> res = new ArrayList<String>();
		try {
			scanner = new Scanner(f);
			if (scanner.hasNextLong()) {
				lastUpdate = scanner.nextLong();
				scanner.nextLine();
			}
			while (scanner.hasNextLine()) {
				res.add(scanner.nextLine());
			}
			scanner.close();

		} catch (FileNotFoundException e) {

		}
		markOffLineVictims(res);
		updateVictimsWebService();
	}

	private boolean markOffLineVictims(ArrayList<String> res) {
		boolean result = false;
		try {
			Log.i("RESCUE", "Number of entries offline " + res.size());
			for (int i = 0; i < res.size(); i++) {

				String[] nodes = res.get(i).split(";");
				for (int j = 0; j < nodes.length; j++) {
					JSONObject jsonObject = new JSONObject(nodes[j]);
					boolean safe = jsonObject.getBoolean("safe");
					String node = jsonObject.getString("nodeid");
					double lat = jsonObject.getDouble("latitude");
					double lon = jsonObject.getDouble("longitude");
					String msg = jsonObject.getString("msg");
					long time = jsonObject.getLong("timestamp");
					long added = jsonObject.getLong("added");
					int steps = jsonObject.getInt("steps");
					int screen = jsonObject.getInt("screen");

					// int distance = jsonObject.getInt("distance");
					int battery = jsonObject.getInt("battery");
					if (lastUpdate < added)
						lastUpdate = added;

					addVictimMarker(node, lat, lon, msg, time, steps, screen,
							0, battery, safe);
					result = true;
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		lastLocalUpdate = System.currentTimeMillis();
		return result;

	}

	private boolean markVictims(String victims) {
		boolean result = false;
		try {
			JSONArray jsonArray = new JSONArray(victims);
			Log.i("RESCUE", "Number of entries  online" + jsonArray.length());
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				int safe = jsonObject.getInt("safe");

				String node = jsonObject.getString("nodeid");
				double lat = jsonObject.getDouble("latitude");
				double lon = jsonObject.getDouble("longitude");
				String msg = jsonObject.getString("msg");
				long time = jsonObject.getLong("timestamp");
				long added = jsonObject.getLong("added");
				int steps = jsonObject.getInt("steps");
				int screen = jsonObject.getInt("screen");
				// int distance = jsonObject.getInt("distance");
				int battery = jsonObject.getInt("battery");
				if (lastUpdate < added)
					lastUpdate = added;

				addVictimMarker(node, lat, lon, msg, time, steps, screen, 0,
						battery, (safe == 1) ? true : false);
				result = true;

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	public void updateVictimsWebService() {
		Handler scheduleUpdates = new Handler();
		scheduleUpdates.postDelayed(new Runnable() {
			public void run() {

				StringBuilder builder = new StringBuilder();
				HttpClient client = new DefaultHttpClient();
				HttpGet httpGet;
				if (lastUpdate == -1) {
					httpGet = new HttpGet(
							"http://accessible-serv.lasige.di.fc.ul.pt/~lost/index.php/rest/victims");
				} else
					httpGet = new HttpGet(
							"http://accessible-serv.lasige.di.fc.ul.pt/~lost/index.php/rest/victims/mintimestamp/"
									+ (lastUpdate + 1));
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

				String victims = builder.toString();
				if (markVictims(victims))
					saveToFile();

				lastWebConnection = System.currentTimeMillis();
				updateVictimsWebService();
			}
		}, UPDATE_TIME);
	}

	private void saveToFile() {
		Iterator it = victims.entrySet().iterator();
		try {
			File file = new File(filepath);
			FileWriter fw;
			fw = new FileWriter(file, false);
			fw.write(lastUpdate + "\n");
			while (it.hasNext()) {
				Map.Entry pairs = (Map.Entry) it.next();
				fw.write(pairs.getValue() + "\n");

				// it.remove(); // avoids a ConcurrentModificationException
			}

			fw.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public String convertTime(long time) {
		Date date = new Date(time);
		Format format = new SimpleDateFormat("dd-MM-yyyy HH:mm");
		return format.format(date).toString();
	}

	public void addVictimMarker(String author, double lat, double lon,
			String message, long timestamp, int steps, int screen,
			int distance, int batery, boolean safe) {
		numberOfPoints++;
		if (victims.containsKey(author)) {

			Victim v = victims.get(author);
			if (v.checkUpdateMarker(timestamp)) {

				addMarker(lat, lon, message, false, author, timestamp, steps,
						screen, distance, batery, safe);

			} else
				v.addNode(new LatLng(lat, lon), timestamp, message, steps,
						screen, distance, batery, safe);
		} else {
			addMarker(lat, lon, message, true, author, timestamp, steps,
					screen, distance, batery, safe);

		}

	}

	public void addFirstVictim(Marker m, String message, long timestamp,
			int steps, int screen, int distance, int batery, boolean safe) {
		Victim vtim = new Victim(m, message, timestamp, steps, screen,
				distance, batery, safe);
		googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(m.getPosition(),
				14));

		victims.put(m.getTitle(), vtim);

	}

	public void addToVictim(Marker m, String message, long timestamp,
			int steps, int screen, int distance, int batery, boolean safe) {
		Victim v = victims.get(m.getTitle());
		v.updateLastMarker(m, timestamp, steps, screen, distance, batery,
				message, safe);
	}

	public void close() {
		// Closes the source of the tiles (Database in our case)
		tilesProvider.close();
		// Clears the tiles held in the tilesProvider
		tilesProvider.clear();

	}

	/***********************************
	 * Stats Methods
	 * 
	 ************************************* */
	public int getVictimNumber() {
		return victims.size();

	}

	public String lastLocalUpdate() {
		Date date = new Date(lastLocalUpdate);
		SimpleDateFormat df2 = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
		String dateText = df2.format(date);
		return dateText;
	}

	public String lastWebConnection() {
		Date date = new Date(lastWebConnection);
		SimpleDateFormat df2 = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
		String dateText = df2.format(date);
		return dateText;
	}

	public int getPointNumber() {
		return numberOfPoints;
	}

	public String lastWebUpdate() {
		Date date = new Date(lastUpdate);
		SimpleDateFormat df2 = new SimpleDateFormat("dd/MM/yy HH:mm:ss ");
		String dateText = df2.format(date);
		return dateText;
	}

	/***********************************
	 * Settings
	 * 
	 ************************************* */
	public int getPointNumberPerVictim() {
		return numberPointsPerVictim;
	}

	public void setPointNumberPerVictim(int number) {
		Log.d(LT, "Number of points per victim: " + number);

		numberPointsPerVictim = number;
	}

	public boolean getShowSafe() {
		return showSafe;
	}

	public void setShowSafe(boolean safe) {
		Log.d(LT, "Show safe: " + safe);
		showSafe = safe;
	}
}
