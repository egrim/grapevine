package edu.utexas.ece.mpc.context.summary;

import edu.utexas.ece.mpc.bloomier.ImmutableBloomierFilter;

public class BloomierContextSummary implements WireContextSummary {
    private ImmutableBloomierFilter<String, Integer> filter;

    private final int id;
    private int hops;
    private final long timestamp;

    public BloomierContextSummary(int m, int k, int q, long hashSeed, byte[][] table, int id,
                                  int hops, long timestamp) {
        this.filter = new ImmutableBloomierFilter<String, Integer>(m, k, q, Integer.class,
                                                                   hashSeed, table);

        this.id = id;
        this.hops = hops;
        this.timestamp = timestamp;
    }

    public BloomierContextSummary(HashMapContextSummary other, long hashSeedHint) {
        // FIXME: make these calculations tunable (get rid of 'magic')
        int m = Math.max(1, (int) (other.keySet().size() * 1.20)); // make m 20% bigger than map size
        int k = (m < 40) ? 2 : 3; // FIXME: found through manual tweaking - need better way to do this
        // int k = Math.min(3, Math.max(1, (int) (m * 0.03))); // make k 3% of m (but at least 1) TODO: limiting to
        // three seems to be best as well - investigate this
        int q = (int) (Integer.SIZE * 1.30); // make q 30% bigger than the Integers being stored
        
        filter = new ImmutableBloomierFilter<String, Integer>(other, m, k, q, Integer.class,
                                                              hashSeedHint);

        id = other.getId();
        hops = 0;
        timestamp = System.nanoTime();
    }

    public BloomierContextSummary(BloomierContextSummary other) {
        filter = new ImmutableBloomierFilter<String, Integer>(other.filter);

        id = other.id;
        hops = other.hops;
        timestamp = other.timestamp;
    }

    @Override
    public String toString() {
        return String.format("BloomierContextSummary with id=%d m=%d k=%d q=%d hashSeed=%d timestamp=%d hops=%d",
                             id, filter.getM(), filter.getK(), filter.getQ(), filter.getHashSeed(),
                             timestamp, hops);
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

    @Override
    public int getId() {
        return id;
    }

    @Override
    public ContextSummary getCopy() {
        return getWireCopy();
    }

    @Override
    public WireContextSummary getWireCopy() {
        return new BloomierContextSummary(this);
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
