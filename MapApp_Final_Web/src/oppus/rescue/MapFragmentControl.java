/*
 * MapApp : Simple offline map application, made by Hisham Ghosheh for tutorial purposes only
 * Tutorial on my blog
 * http://ghoshehsoft.wordpress.com/2012/03/09/building-a-map-app-for-android/
 * 
 * Class tutorial:
 * http://ghoshehsoft.wordpress.com/2012/04/06/mapapp5-mapview-and-activity/
 */

package oppus.rescue;

import java.io.File;
import java.util.Scanner;

import android.app.Fragment;

import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.MapFragment;

public class MapFragmentControl extends Fragment   {

	 MapManager mm;
	boolean haveInternet = false;
	View rootView;

	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		if(rootView==null)
		   rootView = inflater.inflate(R.layout.main,container, false);
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		if(mm==null){
			mm = new MapManager(((MapFragment) getFragmentManager()
				.findFragmentById(R.id.map)).getMap(), inflater);
			if (haveInternet){ 
				updateMap();
			}
		}
		// Getting view from the layout file info_window_layout

		// googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new
		// LatLng(39.405159, -9.133357), 16));

		
		return rootView;
		//
	}

	
	public MapManager getMapManager(){
		return mm;
	}
	
	private void updateMap() {
	
		mm.updateVictimsWebService();
	}
		
		
	

}