/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package report;

import core.DTNHost;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Wiryanto Setya Adi, Sanata Dharma University
 */
public class ConvergenceData {

    private DTNHost source;
    private double convergenceTime;
    private Set<DTNHost> nodeList;
    private double lastNodeTime;

    public ConvergenceData() {
        convergenceTime = 0.0;
        nodeList = new HashSet<>();
        lastNodeTime = 0;
        source = null;
    }

    public double getConvergenceTime() {
        return convergenceTime;
    }

    public void setConvergenceTime(double convergenceTime) {
        this.convergenceTime = convergenceTime;
    }

    public Set<DTNHost> getNodeList() {
        return nodeList;
    }

    public void setNodeList(Set<DTNHost> nodeList) {
        this.nodeList = nodeList;
    }

    public double getLastNodeTime() {
        return lastNodeTime;
    }

    public void setLastNodeTime(double lastNodeTime) {
        this.lastNodeTime = lastNodeTime;
    }

    public DTNHost getSource() {
        return source;
    }

    public void setSource(DTNHost source) {
        this.source = source;
    }
}
