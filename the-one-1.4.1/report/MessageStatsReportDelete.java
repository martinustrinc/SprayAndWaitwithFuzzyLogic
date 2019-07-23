/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import core.Message;
import core.MessageListener;

/**
 * Report for generating different kind of total statistics about message
 * relaying performance. Messages that were created during the warm up period
 * are ignored.
 * <P><strong>Note:</strong> if some statistics could not be created (e.g.
 * overhead ratio if no messages were delivered) "NaN" is reported for
 * double values and zero for integer median(s).
 */
public class MessageStatsReportDelete extends Report implements MessageListener {
	private Map<String, Double> creationTimes;
	private List<Double> latencies;
	private List<Integer> hopCounts;
	private List<Double> msgBufferTime;
	private List<Double> rtt; // round trip times
	
	private int nrofDropped;
	private int nrofRemoved;
	private int nrofStarted;
	private int nrofAborted;
	private int nrofRelayed;
	private int nrofCreated;
	private int nrofResponseReqCreated;
	private int nrofResponseDelivered;
	private int nrofDelivered;
        private Map<DTNHost, Integer> drop;
//        private Map<DTNHost, Integer> nrofDropped;
	
	/**
	 * Constructor.
	 */
	public MessageStatsReportDelete() {
		init();
	}

	@Override
	protected void init() {
		super.init();
		this.creationTimes = new HashMap<String, Double>();
		this.latencies = new ArrayList<Double>();
		this.msgBufferTime = new ArrayList<Double>();
		this.hopCounts = new ArrayList<Integer>();
		this.rtt = new ArrayList<Double>();
		
                this.drop = new HashMap<DTNHost, Integer>();
		this.nrofDropped = 0;
		this.nrofRemoved = 0;
	}

	
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
		if (isWarmupID(m.getId())) {
			return;
		}
		
		if (dropped) {
			this.nrofDropped++;
                        if (drop.containsKey(where)) {
//                            this.drop.replace(where, nrofDropped+1);
                              this.drop.replace(where, drop.get(where)+1);
//                            System.out.println(this.drop);
                        } else {
                            this.drop.put(where, 1);
                        }
//                        System.out.println(this.drop);
		}
//		else {
//			this.nrofRemoved++;
//		}
		
		this.msgBufferTime.add(getSimTime() - m.getReceiveTime());
                
	}

	
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
            
	}

	
	public void messageTransferred(Message m, DTNHost from, DTNHost to,
			boolean finalTarget) {
	}


	public void newMessage(Message m) {
//            if (isWarmup()) {
//		addWarmupID(m.getId());
//		return;
//            }
//		
//            this.creationTimes.put(m.getId(), getSimTime());
//            this.nrofCreated++;
//            if (m.getResponseSize() > 0) {
//		this.nrofResponseReqCreated++;
//            }
	}
	
	
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
	}
	

	@Override
	public void done() {
		String statsText = 
			"message dropped: " + this.nrofDropped +
//                        "\nNode\nDropped: " + this.drop
                        "\n" +
                        "\n" +
                        "Node\tNumber Of Dropped"
			;
                
                String statsText1 = "" ;
                for (Map.Entry<DTNHost, Integer> entry : drop.entrySet()) {
                    statsText1 += entry.getKey()+"\t"+entry.getValue()+"\n";
                }
                
		write(statsText);
                write(statsText1);
		super.done();
	}

//    @Override
//    public void messageSavedToBuffer(Message m, DTNHost to) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    public void connectionUp(DTNHost thisHost) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }
}