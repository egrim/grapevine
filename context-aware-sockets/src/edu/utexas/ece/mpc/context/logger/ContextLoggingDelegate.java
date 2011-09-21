package edu.utexas.ece.mpc.context.logger;

public interface ContextLoggingDelegate {

    void log(String msg);

    void logError(String msg);

    void logDebug(String msg);

    boolean isDebugEnabled();

}
