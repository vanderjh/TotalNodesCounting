package routing.counting;

import routing.community.*;
import routing.counting.*;
import routing.*;
import routing.MessageRouter;
import core.*;
import java.util.*;
import javafx.print.Collation;
import report.Report;
 
/**
 *
 * @author Evander Juliansyah. H 
 * Universitas Sanata Dharma
 */
public class SnWCountingWithBetweeness implements RoutingDecisionEngine, HCInter {

//    public static final String NROF_COPIES = "nrofCopies";
    public static final String BINARY_MODE = "binaryMode";
    public static final String SPRAYANDWAIT_NS = "SnWCountingWithBetweeness";
    public static final String MSG_COUNT_PROPERTY = SPRAYANDWAIT_NS + "."
            + "copies";
    public static final String CENTRALITY_ALG_SETTING2 = "centralityAlg";

    public static final String T_SETTING = "T";
    public static final int DEFAULT_T = 1;

    protected Map<DTNHost, Set<DTNHost>> neighboursNode;
    protected Map<DTNHost, Double> startTimestamps;
    /**
     * Map for Headcount List
     */
    protected Map<DTNHost, Integer> headCountList;
    protected double[][] matrixEgoNetwork;
    protected double betweennessCentrality;
    protected CommunityDetection community;
    protected CentralityDetection centrality;
    protected int token;
    public int headCount;
    public boolean isBinary;

    public SnWCountingWithBetweeness(Settings s) {
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
        if (s.contains(CENTRALITY_ALG_SETTING2)) {
            this.centrality = (CentralityDetection) s.createIntializedObject(s.getSetting(CENTRALITY_ALG_SETTING2));
        } else {
            this.centrality = new BetweennessCentrality(s);
        }
    }

    public SnWCountingWithBetweeness(SnWCountingWithBetweeness r) {
        this.token = r.token;
        this.isBinary = r.isBinary;
        this.centrality = r.centrality.replicate();
        startTimestamps = new HashMap<DTNHost, Double>();
        neighboursNode = new HashMap<DTNHost, Set<DTNHost>>();
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
        SnWCountingWithBetweeness partner = getOtherSnWDecisionEngine(peer);
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
//        if (isWarmup()) {
        DTNHost thisHost = con.getOtherNode(peer);
        SnWCountingWithBetweeness partner = getOtherSnWDecisionEngine(peer);

        if(thisHost.isRadioActive() == true && peer.isRadioActive() == true){
        updateBettwen(thisHost, peer);

        int headMax = Math.max(partner.headCount, this.headCount);
        
        if (this.token == 0 && partner.token == 0) {
            partner.headCount = this.headCount = Math.max(partner.headCount, this.headCount);
        } else {
            if (this.getBetweennessCentrality() > partner.getBetweennessCentrality()) {
                this.token = this.token + partner.token;
                partner.token = 0;
                if (headMax > this.token) {
                    partner.headCount = this.headCount = headMax;
                } else {
                    partner.headCount = this.headCount = this.token;
                }
            } else {
                partner.token = partner.token + this.token;
                this.token = 0;
                if (headMax > partner.token) {
                    this.headCount = partner.headCount = headMax;
                } else {
                    this.headCount = partner.headCount = partner.token;
                }
            }
        }
        }
//            System.out.println(headCount);
//        this.updateBetweenness(thisHost);
//        }
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
    public RoutingDecisionEngine replicate() {
        return new SnWCountingWithBetweeness(this);
    }

    private SnWCountingWithBetweeness getOtherSnWDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (SnWCountingWithBetweeness) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
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

}
