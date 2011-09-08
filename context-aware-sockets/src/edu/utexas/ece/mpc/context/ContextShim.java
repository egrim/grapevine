package edu.utexas.ece.mpc.context;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.ObjectBuffer;
import com.esotericsoftware.kryo.serialize.CollectionSerializer;

public class ContextShim {

	public ContextShim() {
		kryo = new Kryo();
		
		CollectionSerializer summariesSerializer = new CollectionSerializer(kryo);
		summariesSerializer.setCanBeNull(false);
		summariesSerializer.setElementClass(BloomierContextSummary.class, new BloomierContextSummarySerializer(kryo));
		
		kryo.register(ArrayList.class, summariesSerializer);
		
		kryoSerializer = new ObjectBuffer(kryo, 256, Integer.MAX_VALUE);
		
		contextHandler = ContextHandler.getInstance();
	}
	
	public byte[] getContextBytes() {
		ArrayList<BloomierContextSummary> summaries = contextHandler.getSummariesToSend();
		return kryoSerializer.writeObjectData(summaries);
	}
	
	public void processContextBytes(ByteBuffer buffer) {
		@SuppressWarnings("unchecked")
		ArrayList<BloomierContextSummary> summaries = (ArrayList<BloomierContextSummary>) kryo.readObjectData(buffer, ArrayList.class);
		contextHandler.addOrUpdateReceivedSummaries(summaries);
	}

	private ContextHandler contextHandler;
	private Kryo kryo;
	private ObjectBuffer kryoSerializer;
}
