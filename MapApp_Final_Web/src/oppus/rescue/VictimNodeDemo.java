package oppus.rescue;

public class VictimNodeDemo {
	String node;
	double lat;
	double lon; 
	long time;
	String message;
	int steps; 
	int screen;
	int distance;
	int batery;
	boolean safe;
	public VictimNodeDemo(String node, double lat, double lon, long time,
			String message, int steps, int screen, int distance, int batery,
			boolean safe) {
		this.node=node;
		this.lat=lat;
		this.lon=lon; 
		this.time=time;
		this.message=message;
		this.steps=steps; 
		this.screen=screen;
		this.distance=distance;
		this.batery=batery;
		this.safe=safe;
	}

}
