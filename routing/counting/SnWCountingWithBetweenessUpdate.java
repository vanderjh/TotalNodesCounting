package routing.counting;

import routing.*;
import routing.MessageRouter;
import core.*;
import java.util.*;
import routing.community.BetweennessCentrality;
import routing.community.CentralityDetection;

/**
 *
 * @author Evander Juliansyah. H 
 * Universitas Sanata Dharma
 */
public class SnWCountingWithBetweenessUpdate implements RoutingDecisionEngineLevelUp, HCInter {

    public static final String NROF_COPIES = "nrofCopies";
    public static final String BINARY_MODE = "binaryMode";
    public static final String SPRAYANDWAIT_NS = "SnWCountingWithBetweenessUpdate";
    public static final String MSG_COUNT_PROPERTY = SPRAYANDWAIT_NS + "."
            + "copies";
    public static final String CENTRALITY_ALG_SETTING2 = "centralityAlg";

    public static final String T_SETTING = "T";
    public static final int DEFAULT_T = 1;

    protected Map<DTNHost, Set<DTNHost>> neighboursNode;
    protected Map<DTNHost, Double> startTimestamps;
    private Map<Integer, Integer> newHeadcount;
    /**
     * Map for Headcount List
     */
    protected Map<DTNHost, Integer> headCountList;
    protected double[][] matrixEgoNetwork;
    protected double betweennessCentrality;
//    protected CommunityDetection community;
    protected CentralityDetection centrality;
    protected int token;
    public int headCount;
    public boolean isBinary;
    public int initialNrofCopies;

    private int stateTime = 192600;

    public SnWCountingWithBetweenessUpdate(Settings s) {
        if (s.contains(BINARY_MODE)) {
            isBinary = s.getBoolean(BINARY_MODE);
        } else {
            this.isBinary = false;
        }
        if (s.contains(T_SETTING)) {
            token = s.getInt(T_SETTING);
        } else {
            token = DEFAULT_T;
        }
        if (s.contains(NROF_COPIES)) {
            initialNrofCopies = s.getInt(NROF_COPIES);
        }
        if (s.contains(CENTRALITY_ALG_SETTING2)) {
            this.centrality = (CentralityDetection) s.createIntializedObject(s.getSetting(CENTRALITY_ALG_SETTING2));
        } else {
            this.centrality = new BetweennessCentrality(s);
        }
    }

    public SnWCountingWithBetweenessUpdate(SnWCountingWithBetweenessUpdate r) {
        this.token = r.token;
        this.isBinary = r.isBinary;
        this.centrality = r.centrality.replicate();
        this.initialNrofCopies = r.initialNrofCopies;
        startTimestamps = new HashMap<DTNHost, Double>();
        neighboursNode = new HashMap<DTNHost, Set<DTNHost>>();
        newHeadcount = new HashMap<Integer, Integer>();
//        this.newHeadcount.put(1, 0);
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }

    protected void updateBetweenness(DTNHost myHost) {
        this.buildEgoNetwork(this.neighboursNode, myHost); // membangun ego network
        this.betweennessCentrality = this.centrality.getCentrality(this.matrixEgoNetwork); //menghitung nilai betweenness centrality
    }

    protected ArrayList<DTNHost> buildDummyArray(Map<DTNHost, Set<DTNHost>> neighbours, DTNHost myHost) {
        ArrayList<DTNHost> dummyArray = new ArrayList<>();
        dummyArray.add(myHost);
        dummyArray.addAll(neighbours.keySet());
        return dummyArray;
    }

    protected void buildEgoNetwork(Map<DTNHost, Set<DTNHost>> neighboursNode, DTNHost host) {
        ArrayList<DTNHost> dummyArray = buildDummyArray(neighboursNode, host);
        double[][] neighboursAdj = new double[dummyArray.size()][dummyArray.size()];
        for (int i = 0; i < dummyArray.size(); i++) {
            for (int j = i; j < dummyArray.size(); j++) {
                if (i == j) {
                    neighboursAdj[i][j] = 0;
                } else if (neighboursNode.get(dummyArray.get(j)).contains(dummyArray.get(i))) {
                    neighboursAdj[i][j] = 1;
                    neighboursAdj[j][i] = neighboursAdj[i][j];
                } else {
                    neighboursAdj[i][j] = 0;
                    neighboursAdj[j][i] = neighboursAdj[i][j];
                }
            }
        }
        this.matrixEgoNetwork = neighboursAdj;
    }

    private void updateBettwen(DTNHost thisHost, DTNHost peer) {
        SnWCountingWithBetweenessUpdate partner = getOtherSnWDecisionEngine(peer);
        if (this.neighboursNode.containsKey(peer)) {
            partner.neighboursNode.replace(thisHost, this.neighboursNode.keySet());
            this.neighboursNode.replace(peer, neighboursNode.keySet());
        } else {
            partner.neighboursNode.put(thisHost, this.neighboursNode.keySet());
            this.neighboursNode.put(peer, neighboursNode.keySet());
        }
        this.updateBetweenness(thisHost);
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost thisHost = con.getOtherNode(peer);
        SnWCountingWithBetweenessUpdate partner = getOtherSnWDecisionEngine(peer);

        if (thisHost.isRadioActive() == true && peer.isRadioActive() == true) {
            updateBettwen(thisHost, peer);

            int headMax = Math.max(partner.headCount, this.headCount);

            int a = 1;
            if (SimClock.getTime() >= stateTime) {
                a = 2;
            }

            if (this.token == 0 && partner.token == 0) {
                partner.headCount = this.headCount = Math.max(partner.headCount, this.headCount);
            } else {
                if (this.getBetweennessCentrality() > partner.getBetweennessCentrality()) {
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
//        System.out.println(thisHost + " " + headCount);
    }

    @Override
    public boolean newMessage(Message m) {
        m.addProperty(MSG_COUNT_PROPERTY, initialNrofCopies);
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
        return new SnWCountingWithBetweenessUpdate(this);
    }

    private SnWCountingWithBetweenessUpdate getOtherSnWDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouterLevelUp : "This router only works "
                + " with other routers of same type";

        return (SnWCountingWithBetweenessUpdate) ((DecisionEngineRouterLevelUp) otherRouter).getDecisionEngine();
    }

    public double getBetweennessCentrality() {
        return this.betweennessCentrality;
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
        if (time == stateTime) {
            this.token = 1;
            this.newHeadcount.put(2, 0);
            this.headCount = 0;
            System.out.println(host+ " "+ this.token);
        }
    }

}
