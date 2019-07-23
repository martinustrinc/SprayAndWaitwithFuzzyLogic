/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing;

import core.Connection;
import core.DTNHost;

/**
 *
 * @author Gregorius Bima, Sanata Dharma Univeristy
 */
public interface RoutingDecisionEngineImproved extends RoutingDecisionEngine{
    
    public void update(DTNHost host);
    
    public void transferDone(Connection con);
}
