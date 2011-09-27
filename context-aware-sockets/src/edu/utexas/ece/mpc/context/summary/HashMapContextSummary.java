package edu.utexas.ece.mpc.context.summary;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.UUID;

import edu.utexas.ece.mpc.context.ContextHandler;

@SuppressWarnings("serial")
public class HashMapContextSummary extends HashMap<String, Integer> implements ContextSummary {

    protected final int id;
    protected long hashSeedHint = 0;

    public HashMapContextSummary() {
        Enumeration<NetworkInterface> interfaces;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new RuntimeException("Could not retrieve list of network interfaces", e);
        }

        if (interfaces == null) {
            throw new RuntimeException("No network interfaces detected");
        }

        Integer id = null;
        while (interfaces.hasMoreElements()) {
            try {
                NetworkInterface iface = interfaces.nextElement();
                byte[] macAddress = iface.getHardwareAddress();
                if (macAddress == null) {
                    continue; // try next interface
                }

                UUID macUuid = UUID.nameUUIDFromBytes(macAddress);
                id = macUuid.hashCode();
                break;
            } catch (Exception e) {
                continue; // try next interface
            }
        }

        if (id == null) {
            throw new RuntimeException("Could not find an interface with a MAC address");
        }

        this.id = id;
    }

    public HashMapContextSummary(int id) {
        this.id = id;
    }

    public HashMapContextSummary(HashMapContextSummary summary) {
        super(summary);

        this.id = summary.id;
        this.hashSeedHint = summary.hashSeedHint;
    }

    public HashMapContextSummary(int id, long hashSeedHint) {
        this.id = id;
        this.hashSeedHint = hashSeedHint;
    }

    @Override
    public Integer get(String key) {
        // Needed to satisfy interface requirement (even though the parent class already has it)
        return super.get(key);
    }

    @Override
    public int getId() {
        return id;
    }

    public long getHashSeedHint() {
        return hashSeedHint;
    }

    public void setHashSeedHint(long hashSeedHint) {
        this.hashSeedHint = hashSeedHint;
    }

    @Override
    public String toString() {
        return String.format("HashMapContextSummary with id=%d size=%d", id, size());
    }

    @Override
    public ContextSummary getCopy() {
        return new HashMapContextSummary(this);
    }

    @Override
    public WireContextSummary getWireCopy() {
        WireContextSummary summary = null;
        ContextHandler handler = ContextHandler.getInstance();
        switch (handler.getWireSummaryType()) {
            case BLOOMIER:
                BloomierContextSummary bSummary = new BloomierContextSummary(this, hashSeedHint);
                hashSeedHint = bSummary.getHashSeed();
                summary = bSummary;
                break;
            case LABELED:
                summary = new LabeledContextSummary(this);
                break;
        }
        return summary;
    }
}
