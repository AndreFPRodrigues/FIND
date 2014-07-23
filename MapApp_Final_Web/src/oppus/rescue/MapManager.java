package oppus.rescue;

import java.io.BufferedReader;
import com.jjoe64.graphview.GraphView.GraphViewData;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

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

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.LegendAlign;
import com.jjoe64.graphview.GraphViewDataInterface;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.GraphViewStyle.GridStyle;
import com.jjoe64.graphview.LineGraphView;
import com.jjoe64.graphview.ValueDependentColor;
import com.mapapp.tileManagement.TilesProvider;
import android.view.ViewGroup.LayoutParams;

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

	private View infoVictim;
	private int currentIdNode;
	private ArrayList<Integer> messagesID;
	private Marker focusMarker;
	private Victim focus;

	/**
	 * STATISTICS
	 */
	private int totalNext;
	private int totalBack;
	private int totalMarkerClicks;
	private int totalMapClicks;
	private int totalMessageClicks;
	private int totalTimeStatsOpen;
	private long auxTime = -1;
	private int totalScreen;
	private int totalMicro;

	/**
	 * Graphs
	 */
	LinearLayout graphLayout;
	final int MICRO = 0;
	final int SCREEN = 1;
	final int DISTANCE = 2;
	GraphViewData[] micro;
	GraphViewData[] screen;
	GraphViewData[] distance;

	// demo
	public static boolean demo = true;
	public static DemoClusters dm;
	private View rootview;
	private int lastGraph = MICRO;

	/**
	 * Map and listeners initialisation Starts tile provider
	 * 
	 * @param map
	 * @param rootView
	 * @param view
	 */
	public MapManager(GoogleMap map, LayoutInflater inflater, View infoVictim,
			View rootView) {

		this.infoVictim = infoVictim;
		this.context = inflater;
		this.rootview = rootView;

		messagesID = new ArrayList<Integer>();
		googleMap = map;
		googleMap.setMapType(GoogleMap.MAP_TYPE_NONE);
		googleMap.setMyLocationEnabled(true);
		googleMap.setOnMarkerClickListener(this);
		googleMap.setOnMapClickListener(this);
		googleMap.getUiSettings().setMyLocationButtonEnabled(false);
		victims = new HashMap<String, Victim>();
		tempMarkers = new Stack<Marker>();
		filepath = Environment.getExternalStorageDirectory().toString()
				+ "/oppus/victims";

		startTileProvider();
		setInfoWindow();

	}

	/***********************************
	 * Tile Provider methods
	 * 
	 ************************************* */
	private void startTileProvider() {
		// Creating our database tilesProvider to pass it to our MapView
		String path = Environment.getExternalStorageDirectory()
				+ "/mapapp/world.sqlitedb";
		tilesProvider = new TilesProvider(path);

		// Create new TileOverlayOptions instance.
		TileOverlayOptions opts = new TileOverlayOptions();
		opts.tileProvider(tilesProvider);
		// Add the tile overlay to the map.
		TileOverlay overlay = googleMap.addTileOverlay(opts);

		/*
		 * // Update and draw the map view mapView.refresh();
		 */

		// tilesProvider.downloadTilesInBound(39.415817, -9.163834, 39.395955,
		// -9.113635, 16, 17);

	}

	private void close() {
		// Closes the source of the tiles (Database in our case)
		tilesProvider.close();
		// Clears the tiles held in the tilesProvider
		tilesProvider.clear();

	}

	private boolean markOffLineVictims() {

		boolean result = false;
		lastLocalUpdate = System.currentTimeMillis();
		return result;

	}

	private boolean markVictims(String victims) {
		boolean result = false;
		try {
			JSONArray jsonArray = new JSONArray(victims);
			Log.i("RESCUE", "Number of entries  online" + jsonArray.length());
			ContentValues[] nodes = new ContentValues[jsonArray.length()];
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				ContentValues cv = new ContentValues();
				int safe = jsonObject.getInt("safe");
				String node = jsonObject.getString("nodeid");
				double lat = jsonObject.getDouble("latitude");
				double lon = jsonObject.getDouble("longitude");
				String msg = jsonObject.getString("msg");
				long time = jsonObject.getLong("timestamp");
				long added = jsonObject.getLong("added");
				int steps = jsonObject.getInt("steps");
				int llconf = jsonObject.getInt("llconf");
				int screen = jsonObject.getInt("screen");
				// int distance = jsonObject.getInt("distance");
				int battery = jsonObject.getInt("battery");
				if (lastUpdate < added)
					lastUpdate = added;

				cv.put(SQLittleObserver.COL_ADDED, added);
				cv.put(SQLittleObserver.COL_ID, time + node);
				cv.put(SQLittleObserver.COL_NODE, node);
				cv.put(SQLittleObserver.COL_TIME, time);
				cv.put(SQLittleObserver.COL_MSG, msg);
				cv.put(SQLittleObserver.COL_LAT, lat);
				cv.put(SQLittleObserver.COL_LON, lon);
				cv.put(SQLittleObserver.COL_CONF, llconf);
				cv.put(SQLittleObserver.COL_BATTERY, battery);
				cv.put(SQLittleObserver.COL_STEPS, steps);
				cv.put(SQLittleObserver.COL_SCREEN, screen);
				cv.put(SQLittleObserver.COL_DISTANCE, -1);
				cv.put(SQLittleObserver.COL_SAFE, (safe == 1) ? true : false);
				nodes[i] = cv;
				// addVictimMarker(node, lat, lon, msg, time, steps, screen, 0,
				// battery, (safe == 1) ? true : false);
				result = true;

			}
			if (result)
				SQLittleObserver.insertRow(nodes);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	void updateVictimsWebService() {
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
		markVictims(victims);

		lastWebConnection = System.currentTimeMillis();

	}

	/**
	 * It creates a new vitim if it doesnt exist, if it does it adds one point
	 * to the victim, if it is the victim lastest point it updates its display
	 * marker on the map
	 * 
	 * @param author
	 *            - victim identification
	 * @param lat
	 *            - latitude
	 * @param lon
	 *            - longitude
	 * @param message
	 *            - message
	 * @param timestamp
	 *            - timestamp of the point
	 * @param steps
	 * @param screen
	 * @param distance
	 * @param batery
	 * @param safe
	 */
	void addVictimMarker(String author, double lat, double lon, String message,
			long timestamp, int steps, int screen, int distance, int batery,
			boolean safe, boolean updated) {
		numberOfPoints++;
		if (victims.containsKey(author)) {

			Victim v = victims.get(author);
			if (v.checkUpdateMarker(timestamp)) {

				addMarker(lat, lon, message, false, author, timestamp, steps,
						screen, distance, batery, safe, updated);

			} else
				v.addNode(new LatLng(lat, lon), timestamp, message, steps,
						screen, distance, batery, safe);
		} else {
			addMarker(lat, lon, message, true, author, timestamp, steps,
					screen, distance, batery, safe, updated);

		}

	}

	/**
	 * Creates a new victim
	 */
	private void addFirstVictim(Marker m, String message, long timestamp,
			int steps, int screen, int distance, int batery, boolean safe) {
		Victim vtim = new Victim(m, message, timestamp, steps, screen,
				distance, batery, safe);

		googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(m.getPosition(),
				17));

		victims.put(m.getTitle(), vtim);

	}

	/**
	 * Adds the point to an existing victim
	 */
	private void addToVictim(Marker m, String message, long timestamp,
			int steps, int screen, int distance, int batery, boolean safe) {
		Victim v = victims.get(m.getTitle());
		v.updateLastMarker(m, timestamp, steps, screen, distance, batery,
				message, safe);
	}

	/***********************************
	 * Info window markers and overlays Methods
	 * 
	 ************************************* */
	private void setInfoWindow() {
		// Setting a custom info window adapter for the google map
		// googleMap.setInfoWindowAdapter(new InfoWindowAdapter() {
		//
		// // Use default InfoWindow frame
		// @Override
		// public View getInfoWindow(Marker marker) {
		// VictimNode victim = victims.get(marker.getTitle()).getNode(
		// marker.getSnippet());
		// View v;
		// if (!victim.getMessage().equals("")) {
		// v = context.inflate(R.layout.info_window_message, null);
		// // Getting reference to the TextView to set message
		// TextView message = (TextView) v.findViewById(R.id.message);
		// message.setText(victim.getMessage());
		// } else {
		//
		// // Getting view from the layout file info_window_layout
		// v = context.inflate(R.layout.info_window_layout, null);
		// }
		//
		// // Getting reference to the TextView to set name
		// TextView name = (TextView) v.findViewById(R.id.name);
		//
		// // Getting reference to the TextView to set lastTime
		// TextView last = (TextView) v.findViewById(R.id.lastTime);
		// // Getting reference to the TextView to set battery
		// TextView battery = (TextView) v.findViewById(R.id.battery);
		// // Getting reference to the TextView to set from
		// TextView from = (TextView) v.findViewById(R.id.from);
		// // Getting reference to the TextView to set steps
		// TextView steps = (TextView) v.findViewById(R.id.steps);
		// // Getting reference to the TextView to set distance
		// TextView distance = (TextView) v.findViewById(R.id.distance);
		// // Getting reference to the TextView to set screen
		// TextView screen = (TextView) v.findViewById(R.id.screen);
		//
		// name.setText(marker.getTitle());
		// last.setText(convertTime(victim.getTimestamp()));
		// last.setText(convertTime(victim.getTimestamp()));
		// battery.setText(victim.getBatery() + " %");
		// from.setText(convertTime(victim.getTimestamp()));
		// steps.setText(victim.getSteps() + "");
		// distance.setText(victim.getDistance() + " m");
		// screen.setText(victim.getScreen() + " times");
		//
		// // Returning the view containing InfoWindow contents
		// return v;
		//
		// }
		//
		// // Defines the contents of the InfoWindow
		// @Override
		// public View getInfoContents(Marker marker) {
		// return null;
		// }
		// });
	}

	private void addMarker(double lat, double lon, String message,
			boolean firstPoint, String author, long timestamp, int steps,
			int screen, int distance, int batery, boolean safe, boolean updated) {
		MarkerOptions marker;

		if (updated) {
			BitmapDescriptor bd = BitmapDescriptorFactory
					.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
			marker = new MarkerOptions().position(new LatLng(lat, lon))
					.title(author).snippet("0").icon(bd);
		} else
			marker = new MarkerOptions().position(new LatLng(lat, lon))
					.title(author).snippet("0");

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

	private String[] messagesNodes(ArrayList<String> messages) {
		String[] result = new String[messages.size()];
		String[] aux = new String[2];
		messagesID = new ArrayList<Integer>();
		for (int i = 0; i < messages.size(); i++) {

			aux = messages.get(i).split(";;;");
			result[i] = aux[0];
			messagesID.add(Integer.parseInt(aux[1]));
		}
		return result;
	}

	private void viewNode(int index) {
		onMarkerClick(tempMarkers.get(index));

	}

	public void infoVictim(boolean state, Victim v) {
		if (state) {
			infoVictim.setVisibility(View.VISIBLE);
			TextView t = (TextView) infoVictim.findViewById(R.id.victim);
			t.setText(v.getId());
			TextView t1 = (TextView) infoVictim.findViewById(R.id.totalPoints);
			t1.setText(v.getNumberOfPoints() + "");
			TextView t2 = (TextView) infoVictim.findViewById(R.id.lastUpdate);
			t2.setText(convertTime(v.lastTimeStamp()));
			TextView t3 = (TextView) infoVictim.findViewById(R.id.battery);
			t3.setText(v.getBatteryValue() + "%");
			ListView lv = (ListView) infoVictim.findViewById(R.id.listMessages);
			String[] messages = messagesNodes(v.getMessageNodes());
			TableRow tr = (TableRow) infoVictim.findViewById(R.id.messageRow);
			TableRow tr1 = (TableRow) infoVictim.findViewById(R.id.messageRow1);
			TableRow trGraph = (TableRow) infoVictim
					.findViewById(R.id.graphRow);

			graphLayout = (LinearLayout) infoVictim.findViewById(R.id.graph);
			addGraph(v.getMicroValues(), v.getScreenValues(),
					v.getDistanceValues());
			trGraph.setVisibility(View.VISIBLE);

			if (messages.length > 0) {

				tr.setVisibility(View.VISIBLE);
				tr1.setVisibility(View.VISIBLE);

			} else {
				tr.setVisibility(View.INVISIBLE);
				tr1.setVisibility(View.INVISIBLE);

			}

			// Set listview with messages
			int key = 0;
			final String[] matrix = { "_id", "name", "value" };
			final String[] columns = { "name", "value" };
			final int[] layouts = { android.R.id.text1, android.R.id.text2 };

			MatrixCursor cursor = new MatrixCursor(matrix);

			for (String msg : messages) {
				cursor.addRow(new Object[] { key,
						convertTime(focus.getNode(key + "").getTimestamp()),
						msg });
				key++;
			}
			SimpleCursorAdapter data = new SimpleCursorAdapter(
					context.getContext(), R.layout.list_message, cursor,
					columns, layouts);

			lv.setAdapter(data);
			lv.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					totalMessageClicks++;
					viewNode(messagesID.get(arg2));
				}

			});

		} else {
			infoVictim.setVisibility(View.INVISIBLE);

		}
	}

	@Override
	public boolean onMarkerClick(final Marker marker) {
		googleMap.setPadding(0, 0, 1200, 0);

		// marker.setIcon(BitmapDescriptorFactory
		// .defaultMarker(BitmapDescriptorFactory.HUE_RED));
		focusMarker = marker;
		totalMarkerClicks++;
		if (auxTime == -1)
			auxTime = System.currentTimeMillis();
		String idNode = marker.getTitle();
		focus = victims.get(idNode);
		int index = Integer.parseInt(marker.getSnippet());
		infoVictim(true, focus);

		clearTempMarkers();

		ArrayList<VictimNode> nodes = focus.getMarkers();
		int length;
		if (numberPointsPerVictim == -1
				|| nodes.size() < (numberPointsPerVictim + 1))
			length = nodes.size();
		else
			length = numberPointsPerVictim + 1;

		LatLng[] route = new LatLng[length];
		int i = 0;
		BitmapDescriptor bd;

		for (VictimNode n : nodes) {
			String message = n.getMessage();
			bd = BitmapDescriptorFactory
					.defaultMarker(BitmapDescriptorFactory.HUE_CYAN);
			if (!message.equals("")) {
				bd = BitmapDescriptorFactory.fromResource(R.drawable.path_msg);
			}
			if (i == (index)) {
				bd = BitmapDescriptorFactory
						.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA);
			}
			MarkerOptions marker1 = new MarkerOptions()
					.position(
							new LatLng(n.getCoord().latitude,
									n.getCoord().longitude)).title(idNode)
					.snippet("" + n.getId()).icon(bd);

			route[i] = n.getCoord();
			i++;

			tempMarkers.add(googleMap.addMarker(marker1));
			if (i > numberPointsPerVictim && numberPointsPerVictim != -1)
				break;
		}

		// marker.hideInfoWindow();

		tempRoute = googleMap.addPolyline(new PolylineOptions().add(route)
				.width(3).color(Color.RED).zIndex(10));

		return true;
	}

	@Override
	public void onMapClick(LatLng arg0) {
		totalMapClicks++;
		if (auxTime != -1) {
			Log.d("RESCUE", "added to total time aux:" + auxTime + " added:"
					+ ((System.currentTimeMillis() - auxTime) / 1000));
			totalTimeStatsOpen += (((System.currentTimeMillis() - auxTime) / 1000));
		}
		auxTime = -1;
		clearTempMarkers();
		infoVictim(false, null);
		Log.d(LT, "lat:" + arg0.latitude + " lon:" + arg0.longitude);
		googleMap.setPadding(0, 0, 0, 0);

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
			m = null;

		}
		if (focusMarker.equals(focus.getMarker()))
			focusMarker.setIcon(BitmapDescriptorFactory
					.defaultMarker(BitmapDescriptorFactory.HUE_RED));

	}

	public String convertTime(long time) {
		Date date = new Date(time);
		Format format = new SimpleDateFormat("HH:mm 'of' dd-MM ");
		return format.format(date).toString();
	}

	public void next() {
		totalNext++;
		totalMarkerClicks--;
		String idNode = focusMarker.getSnippet();
		int index;
		if (idNode == null) {
			index = tempMarkers.size() - 1;
		} else
			index = (Integer.parseInt(focusMarker.getSnippet()) - 1);
		if (index < 0)
			index = tempMarkers.size() - 1;
		viewNode(index);
	}

	public void back() {
		totalBack++;
		totalMarkerClicks--;

		String idNode = focusMarker.getSnippet();
		int index;
		if (idNode == null) {
			index = 1;
		} else {
			if (tempMarkers.size() < 1)
				return;
			index = (Integer.parseInt(idNode) + 1) % tempMarkers.size();
		}
		viewNode(index);
	}

	/***********************************
	 * Graph Methods
	 * 
	 ************************************* */

	private void addGraph(ArrayList<GraphValue> microMovements,
			ArrayList<GraphValue> screenActivations,
			ArrayList<GraphValue> lastDistance) {
		graphLayout.removeAllViews();

		micro = GraphValue.getGraphView(microMovements);
		screen = GraphValue.getGraphView(screenActivations);
		distance = GraphValue.getGraphView(lastDistance);

		GraphView graphView = new LineGraphView(graphLayout.getContext(),
				"Please select a graph type");

		graphView.setHorizontalLabels(GraphValue.getTimeLabel(micro));

		graphView.getGraphViewStyle().setTextSize(15);
		graphView.getGraphViewStyle().setVerticalLabelsWidth(30);

		graphView.getGraphViewStyle().setGridStyle(GridStyle.VERTICAL);
		graphView.getGraphViewStyle().setGridColor(Color.WHITE);
		graphLayout.setPadding(5, 5, 5, 5);
		// graphView.setShowLegend(true);
		graphView.setManualYAxisBounds(20, 0);

		// graphView.setLegendAlign(LegendAlign.BOTTOM);
		// graphView.setLegendWidth(150);
		// graphView.addSeries(microSeries);

		graphLayout.addView(graphView);
	}

	private void changeGraph(int mode) {
		graphLayout.removeAllViews();

		GraphView graphView = new LineGraphView(graphLayout.getContext(),
				"Micro Movements");
		graphView.setManualYAxisBounds(20, 0);

		GraphViewSeries serie = new GraphViewSeries("Micro", null, micro);
		switch (mode) {
		case SCREEN:
			serie = new GraphViewSeries("Screen", null, screen);
			serie.getStyle().color = Color.GREEN;
			graphView = new LineGraphView(graphLayout.getContext(),
					"Screen Activations");
			graphView.setManualYAxisBounds(10, 0);

			break;
		case DISTANCE:
			serie = new GraphViewSeries("Distance", null, distance);
			serie.getStyle().color = Color.DKGRAY;
			graphView = new LineGraphView(graphLayout.getContext(), "Distance");
			break;

		}
		graphView.setHorizontalLabels(GraphValue.getTimeLabel(micro));

		graphView.getGraphViewStyle().setTextSize(15);
		graphView.getGraphViewStyle().setVerticalLabelsWidth(30);

		graphView.getGraphViewStyle().setGridStyle(GridStyle.VERTICAL);
		graphView.getGraphViewStyle().setGridColor(Color.WHITE);
		graphLayout.setPadding(5, 5, 5, 5);
		// graphView.setShowLegend(true);

		// graphView.setLegendAlign(LegendAlign.BOTTOM);
		// graphView.setLegendWidth(150);
		// graphView.setHorizontalLabels(GraphValue.getTimeLabel(serie));
		graphView.addSeries(serie);

		graphLayout.addView(graphView);
	}

	public void screenGraph() {
		totalScreen++;
		changeGraph(SCREEN);
	}

	public void distanceGraph() {
		changeGraph(DISTANCE);

	}

	public void microGraph() {
		totalMicro++;
		changeGraph(MICRO);

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

	/**
	 * DEMO LOCATION DETECT
	 */
	final int VM = 0;
	final int VV1 = 1;
	final int VV2 = 2;
	final int VV3 = 3;
	int victimsSaved = 0;
	boolean[] isSaving = new boolean[5];
	boolean[] savedSpots = new boolean[5];
	private long timeDemo;

	public void checkSaveVictims(Location location) {

		int indexLocation = DemoClusters.savingPosition(location);
		if (indexLocation == 4) {
			if (DemoClusters.mode == DemoClusters.C2 && !DemoClusters.updated) {
				DemoClusters.updateMessageVictimSpots(this);
				DemoClusters.updated = true;
			}
			return;
		}
		if (indexLocation != -1)
			if (!isSaving[indexLocation] && !savedSpots[indexLocation]) {

				isSaving[indexLocation] = true;
				rootview.findViewById(R.id.saveButton).setVisibility(
						View.VISIBLE);
			}

		if (indexLocation == -1
				&& rootview.findViewById(R.id.saveButton).getVisibility() == View.VISIBLE) {
			rootview.findViewById(R.id.saveButton)
					.setVisibility(View.INVISIBLE);
			for (int i = 0; i < isSaving.length; i++)
				isSaving[i] = false;

		}

	}

	public void saved() {
		for (int i = 0; i < isSaving.length; i++) {
			if (isSaving[i]) {
				victimsSaved += DemoClusters.getSavedVictims(i);
				isSaving[i] = false;
				savedSpots[i] = true;
				TextView t = (TextView) rootview
						.findViewById(R.id.victimsSaved);
				t.setText(victimsSaved + "");
				Toast.makeText(
						rootview.getContext(),
						"SAVED " + DemoClusters.getSavedVictims(i) + " victims",
						Toast.LENGTH_LONG).show();

				if (victimsSaved >= 15) {
					timeDemo = System.currentTimeMillis() - timeDemo;
					Toast.makeText(
							rootview.getContext(),
							"CONGRATILATIONS YOU SAVED " + victimsSaved
									+ " IN " + getDemoTime(), Toast.LENGTH_LONG)
							.show();
					Toast.makeText(
							rootview.getContext(),
							"CONGRATILATIONS YOU SAVED " + victimsSaved
									+ " VICTIMS IN " + getDemoTime(),
							Toast.LENGTH_LONG).show();
					((TextView) rootview.findViewById(R.id.timertitle))
							.setText("     Saved in:");
					((TextView) rootview.findViewById(R.id.timer)).setText(" "
							+ getDemoTime());

					if (auxTime != -1) {
						totalTimeStatsOpen += (((System.currentTimeMillis() - auxTime) / 1000));
					}
					DemoClusters.writeToLog(totalMarkerClicks, totalMapClicks,
							getDemoTime(), victimsSaved, totalMicro,
							totalScreen, totalTimeStatsOpen);
				}

				if (i == 1) {
					DemoClusters.updadeVictimSpots(this);
				}
				return;
			}
		}

	}

	private String getDemoTime() {
		int time = (int) timeDemo / 1000;
		int minutes = time / 60;
		int seconds = time % 60;
		return minutes + " minutes and " + seconds + "seconds";
	}

	public void startDemo() {
		Toast.makeText(rootview.getContext(), "Starting game",
				Toast.LENGTH_LONG).show();
		((TextView) rootview.findViewById(R.id.timertitle))
				.setText("     Saved in:");
		timeDemo = System.currentTimeMillis();

	}

}
