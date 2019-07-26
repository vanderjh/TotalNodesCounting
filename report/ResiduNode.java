package report;

import routing.counting.ResiduInterface;
import core.*;
import java.util.*;
import routing.*;
import routing.community.*;

/**
 *
 * @author Evander Juliansyah. H 
 * Universitas Sanata Dharma
 */
public class ResiduNode extends Report implements UpdateListener {

    public static final String ENGINE_SETTING = "decisionEngine";
    public static final String INTERVAL_COUNT = "Interval";
    public static final String RESIDU_THRESHOLD = "threshold";
    public static final int DEFAULT_INTERVAL1_COUNT = 900;
    public static final int DEFAULT_INTERVAL1_THRESHOLD = 0;
    private double lastRecord = Double.MIN_VALUE;
    private int interval;

    private Map<DTNHost, ArrayList<Integer>> residu = new HashMap<DTNHost, ArrayList<Integer>>();

    public ResiduNode() {
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
        if(SimClock.getTime() - lastRecord >= interval){
            lastRecord = SimClock.getTime();
            printLine(hosts);
        }
    }
    private void printLine(List<DTNHost> hosts) {
        Settings s = new Settings();
        int time = s.getInt("DecisionEngineRouterLevelUp.timeUpdate");
        int residuNode = 0;
        for (DTNHost h : hosts) {
            MessageRouter r = h.getRouter();
            if (!(r instanceof DecisionEngineRouterLevelUp)) {
                continue;
            }
            RoutingDecisionEngineLevelUp de = ((DecisionEngineRouterLevelUp) r).getDecisionEngine();
            ResiduInterface n = (ResiduInterface) de;
            Map temp = n.getResidu();
            if (temp.size() != 2 || (temp.containsKey(2) && temp.containsValue(0))) {
                residuNode++;
            }
        }
        if(SimClock.getTime() >= time){
            String print = format(SimClock.getTime()) + "\t" + residuNode;
            write(print);          
        }
    }
}
