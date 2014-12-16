package oppus.rescue;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView.FindListener;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
/**
 * Settings fragment to show options and filter mechanisms
 * Currently not implemented
 * To enable it add it has a item in MainActivity (navDrawerItems)
 * @author andre
 *
 */
@SuppressLint("ValidFragment")
public class Settings extends Fragment {
	
	private final static String LT = "RESCUE";
	private SeekBar numberPointsPerVictim = null;
	private MapManager mm;
	private int numberOfPoints;
	private boolean showSafe;
	private CheckBox safe;
	

	public Settings (){}

	public Settings(MapManager mapManager){
		mm= mapManager;
		numberOfPoints = mm.getPointNumberPerVictim();
		showSafe = mm.getShowSafe();
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		numberOfPoints = mm.getPointNumberPerVictim();
		showSafe = mm.getShowSafe();
		final View rootView = inflater.inflate(R.layout.fragment_settings, container, false);
		 numberPointsPerVictim = (SeekBar) rootView.findViewById(R.id.numberPerVictim);
		 safe= (CheckBox) rootView.findViewById(R.id.safe);
		 safe.setChecked(showSafe);
		 numberPointsPerVictim.setProgress(numberOfPoints);
		 numberPointsPerVictim.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
	            int progressChanged = 0;
	 
	            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
	                progressChanged = progress;
	            }
	 
	            public void onStartTrackingTouch(SeekBar seekBar) {
	                // TODO Auto-generated method stub
	            }
	 
	            public void onStopTrackingTouch(SeekBar seekBar) {
	                Toast.makeText(rootView.getContext(),"seek bar progress:"+progressChanged, 
	                        Toast.LENGTH_SHORT).show();
	                numberOfPoints= progressChanged;
	            }
	        });
	 
	 

           
        return rootView;
    }
	
	@Override
	public void onDetach(){
		mm.setPointNumberPerVictim(numberOfPoints);
		mm.setShowSafe(safe.isChecked());
		super.onDetach();
	
		
	}

}
