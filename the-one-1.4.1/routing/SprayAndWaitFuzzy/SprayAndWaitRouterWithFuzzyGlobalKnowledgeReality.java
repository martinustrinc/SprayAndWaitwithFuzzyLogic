/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.SprayAndWaitFuzzy;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import java.util.HashMap;
import java.util.Map;
import routing.MessageRouter;

/**
 *
 * @author @author Martinus Tri Nur Cahyono, Sanata Dharma University
 */
public class SprayAndWaitRouterWithFuzzyGlobalKnowledgeReality implements RoutingDecisionEngineSnW {

    /** Set value number of Copies (L copies message) in setting */
    public static final String NROF_COPIES_S = "nrofCopies";            
    /** Set TRUE/FALSE copies message in setting */
    public static final String MSG_COUNT_PROPERTY = "copies";           
    /** Set TRUE/FALSE fuzzy mode running with Spray and Wait router in setting */
    public static final String FUZZY_MODE = "fuzzyMode";                   
    
    /**
     * For setting the fuzzy input set
     */
    /** The output value of the fuzzy set, 25 degree of membership function fuzzy of ToU */
    protected static final double ToU_MF1 = 1, ToU_MF2 = 2,
            ToU_MF3 = 3, ToU_MF4 = 4, ToU_MF5 = 5,
            ToU_MF6 = 6, ToU_MF7 = 7, ToU_MF8 = 8, 
            ToU_MF9 = 9, ToU_MF10 = 10, ToU_MF11 = 11, 
            ToU_MF12 = 12, ToU_MF13 = 13, ToU_MF14 = 14, 
            ToU_MF15 = 15, ToU_MF16 = 16, ToU_MF17 = 17, 
            ToU_MF18 = 18, ToU_MF19 = 19, ToU_MF20 = 20,
            ToU_MF21 = 21,ToU_MF22 = 22,ToU_MF23 = 23,
            ToU_MF24 = 24, ToU_MF25 = 25;
    
    /** variable to save value of number of copy message */
    protected int initialNrofCopies;
    /** variable to save value of isFuzzy (TRUE/FALSE fuzzy mode) */
    protected boolean isFuzzy;
    
    /** variable to save value of meetings per node */
    protected Map<DTNHost, Double> meetings;
    /** variable to save value of disconnects per node */
    protected Map<DTNHost, Double> disconnects;
    /**
     * variable Contact Time Duration to save duration contact 
     */
    protected Map<DTNHost, Double> CTD;
    /**
     * variable Intercontact Time Duration to save interval non duration contact
     */
    protected Map<DTNHost, Double> ITD;

    /**
     * The fuzzy value from defuzzification / the fuzzy output for this scheme called 
     * Transfer of Utility (ToU)
     */
    protected Map<DTNHost, Double> ToU;

    public SprayAndWaitRouterWithFuzzyGlobalKnowledgeReality(Settings s) {
        initialNrofCopies = s.getInt(NROF_COPIES_S);
        isFuzzy = s.getBoolean(FUZZY_MODE);
        meetings = new HashMap<>();
        disconnects = new HashMap<>();
        CTD = new HashMap<>();
        ITD = new HashMap<>();
        ToU = new HashMap<>();
    }

    public SprayAndWaitRouterWithFuzzyGlobalKnowledgeReality(SprayAndWaitRouterWithFuzzyGlobalKnowledgeReality prototype) {
        this.initialNrofCopies = prototype.initialNrofCopies;
        this.isFuzzy = prototype.isFuzzy;
        meetings = new HashMap<>();
        disconnects = new HashMap<>();
        CTD = new HashMap<>();
        ITD = new HashMap<>();
        ToU = new HashMap<>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        /** variable to save value of this time when connection is connected */
        double thisTime = SimClock.getTime();
        
        /** variable to save value of interval of contact duration with node peer */
        double timeDiff = thisTime - getDisconnectFor(peer);
        /** put interval of contact duration with node peer to summary vector NENT with (key = peer, value = timeDiff) */
        this.ITD.put(peer, timeDiff);
        /** put this time duration with node peer to summary vector meetings with (key = peer, value = thisTime)*/
        this.meetings.put(peer, thisTime);
        
        /** Compute the defuzzification peer to thisHost */
        this.computeDefuzzificationFor(peer);
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
    }
    
    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        /** variable to save value of this time when connection is disconnected */
        double thisTime = SimClock.getTime();

        /** variable to save value of interval of contact duration with node peer */
        double timeDifferent = thisTime - getMeetingFor(peer);
        /** put this time duration with node peer to summary vector ENT */
        this.CTD.put(peer, timeDifferent);
        /** put this time duration with node peer to summary vector disconnects */
        this.disconnects.put(peer, thisTime);
    }

    @Override
    public boolean newMessage(Message m) {
        m.addProperty(MSG_COUNT_PROPERTY, initialNrofCopies);
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost thisHost) {
        return m.getTo() == thisHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) { //receiver
//        return m.getTo() != thisHost;
        return true;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) { //sender
        /** make object peer to define pointer node peer */
        SprayAndWaitRouterWithFuzzyGlobalKnowledgeReality peer = getOtherFuzzyRouter(otherHost);
        /** variable to save value of L copies */
        Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
        /* check if destionation is same with node peer (node in connection) */
        if (m.getTo() == otherHost) {
            /* return true to send the copy message */
            return true;
        }
        
        /** variable to save value of sumOfTheToU, 
         * sumOfTheToU is sum of ToU value node thisHost to destination with ToU value node peer to destination */
        double sumOfTheToU = this.getToUValueFor(m.getTo()) + peer.getToUValueFor(m.getTo());
        
        /* check if nrofCopies more than 1 */
        if (nrofCopies > 1) {
            /* check if ToU thisHost to destination less than ToU peer to destionation */
                nrofCopies = (int) Math.floor((peer.getToUValueFor(m.getTo()) / sumOfTheToU)*nrofCopies); //pembulatan ke atas
                /** update L copies message */
                m.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
                /* return true to send the copy message */
                return true;
        }
        
        return false;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        /** make object peer to define pointer node peer */
        SprayAndWaitRouterWithFuzzyGlobalKnowledgeReality peer = getOtherFuzzyRouter(otherHost);
        /** variable to save value of L copies */
        Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
        /** variable to save value of sumOfTheToU, 
         * sumOfTheToU is sum of ToU value node thisHost to destination with ToU value node peer to destination */
        double sumOfTheToU = this.getToUValueFor(m.getTo()) + peer.getToUValueFor(m.getTo());
        
        /* check if nrofCopies more than 1 */
        if (nrofCopies > 1) {
                nrofCopies = (int) Math.floor((peer.getToUValueFor(m.getTo()) / sumOfTheToU) * nrofCopies);
                /** update L copies message */
                m.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
        }

        return false;
     }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return m.getTo() != hostReportingOld;
    }

    @Override
    public RoutingDecisionEngineSnW replicate() {
        return new SprayAndWaitRouterWithFuzzyGlobalKnowledgeReality(this);
    }
    
    private SprayAndWaitRouterWithFuzzyGlobalKnowledgeReality getOtherFuzzyRouter(DTNHost host) {
        MessageRouter otherRouter = host.getRouter();
        assert otherRouter instanceof DecisionEngineRouterSnW : "This router only works "
                + " with other routers of same type";

        return (SprayAndWaitRouterWithFuzzyGlobalKnowledgeReality) ((DecisionEngineRouterSnW) otherRouter).getDecisionEngine();
    }
    
    private void computeDefuzzificationFor(DTNHost host) {
        /** variable to save value of defuzzification */
        double defuzzification = ((degreeOfMembership25Function(host) * 25 + degreeOfMembership24Function(host)* 24 +
                degreeOfMembership23Function(host) *23 + degreeOfMembership22Funciton(host) *22 +
                degreeOfMembership21Funciton(host) *21 + degreeOfMembership20Funciton(host) *20 +
                degreeOfMembership19Funciton(host) *19 + degreeOfMembership18Funciton(host) *18 +
                degreeOfMembership17Funciton(host) *17 + degreeOfMembership16Funciton(host) *16 +
                degreeOfMembership15Funciton(host) *15 + degreeOfMembership14Funciton(host) *14 +
                degreeOfMembership13Funciton(host) *13 + degreeOfMembership12Funciton(host) *12 +
                degreeOfMembership11Funciton(host) *11 + degreeOfMembership10Funciton(host) *10 +
                degreeOfMembership9Funciton(host) *9 + degreeOfMembership8Funciton(host) *8 +
                degreeOfMembership7Funciton(host) *7 + degreeOfMembership6Funciton(host) *6 +
                degreeOfMembership5Funciton(host) *5 + degreeOfMembership4Funciton(host) *4 +
                degreeOfMembership3Funciton(host) *3 + degreeOfMembership2Funciton(host) *2 +
                degreeOfMembership1Funciton(host) *1)
                / sumOfTheDegreeOfMembershipFor(host));
        /** put (key = host, value = defuzzification) to ToU */
        ToU.put(host, defuzzification);
    }

    private double sumOfTheDegreeOfMembershipFor(DTNHost host) {
        /* return sum of value degreeOfMembership all level or membership function */
        return (degreeOfMembership25Function(host) + degreeOfMembership24Function(host) +
                degreeOfMembership23Function(host) + degreeOfMembership22Funciton(host) +
                degreeOfMembership21Funciton(host) + degreeOfMembership20Funciton(host) +
                degreeOfMembership19Funciton(host) + degreeOfMembership18Funciton(host) +
                degreeOfMembership17Funciton(host) + degreeOfMembership16Funciton(host) +
                degreeOfMembership15Funciton(host) + degreeOfMembership14Funciton(host) +
                degreeOfMembership13Funciton(host) + degreeOfMembership12Funciton(host) +
                degreeOfMembership11Funciton(host) + degreeOfMembership10Funciton(host) +
                degreeOfMembership9Funciton(host) + degreeOfMembership8Funciton(host) +
                degreeOfMembership7Funciton(host) + degreeOfMembership6Funciton(host) +
                degreeOfMembership5Funciton(host) + degreeOfMembership4Funciton(host) +
                degreeOfMembership3Funciton(host) + degreeOfMembership2Funciton(host) +
                degreeOfMembership1Funciton(host));
    }

    private double getMeetingFor(DTNHost host){
        /* check if in summary vector meetings have key node host */
        if (meetings.containsKey(host)) {
            /* return value summary vector meetings get/take node host */
            return meetings.get(host);
        } else {
            /* return value 0, because because it is assumed when a node has never met */
            return 0;
        }
    }
    private double getDisconnectFor(DTNHost host){
        /* check if in summary vector disconnects have key node host */
        if (disconnects.containsKey(host)) {
            /* return value summary vector disconnects get/take node host */
            return disconnects.get(host);
        } else {
            /* return value 0, because because it is assumed when a node has never met */
            return 0;
        }
    }
    
    private double getCTDFor(DTNHost host){
        /* check if in summary vector ENT have key node host */
        if (CTD.containsKey(host)) {
            /* return value summary vector ENT get/take node host */
            return CTD.get(host);
        } else {
            /* return value 0, because because it is assumed when a node has never met */
            return 0;
        }
    }
    private double getITDFor(DTNHost host){
        /* check if in summary vector NENT have key node host */
        if (ITD.containsKey(host)) {
            /* return value summary vector NENT get/take node host */
            return ITD.get(host);
        } else {
            /* return value 0, because because it is assumed when a node has never met */
            return 0;
        }
    }
    private double getToUValueFor(DTNHost host) {
        /* check if in summary vector ToU have key node host */
        if (ToU.containsKey(host)) {
            /* return value summary vector ToU get/take node host */
            return ToU.get(host);
        } else {
            /* return value 0, because because it is assumed when a node has never met */
            return 0;
        }
    }

    private double contactTimeDuration_VeryLowFunciton(DTNHost host) {
        if (getCTDFor(host) <= 100) return 1.0;
        else if (100 < getCTDFor(host) && getCTDFor(host) < 200) 
            return (200 - getCTDFor(host)) / (200 - 100); 
        else return 0;
    }
    private double contactTimeDuration_LowFunciton(DTNHost host) {
        if (100 < getCTDFor(host) && getCTDFor(host) < 200)
            return (getCTDFor(host) - 100)/(200 - 100);
        else if (400 < getCTDFor(host) && getCTDFor(host) < 500)
            return (500 - getCTDFor(host))/(500 - 400);
        else if (200 <= getCTDFor(host) && getCTDFor(host) <= 400) 
            return 1.0;
        else return 0;
    }
    private double contactTimeDuration_MediumFunciton(DTNHost host) {
        if (400 < getCTDFor(host) && getCTDFor(host) < 500) 
            return (getCTDFor(host) - 400)/(500 - 400);
        else if (1100 < getCTDFor(host) && getCTDFor(host) < 1200)
            return (1200 - getCTDFor(host))/(1200 - 1100);
        else if (500 <= getCTDFor(host) && getCTDFor(host) <= 1100) 
            return 1.0;
        else return 0;
    }
    private double contactTimeDuration_HighFunciton(DTNHost host) {
        if (1100 < getCTDFor(host) && getCTDFor(host) < 1200)
            return (getCTDFor(host) - 1100)/(1200 - 1100);
        else if (1500 < getCTDFor(host) && getCTDFor(host) < 1600)
            return (1600 - getCTDFor(host))/(1600 - 1500);
        else if (1200 <= getCTDFor(host) && getCTDFor(host) <= 1500) 
            return 1.0;
        else return 0;
    }
    private double contactTimeDuration_VeryHighFunciton(DTNHost host) {
        if (1500 < getCTDFor(host) && getCTDFor(host) < 1600) 
            return (getCTDFor(host) - 1500)/(1600 - 1500);
        else if (getCTDFor(host) >= 1600) return 1;
        else return 0;
    }

    private double IntercontactTimeDuration_VeryLowFunciton(DTNHost host) {
        if (getITDFor(host) <= 200000) return 1.0;
        else if (200000 < getITDFor(host) && getITDFor(host) < 300000)
            return (300000 - getITDFor(host)) / (300000 - 200000); 
        else return 0;
    }
    private double IntercontactTimeDuration_LowFunciton(DTNHost host) {
        if (200000 < getITDFor(host) && getITDFor(host) < 300000)
            return (getITDFor(host) - 200000)/(300000 - 200000);
        else if (500000 < getITDFor(host) && getITDFor(host) < 600000) 
            return (600000 - getITDFor(host))/(600000 - 500000);
        else if (300000 <= getITDFor(host) && getITDFor(host) <= 500000) 
            return 1.0;
        else return 0;
    }
    private double IntercontactTimeDuration_MediumFunciton(DTNHost host) {
        if (500000 < getITDFor(host) && getITDFor(host) < 600000)
            return (getITDFor(host) - 500000)/(600000 - 500000);
        else if (1200000 < getITDFor(host) && getITDFor(host) < 1300000) 
            return (1300000 - getITDFor(host))/(1300000 - 1200000);
        else if (600000 <= getITDFor(host) && getITDFor(host) <= 1200000) 
            return 1.0;
        else return 0;
    }
    private double IntercontactTimeDuration_HighFunciton(DTNHost host) {
        if (1200000 < getITDFor(host) && getITDFor(host) < 1300000)
            return (getITDFor(host) - 1200000)/(1300000 - 1200000);
        else if (1500000 < getITDFor(host) && getITDFor(host) < 1600000) 
            return (1600000 - getITDFor(host))/(1600000 - 1500000);
        else if (1300000 <= getITDFor(host) && getITDFor(host) <= 1500000) 
            return 1.0;
        else return 0;
    }
    private double IntercontactTimeDuration_VeryHighFunciton(DTNHost host) {
        if (1500000 < getITDFor(host) && getITDFor(host) < 1600000)
            return (getITDFor(host) - 1600000)/(1600000 - 1500000);
        else if (getITDFor(host) >= 1600000) 
            return 1;
        else return 0;
    }

    private double degreeOfMembership25Function(DTNHost host) {
        /* return value logical operation AND rule mf number 25 (AND = Math.min) */
        return Math.min(contactTimeDuration_VeryHighFunciton(host), IntercontactTimeDuration_VeryLowFunciton(host));//25
    }
    private double degreeOfMembership24Function(DTNHost host) {
        return Math.min(contactTimeDuration_VeryHighFunciton(host), IntercontactTimeDuration_LowFunciton(host));//24
    }
    private double degreeOfMembership23Function(DTNHost host) {
        return Math.min(contactTimeDuration_VeryHighFunciton(host), IntercontactTimeDuration_MediumFunciton(host));//23
    }
    private double degreeOfMembership22Funciton(DTNHost host) {
        return Math.min(contactTimeDuration_VeryHighFunciton(host), IntercontactTimeDuration_HighFunciton(host));//22
    }
    private double degreeOfMembership21Funciton(DTNHost host) {
        return Math.min(contactTimeDuration_VeryHighFunciton(host), IntercontactTimeDuration_VeryHighFunciton(host));//21
    }
    private double degreeOfMembership20Funciton(DTNHost host) {
        return Math.min(contactTimeDuration_HighFunciton(host), IntercontactTimeDuration_VeryLowFunciton(host));//20
    }
    private double degreeOfMembership19Funciton(DTNHost host) {
        return Math.min(contactTimeDuration_HighFunciton(host), IntercontactTimeDuration_LowFunciton(host));//19
    }
    private double degreeOfMembership18Funciton(DTNHost host) {
        return Math.min(contactTimeDuration_HighFunciton(host), IntercontactTimeDuration_MediumFunciton(host));//18
    }
    private double degreeOfMembership17Funciton(DTNHost host) {
        return Math.min(contactTimeDuration_HighFunciton(host), IntercontactTimeDuration_HighFunciton(host));//17
    }
    private double degreeOfMembership16Funciton(DTNHost host) {
        return Math.min(contactTimeDuration_HighFunciton(host), IntercontactTimeDuration_VeryHighFunciton(host));//16
    }
    private double degreeOfMembership15Funciton(DTNHost host) {
        return Math.min(contactTimeDuration_MediumFunciton(host), IntercontactTimeDuration_VeryLowFunciton(host));//15
    }
    private double degreeOfMembership14Funciton(DTNHost host) {
        return Math.min(contactTimeDuration_MediumFunciton(host), IntercontactTimeDuration_LowFunciton(host));//14
    }
    private double degreeOfMembership13Funciton(DTNHost host) {
        return Math.min(contactTimeDuration_MediumFunciton(host), IntercontactTimeDuration_MediumFunciton(host));//13
    }
    private double degreeOfMembership12Funciton(DTNHost host) {
        return Math.min(contactTimeDuration_MediumFunciton(host), IntercontactTimeDuration_HighFunciton(host));//12
    }
    private double degreeOfMembership11Funciton(DTNHost host) {
        return Math.min(contactTimeDuration_MediumFunciton(host), IntercontactTimeDuration_VeryHighFunciton(host));//11
    }
    private double degreeOfMembership10Funciton(DTNHost host) {
        return Math.min(contactTimeDuration_LowFunciton(host), IntercontactTimeDuration_VeryLowFunciton(host));//10
    }
    private double degreeOfMembership9Funciton(DTNHost host) {
        return Math.min(contactTimeDuration_LowFunciton(host), IntercontactTimeDuration_LowFunciton(host));//9
    }
    private double degreeOfMembership8Funciton(DTNHost host) {
        return Math.min(contactTimeDuration_LowFunciton(host), IntercontactTimeDuration_MediumFunciton(host));//8
    }
    private double degreeOfMembership7Funciton(DTNHost host) {
        return Math.min(contactTimeDuration_LowFunciton(host), IntercontactTimeDuration_HighFunciton(host));//7
    }
    private double degreeOfMembership6Funciton(DTNHost host) {
        return Math.min(contactTimeDuration_LowFunciton(host), IntercontactTimeDuration_VeryHighFunciton(host));//6
    }
    private double degreeOfMembership5Funciton(DTNHost host) {
        return Math.min(contactTimeDuration_VeryLowFunciton(host), IntercontactTimeDuration_VeryLowFunciton(host));//5
    }
    private double degreeOfMembership4Funciton(DTNHost host) {
        return Math.min(contactTimeDuration_VeryLowFunciton(host), IntercontactTimeDuration_LowFunciton(host));//4
    }
    private double degreeOfMembership3Funciton(DTNHost host) {
        return Math.min(contactTimeDuration_VeryLowFunciton(host), IntercontactTimeDuration_MediumFunciton(host));//3
    }
    private double degreeOfMembership2Funciton(DTNHost host) {
        return Math.min(contactTimeDuration_VeryLowFunciton(host), IntercontactTimeDuration_HighFunciton(host));//2
    }
    private double degreeOfMembership1Funciton(DTNHost host) {
        return Math.min(contactTimeDuration_VeryLowFunciton(host), IntercontactTimeDuration_VeryHighFunciton(host));//1
    }
}
