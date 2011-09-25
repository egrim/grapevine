package edu.utexas.ece.mpc.context.summary;

public abstract class WireContextSummary implements ContextSummary {
    protected int id;
    protected int hops = 0;
    protected long timestamp = System.nanoTime();

    public WireContextSummary(int id) {
        this.id = id;
    }

    public WireContextSummary(WireContextSummary other) {
        this.id = other.id;
        this.hops = other.hops;
        this.timestamp = other.timestamp;
    }

    public int getHops() {
        return hops;
    }

    public int incrementHops() {
        return ++hops;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public abstract Integer get(String key);

}