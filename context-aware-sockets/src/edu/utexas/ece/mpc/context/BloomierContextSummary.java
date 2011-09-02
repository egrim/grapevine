package edu.utexas.ece.mpc.context;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import edu.utexas.ece.mpc.bloomier.ImmutableBloomierFilter;

public class BloomierContextSummary extends ImmutableBloomierFilter<String, Integer> implements ContextSummary {
	public BloomierContextSummary(Map<String, Integer> map) throws TimeoutException {
		super(map,  // FIXME: magic numbers here (probably should be tunable through constructor with sane defaults)
			  (int)(map.keySet().size() * 1.20), // make m 20% bigger than it needs to be
			  Math.max(1, (int)(map.keySet().size() * 1.20 * .1)),  // make k 10% of m (but at least 1)
			  (int)(Integer.SIZE * 1.30), // make q 30% bigger than the Integers stored in it
			  Integer.class, 
			  1000); // don't take more than 1 second to make the structure
	}
	
	public BloomierContextSummary(int m, int k, int q, long hashSeed, byte[][] table) {
		super(m, k, q, Integer.class, hashSeed, table);
	}

	@Override
	public int getId() {
		return get(CONTEXT_IDENTIFIER);
	}
	
	private static final String CONTEXT_IDENTIFIER = "_context_identifier_id";

}
