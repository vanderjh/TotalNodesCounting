/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package report;

import core.*;
import java.util.*;
import routing.*;
import routing.community.*;

/**
 *
 * @author fans
 */
public class MarkResiduNode extends Report implements UpdateListener {

    public static final String INTERVAL_COUNT = "Interval";
    public static final int DEFAULT_INTERVAL1_COUNT = 900;
    public static final String ENGINE_SETTING = "decisionEngine";
    private double lastRecord = Double.MIN_VALUE;
    private int interval;
    private int trashold = 108000;

    private Map<DTNHost, ArrayList<Integer>> residu = new HashMap<DTNHost, ArrayList<Integer>>();

    public MarkResiduNode() {
        super();
        Settings settings = getSettings();
        if (settings.contains(INTERVAL_COUNT)) {
            interval = settings.getInt(INTERVAL_COUNT);
        } else {
            interval = -1;
        }
        if (interval < 0) {
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
        int residuNode = 0;
        for (DTNHost h : hosts) {
            MessageRouter r = h.getRouter();
            if (!(r instanceof DecisionEngineRouterLevelUp)) {
                continue;
            }
            RoutingDecisionEngineLevelUp de = ((DecisionEngineRouterLevelUp) r).getDecisionEngine();
            MarkResidu n = (MarkResidu) de;
            ArrayList <Integer> listHC = new ArrayList<>();
            int temp = n.getResidu();
            if(residu.containsKey(h)){
                listHC = residu.get(h);
                listHC.add(temp);
                residu.put(h, listHC);
            }else{
                residu.put(h, listHC);
            }
        }
    }
    public void done() {
        for (Map.Entry<DTNHost, ArrayList<Integer>> entry : residu.entrySet()) {
            String printHost = "Node " + entry.getKey().getAddress() + "\t";
            for (Integer countList : entry.getValue()) {
                printHost = printHost + "\t" + countList;
            }
            write(printHost);
        }
        super.done();
    }
}
