package edu.utexas.ece.mpc.context.shim;

import java.net.DatagramPacket;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class DatagramContextShim extends ContextShim {

    public DatagramPacket getSendPacket(DatagramPacket p) throws SocketException {
        byte[] contextBytes = getContextBytes();
        contextHandler.logDbg("Packet prepared for send has context with size="
                              + contextBytes.length);

        int payloadLength = p.getLength();

        byte[] bytesToSend = new byte[PAYLOAD_LENGTH_FIELD_SIZE + payloadLength
                                      + contextBytes.length];

        ByteBuffer bytesToSendBuffer = ByteBuffer.wrap(bytesToSend);
        bytesToSendBuffer.putInt(payloadLength);
        bytesToSendBuffer.put(p.getData(), p.getOffset(), p.getLength());
        bytesToSendBuffer.put(contextBytes);

        return new DatagramPacket(bytesToSend, bytesToSend.length, p.getSocketAddress());
    }

    public DatagramPacket getReceivePacket(DatagramPacket p) {
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        while (receivePacket.getLength() < p.getLength() + PAYLOAD_LENGTH_FIELD_SIZE) {
            increaseReceiveBufferSize();
            receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        }

        return receivePacket;
    }

    public void processReceivedPacket(DatagramPacket receivePacket, DatagramPacket p) {
        ByteBuffer receivedBytesBuffer = ByteBuffer.wrap(receivePacket.getData(),
                                                         receivePacket.getOffset(),
                                                         receivePacket.getLength());
        int payloadLength = receivedBytesBuffer.getInt();
        byte[] payloadData = new byte[payloadLength];
        receivedBytesBuffer.get(payloadData);

        int packetLength;
        int packetOffset = p.getOffset();
        byte[] packetData = p.getData();
        for (packetLength = 0; packetLength < p.getLength() && packetLength < payloadLength; packetLength++) {
            packetData[packetOffset + packetLength] = payloadData[packetLength];
        }
        p.setLength(payloadLength);

        try {
            int start = receivedBytesBuffer.position();
            processContextBytes(receivedBytesBuffer);
            contextHandler.logDbg("Received packet had context information with size="
                                  + (receivedBytesBuffer.position() - start));
        } catch (Exception e) { // FIXME: catch (or modify context shim to throw) appropriate exception
            contextHandler.logDbg("Receive buffer size exceeded (context information discarded), growing buffer");
            increaseReceiveBufferSize();
        }
    }

    private void increaseReceiveBufferSize() {
        receiveBuffer = new byte[receiveBuffer.length * 2];
        contextHandler.logDbg("Receive buffer size increased to " + receiveBuffer.length);
    }

    private static final int PAYLOAD_LENGTH_FIELD_SIZE = Integer.SIZE / Byte.SIZE;

    private byte[] receiveBuffer = new byte[DEFAULT_RECEIVE_PACKET_SIZE];
    private static final int DEFAULT_RECEIVE_PACKET_SIZE = 2 * 1024;
}
