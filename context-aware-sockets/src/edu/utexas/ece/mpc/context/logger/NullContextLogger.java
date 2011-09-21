package edu.utexas.ece.mpc.context.logger;

public class NullContextLogger implements ContextLoggingDelegate {

    @Override
    public void log(String msg) {

    }

    @Override
    public void logError(String msg) {

    }

    @Override
    public void logDebug(String msg) {

    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

}
