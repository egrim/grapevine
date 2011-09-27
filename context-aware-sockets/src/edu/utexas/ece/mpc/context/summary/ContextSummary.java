package edu.utexas.ece.mpc.context.summary;

public interface ContextSummary {
    int getId();

    Integer get(String key);

    ContextSummary getCopy();

    WireContextSummary getWireCopy();

}
