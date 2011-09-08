package edu.utexas.ece.mpc.context;

import java.net.DatagramPacket;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class DatagramContextShim extends ContextShim {

    public DatagramPacket getSendPacket(DatagramPacket p) throws SocketException {
        int payloadLength = p.getLength();
        byte[] contextBytes = getContextBytes();
        byte[] bytesToSend = new byte[payloadLengthFieldSize + payloadLength + contextBytes.length];

        ByteBuffer bytesToSendBuffer = ByteBuffer.wrap(bytesToSend);
        bytesToSendBuffer.putInt(payloadLength);
        bytesToSendBuffer.put(p.getData(), p.getOffset(), p.getLength());
        bytesToSendBuffer.put(contextBytes);

        return new DatagramPacket(bytesToSend, bytesToSend.length, p.getSocketAddress());
    }

    public DatagramPacket getReceivePacket(DatagramPacket p) {
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        while (receivePacket.getLength() < p.getLength() + payloadLengthFieldSize) {
            increaseReceiveBufferSize();
            receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        }

        return receivePacket;
    }

    public void processReceivedPacket(DatagramPacket receivePacket, DatagramPacket p) {
        ByteBuffer receivedBytesBuffer = ByteBuffer.wrap(receiveBuffer);
        int payloadLength = receivedBytesBuffer.getInt();
        receivedBytesBuffer.get(p.getData(), p.getOffset(), Math.min(payloadLength, p.getLength()));

        try {
            processContextBytes(receivedBytesBuffer);
        } catch (Exception e) { // FIXME: catch (or modify context shim to throw) appropriate exception
            e.printStackTrace();
            increaseReceiveBufferSize();
        }
    }

    private void increaseReceiveBufferSize() {
        receiveBuffer = new byte[receiveBuffer.length * 2];
        System.out.println("Receive buffer size incrase to " + receiveBuffer.length);
    }

    private static final int payloadLengthFieldSize = Integer.SIZE / Byte.SIZE;

    private byte[] receiveBuffer = new byte[DEFAULT_RECEIVE_PACKET_SIZE];
    private static final int DEFAULT_RECEIVE_PACKET_SIZE = 2 * 1024;
}
