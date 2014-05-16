package oppus.rescue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
import android.util.Log;
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

	private MapAppActivity context;

	/**
	 * Map and listeners initialisation Starts tile provider
	 * 
	 * @param map
	 */
	public MapManager(GoogleMap map, MapAppActivity c) {
		this.context = c;
		googleMap = map;
		googleMap.setMapType(GoogleMap.MAP_TYPE_NONE);
		googleMap.setMyLocationEnabled(true);
		googleMap.setOnMarkerClickListener(this);
		googleMap.setOnMapClickListener(this);
		victims = new HashMap<String, Victim>();
		tempMarkers = new Stack<Marker>();
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
				VictimNode victim = victims.get(marker.getTitle())
						.getNode(marker.getSnippet());
				View v;
				if(!victim.getMessage().equals("")){
					v = context.getLayoutInflater().inflate(
							R.layout.info_window_message, null);
					// Getting reference to the TextView to set message
					TextView message = (TextView) v.findViewById(R.id.message);
					message.setText(victim.getMessage());
					message.setTextColor(Color.BLACK);
				} else {

					// Getting view from the layout file info_window_layout
					v = context.getLayoutInflater().inflate(
							R.layout.info_window_layout, null);
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

		updateVictimsWebService();
		/*
		 * // Update and draw the map view mapView.refresh();
		 */

		// tilesProvider.downloadTilesInBound(39.415817, -9.163834, 39.395955,
		// -9.113635, 16, 17);

	}

	private void addMarker(double lat, double lon, String message,
			boolean firstPoint, String author, long timestamp, int steps,
			int screen, int distance, int batery) {
		MarkerOptions marker;
		marker = new MarkerOptions().position(new LatLng(lat, lon)).title(
				author);

		Marker m = googleMap.addMarker(marker);
		if (firstPoint)
			addFirstVictim(m, message, timestamp, steps, screen, distance,
					batery);
		else
			addToVictim(m, message, timestamp, steps, screen, distance, batery);

	}

	@Override
	public boolean onMarkerClick(final Marker marker) {
		String idNode = marker.getTitle();
		Victim focus = victims.get(idNode);
		clearTempMarkers();
		ArrayList<VictimNode> nodes = focus.getMarkers();
		LatLng[] route = new LatLng[nodes.size()];
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

	public void updateVictimsWebService() {

		StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient();
		HttpGet httpGet;
		if (latestTimestamp == -1)
			httpGet = new HttpGet(
					"http://accessible-serv.lasige.di.fc.ul.pt/~lost/index.php/rest/victims");
		else
			httpGet = new HttpGet(
					"http://accessible-serv.lasige.di.fc.ul.pt/~lost/index.php/rest/victims?timestamp="
							+ latestTimestamp);
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
				// Log.e(ParseJSON.class.toString(), "Failed to download file");
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String victims = builder.toString();
		try {
			JSONArray jsonArray = new JSONArray(victims);
			Log.i("RESCUE", "Number of entries " + jsonArray.length());
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				String node = jsonObject.getString("nodeid");
				double lat = jsonObject.getDouble("latitude");
				double lon = jsonObject.getDouble("longitude");
				String msg = jsonObject.getString("msg");
				long time = jsonObject.getLong("timestamp");
				int steps = jsonObject.getInt("steps");
				int screen = jsonObject.getInt("screen");
				// int distance = jsonObject.getInt("distance");
				int battery = jsonObject.getInt("battery");
				if (latestTimestamp < time)
					latestTimestamp = time;
				addVictimMarker(node, lat, lon, msg, time, steps, screen, 0,
						battery);

			}
		} catch (Exception e) {
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
			int distance, int batery) {
		Log.d("RESCUE", "node:" + author + " lat:" + lat + " lon:" + lon
				+ " msg:" + message);
		if (victims.containsKey(author)) {

			Victim v = victims.get(author);
			if (v.checkUpdateMarker(timestamp)) {

				addMarker(lat, lon, message, false, author, timestamp, steps,
						screen, distance, batery);

			} else
				v.addNode(new LatLng(lat, lon), timestamp, message, steps,
						screen, distance, batery);
		} else {
			addMarker(lat, lon, message, true, author, timestamp, steps,
					screen, distance, batery);

		}

	}

	public void addFirstVictim(Marker m, String message, long timestamp,
			int steps, int screen, int distance, int batery) {
		Victim vtim = new Victim(m, message, timestamp, steps, screen,
				distance, batery);
		googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(m.getPosition(),
				14));
		victims.put(m.getTitle(), vtim);

	}

	public void addToVictim(Marker m, String message, long timestamp,
			int steps, int screen, int distance, int batery) {
		Victim v = victims.get(m.getTitle());
		v.updateLastMarker(m, timestamp, steps, screen, distance, batery,
				message);
	}

	public void close() {
		// Closes the source of the tiles (Database in our case)
		tilesProvider.close();
		// Clears the tiles held in the tilesProvider
		tilesProvider.clear();

	}

}
