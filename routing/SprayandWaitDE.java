/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing;

import routing.MessageRouter;
import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.print.Collation;
import routing.community.MarkResidu;

/**
 *
 * @author Evander Juliansyah. H 
 * Universitas Sanata Dharma
 */
public class SprayandWaitDE implements RoutingDecisionEngineLevelUp, MarkResidu{
    public static final String NROF_COPIES = "nrofCopies";
    public static final String BINARY_MODE = "binaryMode";
    public static final String SPRAYANDWAIT_NS = "SprayandWaitDE";
    public static final String MSG_COUNT_PROPERTY = SPRAYANDWAIT_NS + "."
            + "copies";
    public static final String INITIATOR = "initiator";
    public int initialNrofCopies;
    public boolean isBinary;
    protected int nodeInitiator;
    protected boolean nodeMark;
    
//    protected double initial_node;
    private int tuyul;
    private Set<String> markNode = new HashSet<String>();
    private double time = 1.0;
    
    public SprayandWaitDE(Settings s) {
        if (s.contains(BINARY_MODE)) {
            isBinary = s.getBoolean(BINARY_MODE);
        } else {
            this.isBinary = false;
        }
        if (s.contains(NROF_COPIES)) {
            initialNrofCopies = s.getInt(NROF_COPIES);
        }
        if (s.contains(INITIATOR)) {
            nodeInitiator = s.getInt(INITIATOR);
        }
        this.nodeMark = false;
        this.tuyul = 0;
    }
    public SprayandWaitDE(SprayandWaitDE r) {
        this.initialNrofCopies = r.initialNrofCopies;
        this.isBinary = r.isBinary;
        this.nodeInitiator = r.nodeInitiator;
        this.nodeMark = r.nodeMark;
        this.tuyul = r.tuyul;
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
//        if (thisHost.getAddress() == nodeInitiator && this.tuyul == 0) {
//            this.nodeMark = true;
//            this.tuyul = 1;
//        }
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
    }

    @Override
    public boolean newMessage(Message m) {
        if(this.nodeMark == true){
        m.addProperty(MSG_COUNT_PROPERTY, initialNrofCopies);
        return true;
        }
        return false;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
            if (isBinary) {
                nrofCopies = (int) Math.ceil(nrofCopies / 2.0);
            } else {
                nrofCopies = 1;
            }
        m.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
        return m.getTo() != thisHost;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
        DTNHost thisHost = null;
        List<DTNHost> listHop = m.getHops();
        Iterator it = listHop.iterator();
        while(it.hasNext()){
            thisHost = (DTNHost) it.next();
        }
        SprayandWaitDE de = getOtherSnFDecisionEngine(otherHost);
        if (m.getTo() == otherHost) {
            return true;
        }
        Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
        if (nrofCopies > 1) {
            if(thisHost.getAddress() == nodeInitiator){
                //do mark
                de.tuyul = 1;
            }
//            if(this.nodeMark == true){
//                de.tuyul = 1;
//                //do mark
////                de.markNode.add("mark");
//            }
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        if(m.getTo() == otherHost){
            return false;
        }
        Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
        if (isBinary) {
            nrofCopies /= 2;
        } else {
            nrofCopies--;
        }
        m.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return m.getTo() == hostReportingOld;
    }

    @Override
    public RoutingDecisionEngineLevelUp replicate() {
        return new SprayandWaitDE(this);
    }
    private SprayandWaitDE getOtherSnFDecisionEngine(DTNHost h)
	{
		MessageRouter otherRouter = h.getRouter();
		assert otherRouter instanceof DecisionEngineRouterLevelUp : "This router only works " + 
		" with other routers of same type";
		
		return (SprayandWaitDE) ((DecisionEngineRouterLevelUp)otherRouter).getDecisionEngine();
	}

    @Override
    public void update(DTNHost host) {
        double clock = SimClock.getTime();
        if(clock == time){
            if(host.getAddress() == nodeInitiator){
            this.nodeMark = true;
            this.tuyul = 1;
            }
        }
    }

    @Override
    public int getResidu() {
        return this.tuyul;
    }

}
