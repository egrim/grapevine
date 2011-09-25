package edu.utexas.ece.mpc.context.serializer;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;

import edu.utexas.ece.mpc.context.ContextHandler;
import edu.utexas.ece.mpc.context.summary.BloomierContextSummary;
import edu.utexas.ece.mpc.context.summary.HashMapContextSummary;

public class BloomierContextSummarySerializer extends Serializer {
    private final Kryo kryo;
    private final ContextHandler contextHandler = ContextHandler.getInstance();

    public BloomierContextSummarySerializer(Kryo kryo) {
        kryo.register(BloomierContextSummary.class, this);

        this.kryo = kryo;
    }

    @Override
    public void writeObjectData(ByteBuffer buffer, Object object) {
        BloomierContextSummary summary;
        if (object instanceof HashMapContextSummary) {
            HashMapContextSummary hSummary = (HashMapContextSummary) object;
            try {
                summary = new BloomierContextSummary(hSummary, hSummary.getHashSeedHint());
            } catch (TimeoutException e) {
                throw new RuntimeException("Could not create bloomier context summary", e);
            }
            hSummary.setHashSeedHint(summary.getHashSeed());
        } else {
            summary = (BloomierContextSummary) object;
        }

        int k = summary.getK();
        int q = summary.getQ();

        long hashSeed = summary.getHashSeed();

        byte[][] table = summary.getTable();
        int tableDimension1 = table.length;
        int tableDimension2 = tableDimension1 > 0 ? table[0].length : 0;

        int id = summary.getId();
        int hops = summary.getHops();
        long timestamp = summary.getTimestamp();

        kryo.writeObjectData(buffer, k);
        kryo.writeObjectData(buffer, q);
        kryo.writeObjectData(buffer, hashSeed);

        // Kryo's ArraySerializer isn't as efficient as the following (even using all possible tuning)
        kryo.writeObjectData(buffer, tableDimension1);
        kryo.writeObjectData(buffer, tableDimension2);
        for (byte[] subArray: table) {
            for (byte element: subArray) {
                kryo.writeObjectData(buffer, element);
            }
        }

        kryo.writeObjectData(buffer, id);
        kryo.writeObjectData(buffer, hops);
        kryo.writeObjectData(buffer, timestamp);
    }

    @Override
    public <T> T readObjectData(ByteBuffer buffer, Class<T> type) {
        int bufferStart = buffer.position();

        int k = kryo.readObjectData(buffer, int.class);
        int q = kryo.readObjectData(buffer, int.class);

        long hashSeed = kryo.readObjectData(buffer, long.class);

        int tableDimension1 = kryo.readObjectData(buffer, int.class);
        int tableDimension2 = kryo.readObjectData(buffer, int.class);
        byte[][] table = new byte[tableDimension1][tableDimension2];
        for (int i = 0; i < tableDimension1; i++) {
            for (int j = 0; j < tableDimension2; j++) {
                table[i][j] = kryo.readObjectData(buffer, byte.class);
            }
        }

        int m = table.length;

        int id = kryo.readObjectData(buffer, int.class);
        int hops = kryo.readObjectData(buffer, int.class);
        long timestamp = kryo.readObjectData(buffer, long.class);

        @SuppressWarnings("unchecked")
        T summary = (T) new BloomierContextSummary(m, k, q, hashSeed, table, id, hops, timestamp);

        int summarySize = buffer.position() - bufferStart;
        contextHandler.logDbg(String.format("Decoded context summary (size=%d): %s", summarySize,
                                            summary));

        return summary;
    }

}
