package edu.utexas.ece.mpc.context.serializer;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;

import edu.utexas.ece.mpc.context.ContextHandler;
import edu.utexas.ece.mpc.context.summary.HashMapContextSummary;
import edu.utexas.ece.mpc.context.summary.HashMapWireContextSummary;

public class LabeledContextSummarySerializer extends Serializer {

    private Kryo kryo;
    private ContextHandler contextHandler = ContextHandler.getInstance();

    public LabeledContextSummarySerializer(Kryo kryo) {
        kryo.register(HashMapWireContextSummary.class, this);

        this.kryo = kryo;
    }

    @Override
    public void writeObjectData(ByteBuffer buffer, Object object) {
        HashMapWireContextSummary wSummary;
        if (object instanceof HashMapContextSummary) {
            HashMapContextSummary summary = (HashMapContextSummary) object;
            wSummary = new HashMapWireContextSummary(summary);
        } else {
            wSummary = (HashMapWireContextSummary) object;
        }

        kryo.writeObjectData(buffer, wSummary.getId());
        kryo.writeObjectData(buffer, wSummary.getHops());
        kryo.writeObjectData(buffer, wSummary.getTimestamp());

        kryo.writeObjectData(buffer, wSummary.size());
        for (String key: wSummary.keySet()) {
            kryo.writeObjectData(buffer, key);
            kryo.writeObjectData(buffer, wSummary.get(key));
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

        HashMapWireContextSummary wSummary = new HashMapWireContextSummary(summary, hops, timestamp);

        int summarySize = buffer.position() - bufferStart;
        contextHandler.logDbg(String.format("Decoded context summary (size=%d): %s", summarySize,
                                            wSummary));
        @SuppressWarnings("unchecked")
        T retVal = (T) wSummary;
        return retVal;
    }

}
