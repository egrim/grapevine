package edu.utexas.ece.mpc.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;

import edu.utexas.ece.mpc.context.group.GroupDefinition;
import edu.utexas.ece.mpc.context.logger.ContextLoggingDelegate;
import edu.utexas.ece.mpc.context.logger.NullContextLogger;
import edu.utexas.ece.mpc.context.summary.ContextSummary;
import edu.utexas.ece.mpc.context.summary.GroupContextSummary;
import edu.utexas.ece.mpc.context.summary.HashMapGroupContextSummary;
import edu.utexas.ece.mpc.context.summary.WireContextSummary;

public class ContextHandler {
    public static enum WireSummaryType {
        BLOOMIER, LABELED
    }

    private static final int DEFAULT_TAU = 3;

    private static ContextHandler singleton;

    private WireContextSummary myContext;
    private Map<Integer, GroupContextSummary> groupContext = new ConcurrentHashMap<Integer, GroupContextSummary>();
    private Map<Integer, WireContextSummary> receivedSummaries = new ConcurrentHashMap<Integer, WireContextSummary>();

    private ContextLoggingDelegate loggingDelegate = new NullContextLogger();

    private ContextObservable preReceivedSummaryUpdateHook = new ContextObservable();
    private ContextObservable postReceivedSummaryUpdateHook = new ContextObservable();

    private int tau;
    
    private WireSummaryType wireSummaryType = WireSummaryType.BLOOMIER;

    private Map<Integer, GroupDefinition> groupDefinitions = new HashMap<Integer, GroupDefinition>();

//    private WireSummaryType wireSummaryType = WireSummaryType.LABELED;

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

    public synchronized void updateLocalSummary(ContextSummary summary) {
        myContext = summary.getWireCopy();

        for (GroupDefinition groupDefinition: groupDefinitions.values()) {
            int gId = groupDefinition.getId();
            GroupContextSummary groupSummary = groupContext.get(gId);
            groupDefinition.handleContextSummary(groupSummary, myContext);
        }
        logDbg("Updated local summary: " + myContext);
    }

    public synchronized void removeLocalSummary() {
        logDbg("Removing local summary: " + myContext);
        myContext = null;
    }

    public synchronized void handleIncomingSummaries(Collection<WireContextSummary> summaries) {
        Collection<WireContextSummary> summariesToPut = new ArrayList<WireContextSummary>();

        logDbg("Adding/updating received summaries");
        for (WireContextSummary summary: summaries) {
            int id = summary.getId();

            // Bump hop counter
            summary.incrementHops();

            // Is received summary local?
            if (myContext != null && myContext.getId() == id) {
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
        
        performGroupFormations(summariesToPut);

        for (WireContextSummary summaryToPut: summariesToPut) {
            receivedSummaries.put(summaryToPut.getId(), summaryToPut);
            logDbg("Summary put in receivedSummaries: " + summaryToPut);
        }

        if (!summariesToPut.isEmpty()) {
            postReceivedSummaryUpdateHook.setChanged();
            postReceivedSummaryUpdateHook.notifyObservers(summariesToPut);
        }
    }

    private void performGroupFormations(Collection<WireContextSummary> summaries) {
        for (GroupDefinition groupDefinition: groupDefinitions.values()) {
            int gId = groupDefinition.getId();
            GroupContextSummary groupSummary = groupContext.get(gId);
            for (Iterator<WireContextSummary> it = summaries.iterator(); it.hasNext();) {
                ContextSummary summary = it.next();
                if (summary.getId() == gId) {
                    groupDefinition.handleGroupSummary(groupSummary, summary);
                    it.remove();
                } else {
                    groupDefinition.handleContextSummary(groupSummary, summary);
                }
            }
        }
    }

    public synchronized ContextSummary get(int id) {
        if (myContext != null && id == myContext.getId()) {
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

    public synchronized ArrayList<WireContextSummary> getSummariesToSend() {
        ArrayList<WireContextSummary> summaries = new ArrayList<WireContextSummary>();
        
        if (myContext != null) {
            summaries.add(myContext);
        }
        
        for (GroupContextSummary groupSummary: groupContext.values()) {
            summaries.add(groupSummary.getWireCopy());
        }

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

    public synchronized List<ContextSummary> getReceivedSummaries() {
        List<ContextSummary> summaries = new ArrayList<ContextSummary>();
        for (WireContextSummary summary: receivedSummaries.values()) {
            summaries.add(summary.getCopy());
        }

        return summaries;
    }

    public synchronized void resetAllSummaryData() {
        myContext = null;
        groupContext.clear();
        receivedSummaries.clear();
        logDbg("All summary data reset");
    }

    public synchronized void setLoggerDelegate(ContextLoggingDelegate delegate) {
        loggingDelegate = delegate;
    }

    public synchronized void setTau(int newTau) {
        tau = newTau;

        for (WireContextSummary summary: receivedSummaries.values()) {
            if (summary.getHops() >= tau) {
                receivedSummaries.remove(summary.getId());
            }
        }
    }
    
    public synchronized void log(String msg) {
        loggingDelegate.log(msg);
    }

    public synchronized void logError(String msg) {
        loggingDelegate.logError(msg);
    }

    public synchronized void logDbg(String msg) {
        loggingDelegate.logDebug(msg);
    }

    public synchronized boolean isDebugEnabled() {
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

    public synchronized WireSummaryType getWireSummaryType() {
        return wireSummaryType;
    }

    public synchronized void setWireSummaryType(WireSummaryType type) {
        wireSummaryType = type;
        resetAllSummaryData();
        logDbg("Wire summary changed to " + wireSummaryType + " and stored context was cleared");
    }

    public synchronized void addGroupDefinition(GroupDefinition groupDefinition) {
        int gId = groupDefinition.getId();
        GroupContextSummary groupSummary = new HashMapGroupContextSummary(gId);

        groupContext.put(gId, groupSummary);
        
        groupDefinitions.put(gId, groupDefinition);
        
        if (myContext != null) {
            groupDefinition.handleContextSummary(groupSummary, myContext);
        }

        performGroupFormations(receivedSummaries.values());
    }

    public synchronized GroupContextSummary getGroupSummary(int gId) {
        GroupContextSummary groupSummary = groupContext.get(gId);
        if (groupSummary != null) {
            return groupSummary.getGroupCopy();
        } else {
            return null;
        }
    }
    public synchronized List<GroupContextSummary> getGroupSummaries() {
        List<GroupContextSummary> summaries = new ArrayList<GroupContextSummary>();
        for (GroupContextSummary summary: groupContext.values()) {
            summaries.add(summary.getGroupCopy());
        }

        return summaries;
    }
}
