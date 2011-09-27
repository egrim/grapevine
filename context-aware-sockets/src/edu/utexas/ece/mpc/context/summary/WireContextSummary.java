package edu.utexas.ece.mpc.context.summary;


public interface WireContextSummary extends ContextSummary {
    public int getHops();

    public int incrementHops();

    public long getTimestamp();

    public boolean hasCachedSerialization();

    public byte[] getCachedSerializationBytes();
}