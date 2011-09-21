package edu.utexas.ece.mpc.context.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketAddress;

import edu.utexas.ece.mpc.context.shim.DatagramContextShim;

public class ContextShimmedMulticastSocket extends MulticastSocket {

    public ContextShimmedMulticastSocket() throws IOException {
        super();
    }

    public ContextShimmedMulticastSocket(int port) throws IOException {
        super(port);
    }

    public ContextShimmedMulticastSocket(SocketAddress bindaddr) throws IOException {
        super(bindaddr);
    }

    @Override
    public void send(DatagramPacket p) throws IOException {
        super.send(shim.getSendPacket(p));
    }

    @Override
    @Deprecated
    public void send(DatagramPacket p, byte ttl) throws IOException {
        super.send(shim.getSendPacket(p), ttl);
    }

    @Override
    public synchronized void receive(DatagramPacket p) throws IOException {
        DatagramPacket receivePacket = shim.getReceivePacket(p);
        super.receive(receivePacket);
        shim.processReceivedPacket(receivePacket, p);
    }

    private static DatagramContextShim shim = new DatagramContextShim();

}
