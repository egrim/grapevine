package edu.utexas.ece.mpc.context.summary;

import java.util.concurrent.TimeoutException;

public interface ContextSummary {
    int getId();

    Integer get(String key);

    BloomierContextSummary getBloomierCopy() throws TimeoutException;
}
