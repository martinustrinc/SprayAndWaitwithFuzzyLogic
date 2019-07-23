package report;
import core.DTNHost;
import core.Settings;
import core.SimClock;
import core.UpdateListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
/**
 *
 * @author Wanguno
 */
public class BufferOccupancyReport_26Sept_tiapNode extends Report implements UpdateListener{

    public static final String BUFFER_REPORT_INTERVAL = "occupancyInterval";
	/** Default value for the snapshot interval */
	public static final int DEFAULT_BUFFER_REPORT_INTERVAL = 3600;
	
	private double lastRecord = Double.MIN_VALUE;
	private int interval;
	
	private Map<DTNHost, Double> tiapNode = new TreeMap<DTNHost, Double>();
	private int updateCounter = 0;  //new added
	
	
	public BufferOccupancyReport_26Sept_tiapNode() {
		super();
		
		Settings settings = getSettings();
		if (settings.contains(BUFFER_REPORT_INTERVAL)) {
			interval = settings.getInt(BUFFER_REPORT_INTERVAL);
		} else {
			interval = -1; /* not found; use default */
		}
		
		if (interval < 0) { /* not found or invalid value -> use default */
			interval = DEFAULT_BUFFER_REPORT_INTERVAL;
		}
	}
	
	public void updated(List<DTNHost> hosts) {
		if (isWarmup()) {
			return;
		}
		
		if (SimClock.getTime() - lastRecord >= interval) {
			lastRecord = SimClock.getTime();
                        for (DTNHost h : hosts) {
                            if (tiapNode.containsKey(h)) {
                                tiapNode.replace(h, h.getBufferOccupancy());
//                                double temp = h.getBufferOccupancy();
                            } else {
                                tiapNode.put(h, h.getBufferOccupancy());
                            }
                        }
			printLine(hosts);
			updateCounter++; // new added
		}
	}	
	/**
	 * Prints a snapshot of the average buffer occupancy
	 * @param hosts The list of hosts in the simulation
	 */	 
	private void printLine(List<DTNHost> hosts) {	
            String z ="";
            write((int)getSimTime()+ " Interval Sekarang :");
            write("Node\tBuffer");
                for (Map.Entry<DTNHost, Double> entry: tiapNode.entrySet()) {
                    z+=entry.getKey() +"\t"+ format(entry.getValue()) +"\n";
                }
                write(z);
	}    
//        
//        @Override
//	public void done()
//	{
//            String z = " ";
//            for (Map.Entry<DTNHost, Double> entry : tiapNode.entrySet()) {		
//		z+= entry.getKey() + "\t" + entry.getValue() + "\n";
//		write("" + z + ' ');
//            }
//            super.done();
//	}
}