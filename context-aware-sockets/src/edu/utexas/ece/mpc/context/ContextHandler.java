package edu.utexas.ece.mpc.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ContextHandler {
	private static ContextHandler me = new ContextHandler();
	
	private Map<Integer, ContextSummary> summaries = new HashMap<Integer, ContextSummary>();
	
	// Note: Private to prevent anyone else from creating what is intended to be a singleton
	private ContextHandler() {
		
	}

	public static synchronized ContextHandler getInstance() {
		if (me == null) {
			me = new ContextHandler();
		}
		
		return me;
	}

	public void addContextSummary(ContextSummary summary) {
		Integer id = summary.getId();
		summaries.put(id, summary);
	}
	
	public void addContextSummaries(Collection<ContextSummary> summaries) {
		for (ContextSummary summary: summaries) {
			addContextSummary(summary);
		}
	}
	
	public ContextSummary get(int id) {
		ContextSummary summary = summaries.get(id);
		
		if (summary == null) {
			throw new IllegalArgumentException("No context summary available with provided id: " + id);
		}
		
		return summary;
	}
	
	public Integer get(int id, String key) {
		ContextSummary summary = get(id);		
		return summary.get(key);
	}

	public Collection<BloomierContextSummary> getBloomierSummaries() {
		try {
			ArrayList<BloomierContextSummary> bloomierSummaries = new ArrayList<BloomierContextSummary>(summaries.size());
			for (ContextSummary summary: summaries.values()) {
				if (summary instanceof HashMapContextSummary) {
					summary = new BloomierContextSummary((HashMapContextSummary)summary);
				}
				bloomierSummaries.add((BloomierContextSummary)summary);
			}
			
			return bloomierSummaries;
		} catch (Exception e) {
			throw new RuntimeException("Could not retrieve context summaries", e);
		}
	}
}
