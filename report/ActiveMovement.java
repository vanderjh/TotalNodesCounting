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
public class ActiveMovement extends Report implements UpdateListener {

    public static final String REPORT_INTERVAL = "Interval";
    public static final int DEFAULT_REPORT_INTERVAL = 900;
    private double lastRecord = Double.MIN_VALUE;
    private int interval;
    private Map<DTNHost, ArrayList<Boolean>> movementOn = new HashMap<DTNHost, ArrayList<Boolean>>();

    public ActiveMovement() {
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
            ArrayList<Boolean> moveList = new ArrayList<Boolean>();
            boolean a = h.isMovementActive();
            if (movementOn.containsKey(h)) {
                moveList = movementOn.get(h);
                moveList.add(a);
                movementOn.put(h, moveList);
            } else {
                movementOn.put(h, moveList);
            }
        }
    }

    @Override
    public void done() {
        for (Map.Entry<DTNHost, ArrayList<Boolean>> entry : movementOn.entrySet()) {
            String printHost = "Node " + entry.getKey().getAddress() + "\t";
            for (Boolean moveList : entry.getValue()) {
                printHost = printHost + "\t" + moveList;
            }
            write(printHost);
        }
        super.done();
    }
}
