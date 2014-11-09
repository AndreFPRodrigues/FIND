package oppus.rescue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import android.location.Location;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.model.LatLng;

import oppus.rescue.Victim.VictimNode;

public class DemoClusters {
	static ArrayList<VictimNodeDemo> VV2; 
	static ArrayList<VictimNodeDemo> VV3;

	private static ArrayList<String> log;

	static Location vm;
	static Location vv1; 
	static Location vv2;
	static Location vv3;
	static Location update_vv3;

	final static int iVM = 0;
	final static int iVV1 = 1;
	final static int iVV2 = 2;
	final static int iVV3 = 3;
	final static int updateVV3 = 4;

	static boolean updated = false;

	final static int C1 = 0;
	final static int C2 = 1;
	static int mode = C1;

	public DemoClusters() {
		VV2 = new ArrayList<VictimNodeDemo>();
		VV3 = new ArrayList<VictimNodeDemo>();
		log = new ArrayList<String>();
		vm = new Location("");
		vm.setLatitude(38.75498845);
		vm.setLongitude(-9.157368);
		// vm.setLatitude(38.74444802367031);
		// vm.setLongitude(-9.157269448041916);
		vm.setAccuracy(3333);
		vm.setBearing(333);

		vv1 = new Location("");
		vv1.setLatitude(38.755934139);
		vv1.setLongitude(-9.15700793);
		// debug
		// vv1.setLatitude(38.74444802367031);
		// vv1.setLongitude(-9.157269448041916);
		vv1.setAccuracy(3333);
		vv1.setBearing(333);

		vv2 = new Location("");
		vv2.setLatitude(38.7564583548916);
		vv2.setLongitude(-9.15705218911171);

		vv2.setAccuracy(3333);
		vv2.setBearing(333);

		vv3 = new Location("");
		if (mode == C1) {

			vv3.setLatitude(38.755480704981397);
			vv3.setLongitude(-9.156592190265656);
		} else {
			vv3.setLatitude(38.756014);
			vv3.setLongitude(-9.156285);

		}
		vv3.setAccuracy(3333);
		vv3.setBearing(333);

		update_vv3 = new Location("");
		update_vv3.setLatitude(38.755872);
		update_vv3.setLongitude(-9.156785);
		// fcul
		// update_vv3.setLatitude(38.755899);
		// update_vv3.setLongitude(-9.157983);
		// update_vv3.setLatitude(38.74444802367031);
		// update_vv3.setLongitude(-9.157269448041916);
		update_vv3.setAccuracy(3333);
		update_vv3.setBearing(333);
	}

	public void addVV2(String node, double lat, double lon, long time,
			String message, int steps, int screen, int distance, int batery,
			boolean safe) {
		VV2.add(new VictimNodeDemo(node, lat, lon, time, message, steps,
				screen, distance, batery, safe));
	}

	public void addVV3(String node, double lat, double lon, long time,
			String message, int steps, int screen, int distance, int batery,
			boolean safe) {
		VV3.add(new VictimNodeDemo(node, lat, lon, time, message, steps,
				screen, distance, batery, safe));
	}

	public static int savingPosition(Location location) {
		if (location.distanceTo(vm) < 20)
			return 0;
		else if (location.distanceTo(vv1) < 20)
			return 1;
		else if (location.distanceTo(vv2) < 10)
			return 2;
		if (location.distanceTo(vv3) < 10)
			return 3;
		else if (location.distanceTo(update_vv3) < 15)
			return 4;
		return -1;
	}

	public static int getSavedVictims(int index) {
		switch (index) {
		case iVM:
			return 2;
		case iVV1:
			if (mode == C1) {
				return 10;
			} else
				return 5;
		case iVV2:
			if (mode == C1) {
				return 5;
			} else
				return 10;
		case iVV3:
			return 5;
		}

		return 0;
	}

	public static void updadeVictimSpots(MapManager mapManager) {
		for (int i = 0; i < VV2.size(); i++) {
			mapManager.addVictimMarker(VV2.get(i).node, VV2.get(i).lat,
					VV2.get(i).lon, VV2.get(i).message, VV2.get(i).time,
					VV2.get(i).steps, VV2.get(i).screen, 0, VV2.get(i).batery,
					VV2.get(i).safe, true);
		}

		if (mode == C1) {
			for (int i = 0; i < VV3.size(); i++) {
				mapManager.addVictimMarker(VV3.get(i).node, VV3.get(i).lat,
						VV3.get(i).lon, VV3.get(i).message, VV3.get(i).time,
						VV3.get(i).steps, VV3.get(i).screen, 0,
						VV3.get(i).batery, VV3.get(i).safe, true);
			}

		}

	}

	public static synchronized void updateMessageVictimSpots(
			MapManager mapManager) {
		for (int i = 0; i < VV3.size(); i++) {
			mapManager.addVictimMarker(VV3.get(i).node, VV3.get(i).lat,
					VV3.get(i).lon, VV3.get(i).message, VV3.get(i).time,
					VV3.get(i).steps, VV3.get(i).screen, 0, VV3.get(i).batery,
					VV3.get(i).safe, true);
		}
	}

	public static void addToLog(String s) {
		log.add(s);
	}

	public static void writeToLog(int numberMarkerClicks, int numberMapClicks,
			String timeDemo, int numberVictims, int totalMicro,
			int totalScreen, long totalTimeStatsOpen) {
		int minutes = (int) (totalTimeStatsOpen / 60);
		int seconds = (int) (totalTimeStatsOpen % 60);
		File file = new File(Environment.getExternalStorageDirectory()
				+ "/rescueLog/RESCUE" + System.currentTimeMillis());
		FileWriter fw;

		try {
			fw = new FileWriter(file, true);
			fw.write("Time:" + timeDemo + "\n");
			fw.write("Victims:" + numberVictims + "\n");
			fw.write("Map clicks:" + numberMapClicks + "\n");
			fw.write("Marker clicks:" + numberMarkerClicks + "\n");
			fw.write("Marker window opened: " + minutes + ":" + seconds + "\n");
			fw.write("Micro Graph:" + totalMicro + "\n");
			fw.write("Screen Graph:" + totalScreen + "\n");

			for (int i = 0; i < log.size(); i++) {
				fw.write(log.get(i) + "\n");
			}
			fw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
