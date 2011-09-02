/*
 * Copyright (c) 2011, The University of Texas at Austin
 * Produced in the Mobile and Pervasive Computing Lab
 * Originally written by Evan Grim
 * 
 * All rights reserved.
 * 
 * See included LICENSE.txt for licensing details
 * 
 */

package edu.utexas.ece.mpc.bloomier;


import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.utexas.ece.mpc.bloomier.ImmutableBloomierFilter;

public class ImmutableBloomierFilterTest {
	ImmutableBloomierFilter<Integer, Integer> uut;

	@Before
	public void setUp() throws Exception {
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		
		for (int i=0; i < 1000; i++) {
			map.put(i,i);
		}
		
		uut = new ImmutableBloomierFilter<Integer, Integer>(map, map.keySet().size()*10, 10, 32, Integer.class, 10000);
	}
	
	@Test
	public void member() {
		assertEquals(Integer.valueOf(1), uut.get(1));
	}
	
	@Test
	public void notMember() {
		Integer result = uut.get(2000);
		Assert.assertNull(result);
	}

}
