/*
 * MapApp : Simple offline map application, made by Hisham Ghosheh for tutorial purposes only
 * Tutorial on my blog
 * http://ghoshehsoft.wordpress.com/2012/03/09/building-a-map-app-for-android/
 * 
 * Class tutorial:
 * http://ghoshehsoft.wordpress.com/2012/04/06/mapapp5-mapview-and-activity/
 */

package oppus.rescue;


import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.location.Location;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
//import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.MapFragment;

public class MapFragmentControl extends Fragment implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {
	private final static String LT = "RESCUE";

	MapManager mm;
	SQLittleObserver sqlObserver;
	boolean haveInternet = false;
	View rootView;
	BroadcastReceiver receiver;

	// DEMO
	LocationClient mLocationClient;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		// demo
		if (true) {
			mLocationClient = new LocationClient(inflater.getContext(), this,
					this);
			// Start with updates turned off
			// Use high accuracy 

			mLocationClient.connect();
		}
		
		if (rootView == null)
			rootView = inflater.inflate(R.layout.main, container, false); 
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
				.permitAll().build();
		StrictMode.setThreadPolicy(policy);

		if (mm == null) {
			mm = new MapManager(((MapFragment) getFragmentManager()
					.findFragmentById(R.id.map)).getMap(), inflater,
					rootView.findViewById(R.id.infoVictim), rootView);

			sqlObserver = new SQLittleObserver(getActivity(), mm);
			sqlObserver.retriveAllNodes();

			IntentFilter filter = new IntentFilter();
			filter.addAction("stateChange");
			receiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					String state = intent.getStringExtra("State");
					Log.d("gcm", "state:" +state);

					if (state.equals("InternetConn"))
						updateMap();

				}
			};
			getActivity().registerReceiver(receiver, filter);
		}
		// Getting view from the layout file info_window_layout

		// googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new
		// LatLng(39.405159, -9.133357), 16));

		return rootView;
		//
	}

	public MapManager getMapManager() {
		return mm;
	}

	private void updateMap() {
		mm.updateVictimsWebService();
	}

	/*public void next() {
		mm.next();
	}

	public void back() {
		mm.back();

	}*/

	public void screenGraph() {
		mm.screenGraph();
	}

	public void distanceGraph() {
		mm.distanceGraph();

	}

	public void microGraph() {
		mm.microGraph();

	}

	public void hideInfo() {
		mm.hideInfo();
		
	}

	//Testing
	public void addTestVictim() {
		mm.addVictimMarker("Testing", 38.755040, -9.153594, "HELP ME!",  System.currentTimeMillis(),20, 2, 0, 55, false, true);
		mm.addVictimMarker("Testing", 38.751860, -9.155697, "runn",  System.currentTimeMillis()+100000, 100, 5, 0, 55, false, true);
		mm.addVictimMarker("Testing", 38.751860, -9.155897, "",  System.currentTimeMillis()+200000,200 , 5, 0, 55, false, true);

	}

	/*************************
	 * DEMO
	 ************************/
	@Override
	public void onLocationChanged(Location location) {		
		mm.setLocation(location);
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConnected(Bundle arg0) {
		LocationRequest mLocationRequest = LocationRequest.create();

		mLocationRequest.setPriority(LocationRequest.PRIORITY_NO_POWER);
		// Set the update interval to 15 min
		mLocationRequest.setInterval(60*1000*15);
		// Set the fastest update interval to 10 second
		mLocationRequest.setFastestInterval(60*1000*10);
		mLocationClient.requestLocationUpdates(mLocationRequest, this);
	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub

	}
//
//	public void saved() {
//		mm.saved();
//		
//	}
//
//	public void startDemo() {
//		mm.startDemo();
//		
//	}}

}