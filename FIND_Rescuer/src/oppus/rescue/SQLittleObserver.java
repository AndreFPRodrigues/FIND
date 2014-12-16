package oppus.rescue;

import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.content.ContentValues;
import android.content.Context;
import android.widget.Toast;

/**
 * Observers the FIND Service database When a message is added it receives an
 * update adding the victim node to the map
 * 
 * @author andre
 * 
 */
public class SQLittleObserver {
	public static final String COL_ID = "_id";
	public static final String COL_NODE = "nodeid";
	public static final String COL_TIME = "timestamp";
	public static final String COL_MSG = "message";
	public static final String COL_LAT = "latitude";
	public static final String COL_LON = "longitude";
	public static final String COL_CONF = "llconf";
	public static final String COL_BATTERY = "battery";
	public static final String COL_STEPS = "steps";
	public static final String COL_SCREEN = "screen";
	public static final String COL_DISTANCE = "distance";
	public static final String COL_SAFE = "safe";
	public static final String COL_ADDED = "local_added";
	public static final String COL_DIRECTION = "direction";
	private static Context context;
	private MapManager mm;
	private MyObserver obs;

	private long lastToast;
	private final static String LT = "RESCUE";

	public SQLittleObserver(Context context, MapManager mm) {
		this.context = context;
		this.mm = mm;
		lastToast = 0;
		String URL1 = "content://find.service.net.diogomarques.wifioppish.MessagesProvider/received";
		Uri msg1 = Uri.parse(URL1);
		obs = new MyObserver(new Handler());
		context.getContentResolver().registerContentObserver(msg1, true, obs);
	}

	// DEMO
	// private boolean first = true;

	/**
	 * Retrived all stored nodes
	 */
	public void retriveAllNodes() {
		// Retrieve student records
		String URL = "content://find.service.net.diogomarques.wifioppish.MessagesProvider/received";
		Uri uri = Uri.parse(URL);
		Cursor c = context.getContentResolver().query(uri, null, null, null,
				"timestamp");
		int i = -1;

		if (c.moveToFirst()) {
			do {
				i++;
				// if (first) {
				// String cluster1 = c.getString(c.getColumnIndex("cluster"));
				// if (cluster1.contains("C1"))
				// DemoClusters.mode = DemoClusters.C1;
				// else
				// DemoClusters.mode = DemoClusters.C2;
				// MapManager.dm = new DemoClusters();
				// first = false;
				// }
				double lat = c.getDouble(c.getColumnIndex("latitude"));
				double lon = c.getDouble(c.getColumnIndex("longitude"));

				boolean safe = c.getInt(c.getColumnIndex("safe")) > 0;
				String node = c.getString(c.getColumnIndex("nodeid"));

				String msg = c.getString(c.getColumnIndex("message"));
				long time = c.getLong(c.getColumnIndex("timestamp"));
				// long added = c.getLong(c.getColumnIndex("local_added"));
				int steps = c.getInt(c.getColumnIndex("steps"));
				int screen = c.getInt(c.getColumnIndex("screen"));
				// int distance = c.getInt(c.getColumnIndex("distance"));
				int battery = c.getInt(c.getColumnIndex("battery"));
				// int id = c.getInt(c.getColumnIndex("_id"));

				// if (MapManager.demo) {
				// String cluster = c.getString(c.getColumnIndex("cluster"));
				// Log.d("RESCUE", "cluster:" + cluster);
				// if (cluster.equals("C1VV2") || cluster.equals("C2VV2")) {
				//
				// Log.d("RESCUE", "vv2:" + node);
				//
				// MapManager.dm.addVV2(node, lat, lon, time, msg, steps,
				// screen, distance, battery, true);
				// return;
				// } else if (cluster.equals("C1VV3")
				// || cluster.equals("C2VV3")) {
				// Log.d("RESCUE", "vv3:" + node);
				//
				// MapManager.dm.addVV3(node, lat, lon, time, msg, steps,
				// screen, distance, battery, true);
				// return;
				// }
				//
				// }

				if (c.getInt(c.getColumnIndex("llconf")) < 1 && lat == 0
						&& lon == 0) {

				} else {
					// set true for demo was false
					mm.addVictimMarker(node, lat, lon, msg, time, steps,
							screen, 0, battery, safe, true);
				}
			} while (c.moveToNext());
		}

	}

	/**
	 * Insert rows into the find service permanent storage
	 * 
	 * @param nodes
	 */
	public static void insertRow(ContentValues[] nodes) {

		Log.d("Node", "INSERT BULK");
		String URL1 = "content://find.service.net.diogomarques.wifioppish.MessagesProvider/received";
		Uri msg1 = Uri.parse(URL1);
		context.getContentResolver().bulkInsert(msg1, nodes);
	}

	class MyObserver extends ContentObserver {
		public MyObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			this.onChange(selfChange, null);
		}

		/**
		 * Updates the map when a victim node is addded to the database
		 */
		@Override
		public void onChange(boolean selfChange, Uri uri) {

			String split = uri.getLastPathSegment();

			String URL1 = "content://find.service.net.diogomarques.wifioppish.MessagesProvider/received/"
					+ split;
			Uri msg1 = Uri.parse(URL1);

			Cursor c = context.getContentResolver().query(msg1, null, null,
					null, "_id");
			// Log.d(LT, "url:" +uri.toString());
			// Log.d(LT, "last:" +split);
			// Log.d(LT, "msg:" +msg1.toString());

			int i = -1;
			if (c.moveToFirst()) {
				do {

					i++;
					double lat = c.getDouble(c.getColumnIndex("latitude"));
					double lon = c.getDouble(c.getColumnIndex("longitude"));

					// if
					// (c.moveToPosition(Integer.parseInt(split[split.length-1])))
					// {

					if (c.getInt(c.getColumnIndex("llconf")) < 1 && lat == 0
							&& lon == 0) {
						return;
					}

					boolean safe = c.getInt(c.getColumnIndex("safe")) > 0;
					String node = c.getString(c.getColumnIndex("nodeid"));
					String msg = c.getString(c.getColumnIndex("message"));
					long time = c.getLong(c.getColumnIndex("timestamp"));
					long added = c.getLong(c.getColumnIndex("local_added"));
					int steps = c.getInt(c.getColumnIndex("steps"));
					int screen = c.getInt(c.getColumnIndex("screen"));
					int distance = c.getInt(c.getColumnIndex("distance"));
					int battery = c.getInt(c.getColumnIndex("battery"));

					// if (MapManager.demo) {
					// String cluster = c.getString(c
					// .getColumnIndex("cluster"));
					// if (cluster.equals("C1VV2") || cluster.equals("C2VV2")) {
					// MapManager.dm.addVV2(node, lat, lon, time, msg,
					// steps, screen, distance, battery, true);
					// return;
					// } else if (cluster.equals("C1VV3")
					// || cluster.equals("C2VV3")) {
					//
					// MapManager.dm.addVV3(node, lat, lon, time, msg,
					// steps, screen, distance, battery, true);
					// return;
					// }
					// if(cluster.length()==0){
					// Log.d("RESCUE", "local node added to log");
					// DemoClusters.addToLog(node + " " + lat +" " +lon+" "+ +
					// time + " " + steps + " " + screen + " " +msg );
					//
					// }
					// }
					mm.addVictimMarker(node, lat, lon, msg, time, steps,
							screen, 0, battery, safe, true);

				} while (c.moveToNext());
			}
			if ((System.currentTimeMillis() - lastToast) > 20000) {
				Toast.makeText(context, "Updated", Toast.LENGTH_LONG).show();
				lastToast = System.currentTimeMillis();
				mm.notificationSound();

			}

		}
	}
}
