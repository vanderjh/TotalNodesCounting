package report;
/** 
 * Records the average buffer occupancy and its variance with format:
 * <p>
 * <Simulation time> <average buffer occupancy % [0..100]> <variance>
 * </p>
 * 
 */
import java.util.*;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Settings;
import core.UpdateListener;
public class BufferOccupancyPerTime extends Report implements UpdateListener, MessageListener {

	/**
	 * Record occupancy every nth second -setting id ({@value}). 
	 * Defines the interval how often (seconds) a new snapshot of buffer
	 * occupancy is taken previous:5
	 */
	public static final String BUFFER_REPORT_INTERVAL = "Interval";
	/** Default value for the snapshot interval */
	public static final int DEFAULT_BUFFER_REPORT_INTERVAL = 3600;
	private double lastRecord = Double.MIN_VALUE;
	private int interval;
        private Map<String, Double> creationTimes = new HashMap<String, Double>();
	private List<Double> latencies = new ArrayList<Double>();
	private List<Integer> hopCounts = new ArrayList<Integer>();
	private List<Double> rtt = new ArrayList<Double>();
         // round trip times

	private int nrofStarted = 0;
	private int nrofRelayed = 0;
	private int nrofCreated = 0;
	private int nrofResponseReqCreated = 0;
	private int nrofResponseDelivered = 0;
	private int nrofDelivered = 0;
	private Map<DTNHost, ArrayList<Integer>> bufferCounts = new HashMap<DTNHost, ArrayList<Integer>>();
	public BufferOccupancyPerTime() {
		super();
                
		Settings settings = getSettings();
		if (settings.contains(BUFFER_REPORT_INTERVAL)) {
			interval = settings.getInt(BUFFER_REPORT_INTERVAL);
		} else {
			interval = -1; /* not found; use default */
		}
		
		if (interval < 0) { /* not found or invalid value -> use default */
			interval = DEFAULT_BUFFER_REPORT_INTERVAL;
		}
	}

    @Override
    public void newMessage(Message m) {
        if (isWarmup()) {
			addWarmupID(m.getId());
			return;
		}
		
		this.creationTimes.put(m.getId(), getSimTime());
		this.nrofCreated++;
		if (m.getResponseSize() > 0) {
			this.nrofResponseReqCreated++;
		}
    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
    }

    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
    }
    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean finalTarget) {
        if (isWarmupID(m.getId())) {
			return;
		}

		this.nrofRelayed++;
		if (finalTarget) {
			this.latencies.add(getSimTime() - 
				this.creationTimes.get(m.getId()) );
			this.nrofDelivered++;
			this.hopCounts.add(m.getHops().size() - 1);
			
			if (m.isResponse()) {
				this.rtt.add(getSimTime() -	m.getRequest().getCreationTime());
				this.nrofResponseDelivered++;
			}
		}
    }
    
        @Override
    	public void updated(List<DTNHost> hosts) {
		double simTime = getSimTime();
		if (isWarmup()) {
			return;
		}
		
		if (simTime - lastRecord >= interval) {
			//lastRecord = SimClock.getTime();
			printLine(hosts);
			this.lastRecord = simTime - simTime % interval;
		}
	}
	private void printLine(List<DTNHost> hosts) {
		for (DTNHost h : hosts ) {
			ArrayList<Integer> bufferList = new ArrayList<Integer>();
                        int as = this.nrofDelivered;
//			double temp = h.getBufferOccupancy();
//                        int a = h.messageTransferred(NAN, h);
//			temp = (temp<=100.0)?(temp):(100.0);
			if (bufferCounts.containsKey(h)){
				//bufferCounts.put(h, (bufferCounts.get(h)+temp)/2); seems WRONG
				//bufferCounts.put(h, bufferCounts.get(h)+temp);
				//write (""+ bufferCounts.get(h));
				bufferList = bufferCounts.get(h);
				bufferList.add(as);
				bufferCounts.put(h, bufferList);
			}
			else {
				bufferCounts.put(h, bufferList);
				//write (""+ bufferCounts.get(h));
			}
		}
	}
	@Override
	public void done()
	{
		for (Map.Entry<DTNHost, ArrayList<Integer>> entry : bufferCounts.entrySet()) {
			String printHost = "Node "+entry.getKey().getAddress()+"\t";
			for (Integer bufferList : entry.getValue()){
				printHost = printHost + "\t" + bufferList;
			}
			write(printHost);
			//write("" + b + ' ' + entry.getValue());
		}
		super.done();
	}
}