package oppus.rescue;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView.FindListener;
import android.widget.TextView;

/**
 * Stats fragment (similar to Settings) Should present the different gathered
 * stats from the alert and current rescue efforts
 * 
 * @author andre
 * 
 */
@SuppressLint("ValidFragment")
public class Stats extends Fragment {

	private MapManager mm;

	public Stats() {
	}

	public Stats(MapManager mapManager) {
		mm = mapManager;

	}

	private final static String LT = "RESCUE";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.fragment_stats, container,
				false);

		TextView victims = (TextView) rootView
				.findViewById(R.id.unsavedVictims);
		victims.setText(mm.getVictimNumber() + "");
		TextView points = (TextView) rootView.findViewById(R.id.numberPoints);
		points.setText(mm.getPointNumber() + "");
		TextView webUpdate = (TextView) rootView
				.findViewById(R.id.lastWebUpdate);
		webUpdate.setText(mm.lastWebUpdate() + "");
		TextView webConnection = (TextView) rootView
				.findViewById(R.id.lastWebConnect);
		webConnection.setText(mm.lastWebConnection() + "");
		TextView localUpdate = (TextView) rootView
				.findViewById(R.id.lastLocalUpdate);
		localUpdate.setText(mm.lastLocalUpdate() + "");

		return rootView;
	}
}
