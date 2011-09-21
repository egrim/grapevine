package edu.utexas.ece.mpc.context.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;

import edu.utexas.ece.mpc.context.shim.DatagramContextShim;

public class ContextShimmedDatagramSocket extends DatagramSocket {

    public ContextShimmedDatagramSocket() throws SocketException {
        super();
    }

    protected ContextShimmedDatagramSocket(DatagramSocketImpl impl) {
        super(impl);
    }

    public ContextShimmedDatagramSocket(int port, InetAddress laddr) throws SocketException {
        super(port, laddr);
    }

    public ContextShimmedDatagramSocket(int port) throws SocketException {
        super(port);
    }

    public ContextShimmedDatagramSocket(SocketAddress bindaddr) throws SocketException {
        super(bindaddr);
    }

    @Override
    public void send(DatagramPacket p) throws IOException {
        super.send(shim.getSendPacket(p));
    }

    @Override
    public synchronized void receive(DatagramPacket p) throws IOException {
        DatagramPacket receivePacket = shim.getReceivePacket(p);
        super.receive(receivePacket);
        shim.processReceivedPacket(receivePacket, p);
    }

    private static DatagramContextShim shim = new DatagramContextShim();

}
