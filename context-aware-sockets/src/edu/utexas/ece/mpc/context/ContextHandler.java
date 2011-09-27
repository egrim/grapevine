package edu.utexas.ece.mpc.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;

import edu.utexas.ece.mpc.context.logger.ContextLoggingDelegate;
import edu.utexas.ece.mpc.context.logger.NullContextLogger;
import edu.utexas.ece.mpc.context.summary.ContextSummary;
import edu.utexas.ece.mpc.context.summary.WireContextSummary;

public class ContextHandler {
    public static enum WireSummaryType {
        BLOOMIER, LABELED
    }

    private static final int DEFAULT_TAU = 3;

    private static ContextHandler singleton;

    private WireContextSummary myContext;
    private Map<Integer, WireContextSummary> groupContext = new ConcurrentHashMap<Integer, WireContextSummary>();
    private Map<Integer, WireContextSummary> receivedSummaries = new ConcurrentHashMap<Integer, WireContextSummary>();

    private ContextLoggingDelegate loggingDelegate = new NullContextLogger();

    private ContextObservable preReceivedSummaryUpdateHook = new ContextObservable();
    private ContextObservable postReceivedSummaryUpdateHook = new ContextObservable();

    private int tau;
    
    private WireSummaryType wireSummaryType = WireSummaryType.BLOOMIER;

    // Note: Hide constructor (singleton pattern)
    private ContextHandler() {
        this(DEFAULT_TAU);
    }

    private ContextHandler(int tau) {
        this.tau = tau;
    }

    public static synchronized ContextHandler getInstance() {
        if (singleton == null) {
            singleton = new ContextHandler();
        }

        return singleton;
    }

    public void updateLocalSummary(ContextSummary summary) {
        myContext = summary.getWireCopy();
        logDbg("Updated local summary: " + myContext);
    }

    public void removeLocalSummary() {
        logDbg("Removing local summary: " + myContext);
        myContext = null;
    }

    public void handleIncomingSummaries(Collection<WireContextSummary> summaries) {
        Collection<WireContextSummary> summariesToPut = new ArrayList<WireContextSummary>();

        logDbg("Adding/updating received summaries");
        for (WireContextSummary summary: summaries) {
            int id = summary.getId();

            // Bump hop counter
            summary.incrementHops();

            // Is received summary local?
            if (myContext.getId() == id) {
                logDbg("Skipping summary (local): " + summary);
                continue;
            }

            // Do we already have the best version of this summary?
            if (receivedSummaries.containsKey(id)) {
                WireContextSummary existing = receivedSummaries.get(id);
                if ((summary.getTimestamp() < existing.getTimestamp())
                    || (summary.getTimestamp() == existing.getTimestamp() && summary.getHops() >= existing.getHops())) {
                    logDbg("Skipping summary (not new or with less hops): " + summary);
                    continue;
                }
            }

            summariesToPut.add(summary);
            logDbg("Marking  summary for add/update: " + summary);
        }

        if (!summariesToPut.isEmpty()) {
            preReceivedSummaryUpdateHook.setChanged();
            preReceivedSummaryUpdateHook.notifyObservers(summariesToPut);
        }
        
        for (WireContextSummary summaryToPut: summariesToPut) {
            receivedSummaries.put(summaryToPut.getId(), summaryToPut);
            logDbg("Summary put in receivedSummaries: " + summaryToPut);
        }

        if (!summariesToPut.isEmpty()) {
            postReceivedSummaryUpdateHook.setChanged();
            postReceivedSummaryUpdateHook.notifyObservers(summariesToPut);
        }
    }

    public ContextSummary get(int id) {
        if (id == myContext.getId()) {
            return myContext.getCopy();
        }

        if (groupContext.containsKey(id)) {
            return groupContext.get(id).getCopy();
        }

        if (receivedSummaries.containsKey(id)) {
            return receivedSummaries.get(id).getCopy();
        }

        return null;
    }

    public Integer get(int id, String key) {
        ContextSummary summary = get(id);
        if (summary == null) {
            return null;
        }

        return summary.get(key);
    }

    public ArrayList<WireContextSummary> getSummariesToSend() {
        ArrayList<WireContextSummary> summaries = new ArrayList<WireContextSummary>();
        summaries.add(myContext);
        summaries.addAll(groupContext.values());

        for (WireContextSummary summary: receivedSummaries.values()) {
            if (summary.getHops() < tau) {
                summaries.add(summary);
            } else {
                logDbg("Received summary not included due to tau: " + summary);
            }
        }

        logDbg("Prepared outgoing summaries:");
        for (ContextSummary summary: summaries) {
            logDbg("  " + summary);
        }

        return summaries;
    }

    public List<ContextSummary> getReceivedSummaries() {
        List<ContextSummary> summaries = new ArrayList<ContextSummary>();
        for (WireContextSummary summary: receivedSummaries.values()) {
            summaries.add(summary.getCopy());
        }

        return summaries;
    }

    public void resetAllSummaryData() {
        myContext = null;
        groupContext.clear();
        receivedSummaries.clear();
        logDbg("All summary data reset");
    }

    public void setLoggerDelegate(ContextLoggingDelegate delegate) {
        loggingDelegate = delegate;
    }

    public void setTau(int newTau) {
        tau = newTau;

        for (WireContextSummary summary: receivedSummaries.values()) {
            if (summary.getHops() >= tau) {
                receivedSummaries.remove(summary.getId());
            }
        }
    }
    
    public void log(String msg) {
        loggingDelegate.log(msg);
    }

    public void logError(String msg) {
        loggingDelegate.logError(msg);
    }

    public void logDbg(String msg) {
        loggingDelegate.logDebug(msg);
    }

    public boolean isDebugEnabled() {
        return loggingDelegate.isDebugEnabled();
    }

    public void addPreReceivedSummariesUpdateObserver(Observer observer) {
        preReceivedSummaryUpdateHook.addObserver(observer);
    }

    public void addPostReceiveSummariesUpdateObserver(Observer observer) {
        postReceivedSummaryUpdateHook.addObserver(observer);
    }

    private class ContextObservable extends Observable {
        // Make setChanged visible within the parent class
        @Override
        protected synchronized void setChanged() {
            super.setChanged();
        }
    }

    public WireSummaryType getWireSummaryType() {
        return wireSummaryType;
    }

    public void setWireSummaryType(WireSummaryType type) {
        wireSummaryType = type;
    }

    public void addGroupSummary(ContextSummary groupSummary) {
        groupContext.put(groupSummary.getId(), groupSummary.getWireCopy());
    }

    public void addGroupSummaries(Collection<ContextSummary> groupSummaries) {
        for (ContextSummary summary: groupSummaries) {
            addGroupSummary(summary);
        }
    }
}
