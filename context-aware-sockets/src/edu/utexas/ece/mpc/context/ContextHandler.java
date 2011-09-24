package edu.utexas.ece.mpc.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import edu.utexas.ece.mpc.context.logger.ContextLoggingDelegate;
import edu.utexas.ece.mpc.context.logger.NullContextLogger;
import edu.utexas.ece.mpc.context.summary.BloomierContextSummary;
import edu.utexas.ece.mpc.context.summary.ContextSummary;
import edu.utexas.ece.mpc.context.summary.HashMapContextSummary;

public class ContextHandler {
    private static final int DEFAULT_TAU = 3;

    private static ContextHandler me;

    private Map<Integer, HashMapContextSummary> localSummaries = new ConcurrentHashMap<Integer, HashMapContextSummary>();
    private Map<Integer, BloomierContextSummary> receivedSummaries = new ConcurrentHashMap<Integer, BloomierContextSummary>();

    private ContextLoggingDelegate loggingDelegate = new NullContextLogger();

    private ContextObservable preReceivedSummaryUpdateHook = new ContextObservable();
    private ContextObservable postReceivedSummaryUpdateHook = new ContextObservable();

    private int tau;

    // Note: Hide constructor (singleton pattern)
    private ContextHandler() {
        tau = DEFAULT_TAU;
    }

    public static synchronized ContextHandler getInstance() {
        if (me == null) {
            me = new ContextHandler();
        }

        return me;
    }

    public void putLocalSummary(HashMapContextSummary summary) {
        Integer id = summary.getId();
        localSummaries.put(id, new HashMapContextSummary(summary));

        logDbg("Added local summary: " + summary);
    }

    public void removeLocalSummary(HashMapContextSummary summary) {
        Integer id = summary.getId();
        localSummaries.remove(id);
        
        logDbg("Removed local summary: " + summary);
    }

    public void putReceivedSummaries(Collection<BloomierContextSummary> summaries) {
        Map<Integer, BloomierContextSummary> summariesToPut = new HashMap<Integer, BloomierContextSummary>();

        logDbg("Adding/updating received summaries");
        for (BloomierContextSummary summary : summaries) {
            int id = summary.getId();

            // Bump hop counter
            summary.incrementHops();

            // Is received summary local?
            if (localSummaries.containsKey(id)) {
                logDbg("Skipping summary (local): " + summary);
                continue;
            }

            // Is received summary not new or from a closer hop?
            if (receivedSummaries.containsKey(id)
                && summary.getTimestamp() <= receivedSummaries.get(id).getTimestamp()
                && summary.getHops() >= receivedSummaries.get(id).getHops()) {
                logDbg("Skipping summary (not new or with less hops): " + summary);
                continue;
            }

            summariesToPut.put(id, summary);
            logDbg("Summary marked for add/update: " + summary);
        }

        if (!summariesToPut.isEmpty()) {
            preReceivedSummaryUpdateHook.setChanged();
            preReceivedSummaryUpdateHook.notifyObservers(summariesToPut);
        }
        
        receivedSummaries.putAll(summariesToPut);
        for (ContextSummary sum: summariesToPut.values()) {
            logDbg("Summary put in receivedSummaries: " + sum);
        }

        if (!summariesToPut.isEmpty()) {
            postReceivedSummaryUpdateHook.setChanged();
            postReceivedSummaryUpdateHook.notifyObservers(summariesToPut);
        }
    }

    public ContextSummary get(int id) {
        ContextSummary summary;

        summary = localSummaries.get(id);
        if (summary != null) {
            return summary;
        }

        return receivedSummaries.get(id);
    }

    public Integer get(int id, String key) {
        ContextSummary summary = get(id);
        if (summary == null) {
            return null;
        }

        return summary.get(key);
    }

    public ArrayList<BloomierContextSummary> getSummariesToSend() {
        ArrayList<BloomierContextSummary> bloomierSummaries = new ArrayList<BloomierContextSummary>(
                                                                                                    localSummaries.size()
                                                                                                            + receivedSummaries.size());

        for (ContextSummary summary : localSummaries.values()) {
            try {
                bloomierSummaries.add(summary.getBloomierCopy());
            } catch (TimeoutException e) {
                System.err.println("Warning: A local summary could not be added");
                e.printStackTrace();
            }
        }

        for (BloomierContextSummary summary : receivedSummaries.values()) {
            if (summary.getHops() < tau) {
                bloomierSummaries.add(summary);
            }
        }

        StringBuilder builder = new StringBuilder("Prepared outgoing summaries:\n");
        for (ContextSummary summary : bloomierSummaries) {
            builder.append("  " + summary + "\n");
        }

        logDbg(builder.toString());

        return bloomierSummaries;
    }

    public List<ContextSummary> getReceivedSummaries() {
        return new ArrayList<ContextSummary>(receivedSummaries.values());
    }

    public void resetAllSummaryData() {
        localSummaries.clear();
        receivedSummaries.clear();
        logDbg("All summary data reset");
    }

    public void setLoggerDelegate(ContextLoggingDelegate delegate) {
        loggingDelegate = delegate;
    }

    public void setTau(int newTau) {
        tau = newTau;
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

}
