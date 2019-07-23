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
import core.UpdateListener;

/**
 * Report for generating different kind of total statistics about message
 * relaying performance. Messages that were created during the warm up period
 * are ignored.
 * <P><strong>Note:</strong> if some statistics could not be created (e.g.
 * overhead ratio if no messages were delivered) "NaN" is reported for
 * double values and zero for integer median(s).
 */
public class MessageStatsReport_19Sept_NodeTerkirim extends Report implements MessageListener, UpdateListener{
	private Map<String, Double> creationTimes;
	private List<Double> latencies;
	private List<Integer> hopCounts;
	private List<Double> msgBufferTime;
	private List<Double> rtt; // round trip times
        private Map<Integer, Integer> NrofTransmitted;
	
	private int nrofDropped;
	private int nrofRemoved;
	private int nrofStarted;
	private int nrofAborted;
	private int nrofRelayed;
	private int nrofCreated;
	private int nrofResponseReqCreated;
	private int nrofResponseDelivered;
	private int nrofDelivered;
        
//        private Map<DTNHost, Integer> nrofDropped;
	public static final String BUFFER_REPORT_INTERVAL = "occupancyInterval";
	/** Default value for the snapshot interval */
	public static final int DEFAULT_MESSAGE_REPORT_INTERVAL = 3600; //per second
	
	private double lastRecord = Double.MIN_VALUE;
	private int interval;
	
        
	/**
	 * Constructor.
	 */
	public MessageStatsReport_19Sept_NodeTerkirim() {
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
		this.NrofTransmitted = new HashMap<Integer, Integer>();
		this.nrofDropped = 0;
		this.nrofRemoved = 0;
		this.nrofStarted = 0;
		this.nrofAborted = 0;
		this.nrofRelayed = 0;
		this.nrofCreated = 0;
		this.nrofResponseReqCreated = 0;
		this.nrofResponseDelivered = 0;
		this.nrofDelivered = 0;
                this.interval = DEFAULT_MESSAGE_REPORT_INTERVAL;
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
	
        @Override
        public void updated(List<DTNHost> hosts) {
            double simTime = getSimTime();
            if (isWarmup()) {
		return;
            }
            if (simTime - lastRecord >= interval) {
                NrofTransmitted.put((int)simTime, nrofDelivered);
		lastRecord = simTime - simTime % interval;
            }
	}
        
	@Override
	public void done() {
		write("Waktu \t Jumlah Pengiriman(Nrof Node Terkirim)");
                String temp = "";
                for (Map.Entry<Integer, Integer> entry : NrofTransmitted.entrySet()) {
                    Integer key = entry.getKey();
                    Integer value = entry.getValue();
                    temp += key +" \t "+value+"\n";
                }
		write(temp);
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