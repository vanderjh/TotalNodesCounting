package routing.counting;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import java.util.HashMap;
import java.util.Map;
import routing.*;

/**
 *
 * @author Evander Juliansyah. H 
 * Universitas Sanata Dharma
 */
public class SnWCountingWithRandomCollector implements RoutingDecisionEngineLevelUp, HCInter, ResiduInterface {

    public static final String NROF_COPIES = "nrofCopies";
    public static final String BINARY_MODE = "binaryMode";
    public static final String SPRAYANDWAIT_NS = "SnWCountingWithRandomCollector";
    public static final String MSG_COUNT_PROPERTY = SPRAYANDWAIT_NS + "."
            + "copies";

    public static final String TIME_UPDATE = "timeUpdate";
    public static final String RUNNING = "running";
    public static final int DEFAULT_UPDATE = 0;
    public static final int DEFAULT_T = 1;

    private Map<Integer, Integer> newHeadcount;

    public int initialNrofCopies;
    protected int token;
    protected int headCount;
    protected boolean isBinary;
    private int timeUpdate;
    private int run;

    public SnWCountingWithRandomCollector(Settings s) {
        if (s.contains(BINARY_MODE)) {
            isBinary = s.getBoolean(BINARY_MODE);
        } else {
            this.isBinary = false;
        }
        if (s.contains(NROF_COPIES)) {
            initialNrofCopies = s.getInt(NROF_COPIES);
        }
        if(s.contains(TIME_UPDATE)){
            timeUpdate = s.getInt(TIME_UPDATE);
        }else{
            this.timeUpdate = DEFAULT_UPDATE;
        }
        if(s.contains(RUNNING)){
            this.run = s.getInt(RUNNING);
        }
        this.token = DEFAULT_T;
    }

    public SnWCountingWithRandomCollector(SnWCountingWithRandomCollector proto) {
        this.isBinary = proto.isBinary;
        this.token = proto.token;
        this.initialNrofCopies = proto.initialNrofCopies;
        this.timeUpdate = proto.timeUpdate;
        newHeadcount = new HashMap<Integer, Integer>();
        this.headCount = 0;
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost thisHost = con.getOtherNode(peer);
        SnWCountingWithRandomCollector partner = getOtherSnFDecisionEngine(peer);
        
        double b = Math.random();
        double c = Math.random();
        
            int a = 1;
            if (SimClock.getTime() >= timeUpdate) {
                a = 2;
            }

        if (thisHost.isRadioActive() == true && peer.isRadioActive() == true) {
            int headMax = Math.max(this.headCount, partner.headCount);
            
            if (this.token == 0 && partner.token == 0) {
                partner.headCount = this.headCount = Math.max(this.headCount, partner.headCount);
            } else {
                if (b > c) {
                    this.token = this.token + partner.token;
                    partner.token = 0;
                    if (this.newHeadcount.size() == 2 && partner.newHeadcount.size() == 2) {
                        if (Math.max(this.newHeadcount.get(2), partner.newHeadcount.get(2)) > this.token) {
                            this.newHeadcount.replace(a, Math.max(this.newHeadcount.get(2), partner.newHeadcount.get(2)));
                            partner.newHeadcount.replace(a, Math.max(this.newHeadcount.get(2), partner.newHeadcount.get(2)));
                            this.headCount = partner.headCount = Math.max(this.newHeadcount.get(2), partner.newHeadcount.get(2));
                        } else {
                            this.newHeadcount.replace(a, this.token);
                            partner.newHeadcount.replace(a, this.token);
                            this.headCount = partner.headCount = this.token;
                        }
                    } else {
                        if (headMax > this.token) {
                            this.newHeadcount.put(a, headMax);
                            partner.newHeadcount.put(a, headMax);
                            partner.headCount = this.headCount = headMax;
                        } else {
                            this.newHeadcount.put(a, this.token);
                            partner.newHeadcount.put(a, this.token);
                            partner.headCount = this.headCount = this.token;
                        }
                    }
                } else {
                    partner.token = partner.token + this.token;
                    this.token = 0;
                    if (this.newHeadcount.size() == 2 && partner.newHeadcount.size() == 2) {
                        if (Math.max(this.newHeadcount.get(a), partner.newHeadcount.get(a)) > partner.token) {
                            this.newHeadcount.replace(a, Math.max(this.newHeadcount.get(a), partner.newHeadcount.get(a)));
                            partner.newHeadcount.replace(a, Math.max(this.newHeadcount.get(a), partner.newHeadcount.get(a)));
                            this.headCount = partner.headCount = partner.newHeadcount.get(a);
                        } else {
                            this.newHeadcount.replace(a, partner.token);
                            partner.newHeadcount.replace(a, partner.token);
                            this.headCount = partner.headCount = partner.token;
                        }
                    } else {
                        if (headMax > partner.token) {
                            this.newHeadcount.put(a, headMax);
                            partner.newHeadcount.put(a, headMax);
                            partner.headCount = this.headCount = headMax;
                        } else {
                            this.newHeadcount.put(a, partner.token);
                            partner.newHeadcount.put(a, partner.token);
                            partner.headCount = this.headCount = partner.token;
                        }
                    }
                }
            }
        }
//        System.out.println(a);
//        System.out.println(newHeadcount);

    }

    @Override
    public boolean newMessage(Message m) {
        m.addProperty(MSG_COUNT_PROPERTY, copyPesan()); //untuk menentukan copy
        return true;
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

    private SnWCountingWithRandomCollector getOtherSnFDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouterLevelUp : "This router only works "
                + " with other routers of same type";

        return (SnWCountingWithRandomCollector) ((DecisionEngineRouterLevelUp) otherRouter).getDecisionEngine();
    }

    @Override
    public RoutingDecisionEngineLevelUp replicate() {
        return new SnWCountingWithRandomCollector(this);
    }

    @Override
    public int getToken() {
        return this.token;
    }

    @Override
    public void update(DTNHost host) {
        double time = SimClock.getTime();
        if (time == timeUpdate) {
            this.token = 1;
            this.headCount = this.headCount/2;
            this.newHeadcount.put(2, 0);
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

    @Override
    public Map getResidu() {
        return newHeadcount;
    }
}
