package edu.utexas.ece.mpc.context;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.serialize.ArraySerializer;

public class BloomierContextSummarySerializer extends Serializer {
	private final Kryo kryo;

	public BloomierContextSummarySerializer(Kryo kryo) {
		super();
		
		ArraySerializer arraySerializer = new ArraySerializer(kryo);
		arraySerializer.setDimensionCount(2);
		arraySerializer.setElementsAreSameType(true);
		
		kryo.register(byte[][].class, arraySerializer);
		
		this.kryo = kryo;
	}

	@Override
	public void writeObjectData(ByteBuffer buffer, Object object) {
		try {
			BloomierContextSummary summary = (BloomierContextSummary) object;
			
			int m = summary.getM();
			int k = summary.getK();
			int q = summary.getQ();
			
			long hashSeed = summary.getHashSeed();
			
			byte[][] table = summary.getTable();
			
			kryo.writeObjectData(buffer, m);
			kryo.writeObjectData(buffer, k);
			kryo.writeObjectData(buffer, q);
			kryo.writeObjectData(buffer, hashSeed);
			kryo.writeObjectData(buffer, table);
		} catch (Exception e) {
			throw new RuntimeException("Could not serialize provided object", e);
		}
	}

	@Override
	public <T> T readObjectData(ByteBuffer buffer, Class<T> type) {
		int m = kryo.readObjectData(buffer, int.class);
		int k = kryo.readObjectData(buffer, int.class);
		int q = kryo.readObjectData(buffer, int.class);
		
		long hashSeed = kryo.readObjectData(buffer, long.class);
		
		byte[][] table = kryo.readObjectData(buffer, byte[][].class);
		
		@SuppressWarnings("unchecked")
		T summary = (T) new BloomierContextSummary(m, k, q, hashSeed, table);
		return summary;
	}
	
}
