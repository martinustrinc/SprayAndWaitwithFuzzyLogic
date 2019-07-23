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
import core.ConnectionListener;
/**
 * Report for generating different kind of total statistics about message
 * relaying performance. Messages that were created during the warm up period
 * are ignored.
 * <P><strong>Note:</strong> if some statistics could not be created (e.g.
 * overhead ratio if no messages were delivered) "NaN" is reported for
 * double values and zero for integer median(s).
 */
public class MessageStatsReport_26Sept_TiapKontak extends Report implements MessageListener,ConnectionListener {
	private Map<String, Double> creationTimes;
	private List<Double> latencies;
	private List<Integer> hopCounts;
	private List<Double> msgBufferTime;
	private List<Double> rtt; // round trip times
	private int lastRecord = 0;
        
        public static final int DEFAULT_CONTACT_COUNT = 500;
        private int interval;
        
        private Map<Integer, Integer> nrofTotalCopy;
        private int TOTAL_CONTACT = 2;
//        private double lastRecord = Double.MIN_VALUE;
	private int nrofDropped;
	private int nrofRemoved;
	private int nrofStarted;
	private int nrofAborted;
	private int nrofRelayed;
	private int nrofCreated;
	private int nrofResponseReqCreated;
	private int nrofResponseDelivered;
	private int nrofDelivered;
//        private DTNHos
	
	/**
	 * Constructor.
	 */
	public MessageStatsReport_26Sept_TiapKontak() {
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
                
		this.interval = DEFAULT_CONTACT_COUNT;
                this.nrofTotalCopy = new HashMap<>();
                
		this.nrofDropped = 0;
		this.nrofRemoved = 0;
		this.nrofStarted = 0;
		this.nrofAborted = 0;
		this.nrofRelayed = 0;
		this.nrofCreated = 0;
                this.nrofResponseReqCreated = 0;
		this.nrofResponseDelivered = 0;
		this.nrofDelivered = 0;
	}

	
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
		if (isWarmupID(m.getId())) {
			return;
		}
		
		if (dropped) {
			this.nrofDropped++;
		}
		else {
			this.nrofRemoved++;
		}
		
		this.msgBufferTime.add(getSimTime() - m.getReceiveTime());
	}

	
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
		if (isWarmupID(m.getId())) {
			return;
		}
		
		this.nrofAborted++;
	}

	
	public void messageTransferred(Message m, DTNHost from, DTNHost to,
			boolean finalTarget) {
		if (isWarmupID(m.getId())) {
			return;
		}

		this.nrofRelayed++;
		if (finalTarget) {
			this.latencies.add(getSimTime() - 
				this.creationTimes.get(m.getId()) );
			this.nrofDelivered++;
			this.hopCounts.add(m.getHops().size() - 1);
			
			if (m.isResponse()) {
				this.rtt.add(getSimTime() -	m.getRequest().getCreationTime());
				this.nrofResponseDelivered++;
			}
		}
	}

	public void newMessage(Message m) {
		if (isWarmup()) {
			addWarmupID(m.getId());
			return;
		}
		
		this.creationTimes.put(m.getId(), getSimTime());
		this.nrofCreated++;
		if (m.getResponseSize() > 0) {
			this.nrofResponseReqCreated++;
		}
	}
	
	
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
		if (isWarmupID(m.getId())) {
			return;
		}

		this.nrofStarted++;
	}
	
        public void updated(List<DTNHost> hosts) {
            double simTime = getSimTime();
            if (isWarmup()) {
		return;
            }
            if (simTime - lastRecord >= interval) {
                nrofTotalCopy.put((int)simTime, nrofDelivered);
		lastRecord = (int)simTime - (int)simTime % interval;
            }
	}

	@Override
	public void done() {	
                String statsText = "NrofTotalCopy\tContact\n";
                for (Map.Entry<Integer, Integer> entry : nrofTotalCopy.entrySet()) {
                Integer key = entry.getKey();
                Integer value = entry.getValue();
                statsText += key+"\t\t"+value+"\n";
            }
		write(statsText);
		super.done();
	}

//    @Override
//    public void messageSavedToBuffer(Message m, DTNHost to) {}
//
//    @Override
//    public void connectionUp(DTNHost thisHost) {
//        TOTAL_CONTACT++;
//	if (TOTAL_CONTACT - lastRecord >= interval) {
//            lastRecord = TOTAL_CONTACT;
//            nrofTotalCopy.put(lastRecord, this.nrofRelayed-this.nrofDelivered);
//	}
//    }

    @Override
    public void hostsConnected(DTNHost host1, DTNHost host2) {
        TOTAL_CONTACT++;
	if (TOTAL_CONTACT - lastRecord >= interval) {
            lastRecord = TOTAL_CONTACT;
            nrofTotalCopy.put(lastRecord, this.nrofRelayed-this.nrofDelivered);
	}
    }

    @Override
    public void hostsDisconnected(DTNHost host1, DTNHost host2) {
        
    }
}