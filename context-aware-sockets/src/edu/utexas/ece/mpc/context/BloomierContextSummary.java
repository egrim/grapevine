package edu.utexas.ece.mpc.context;

import java.util.concurrent.TimeoutException;

import edu.utexas.ece.mpc.bloomier.ImmutableBloomierFilter;

public class BloomierContextSummary extends ImmutableBloomierFilter<String, Integer> implements ContextSummary {
    public BloomierContextSummary(int m, int k, int q, long hashSeed, byte[][] table, int id,
                                  int hops, long timestamp) {
        super(m, k, q, Integer.class, hashSeed, table);
        this.id = id;
        this.hops = hops;
        this.timestamp = timestamp;
    }

    public BloomierContextSummary(HashMapContextSummary other, long hashSeedHint)
            throws TimeoutException {
        super(other, // FIXME: magic numbers here (probably should be tunable
                     // through constructor with sane defaults)
              (int) (other.keySet().size() * 1.20), // make m 20% bigger than it needs to be
              Math.max(1, (int) (other.keySet().size() * 1.20 * 0.03)), // make k 3% of m (but at least 1)
              (int) (Integer.SIZE * 1.30), // make q 30% bigger than the Integers stored in it
              Integer.class, 10000, // don't take more than 10 seconds to make the structure
              hashSeedHint);

        id = other.getId();
        hops = 0;
        timestamp = System.nanoTime();
    }

    public BloomierContextSummary(BloomierContextSummary other) {
        super(other);

        id = other.id;
        hops = other.hops;
        timestamp = other.timestamp;
    }

    @Override
    public int getId() {
        return id;
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
    public BloomierContextSummary getBloomierCopy() throws TimeoutException {
        return new BloomierContextSummary(this);
    }

    public String toString() {
        return "BloomierContextSummary with id=" + id + " timestamp=" + timestamp + " hops=" + hops;
    }

    private int id;
    private int hops = 0;
    private long timestamp;

}
