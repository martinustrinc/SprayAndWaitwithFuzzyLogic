/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing.SprayAndWait;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import java.util.HashMap;
import routing.MessageRouter;

/**
 * Implementation of Spray and wait router as depicted in
 * <I>Spray and Wait: An Efficient Routing Scheme for Intermittently Connected
 * Mobile Networks</I> by Thrasyvoulos Spyropoulus et al.
 * @author @author Martinus Tri Nur Cahyono, Sanata Dharma University
 */
public class SprayAndWaitRouterRoutingDecision implements RoutingDecisionEngineSnW {

    public static final String NROF_COPIES = "nrofCopies";
    public static final String BINARY_MODE = "binaryMode";
    public static final String SPRAYANDWAIT_NS = "SprayAndWaitRouterRoutingDecision";
    public static final String MSG_COUNT_PROPERTY = "." + "copies";
    protected int initialNrofCopies;
    protected boolean isBinary;
    private HashMap<String, Message> MsgID;

    public SprayAndWaitRouterRoutingDecision(Settings s) {
        if (s.contains(BINARY_MODE)) { 
            isBinary = s.getBoolean(BINARY_MODE);
        } else {
            isBinary = false; //default value
        }

        if (s.contains(NROF_COPIES)) {
            initialNrofCopies = s.getInt(NROF_COPIES);
        } else {
//            initialNrofCopies = this.initialNrofCopies;
            initialNrofCopies = 100; //default value
        }
        
	initialNrofCopies = s.getInt(NROF_COPIES);
	isBinary = s.getBoolean(BINARY_MODE);
        MsgID = new HashMap<>();
    }

    protected SprayAndWaitRouterRoutingDecision(SprayAndWaitRouterRoutingDecision prototype) {
        this.initialNrofCopies = prototype.initialNrofCopies;
        this.isBinary = prototype.isBinary;
        MsgID = new HashMap<>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
    }

    @Override
    public boolean newMessage(Message m) {
        m.addProperty(MSG_COUNT_PROPERTY, initialNrofCopies);
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) { //penerima
        Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);

        if (isBinary) {                                         //cek jika binary spray
            nrofCopies = (int) Math.ceil(nrofCopies / 2.0);     //pembulatan ke atas
        } else {
            nrofCopies = 1;                                     //source spray, single copy
        }

        m.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
        return m.getTo() != thisHost;
//        return true;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) { //pengirim
        if (m.getTo() == otherHost) { // deliver to final destination, ketika didepannya adalah final destinasinya / tujuannya
            return true;
        }

        Integer nrofCopies = (int) m.getProperty(MSG_COUNT_PROPERTY);
        
        if (nrofCopies > 1) {                                   //cek jika ada pesan yang lebih dari 1
//            MsgID.put(m.getId(), m);
            return true;                                        //send message
        }

        return false;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return m.getTo() != hostReportingOld;
    }
    
    @Override
    public RoutingDecisionEngineSnW replicate() {
        return new SprayAndWaitRouterRoutingDecision(this);
    }

    private SprayAndWaitRouterRoutingDecision getOtherDecisionEngine(DTNHost h) {
        
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouterSnW : "This router only works "
                + "with other routers of the same type";
        
        return (SprayAndWaitRouterRoutingDecision) ((DecisionEngineRouterSnW) otherRouter).getDecisionEngine();
    }
    
    //Transfer done to make update message property
    @Override
    public void transferDone(Connection con) { 
//        Integer nrofCopies;
	String msgId = con.getMessage().getId();
	/* get this router's copy of the message */
	Message msg = MsgID.get(msgId);

	if (msg == null) { // message has been dropped from the buffer after..
            return; // ..start of transfer -> no need to reduce amount of copies
	}
		
	/* reduce the amount of copies left */
	Integer nrofCopies = (Integer)msg.getProperty(MSG_COUNT_PROPERTY);
	if (isBinary) { 
            nrofCopies /= 2;
	} else {
            nrofCopies--;
	}
	msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
    }
}
