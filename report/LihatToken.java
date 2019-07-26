package report;

import routing.counting.HCInter;
import core.*;
import java.util.*;
import routing.*;
import routing.community.*;

/**
 *
 * @author Evander Juliansyah. H 
 * Universitas Sanata Dharma
 */
public class LihatToken extends Report implements UpdateListener {

    public static final String INTERVAL_COUNT = "Interval";
    public static final int DEFAULT_INTERVAL1_COUNT = 1800;
    public static final String ENGINE_SETTING = "decisionEngine";
    private double lastRecord = Double.MIN_VALUE;
    private int interval;

    private Map<DTNHost, ArrayList<Integer>> tokenArry = new HashMap<DTNHost, ArrayList<Integer>>();

    public LihatToken() {
        super();
        Settings settings = getSettings();
        if (settings.contains(INTERVAL_COUNT)) {
            interval = settings.getInt(INTERVAL_COUNT);
        } else {
            interval = DEFAULT_INTERVAL1_COUNT;
        }
    }

    @Override
    public void updated(List<DTNHost> hosts) {
        double simTime = getSimTime();
        if (isWarmup()) {
            return;
        }
        if (simTime - lastRecord >= interval) {
            printLine(hosts);
            this.lastRecord = simTime - simTime % interval;
        }

    }

    private void printLine(List<DTNHost> hosts) {
        for (DTNHost h : hosts) {
            MessageRouter r = h.getRouter();
            if (!(r instanceof DecisionEngineRouterLevelUp)) {
                continue;
            }
            RoutingDecisionEngineLevelUp de = ((DecisionEngineRouterLevelUp) r).getDecisionEngine();
            HCInter n = (HCInter) de;
            ArrayList<Integer> listHC = new ArrayList<>();
            int temp = n.getToken();

            if (tokenArry.containsKey(h)) {
                listHC = tokenArry.get(h);
                listHC.add(temp);
                tokenArry.put(h, listHC);
            } else {
                tokenArry.put(h, listHC);
            }
        }
    }

    public void done() {
        for (Map.Entry<DTNHost, ArrayList<Integer>> entry : tokenArry.entrySet()) {
            String printHost = "Node " + entry.getKey().getAddress() + "\t";
            for (Integer countList : entry.getValue()) {
                printHost = printHost + "\t" + countList;
            }
            write(printHost);
        }
        super.done();
    }

}
