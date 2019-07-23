/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package report;

import core.ConnectionListener;
import core.DTNHost;

/**
 *
 * @author Gregorius Bima, Sanata Dharma University
 */
public class EncounterValueConvergenceReport extends Report implements ConnectionListener{
    
    public EncounterValueConvergenceReport(){
        init();
    }

    public void done(){
        
    }
    
    
    @Override
    public void hostsConnected(DTNHost host1, DTNHost host2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void hostsDisconnected(DTNHost host1, DTNHost host2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
