package gcm;

import gcm.map.TilesProvider;

import java.io.File;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

public class Simulation {
	String name;
	String location;
	String date;
	String duration;
	String latS;
	String lonS;
	String latE;
	String lonE;
	
	private final static int MIN_ZOOM =0;
	private final static int MAX_ZOOM =14;

	public Simulation(JSONObject jsonObject) {
		name = jsonObject.getString("name");
		location = jsonObject.getString("location");
		date = jsonObject.getString("start_date");
		duration = jsonObject.getString("duration_m");
		latS = jsonObject.getString("latS");
		lonS = jsonObject.getString("lonS");
		latE = jsonObject.getString("latE");
		lonE = jsonObject.getString("lonE");

	}

	public String getName() {
		return name;
	}

	public String getLocation() {
		return location;
	}

	public String getDate() {

		String[] d = date.split(" ");
		String[] da = d[0].split("-");
		String[] time = d[1].split(":");
		return da[1] + "/" + da[2] + " at " + time[0] + ":" + time[1];
	}

	public String getDuration() {
		return duration;
	}

	@Override
	public String toString() {
		return name + ", " + location + " " + getDate() + " for " + duration
				+ "min";
	}

	public void activate( Context c ) {
		final double f_latS = Double.parseDouble(latS);
		final double f_lonS = Double.parseDouble(lonS);

		final double f_latE = Double.parseDouble(latE);
		final double f_lonE = Double.parseDouble(lonE);
		final TilesProvider tp = new TilesProvider(DemoActivity.PATH);

		//tp.downloadTilesInBound(f_latS, f_lonE, f_latE, f_lonS , MIN_ZOOM, MAX_ZOOM, c);
	}
	
	public static void preDownloadTiles(double f_latS, double f_lonS, double f_latE, double f_lonE , Context c ) {
		
		final TilesProvider tp = new TilesProvider(DemoActivity.PATH);
		Log.d("gcm" , "downloading " +  f_latS + " "+ f_lonS + " " + f_latE + " " + f_lonE);

		tp.downloadTilesInBound(f_latS, f_lonE, f_latE, f_lonS , MIN_ZOOM, MAX_ZOOM, c);
	}

}
