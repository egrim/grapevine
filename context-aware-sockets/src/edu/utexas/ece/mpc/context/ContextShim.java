package edu.utexas.ece.mpc.context;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.esotericsoftware.kryo.CustomSerialization;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.ObjectBuffer;
import com.esotericsoftware.kryo.serialize.CollectionSerializer;

public class ContextShim implements CustomSerialization {

	public ContextShim() {
		kryo = new Kryo();
		
		CollectionSerializer summariesSerializer = new CollectionSerializer(kryo);
		summariesSerializer.setCanBeNull(false);
		summariesSerializer.setElementClass(BloomierContextSummary.class, new BloomierContextSummarySerializer(kryo));
		
		kryo.register(ArrayList.class, summariesSerializer);
		
		kryoSerializer = new ObjectBuffer(kryo, 256, Integer.MAX_VALUE);
		
		contextHandler = ContextHandler.getInstance();
	}
	
	public byte[] injectContextBytes(byte[] data) {
		ArrayList<ContextSummary> summaries = new ArrayList<ContextSummary>(contextHandler.getBloomierSummaries()); // create new list so that serialized type matches what is expected
		byte[] shimBytes = kryoSerializer.writeObjectData(summaries);
		
		byte[] newData = new byte[shimBytes.length + data.length];
		System.arraycopy(shimBytes, 0, newData, 0, shimBytes.length);
		System.arraycopy(data, 0, newData, shimBytes.length, data.length);
		
		return newData;
	}

	public byte[] extractContextBytes(byte[] data) {
		// TODO: handle errors (currently makes assumptions about data)
		ByteBuffer buffer = ByteBuffer.wrap(data);
		@SuppressWarnings("unchecked")
		ArrayList<ContextSummary> summaries = (ArrayList<ContextSummary>) kryo.readObjectData(buffer, ArrayList.class);
		contextHandler.addContextSummaries(summaries);
		byte[] newData = new byte[buffer.remaining()];
		buffer.get(newData);
		return newData;
	}
	
	private ContextHandler contextHandler;
	private Kryo kryo;
	private ObjectBuffer kryoSerializer;
	@Override
	public void readObjectData(Kryo arg0, ByteBuffer arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeObjectData(Kryo arg0, ByteBuffer arg1) {
		// TODO Auto-generated method stub
		
	}
}
