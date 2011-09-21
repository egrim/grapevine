package edu.utexas.ece.mpc.context.logger;

public class SysoutLoggingDelegate implements ContextLoggingDelegate {

    @Override
    public void log(String msg) {
        System.out.println(String.format("[%d] INFO: %s", System.currentTimeMillis(), msg));
    }

    @Override
    public void logError(String msg) {
        System.out.println(String.format("[%d] ERROR: %s", System.currentTimeMillis(), msg));
    }

    @Override
    public void logDebug(String msg) {
        System.out.println(String.format("[%d] DEBUG: %s", System.currentTimeMillis(), msg));
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

}
