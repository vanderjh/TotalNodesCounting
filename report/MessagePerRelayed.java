package report;

import core.ConnectionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import core.Message;
import core.MessageListener;

/**
 *
 * @author Evander Juliansyah. H 
 * Universitas Sanata Dharma
 */
public class MessagePerRelayed extends Report implements MessageListener {

    private Map<String, Double> creationTimes;
    private List<Double> latencies;
    private List<Integer> hopCounts;
    private List<Double> msgBufferTime;
    private List<Double> rtt; // round trip times
    private double lastRecord = Double.MIN_VALUE;
    public static final int DEFAULT_CONTACT_COUNT = 3600;
    private int interval;

    private Map<Double, Integer> nrofDeliver;
    private int nrofRelayed;
    private int nrofCreated;
    private int nrofResponseReqCreated;

    /**
     * Constructor.
     */
    public MessagePerRelayed() {
        init();
    }

    @Override
    protected void init() {
        super.init();
        this.creationTimes = new HashMap<String, Double>();
        this.rtt = new ArrayList<Double>();
        this.interval = DEFAULT_CONTACT_COUNT;
        this.nrofDeliver = new HashMap<>();
        this.nrofRelayed = 0;
        this.nrofCreated = 0;
        this.nrofResponseReqCreated = 0;
    }

    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
    }

    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
    }

    public void messageTransferred(Message m, DTNHost from, DTNHost to,
            boolean finalTarget) {
        if (isWarmupID(m.getId())) {
            return;
        }

        this.nrofRelayed++;
        if (getSimTime() - lastRecord >= interval) {
            this.lastRecord = getSimTime() - getSimTime() % interval;
            nrofDeliver.put(lastRecord, nrofRelayed);
            this.nrofRelayed = 0;
        }
    }

    public void newMessage(Message m) {
    }

    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
    }

    @Override
    public void done() {
//		
        String statsText = "Time\tRelayed\n";
        for (Map.Entry<Double, Integer> entry : nrofDeliver.entrySet()) {
            Double key = entry.getKey();
            Integer value = entry.getValue();
            statsText += key + "\t" + value + "\n";
        }
        write(statsText);
        super.done();
    }

}
