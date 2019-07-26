package report;

import core.DTNHost;
import core.Settings;
import core.SimClock;
import core.UpdateListener;
import java.util.List;
import routing.DecisionEngineRouterLevelUp;
import routing.MessageRouter;
import routing.RoutingDecisionEngineLevelUp;
import routing.counting.HCInter;

/**
 *
 * @author Evander Juliansyah. H 
 * Universitas Sanata Dharma
 */
public class AverageHeadcount extends Report implements UpdateListener {

    public static final String HEADCOUNT_REPORT_INTERVAL = "headcountInterval";
    public static final int DEFAULT_HEADCOUNT_REPORT_INTERVAL = 900;
    private double lastRecord = Double.MIN_VALUE;
    private int interval;

    public AverageHeadcount() {
        super();

        Settings settings = getSettings();
        if (settings.contains(HEADCOUNT_REPORT_INTERVAL)) {
            interval = settings.getInt(HEADCOUNT_REPORT_INTERVAL);
        } else {
            interval = DEFAULT_HEADCOUNT_REPORT_INTERVAL;
        }
    }

    @Override
    public void updated(List<DTNHost> hosts) {
        if (SimClock.getTime() - lastRecord >= interval) {
            lastRecord = SimClock.getTime();
            printLine(hosts);
        }
    }

    private void printLine(List<DTNHost> hosts) {
        double headCount = 0;

        for (DTNHost host : hosts) {
            MessageRouter r = host.getRouter();
            if (!(r instanceof DecisionEngineRouterLevelUp)) {
                continue;
            }
            RoutingDecisionEngineLevelUp de = ((DecisionEngineRouterLevelUp) r).getDecisionEngine();
            HCInter n = (HCInter) de;
            int temp = n.getCount();
            headCount += temp;
        }
        double AV_Count = headCount / hosts.size();

        String output = format((int) SimClock.getTime()) + " \t " + format(AV_Count);
        write(output);
    }

}
