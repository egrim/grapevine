package edu.utexas.ece.mpc.context.summary;


@SuppressWarnings("serial")
public class LabeledContextSummary extends HashMapContextSummary implements WireContextSummary {
    private int hops;
    private final long timestamp;

    public LabeledContextSummary(HashMapContextSummary map, int hops, long timestamp) {
        super(map);
        this.hops = hops;
        this.timestamp = timestamp;
    }

    public LabeledContextSummary(HashMapContextSummary summary) {
        this(summary, 0, System.nanoTime());
    }

    public LabeledContextSummary(LabeledContextSummary other) {
        this(other, other.hops, other.timestamp);
    }

    @Override
    public String toString() {
        return String.format("LabeledContextSummary with id=%d timestamp=%d hops=%d size=%d", id,
                             timestamp, hops, size());
    }

    @Override
    public WireContextSummary getWireCopy() {
        return new LabeledContextSummary(this);
    }

    @Override
    public int getHops() {
        return hops;
    }

    @Override
    public int incrementHops() {
        return ++hops;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean hasCachedSerialization() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public byte[] getCachedSerializationBytes() {
        // TODO Auto-generated method stub
        return null;
    }

}
