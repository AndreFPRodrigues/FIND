package find.service.net.diogomarques.wifioppish.sensors;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
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

	// location providers will only send updates when the location has changed
	// by at least DISTANCE meters, AND at least X_UPDATE_INTERVAL milliseconds
	// have passed
	private static final int GPS_UPDATE_INTERVAL = 3 * 60 * 1000; // 3 minutes
	private static final int WIFI_UPDATE_INTERVAL = 2 * 60 * 1000; // 2 minutes
	private static final int DISTANCE = 5; // meters

	private Context context;
	private LocationManager mLocManager;

	// data
	private Location currentLocation;
	private int accuracy;

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
			
			updateLocation(location);
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
		currentLocation = new Location("");
		currentLocation.setAccuracy(10000000);
		context = c;
	}

	@Override
	public void startSensor() {
		registerLocationListeners(locationListener);
	}

	@Override
	public Object getCurrentValue() {
		double lat = currentLocation.getLatitude();
		double lon = currentLocation.getLongitude();

		return new double[] { lat, lon, 10 };
	}

	@Override
	public void stopSensor() {
		unregisterLocationListener(locationListener);
	}

	/**
	 * Registers an event listener to get GPS/wifi updates
	 * 
	 * @param mLocListener
	 *            location listener to receive coordinate updates
	 */
	private void registerLocationListeners(LocationListener locListener) {
		context.hashCode();

		if (mLocManager == null)
			mLocManager = (LocationManager) context
					.getSystemService(Context.LOCATION_SERVICE);

		unregisterLocationListener(locListener);

		mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				GPS_UPDATE_INTERVAL, DISTANCE, locListener);
		mLocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
				WIFI_UPDATE_INTERVAL, DISTANCE, locListener);
	}

	/**
	 * Unregisters a previously registered location listener
	 * 
	 * @param mLocationListener
	 *            location listener to remove
	 */
	private void unregisterLocationListener(
			LocationListener locListener) {

		if (mLocManager != null)
			mLocManager.removeUpdates(locListener);
	}

	public void updateLocation(Location newLocation) {
		if (isBetterLocation(newLocation, currentLocation)) {
			currentLocation = newLocation;
			Log.d(TAG, "Current best location updated!");
		}
	}

	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix
	 * 
	 * @param location
	 *            The new Location that you want to evaluate
	 * @param currentBestLocation
	 *            The current Location fix, to which you want to compare the new
	 *            one
	 */
	private boolean isBetterLocation(Location newLocation,
			Location currentBestLocation) {

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (newLocation.getAccuracy() - (currentBestLocation
				.getAccuracy() + accuracy));
		boolean isMoreAccurate = accuracyDelta < 0;

		// Determine location quality using its accuracy
		if (isMoreAccurate)
			accuracy = DISTANCE;
		else
			accuracy += DISTANCE;

		return isMoreAccurate;
	}

}
