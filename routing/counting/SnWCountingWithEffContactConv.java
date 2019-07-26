package routing.counting;

import routing.*;
import routing.MessageRouter;
import core.*;
import java.util.*;
import routing.community.NodeIn;

/**
 *
 * @author Evander Juliansyah. H 
 * Universitas Sanata Dharma
 */
public class SnWCountingWithEffContactConv implements RoutingDecisionEngineLevelUp, HCInter {

    public static final String BINARY_MODE = "binaryMode";
    public static final String SPRAYANDWAIT_NS = "SnWCountingWithEffContactConv";
    public static final String MSG_COUNT_PROPERTY = SPRAYANDWAIT_NS + "."
            + "copies";
    public static final String I_ALFA = "alfa";
    public static final String INITIATOR = "initiator";
    public static final String TIME_UPDATE = "timeUpdate";
    public static final int DEFAULT_T = 1;
    public static final int DEFAULT_H = 0;
    public static final int DEFAULT_UPDATE = 0;

    private Map<Integer, Integer> newHeadcount;
    protected int token;
    protected int headCount;
    protected int nodeInitiator;
    public boolean isBinary;
    protected double nilaiAlfa;
    private int timeUpdate;
    private double init_node;
 
    public SnWCountingWithEffContactConv(Settings s) {
        if (s.contains(BINARY_MODE)) {
            isBinary = s.getBoolean(BINARY_MODE);
        } else {
            this.isBinary = false;
        }
        if (s.contains(INITIATOR)) {
            this.nodeInitiator = s.getInt(INITIATOR);
        }
        if (s.contains(I_ALFA)) {
            this.nilaiAlfa = s.getDouble(I_ALFA);
        }
        if(s.contains(TIME_UPDATE)){
            timeUpdate = s.getInt(TIME_UPDATE);
        }else{
            this.timeUpdate = DEFAULT_UPDATE;
        }
        this.token = DEFAULT_T;
        this.headCount = DEFAULT_H;
    }

    public SnWCountingWithEffContactConv(SnWCountingWithEffContactConv r) {
        this.token = r.token;
        this.isBinary = r.isBinary;
        this.nodeInitiator = r.nodeInitiator;
        this.nilaiAlfa = r.nilaiAlfa;
        this.newHeadcount = new HashMap<Integer, Integer>();
        this.headCount = r.headCount;
        this.timeUpdate = r.timeUpdate;
        this.init_node = 0;
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
        SnWCountingWithEffContactConv partner = getOtherSnFDecisionEngine(peer);
        int headMax = Math.max(partner.headCount, this.headCount);

        if (thisHost.isRadioActive() == true && peer.isRadioActive() == true) {
            effContact(thisHost, peer);
            int a = 1;
            if (SimClock.getTime() >= timeUpdate) {
                a = 2;
            }

            if (this.token == 0 && partner.token == 0) {
                partner.headCount = this.headCount = Math.max(this.headCount, partner.headCount);
            } else {
                if (this.init_node > partner.init_node) {
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
                    partner.init_node = (1 - nilaiAlfa) * partner.init_node + nilaiAlfa * this.init_node;
                    this.init_node = (1 - nilaiAlfa) * this.init_node;
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
                    this.init_node = (1 - nilaiAlfa) * this.init_node + nilaiAlfa * partner.init_node;
                    partner.init_node = (1 - nilaiAlfa) * partner.init_node;
                }
            }
            System.out.println(newHeadcount);
        }
    }

    @Override
    public boolean newMessage(Message m) {
        m.addProperty(MSG_COUNT_PROPERTY, this.headCount / 2);
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
        if (m.getTo() == otherHost) {
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
        return new SnWCountingWithEffContactConv(this);
    }

    private SnWCountingWithEffContactConv getOtherSnFDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouterLevelUp : "This router only works "
                + " with other routers of same type";

        return (SnWCountingWithEffContactConv) ((DecisionEngineRouterLevelUp) otherRouter).getDecisionEngine();
    }

    private void effContact(DTNHost thisHost, DTNHost peer) {
        SnWCountingWithEffContactConv partner = getOtherSnFDecisionEngine(peer);
        double myOldValue = this.init_node;
        double partnerOldValue = partner.init_node;

        if (thisHost.getAddress() == nodeInitiator) {
            if (partner.init_node > myOldValue) {
                this.init_node = (1 - nilaiAlfa) * myOldValue + nilaiAlfa * partner.init_node;
            } else if (partner.init_node <= myOldValue) {
                this.init_node = (1 - nilaiAlfa) * myOldValue;
            }
        } else {
            if (this.init_node > partnerOldValue) {
                partner.init_node = (1 - nilaiAlfa) * partnerOldValue + nilaiAlfa * this.init_node;
            } else if (this.init_node <= partnerOldValue) {
                partner.init_node = (1 - nilaiAlfa) * partnerOldValue;
            }
        }
    }

    @Override
    public int getCount() {
        return this.headCount;
    }

    @Override
    public int getToken() {
        return this.token;
    }
    
    @Override
    public void update(DTNHost host) {
        double time = SimClock.getTime();
        if(host.getAddress() == nodeInitiator){
            this.init_node = 1;
        }
        if (time == timeUpdate) {
            this.token = 1;
            this.headCount = this.headCount/2;
            this.newHeadcount.put(2, 0);
        }
    }

}
