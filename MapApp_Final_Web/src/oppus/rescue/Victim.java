package oppus.rescue;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import oppus.rescue.Victim.VictimNode;

import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class Victim {

	private ArrayList<VictimNode> points;
	private long lastTimeStamp;
	private Marker mainMarker;
	private int idMainMarker;
	private String idNode;
	private int idPoints;
	private boolean safe; 
	private int totalMicro;
	private int totalScreen;
	

	public Victim(Marker marker, String message, long timestamp, int steps,
			int screen, int distance, int batery, boolean safe) {
		this.safe = safe;
		idNode = marker.getTitle();
		mainMarker = marker;
		points = new ArrayList<VictimNode>();
		points.add(new VictimNode(marker.getPosition(), timestamp, message,
				steps, screen, distance, batery, this.safe));
		idMainMarker = 0;
		lastTimeStamp = timestamp;
	}

	public Victim() {
		// TODO Auto-generated constructor stub
	}

	public ArrayList<VictimNode> getMarkers() {
		return points;
	}

	public long lastTimeStamp() {
		return lastTimeStamp;
	}

	public String getId() {
		return idNode;
	}

	public int getNumberOfPoints() {
		return points.size();
	}

	public boolean checkUpdateMarker(long timestamp) {
		if (lastTimeStamp < timestamp) {
			return true;
		} else
			return false;

	}

	public ArrayList<String> getMessageNodes() {
		ArrayList<String> result = new ArrayList<String>();
		for (VictimNode vn : points) {
			String m;
			if ((m = vn.getMessage()) != null && !m.equals("")) {
				result.add(m + ";;;" + vn.id);
			}
		}
		return result;
	}
	
	public int getBatteryValue() {
	
		return points.get(idMainMarker).batery;
	}

	public ArrayList<GraphValue> getScreenValues() {
		ArrayList<GraphValue> result = new ArrayList<GraphValue>();
		for (VictimNode vn : points) {
			result.add(new GraphValue(vn.screen, vn.timestamp));
		}
		return result;
	}
	
	public ArrayList<GraphValue> getMicroValues() {
		ArrayList<GraphValue> result = new ArrayList<GraphValue>();
		for (VictimNode vn : points) {
			result.add(new GraphValue(vn.steps, vn.timestamp));
		}
		return result;
	}
	public ArrayList<GraphValue> getDistanceValues() {
		ArrayList<GraphValue> result = new ArrayList<GraphValue>();
		int lastDistance = 0;
		for (VictimNode vn : points) {
			result.add(new GraphValue(vn.distance-lastDistance, vn.timestamp));
			lastDistance= vn.distance;
		}
		return result;
	}
 
	public Marker getMarker() {
		return mainMarker;
	}

	public void updateLastMarker(Marker m, long timestamp, int steps,
			int screen, int distance, int batery, String message, boolean safe) {
		mainMarker.remove();
		mainMarker = m;
		lastTimeStamp = timestamp;
		this.safe = safe;

		idMainMarker = addNode(m.getPosition(), timestamp, message, steps,
				screen, distance, batery, this.safe);
	}

	int addNode(LatLng position, long timestamp, String message, int steps,
			int screen, int distance, int batery, boolean safe) {
		
		points.add((new VictimNode(position, timestamp, message, (steps- totalMicro), screen- totalScreen,
				distance, batery, safe)));
		totalMicro=steps;
		totalScreen=screen;
		return points.size() - 1;

	}

	class VictimNode {
		private LatLng coord;
		private long timestamp;
		private String message;
		private int steps;
		private int screen;
		private int distance;
		private int batery;
		private int id;
		private boolean safe;

		VictimNode(LatLng coord, long timestamp, String message, int steps,
				int screen, int distance, int batery, boolean safe) {
			this.coord = coord; 
			this.timestamp = timestamp;
			this.message = message;
			this.steps = steps;
			this.screen = screen;
			this.distance = distance;
			this.batery = batery;
			this.id = idPoints;
			this.safe = safe;
			idPoints++;
		}

		public int getDistance() {
			return distance;
		}

		public int getBatery() {
			return batery;
		}

		public int getScreen() {
			return screen;
		}

		public int getSteps() {
			return steps;
		}

		public String getMessage() {
			return message;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public LatLng getCoord() {
			return coord;
		}

		public int getId() {
			return id;
		}

	}

	public VictimNode getNode(String id) {
		if (id != null) {
			int idv = Integer.parseInt(id);
			return points.get(idv);
		} else
			return points.get(idMainMarker);
	}

	@Override
	public String toString() {
		StringBuilder list = new StringBuilder();
		for (VictimNode p : points) {
			StringBuilder node = new StringBuilder();
			node.append("{\"nodeid\":\"" + idNode + "\",");
			node.append("\"timestamp\":\"" + p.timestamp + "\",");
			node.append("\"msg\":\"" + p.message + "\",");
			node.append("\"latitude\":\"" + p.getCoord().latitude + "\",");
			node.append("\"longitude\":\"" + p.getCoord().longitude + "\",");
			node.append("\"battery\":\"" + p.batery + "\",");
			node.append("\"steps\":\"" + p.steps + "\",");
			node.append("\"screen\":\"" + p.screen + "\",");
			node.append("\"distance\":\"" + p.distance + "\",");
			node.append("\"safe\":\"" + p.safe + "\",");
			node.append("\"added\":\"" + lastTimeStamp + "\"};");
			list.append(node.toString());

		}
		return list.substring(0, list.length() - 1);

	}
}
