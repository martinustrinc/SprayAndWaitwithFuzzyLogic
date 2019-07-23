/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package report;

import core.DTNHost;
import core.SimClock;
import core.UpdateListener;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Martinus Tri Nur Cahyono, Sanata Dharma University
 */
public class FuzzyReport extends Report implements UpdateListener{

    protected Map<DTNHost, Double> upTime;
    protected Map<DTNHost, Double> downTime;
    protected Map<DTNHost, Double> TROP;
    
    @Override
    public void updated(List<DTNHost> hosts) {
        SimClock.getTime();
        
    }
    
    /**
	 * Prints a snapshot of the average buffer occupancy
	 * @param hosts The list of hosts in the simulation
	 */	 
	private void printLine(List<DTNHost> hosts) {	
            String z ="";
            write((int)getSimTime()+ " Interval Sekarang :");
            write("Node\tBuffer");
//                for (Map.Entry<DTNHost, Double> entry: tiapNode.entrySet()) {
//                    z+=entry.getKey() +"\t"+ format(entry.getValue()) +"\n";
//                }
                write(z);
	}    
}
