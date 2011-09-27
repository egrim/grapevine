package edu.utexas.ece.mpc.context.shim;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.ObjectBuffer;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.minlog.Log;

import edu.utexas.ece.mpc.context.ContextHandler;
import edu.utexas.ece.mpc.context.serializer.BloomierContextSummarySerializer;
import edu.utexas.ece.mpc.context.serializer.LabeledContextSummarySerializer;
import edu.utexas.ece.mpc.context.summary.BloomierContextSummary;
import edu.utexas.ece.mpc.context.summary.ContextSummary;
import edu.utexas.ece.mpc.context.summary.HashMapContextSummary;
import edu.utexas.ece.mpc.context.summary.LabeledContextSummary;
import edu.utexas.ece.mpc.context.summary.WireContextSummary;

public class ContextShim {

    protected ContextHandler contextHandler;
    protected Kryo kryo;
    protected ObjectBuffer buffer;

    public ContextShim() {
        kryo = new Kryo();
        kryo.register(ArrayList.class, new ContextSummarySerializer());
        kryo.register(BloomierContextSummary.class, new BloomierContextSummarySerializer(kryo));
        kryo.register(LabeledContextSummary.class, new LabeledContextSummarySerializer(kryo));

        buffer = new ObjectBuffer(kryo, 2 * 1024, Integer.MAX_VALUE);

        contextHandler = ContextHandler.getInstance();

        if (contextHandler.isDebugEnabled()) {
            Log.set(Log.LEVEL_DEBUG);
        }
    }

    public byte[] getContextBytes() {
        ArrayList<WireContextSummary> summaries = contextHandler.getSummariesToSend();
        return buffer.writeObjectData(summaries);
    }

    public void processContextBytes(ByteBuffer buffer) {
        @SuppressWarnings("unchecked")
        ArrayList<WireContextSummary> summaries = (ArrayList<WireContextSummary>) kryo.readObjectData(buffer,
                                                                                                              ArrayList.class);
        contextHandler.handleIncomingSummaries(summaries);
    }

    private class ContextSummarySerializer extends Serializer {

        @Override
        public void writeObjectData(ByteBuffer buffer, Object object) {
            @SuppressWarnings("unchecked")
            ArrayList<ContextSummary> summaries = (ArrayList<ContextSummary>) object;
            
            kryo.writeObjectData(buffer, summaries.size());

            for (ContextSummary summary: summaries) {
                kryo.writeObjectData(buffer, summary);
            }

        }

        @Override
        public <T> T readObjectData(ByteBuffer buffer, Class<T> type) {
            ArrayList<WireContextSummary> summaries = new ArrayList<WireContextSummary>();

            updateWireSummarySerializationConfig();

            int size = kryo.readObjectData(buffer, int.class);
            for (int i = 0; i < size; i++) {
                summaries.add(kryo.readObjectData(buffer, WireContextSummary.class));
            }

            @SuppressWarnings("unchecked")
            T retVal = (T) summaries;
            return retVal;
        }

        private Serializer getCurrentWireSummarySerializer() {
            Serializer serializer = null;
            switch (contextHandler.getWireSummaryType()) {
                case BLOOMIER:
                    serializer = kryo.getSerializer(BloomierContextSummary.class);
                    break;
                case LABELED:
                    serializer = kryo.getSerializer(LabeledContextSummary.class);
                    break;
            }
            return serializer;
        }

        private void updateWireSummarySerializationConfig() {
            Serializer currentSerializer = getCurrentWireSummarySerializer();
            kryo.register(HashMapContextSummary.class, currentSerializer);
            kryo.register(LabeledContextSummary.class, currentSerializer);
            kryo.register(WireContextSummary.class, currentSerializer);
        }
    }
}
