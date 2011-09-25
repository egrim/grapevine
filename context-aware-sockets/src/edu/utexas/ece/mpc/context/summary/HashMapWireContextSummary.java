package edu.utexas.ece.mpc.context.summary;

import java.util.Set;

public class HashMapWireContextSummary extends WireContextSummary {
    private HashMapContextSummary summary;

    public HashMapWireContextSummary(HashMapContextSummary summary) {
        super(summary.getId());
        this.summary = summary;
    }

    public HashMapWireContextSummary(HashMapContextSummary summary, int hops, long timestamp) {
        this(summary);
        this.hops = hops;
        this.timestamp = timestamp;
    }

    @Override
    public int getId() {
        return summary.getId();
    }

    @Override
    public Integer get(String key) {
        return summary.get(key);
    }

    public int size() {
        return summary.size();
    }

    public Set<String> keySet() {
        return summary.keySet();
    }

    @Override
    public String toString() {
        return String.format("HashMapWireContextSummary with id=%d timestamp=%d hops=%d",
 id,
                             timestamp, hops);
    }

}
