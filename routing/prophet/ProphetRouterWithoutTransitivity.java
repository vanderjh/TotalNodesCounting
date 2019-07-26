/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.prophet;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

/**
 *
 * @author ASUS
 */
public class ProphetRouterWithoutTransitivity implements RoutingDecisionEngine{
    protected final static String BETA_SETTING = "beta";
    protected final static String P_INIT_SETTING = "initial_p";
    protected final static String SECONDS_IN_UNIT_S = "secondsInTimeUnit";

    protected static final double DEFAULT_P_INIT = 0.75;
    protected static final double GAMMA = 0.92;
    protected static final double DEFAULT_BETA = 0;
    protected static final int DEFAULT_UNIT = 30;

    protected static final double P_WITHOUT_TRANSITIVITY1 = 0;
    protected static final double P_WITHOUT_TRANSITIVITY2 = 0.5;
    protected static final double P_WITHOUT_TRANSITIVITY3 = 1.0;

    protected static final double P_WITH_TRANSITIVITY1 = 0;
    protected static final double P_WITH_TRANSITIVITY2 = 0.5;
    protected static final double P_WITH_TRANSITIVITY3 = 1.0;

    protected double beta;
    protected double pinit;
    protected double lastAgeUpdate;
    protected int secondsInTimeUnit;

    /**
     * delivery predictabilities
     */
    private Map<DTNHost, Double> preds;

    public ProphetRouterWithoutTransitivity(Settings s) {
        if (s.contains(BETA_SETTING)) {
            beta = s.getDouble(BETA_SETTING);
        } else {
            beta = DEFAULT_BETA;
        }

        if (s.contains(P_INIT_SETTING)) {
            pinit = s.getDouble(P_INIT_SETTING);
        } else {
            pinit = DEFAULT_P_INIT;
        }

        if (s.contains(SECONDS_IN_UNIT_S)) {
            secondsInTimeUnit = s.getInt(SECONDS_IN_UNIT_S);
        } else {
            secondsInTimeUnit = DEFAULT_UNIT;
        }

        preds = new HashMap<DTNHost, Double>();
        this.lastAgeUpdate = 0.0;
    }

    public ProphetRouterWithoutTransitivity(ProphetRouterWithoutTransitivity de) {
        beta = de.beta;
        pinit = de.pinit;
        secondsInTimeUnit = de.secondsInTimeUnit;
        preds = new HashMap<DTNHost, Double>();
        this.lastAgeUpdate = de.lastAgeUpdate;
    }

    public RoutingDecisionEngine replicate() {
        return new ProphetRouterWithoutTransitivity(this);
    }

    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }

    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        ProphetRouterWithoutTransitivity myPartner = getOtherProphetDecisionEngine(peer);
        this.updateDeliveryPredFor(peer, con);
        myPartner.updateDeliveryPredFor(myHost, con);
        
        
        
    }

    public boolean newMessage(Message m) {
        return true;
    }

    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return m.getTo() != thisHost;
    }

    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
        if (m.getTo() == otherHost) {
            return true;
        }

        ProphetRouterWithoutTransitivity de = getOtherProphetDecisionEngine(otherHost);

        return de.getPredFor(m.getTo()) > this.getPredFor(m.getTo());
    }

    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }

    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return m.getTo() == hostReportingOld;
    }

    private ProphetRouterWithoutTransitivity getOtherProphetDecisionEngine(DTNHost host) {
        MessageRouter otherRouter = host.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (ProphetRouterWithoutTransitivity) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    private void updateDeliveryPredFor(DTNHost host, Connection con) {
        DTNHost myHost = con.getOtherNode(host);
        ProphetRouterWithoutTransitivity de = getOtherProphetDecisionEngine(host);

        this.agePreds();
        de.agePreds();

        double myOldValue = this.getPredFor(host),
                peerOldValue = de.getPredFor(myHost),
                myPforHost = myOldValue + (1 - myOldValue) * pinit,
                peerPforMe = peerOldValue + (1 - peerOldValue) * de.pinit;
        preds.put(host, myPforHost);
        de.preds.put(myHost, peerPforMe);
    }

    
    
    private void agePreds() {
        double timeDiff = (SimClock.getTime() - this.lastAgeUpdate)
                / secondsInTimeUnit;

        if (timeDiff == 0) {
            return;
        }

        double mult = Math.pow(GAMMA, timeDiff);
        for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
            e.setValue(e.getValue() * mult);
        }

        this.lastAgeUpdate = SimClock.getTime();
    }

    /**
     * Returns the current prediction (P) value for a host or 0 if entry for the
     * host doesn't exist.
     *
     * @param host The host to look the P for
     * @return the current P value
     */
    private double getPredFor(DTNHost host) {
        agePreds(); // make sure preds are updated before getting
        if (preds.containsKey(host)) {
            return preds.get(host);
        } else {
            return 0;
        }
    } 
}
