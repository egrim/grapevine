package edu.utexas.ece.mpc.context.shim;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.ObjectBuffer;
import com.esotericsoftware.kryo.serialize.CollectionSerializer;
import com.esotericsoftware.minlog.Log;

import edu.utexas.ece.mpc.context.ContextHandler;
import edu.utexas.ece.mpc.context.serializer.BloomierContextSummarySerializer;
import edu.utexas.ece.mpc.context.summary.BloomierContextSummary;

public class ContextShim {

    public ContextShim() {
        kryo = new Kryo();

        CollectionSerializer summariesSerializer = new CollectionSerializer(kryo);
        summariesSerializer.setCanBeNull(false);
        summariesSerializer.setElementClass(BloomierContextSummary.class,
                                            new BloomierContextSummarySerializer(kryo));

        kryo.register(ArrayList.class, summariesSerializer);

        kryoSerializer = new ObjectBuffer(kryo, 256, Integer.MAX_VALUE);

        contextHandler = ContextHandler.getInstance();
        if (contextHandler.isDebugEnabled()) {
            Log.set(Log.LEVEL_DEBUG);
        }
    }

    public byte[] getContextBytes() {
        ArrayList<BloomierContextSummary> summaries = contextHandler.getSummariesToSend();
        return kryoSerializer.writeObjectData(summaries);
    }

    public void processContextBytes(ByteBuffer buffer) {
        @SuppressWarnings("unchecked")
        ArrayList<BloomierContextSummary> summaries = (ArrayList<BloomierContextSummary>) kryo.readObjectData(buffer,
                                                                                                              ArrayList.class);
        contextHandler.putReceivedSummaries(summaries);
    }

    private ContextHandler contextHandler;
    private Kryo kryo;
    private ObjectBuffer kryoSerializer;
}
