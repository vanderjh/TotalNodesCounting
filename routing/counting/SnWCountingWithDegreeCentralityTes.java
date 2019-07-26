package routing.counting;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import routing.*;
import routing.community.Centrality;
import routing.community.DegreeCentrality;
import routing.community.Duration;

/**
 *
 * @author Evander Juliansyah. H 
 * Universitas Sanata Dharma
 */
public class SnWCountingWithDegreeCentralityTes implements RoutingDecisionEngineLevelUp, HCInter {

    public static final String BINARY_MODE = "binaryMode";
    public static final String SPRAYANDWAIT_NS = "SnWCountingWithDegreeCentrality";
    public static final String MSG_COUNT_PROPERTY = SPRAYANDWAIT_NS + "."
            + "copies";

    public static final String CENTRALITY_ALG_SETTING = "centralityAlg";
    public static final String TIME_UPDATE = "timeUpdate";
    public static final String RUNNING = "running";
    public static final int DEFAULT_UPDATE = 0;
    public static final int DEFAULT_T = 1;
    public static final int DEFAULT_H = 0;

    protected Map<DTNHost, Double> startTimestamps;
    protected Map<DTNHost, List<Duration>> connHistory;
//    private Map<Integer, Integer> newHeadcount;

    protected int token;
    protected int headCount;
    protected boolean isBinary;
    private int timeUpdate;
    private int run;

    protected Centrality centrality;

    public SnWCountingWithDegreeCentralityTes(Settings s) {
        if (s.contains(BINARY_MODE)) {
            isBinary = s.getBoolean(BINARY_MODE);
        } else {
            this.isBinary = false;
        }
        if(s.contains(TIME_UPDATE)){
            timeUpdate = s.getInt(TIME_UPDATE);
        }else{
            this.timeUpdate = DEFAULT_UPDATE;
        }
        if (s.contains(RUNNING)) {
            run = s.getInt(RUNNING);
        }
        if (s.contains(CENTRALITY_ALG_SETTING)) {
            this.centrality = (Centrality) s.createIntializedObject(s.getSetting(CENTRALITY_ALG_SETTING));
        } else {
            this.centrality = new DegreeCentrality(s);
        }
        this.token = DEFAULT_T;
        this.headCount = DEFAULT_H;
    }

    public SnWCountingWithDegreeCentralityTes(SnWCountingWithDegreeCentralityTes proto) {
        this.isBinary = proto.isBinary;
        this.run = proto.run;
        this.token = proto.token;
        this.headCount = proto.headCount;
        this.timeUpdate = proto.timeUpdate;
        this.centrality = proto.centrality.replicate();
        connHistory = new HashMap<DTNHost, List<Duration>>();
        startTimestamps = new HashMap<DTNHost, Double>();
//        newHeadcount = new HashMap<Integer, Integer>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        double time = startTimestamps.get(peer);
        double etime = SimClock.getTime();

        // Find or create the connection history list
        List<Duration> history;
        if (!connHistory.containsKey(peer)) {
            history = new LinkedList<Duration>();
            connHistory.put(peer, history);
        } else {
            history = connHistory.get(peer);
        }

        // add this connection to the list
        if (etime - time > 0) {
            history.add(new Duration(time, etime));
        }
        startTimestamps.remove(peer);
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost thisHost = con.getOtherNode(peer);
        SnWCountingWithDegreeCentralityTes de = getOtherSnFDecisionEngine(peer);
        
        if (thisHost.isRadioActive() == true && peer.isRadioActive() == true) {
            int headMax = Math.max(this.headCount, de.headCount);
            
            int maxHead = Math.max(this.headCount, de.headCount);

        if (this.token == 0 && de.token == 0) {
            de.headCount = this.headCount = Math.max(this.headCount, de.headCount);
        } else {
            if (this.getGlobalCentrality() > de.getGlobalCentrality()) {
                this.token = this.token + de.token;
                de.token = 0;
                if (maxHead > this.token) {
                    de.headCount = this.headCount = maxHead;
                } else {
                    de.headCount = this.headCount = this.token;
                }
            } else {
                de.token = de.token + this.token;
                this.token = 0;
                if (maxHead > de.token) {
                    this.headCount = de.headCount = maxHead;
                } else {
                    this.headCount = de.headCount = de.token;
                }
            }
        }
//        System.out.println(getResidu());
        this.startTimestamps.put(peer, SimClock.getTime());
        de.startTimestamps.put(thisHost, SimClock.getTime());
        }
    }
        

    @Override
    public boolean newMessage(Message m) {
        m.addProperty(MSG_COUNT_PROPERTY, copyPesan()); //untuk menentukan copy
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
        if (m.getTo() == otherHost) {
            return true;
        }
        Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
        if (nrofCopies > 1) {
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
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

    private SnWCountingWithDegreeCentralityTes getOtherSnFDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouterLevelUp : "This router only works "
                + " with other routers of same type";

        return (SnWCountingWithDegreeCentralityTes) ((DecisionEngineRouterLevelUp) otherRouter).getDecisionEngine();
    }

    private double getGlobalCentrality() { //Mengambil nilai degree centrality nya
        return this.centrality.getGlobalCentrality(this.connHistory);
    }

    @Override
    public RoutingDecisionEngineLevelUp replicate() {
        return new SnWCountingWithDegreeCentralityTes(this);
    }

    @Override
    public int getToken() {
        return this.token;
    }

    @Override
    public void update(DTNHost host) {
        double time = SimClock.getTime();
        if(host.isRadioActive() == true || host.isRadioActive() == false){
        if (time == timeUpdate) {
            this.token = 1;
//            this.headCount = this.headCount/2;
            this.headCount = 0;
//            this.newHeadcount.put(2, 0);
//        System.out.println(host+" "+this.headCount);
        }
        }
    }

    @Override
    public int getCount() {
        return this.headCount;
    }
    private int copyPesan(){
        int lCopy;
//        if(this.headCount == 0){
//            return lCopy = 5;
//        }else
            return lCopy = this.headCount/2;
    }

//    @Override
//    public Map getResidu() {
////        return newHeadcount;
//    }
}
