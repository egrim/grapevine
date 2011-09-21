package edu.utexas.ece.mpc.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.TimeoutException;


import edu.utexas.ece.mpc.context.logger.ContextLoggingDelegate;
import edu.utexas.ece.mpc.context.logger.NullContextLogger;
import edu.utexas.ece.mpc.context.summary.BloomierContextSummary;
import edu.utexas.ece.mpc.context.summary.ContextSummary;

public class ContextHandler extends Observable {
    private static final int TAU = 3; // TODO: TAU should not be hard-coded

    private static ContextHandler me;

    private Map<Integer, ContextSummary> localSummaries = new HashMap<Integer, ContextSummary>();
    private Map<Integer, BloomierContextSummary> receivedSummaries = new HashMap<Integer, BloomierContextSummary>();

    private ContextLoggingDelegate loggingDelegate = new NullContextLogger();

    // Note: Hide constructor (singleton pattern)
    private ContextHandler() {

    }

    public static synchronized ContextHandler getInstance() {
        if (me == null) {
            me = new ContextHandler();
        }

        return me;
    }

    // FIXME: synchronized all methods that access summaries for multithreaded simulation - may instead want to use
    // concurrent versions of the underlying maps

    // TODO: may want to change this so that the context summary must be explicitly updated (i.e.: make a copy instead
    // of storing the original reference passed)
    public synchronized void addLocalContextSummary(ContextSummary summary) {
        Integer id = summary.getId();
        localSummaries.put(id, summary);

        logDbg("Added local summary: " + summary);
    }

    public synchronized void addOrUpdateReceivedSummaries(Collection<BloomierContextSummary> summaries) {
        Collection<Integer> idsUpdated = new HashSet<Integer>();

        logDbg("Adding/updating received summaries");
        for (BloomierContextSummary summary : summaries) {
            int id = summary.getId();

            // Is received summary local?
            if (localSummaries.containsKey(id)) {
                logDbg("Skipping summary (local): " + summary);
                continue;
            }

            // Is received summary old?
            if (receivedSummaries.containsKey(id)
                && summary.getTimestamp() <= receivedSummaries.get(id).getTimestamp()) {
                logDbg("Skipping summary (old): " + summary);
                continue;
            }

            // Bump hop counter
            summary.incrementHops();
            receivedSummaries.put(id, summary);
            logDbg("Added/updated summary: " + summary);

            idsUpdated.add(id);
        }

        if (!idsUpdated.isEmpty()) {
            setChanged();
            notifyObservers(idsUpdated);
        }
    }

    public synchronized ContextSummary get(int id) {
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

    public synchronized ArrayList<BloomierContextSummary> getSummariesToSend() {
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
            if (summary.getHops() < TAU) {
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

    public void setLoggerDelegate(ContextLoggingDelegate delegate) {
        loggingDelegate = delegate;
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

    public static interface ContextHandlerObserver extends Observer {
        public void update(ContextHandler handler, Collection<Integer> idsUpdated);
    }
}
