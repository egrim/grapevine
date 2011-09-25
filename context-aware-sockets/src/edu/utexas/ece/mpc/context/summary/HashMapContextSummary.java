package edu.utexas.ece.mpc.context.summary;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.UUID;

@SuppressWarnings("serial")
public class HashMapContextSummary extends HashMap<String, Integer> implements ContextSummary {

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

    @Override
    public Integer get(String key) {
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
        return String.format("HashMapContextSummary with id=%d", id);
    }

    private int id;
    private long hashSeedHint = 0;
}
