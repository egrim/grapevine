package edu.utexas.ece.mpc.context.serializer;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;

import edu.utexas.ece.mpc.context.ContextHandler;
import edu.utexas.ece.mpc.context.summary.HashMapContextSummary;
import edu.utexas.ece.mpc.context.summary.LabeledContextSummary;

public class LabeledContextSummarySerializer extends Serializer {

    private Kryo kryo;
    private ContextHandler contextHandler = ContextHandler.getInstance();

    public LabeledContextSummarySerializer(Kryo kryo) {
        this.kryo = kryo;
    }

    @Override
    public void writeObjectData(ByteBuffer buffer, Object object) {
        // TODO: detect wire type changes that will cause the following cast to fail
        LabeledContextSummary summary = (LabeledContextSummary) object;

        kryo.writeObjectData(buffer, summary.getId());
        kryo.writeObjectData(buffer, summary.getHops());
        kryo.writeObjectData(buffer, summary.getTimestamp());

        kryo.writeObjectData(buffer, summary.size());
        for (String key: summary.keySet()) {
            kryo.writeObjectData(buffer, key);
            kryo.writeObjectData(buffer, summary.get(key));
        }
    
    }

    @Override
    public <T> T readObjectData(ByteBuffer buffer, Class<T> type) {
        int bufferStart = buffer.position();

        int id = kryo.readObjectData(buffer, int.class);
        int hops = kryo.readObjectData(buffer, int.class);
        long timestamp = kryo.readObjectData(buffer, long.class);

        HashMapContextSummary summary = new HashMapContextSummary(id);

        int size = kryo.readObjectData(buffer, int.class);
        for (int i = 0; i < size; i++) {
            String key = kryo.readObjectData(buffer, String.class);
            int value = kryo.readObjectData(buffer, int.class);

            summary.put(key, value);
        }

        LabeledContextSummary wSummary = new LabeledContextSummary(summary, hops, timestamp);

        int summarySize = buffer.position() - bufferStart;
        contextHandler.logDbg(String.format("Decoded context summary (size=%d): %s", summarySize,
                                            wSummary));
        @SuppressWarnings("unchecked")
        T retVal = (T) wSummary;
        return retVal;
    }

}
