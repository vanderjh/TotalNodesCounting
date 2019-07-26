package routing.counting;

import routing.*;
import core.*;
import java.util.*;
import report.Report;

/**
 *
 * @author Evander Juliansyah. H 
 * Universitas Sanata Dharma
 */
public class SnWCountingWithFairRouting implements RoutingDecisionEngine, HCInter {

    /**
     * short term -setting id {@value}
     */
    public static final String R_SIGMA = "shortTermR";

    /**
     * long term -setting id {@value}
     */
    public static final String R_LAMBDA = "longTermR";

    public static final String BINARY_MODE = "binaryMode";
    public static final String SPRAYANDWAIT_NS = "SnWCountingWithFairRouting";
    public static final String MSG_COUNT_PROPERTY = SPRAYANDWAIT_NS + "."
            + "copies";

    public static final String T_SETTING = "T";
    public static final int DEFAULT_T = 1;

    private static final Double R_LAMBDA_DEFAULT = 0.1;
    private static final Double R_SIGMA_DEFAULT = 0.2;
    /**
     * Map for Headcount List
     */
//    public static Map<DTNHost, Integer> headCountList;
    protected Map<DTNHost, ArrayList<Double>> neighborsHistory;

    protected int token;
    protected int headCount;
    public boolean isBinary;

    private double rSigma; //eksponential rate short term
    private double rLambda; //eksponential rate long term

    double util; //nilai util
    double s_ik, //interaction strength node i to k
            s_jk, //interaction strength node j to k
            lambda_ik = 0, //
            lambda_jk = 0,
            sigma_ik = 0,
            sigma_jk = 0;

    public SnWCountingWithFairRouting(Settings s) {
        if (s.contains(BINARY_MODE)) {
            isBinary = s.getBoolean(BINARY_MODE);
        } else {
            this.isBinary = false;
        }
        if (s.contains(T_SETTING)) {
            token = s.getInt(T_SETTING);
        } else {
            this.token = DEFAULT_T;
        }
        if (s.contains(R_SIGMA)) {
            rSigma = s.getDouble(R_SIGMA);
        } else {
            this.rSigma = R_SIGMA_DEFAULT;
        }
        if (s.contains(R_LAMBDA)) {
            rLambda = s.getDouble(R_LAMBDA);
        } else {
            this.rLambda = R_LAMBDA_DEFAULT;
        }
    }

    public SnWCountingWithFairRouting(SnWCountingWithFairRouting r) {
        this.token = r.token;
        this.isBinary = r.isBinary;
        this.rLambda = r.rLambda;
        this.rSigma = r.rSigma;
        neighborsHistory = new HashMap<DTNHost, ArrayList<Double>>();
//        headCountList = new HashMap<>();
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
        SnWCountingWithFairRouting partner = getOtherSnWDecisionEngine(peer);
        
        if (thisHost.isRadioActive() == true && peer.isRadioActive() == true) {
        saveNeighbor(thisHost, peer);

        double sumOf_s_jk = this.countSumOfAgrIntStrength(partner.neighborsHistory);
        double sumOf_s_ik = this.countSumOfAgrIntStrength(this.neighborsHistory);

        util = this.countUtil(sumOf_s_jk, sumOf_s_ik);

        int a = partner.headCount;
        int b = this.headCount;
        boolean c = false;

        if (this.token == 0 && partner.token == 0) {
            partner.headCount = this.headCount = Math.max(partner.headCount, this.headCount);
        } else {
            if (util < 0.5) {
                this.token = this.token + partner.token;
                partner.token = 0;
                if (b > this.token) {
                    partner.headCount = this.headCount = b;
                } else {
                    partner.headCount = this.headCount = this.token;
                }
//                partner.headCount = this.headCount = Math.max(this.token, partner.headCount);
//                partner.headCount = this.headCount = this.token;
            } else {
                partner.token = partner.token + this.token;
                this.token = 0;
                if (a > partner.token) {
                    this.headCount = partner.headCount = a;
                } else {
                    this.headCount = partner.headCount = partner.token;
                }
//                this.headCount = partner.headCount = Math.max(partner.token, this.headCount);
//                this.headCount = partner.headCount = partner.token;

            }
        }
    }
    }

    private void updatePercieveInteractionStrength(DTNHost peer) {
        double sigma;
        double lambda;
        double timeLastEncountered;
        double timeNew = SimClock.getTime();

        ArrayList<Double> nodeInformationList;

        for (Map.Entry<DTNHost, ArrayList<Double>> data : this.neighborsHistory.entrySet()) {

            if (data.getKey() == peer) {
                nodeInformationList = data.getValue();
                lambda = nodeInformationList.get(0);
                sigma = nodeInformationList.get(1);

                lambda++;
                sigma++;

                nodeInformationList.set(0, lambda);
                nodeInformationList.set(1, sigma);
                nodeInformationList.set(2, timeNew);

                this.neighborsHistory.replace(data.getKey(), nodeInformationList);
            } else {
                nodeInformationList = data.getValue();
                lambda = nodeInformationList.get(0);
                sigma = nodeInformationList.get(1);
                timeLastEncountered = nodeInformationList.get(2);

                lambda = lambda * (Math.pow(Math.E, (-(this.rLambda) * (timeNew - timeLastEncountered))));
                sigma = sigma * (Math.pow(Math.E, (-(this.rSigma) * (timeNew - timeLastEncountered))));

                nodeInformationList.set(0, lambda);
                nodeInformationList.set(1, sigma);

                this.neighborsHistory.replace(data.getKey(), nodeInformationList);

            }
        }
    }

    public double getUtil() {
        return this.util;
    }

    @Override
    public boolean newMessage(Message m) {
        m.addProperty(MSG_COUNT_PROPERTY, this.headCount / 2);
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

    protected double countAgrIntStrength(double lambda, double sigma) {
        return lambda * (lambda - sigma);
    }

    protected double countUtil(double s_jk, double s_ik) {
        return s_jk / (s_jk + s_ik);
    }

    protected double countSumOfAgrIntStrength(Map<DTNHost, ArrayList<Double>> neighborsHist) {
        double sumOfIntStrength = 0;
        double lambda = 0, sigma = 0;
        for (Map.Entry<DTNHost, ArrayList<Double>> data : neighborsHist.entrySet()) {
            lambda = data.getValue().get(0);
            sigma = data.getValue().get(1);

            sumOfIntStrength = sumOfIntStrength + this.countAgrIntStrength(lambda, sigma);
        }

        return sumOfIntStrength;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return m.getTo() == hostReportingOld;
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new SnWCountingWithFairRouting(this);
    }

    private void saveNeighbor(DTNHost thisHost, DTNHost peer) {
        SnWCountingWithFairRouting partner = this.getOtherSnWDecisionEngine(peer);

        double sigma = 0;
        double lambda = 0;
        double time = 0;

        if (!this.neighborsHistory.containsKey(peer)) {
            ArrayList<Double> nodeInformationList = new ArrayList<Double>();
            nodeInformationList.add(lambda);
            nodeInformationList.add(sigma);
            nodeInformationList.add(time);
            this.neighborsHistory.put(peer, nodeInformationList);
            partner.neighborsHistory.put(thisHost, nodeInformationList);
        }
        this.updatePercieveInteractionStrength(peer);
    }

    private SnWCountingWithFairRouting getOtherSnWDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (SnWCountingWithFairRouting) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
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
