package report;

import java.util.*;
import core.DTNHost;
import core.Settings;
import core.UpdateListener;

/**
 *
 * @author Evander Juliansyah. H 
 * Universitas Sanata Dharma
 */
public class ActiveInterface extends Report implements UpdateListener {

    public static final String REPORT_INTERVAL = "Interval";
    public static final int DEFAULT_REPORT_INTERVAL = 900;
    private double lastRecord = Double.MIN_VALUE;
    private int interval;
    private Map<DTNHost, ArrayList<Boolean>> interfaceOn = new HashMap<DTNHost, ArrayList<Boolean>>();

    public ActiveInterface() {
        super();

        Settings settings = getSettings();
        if (settings.contains(REPORT_INTERVAL)) {
            interval = settings.getInt(REPORT_INTERVAL);
        } else {
            interval = DEFAULT_REPORT_INTERVAL;
        }
    }

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
            ArrayList<Boolean> interfaceList = new ArrayList<Boolean>();
            boolean a = h.isRadioActive();
            if (interfaceOn.containsKey(h)) {
                interfaceList = interfaceOn.get(h);
                interfaceList.add(a);
                interfaceOn.put(h, interfaceList);
            } else {
                interfaceOn.put(h, interfaceList);
            }
        }

    }

    @Override
    public void done() {
        for (Map.Entry<DTNHost, ArrayList<Boolean>> entry : interfaceOn.entrySet()) {
            String printHost = "Node " + entry.getKey().getAddress() + "\t";
            for (Boolean interfaceList : entry.getValue()) {
                printHost = printHost + "\t" + interfaceList;
            }
            write(printHost);
        }
        super.done();
    }
}
