package edu.utexas.ece.mpc.context.summary;

import java.util.concurrent.TimeoutException;

import edu.utexas.ece.mpc.bloomier.ImmutableBloomierFilter;

public class BloomierContextSummary extends WireContextSummary {
    private ImmutableBloomierFilter<String, Integer> filter;

    public BloomierContextSummary(int m, int k, int q, long hashSeed, byte[][] table, int id,
                                  int hops, long timestamp) {
        super(id);
        this.filter = new ImmutableBloomierFilter<String, Integer>(m, k, q, Integer.class,
                                                                   hashSeed, table);
        this.hops = hops;
        this.timestamp = timestamp;
    }

    public BloomierContextSummary(HashMapContextSummary other, long hashSeedHint)
            throws TimeoutException {
        super(other.getId());
        filter = new ImmutableBloomierFilter<String, Integer>(other, // FIXME: magic numbers here (probably should be
                                                                     // tunable
              // through constructor with sane defaults)
              Math.max(1, (int) (other.keySet().size() * 1.20)), // make m 20% bigger than it needs to be (but at least
              // 1)
              // Math.max(1, (int) (other.keySet().size() * 1.20 * 0.03)), // make k 3% of m (but at least 1)
              3, // 3 seems to work best from tweaking attempts
              (int) (Integer.SIZE * 1.30), // make q 30% bigger than the Integers stored in it
              Integer.class, Integer.MAX_VALUE, // don't take more than 10 seconds to make the structure
              hashSeedHint);

        id = other.getId();
        hops = 0;
        timestamp = System.nanoTime();
    }

    // public BloomierContextSummary(BloomierContextSummary other) {
    // super(other);
    //
    // id = other.id;
    // hops = other.hops;
    // timestamp = other.timestamp;
    // }

    @Override
    public String toString() {
        return String.format("BloomierContextSummary with id=%d m=%d k=%d q=%d hashSeed=%d timestamp=%d hops=%d",
                             id, filter.getM(), filter.getK(), filter.getQ(), filter.getHashSeed(),
                             timestamp, hops);
    }

    int id;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public Integer get(String key) {
        return filter.get(key);
    }

    public int getM() {
        return filter.getM();
    }

    public int getK() {
        return filter.getK();
    }

    public int getQ() {
        return filter.getQ();
    }

    public long getHashSeed() {
        return filter.getHashSeed();
    }

    public byte[][] getTable() {
        return filter.getTable();
    }

}
