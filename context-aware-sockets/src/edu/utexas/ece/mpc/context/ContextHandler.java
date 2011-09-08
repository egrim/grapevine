package edu.utexas.ece.mpc.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class ContextHandler {
    private static final int TAU = 3; // TODO: TAU should not be hard-coded

    private static ContextHandler me = new ContextHandler();

    private Map<Integer, ContextSummary> localSummaries = new HashMap<Integer, ContextSummary>();
    private Map<Integer, BloomierContextSummary> receivedSummaries = new HashMap<Integer, BloomierContextSummary>();

    // Note: Hide constructor (singleton pattern)
    private ContextHandler() {

    }

    public static synchronized ContextHandler getInstance() {
        if (me == null) {
            me = new ContextHandler();
        }

        return me;
    }

    // FIXME: synchronized all methods that access summaries for multithreaded simulation - may want to back out of that
    // change
    public synchronized void addLocalContextSummary(ContextSummary summary) {
        Integer id = summary.getId();
        localSummaries.put(id, summary);
    }

    public synchronized void addOrUpdateReceivedSummaries(Collection<BloomierContextSummary> summaries) {
        for (BloomierContextSummary summary : summaries) {
            int id = summary.getId();

            // Is received summary local?
            if (localSummaries.containsKey(id)) {
                continue;
            }

            // Is received summary old?
            if (receivedSummaries.containsKey(id)
                && summary.getTimestamp() <= receivedSummaries.get(id).getTimestamp()) {
                continue;
            }

            // Bump hop counter
            summary.incrementHops();
            receivedSummaries.put(id, summary);
        }
    }

    public synchronized ContextSummary get(int id) {
        ContextSummary summary;

        summary = localSummaries.get(id);
        if (summary != null) {
            return summary;
        }

        return receivedSummaries.get(id);
    }

    public Integer get(int id, String key) {
        ContextSummary summary = get(id);
        if (summary == null) {
            return null;
        }

        return summary.get(key);
    }

    public synchronized ArrayList<BloomierContextSummary> getSummariesToSend() {
        ArrayList<BloomierContextSummary> bloomierSummaries = new ArrayList<BloomierContextSummary>(
                                                                                                    localSummaries.size()
                                                                                                            + receivedSummaries.size());

        for (ContextSummary summary : localSummaries.values()) {
            try {
                bloomierSummaries.add(summary.getBloomierCopy());
            } catch (TimeoutException e) {
                System.err.println("Warning: A local summary could not be added");
                e.printStackTrace();
            }
        }

        for (BloomierContextSummary summary : receivedSummaries.values()) {
            if (summary.getHops() < TAU) {
                bloomierSummaries.add(summary);
            }
        }

        // FIXME: remove debug print
        System.out.println("Prepared outgoing summaries:");
        for (ContextSummary summary : bloomierSummaries) {
            System.out.println("  " + summary);
        }

        return bloomierSummaries;
    }

    public List<ContextSummary> getReceivedSummaries() {
        return new ArrayList<ContextSummary>(receivedSummaries.values());
    }

    // private class CombinedMap<K, V> implements Map<K, V> {
    // // FIXME: probably doesn't gracefully handle underlying maps that share duplicate keys
    //
    // private List<Map<K, V>> maps;
    //
    // public CombinedMap(Map<K, V>... maps) {
    // this.maps = new ArrayList<Map<K, V>>(maps.length);
    // for (Map<K, V> map: maps) {
    // this.maps.add(map);
    // }
    // }
    //
    // @Override
    // public void clear() {
    // for (Map<K, V> map: maps) {
    // map.clear();
    // }
    // }
    //
    // @Override
    // public boolean containsKey(Object key) {
    // for (Map<K, V> map: maps) {
    // if (map.containsKey(key)) {
    // return true;
    // }
    // }
    // return false;
    // }
    //
    // @Override
    // public boolean containsValue(Object value) {
    // for (Map<K, V> map: maps) {
    // if (map.containsValue(value)) {
    // return true;
    // }
    // }
    // return false;
    // }
    //
    // @Override
    // public Set<Entry<K, V>> entrySet() {
    // Set<Entry<K, V>> result = new HashSet<Entry<K,V>>();
    // for (Map<K, V> map: maps) {
    // result.addAll(map.entrySet());
    // }
    // return result;
    // }
    //
    // @Override
    // public V get(Object key) {
    // for (Map<K, V> map: maps) {
    // V value = map.get(key);
    // if (value != null) {
    // return value;
    // }
    // }
    // return null;
    // }
    //
    // @Override
    // public boolean isEmpty() {
    // for (Map<K, V> map: maps) {
    // if (!map.isEmpty()) {
    // return false;
    // }
    // }
    // return true;
    // }
    //
    // @Override
    // public Set<K> keySet() {
    // Set<K> result = new HashSet<K>();
    // for (Map<K, V> map: maps) {
    // result.addAll(map.keySet());
    // }
    // return result;
    // }
    //
    // @Override
    // // Puts into first map
    // public V put(K key, V value) {
    // return maps.get(0).put(key, value);
    // }
    //
    // @Override
    // public void putAll(Map<? extends K, ? extends V> m) {
    // maps.get(0).putAll(m);
    // }
    //
    // @Override
    // public V remove(Object key) {
    // for (Map<K, V> map: maps) {
    // V value = map.remove(key);
    // if (value != null) {
    // return value;
    // }
    // }
    // return null;
    // }
    //
    // @Override
    // public int size() {
    // return keySet().size();
    // }
    //
    // @Override
    // public Collection<V> values() {
    // Collection<V> result = new ArrayList<V>();
    // for (Map<K, V> map: maps) {
    // result.addAll(map.values());
    // }
    // return result;
    // }
    //
    // }

}
