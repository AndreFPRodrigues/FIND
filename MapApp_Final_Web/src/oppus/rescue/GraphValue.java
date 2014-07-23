package oppus.rescue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.util.Log;

import com.jjoe64.graphview.GraphView.GraphViewData;

public class GraphValue {
	long timestamp;
	int value;

	public long getTimestamp() {
		return timestamp;
	}

	public int getValue() {
		return value; 
	}

	public GraphValue(int value, long timestamp) {
		this.timestamp = timestamp;
		this.value = value;
	}

	public static GraphViewData[] getGraphView(ArrayList<GraphValue> values) {
		GraphViewData[] gvd = new GraphViewData[values.size()];
		for (int i = 0; i < values.size(); i++) {
			gvd[i] = new GraphViewData(values.get(i).getTimestamp(), values
					.get(i).getValue());

		}
		return gvd;
	}

	public static String[] getTimeLabel(GraphViewData[] values) {
		String [] result =  new String[2];
		int size=values.length;
		if(size>0){
			long v0=(long) values[0].getX();
			long v2 =(long) values[size-1].getX();
			result[0]=getDate(v0);
			//esult[1] = getDate((v2+v0)/2);
		result[1] = getDate(v2);
		}
		return result;
	}

	private static String getDate(long v) {
		Date date = new Date(v);

		SimpleDateFormat df2 = new SimpleDateFormat("dd/MM/yy HH:mm:ss");

		return df2.format(date);
		
	}
}
