/*
 * MapApp : Simple offline map application, made by Hisham Ghosheh for tutorial purposes only
 * Tutorial on my blog
 * http://ghoshehsoft.wordpress.com/2012/03/09/building-a-map-app-for-android/
 * 
 * Class tutorial:
 * http://ghoshehsoft.wordpress.com/2012/04/06/mapapp5-mapview-and-activity/
 */

package oppus.rescue;

import android.app.Activity;
import android.util.Log;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.gms.maps.MapFragment;

public class MapAppActivity extends Activity   {

	MapManager mm;
	boolean haveInternet = false;

	@Override
	protected void onResume() {
		setContentView(R.layout.main);

		mm = new MapManager(((MapFragment) getFragmentManager()
				.findFragmentById(R.id.map)).getMap(), this);
		// Getting view from the layout file info_window_layout

		// googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new
		// LatLng(39.405159, -9.133357), 16));

		if (haveInternet)
			updateMap();

		super.onResume();
	}

	private void updateMap() {
		mm.updateVictimsWebService();
	}

	@Override
	protected void onPause() {
		mm.close();

		super.onPause();
	}



}