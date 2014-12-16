package oppus.rescue;

import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.storage.StorageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
//import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.MapFragment;

/**
 * Map fragment
 * 
 * @author andre
 * 
 */
public class MapFragmentControl extends Fragment implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {
	private final static String LT = "RESCUE";

	MapManager mm;
	SQLittleObserver sqlObserver;
	boolean haveInternet = false;
	View rootView;
	BroadcastReceiver receiver;
	LocationClient mLocationClient;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		// Ensure Find Service is installed
		Intent scanIntent = getActivity().getPackageManager()
				.getLaunchIntentForPackage("find.service");
		if (scanIntent == null) {
			Toast.makeText(getActivity().getApplicationContext(), "This application requires FIND Service installed", Toast.LENGTH_LONG).show();
			Intent marketIntent = new Intent(Intent.ACTION_VIEW,
					Uri.parse("market://details?id=find.service"));

			try {
				startActivity(marketIntent);
			} catch (ActivityNotFoundException e2) {

			}
		} else {
			// TODO get location from the FIND SERVICE
			// gathered location independently of the service to get points only
			// in
			// our vicinity and in the DEMO to trigger updates on locations
			mLocationClient = new LocationClient(inflater.getContext(), this,
					this);
			// Start with updates turned off
			// Use high accuracy
			mLocationClient.connect();

			if (rootView == null)
				rootView = inflater.inflate(R.layout.main, container, false);

			// TODO remove the thread policy, create a new thread to handle it
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
					.permitAll().build();
			StrictMode.setThreadPolicy(policy);

			if (mm == null) {
				// init map manager
				mm = new MapManager(((MapFragment) getFragmentManager()
						.findFragmentById(R.id.map)).getMap(), inflater,
						rootView.findViewById(R.id.infoVictim), rootView);

				// Retrives all poitns stored in the Find service
				sqlObserver = new SQLittleObserver(getActivity(), mm);
				sqlObserver.retriveAllNodes();

				// listenings for te internet connect state to request an map
				// update
				// from the webservice
				IntentFilter filter = new IntentFilter();
				filter.addAction("stateChange");
				receiver = new BroadcastReceiver() {
					@Override
					public void onReceive(Context context, Intent intent) {
						String state = intent.getStringExtra("State");
						Log.d("gcm", "state:" + state);
						if (state.equals("InternetConn"))
							updateMap();
					}
				};
				getActivity().registerReceiver(receiver, filter);
			}
		}
		return rootView;
	}

	public MapManager getMapManager() {
		return mm;
	}

	// TODO clean all this calls from the mainactivity->mapFragment->mapManager
	private void updateMap() {
		mm.updateVictimsWebService();
	}

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

	// Testing: Adds a dummy victim to the map
	public void addTestVictim() {
		mm.addVictimMarker("Testing", 38.755040, -9.153594, "HELP ME!",
				System.currentTimeMillis(), 20, 2, 0, 55, false, true);
		mm.addVictimMarker("Testing", 38.751860, -9.155697, "runn",
				System.currentTimeMillis() + 100000, 100, 5, 0, 55, false, true);
		mm.addVictimMarker("Testing", 38.751860, -9.155897, "",
				System.currentTimeMillis() + 200000, 200, 5, 0, 55, false, true);

	}

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
		mLocationRequest.setInterval(60 * 1000 * 15);
		// Set the fastest update interval to 10 second
		mLocationRequest.setFastestInterval(60 * 1000 * 10);
		mLocationClient.requestLocationUpdates(mLocationRequest, this);
	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub

	}

	/*************************
	 * DEMO
	 ************************/
	//
	// public void saved() {
	// mm.saved();
	//
	// }
	//
	// public void startDemo() {
	// mm.startDemo();
	//
	// }}

}