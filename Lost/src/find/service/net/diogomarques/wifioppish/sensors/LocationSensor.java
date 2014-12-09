package find.service.net.diogomarques.wifioppish.sensors;

import java.lang.reflect.Method;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

/**
 * Updates geographical location positioning data. It uses
 * {@link android.content.SharedPreferences SharedPreferences} to store the
 * value temporally and allow shared access to other components.
 * <p>
 * The Location Provider uses the device's GPS to get the current location. Each
 * location has an associated confidence level
 * 
 * @author André Silva <asilva@lasige.di.fc.ul.pt>
 */
public class LocationSensor extends AbstractSensor {

	private static final String TAG = "LocationSensor";

	private static final int INITIAL_INTERVAL =  30 * 1000; // 30seg
	private static final int SUBSEQUENT_INTERVAL = 2* 60 * 1000; // 2 minutes
	private static final int DISTANCE = 5; // meters

	private int currentInterval;
	private boolean changedInterval;
	private String currentProvider;

	private static final int CONFIDENCE_HIGH = 10;
	private static final int CONFIDENCE_LOW = 5;

	private Context context;
	private LocationManager mLocManager; 
	private Handler handler;

	// location data
	private double latitude;
	private double longitude;
	private long time;

	private LocationListener locationListener = new LocationListener() {

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			Log.i(TAG, "Status change: #status=" + status + " for " + provider);
		}

		@Override
		public void onProviderEnabled(String provider) {
			Log.i(TAG, "Provider enabled: " + provider);
		}

		@Override
		public void onProviderDisabled(String provider) {
			Log.i(TAG, "Provider disabled: " + provider);
		}

		@Override
		public void onLocationChanged(Location location) {
			Log.i(TAG, "Location Changed. Updated by " + location.getProvider()
					+ " provider.");
			
			latitude = location.getLatitude();
			longitude = location.getLongitude();
			time = location.getTime();

			Log.i(TAG, "Latitude is " + latitude + ". Longitude is "
					+ longitude);

			if (currentInterval == INITIAL_INTERVAL && latitude!=0 ) { // first location found
				// set less frequent updates
				currentInterval = SUBSEQUENT_INTERVAL;
				changedInterval = true;
			}
		}
	};

	/**
	 * Creates a new LocationSensor to gather geographical location updates
	 * 
	 * @param c
	 *            Android context
	 */
	public LocationSensor(Context c) {
		super(c);
		context = c;
		mLocManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		handler = new Handler();
		currentInterval = INITIAL_INTERVAL;
	}

	@Override
	public void startSensor() {
		currentProvider = getBestProvider();
		Log.i(TAG, "Chosen provider: " + currentProvider);
		registerLocationListeners(locationListener);
		handler.postDelayed(mRunnable, currentInterval);
	}

	@Override
	public Object getCurrentValue() {

		int confidence = getConfidenceValue();
		return new double[] { latitude, longitude, confidence };
	}

	private int getConfidenceValue() {
		int confidence = CONFIDENCE_HIGH;

		if (latitude == 0 && longitude == 0) {
			confidence = 0;
		} else if (System.currentTimeMillis() - time >= SUBSEQUENT_INTERVAL / 2) {
			confidence = CONFIDENCE_LOW;
		}

		return confidence;
	}

	@Override
	public void stopSensor() {
		handler.removeCallbacks(mRunnable);
		unregisterLocationListener(locationListener);
	}

	/**
	 * Registers an event listener to get GPS/wifi updates
	 * 
	 * @param mLocListener
	 *            location listener to receive coordinate updates
	 */
	private void registerLocationListeners(LocationListener locListener) {

		unregisterLocationListener(locListener);
		mLocManager.requestLocationUpdates(currentProvider, currentInterval,
				DISTANCE, locListener);
	}

	private String getBestProvider() {
		Criteria myCriteria = new Criteria();
		myCriteria.setAccuracy(Criteria.ACCURACY_LOW);
		myCriteria.setPowerRequirement(Criteria.POWER_LOW);

		return mLocManager.getBestProvider(myCriteria, true);
	}

	/**
	 * Unregisters a previously registered location listener
	 * 
	 * @param mLocationListener
	 *            location listener to remove
	 */
	private void unregisterLocationListener(LocationListener locListener) {

		mLocManager.removeUpdates(locListener);
	}

	/**
	 * Checks if current provider is still enabled. If not, then tries to change
	 * provider
	 */
	private Runnable mRunnable = new Runnable() {

		@Override
		public void run() {
			Log.e(TAG, "run");

			if (betterConnectionAvailable()) {
				// change provider
				currentProvider = currentProvider
						.equals(LocationManager.GPS_PROVIDER) ? LocationManager.NETWORK_PROVIDER
						: LocationManager.GPS_PROVIDER;
				Log.i(TAG, "Chosen provider: " + currentProvider);
				registerLocationListeners(locationListener);
			} else if (changedInterval) {
				// register again for the changes to take effect
				registerLocationListeners(locationListener);
				changedInterval = false;
				Log.i(TAG, "changed interval");
			}
			handler.postDelayed(mRunnable, currentInterval);
		}

	};

	private boolean betterConnectionAvailable() {
		// change from gps to network provider even if ap is enabled
		if (currentProvider.equals(LocationManager.GPS_PROVIDER)
				&& (isWifiEnabled() || isAPEnabled())) {
			return true;
		} else if (currentProvider.equals(LocationManager.NETWORK_PROVIDER)
				&& !isWifiEnabled() && !isAPEnabled()) {
			return true;
		}
		return false;
	}

	private boolean isWifiEnabled() {
		// TODO: check if it really has Internet access

		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		return ni.isConnected();
	}

	private boolean isAPEnabled() {
		boolean apEnabled = false;

		WifiManager manager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);

		try {
			final Method method = manager.getClass().getDeclaredMethod(
					"isWifiApEnabled");
			method.setAccessible(true);
			apEnabled = (Boolean) method.invoke(manager);

		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, "Could not check if AP is enabled.");
		}

		return apEnabled;
	}
}
