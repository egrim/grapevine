package edu.utexas.ece.mpc.context;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Arrays;

public class ContextShimmedDatagramSocket extends DatagramSocket {
	
	public ContextShimmedDatagramSocket() throws SocketException {
		super();
	}

	protected ContextShimmedDatagramSocket(DatagramSocketImpl impl) {
		super(impl);
	}

	public ContextShimmedDatagramSocket(int port, InetAddress laddr)
			throws SocketException {
		super(port, laddr);
	}

	public ContextShimmedDatagramSocket(int port) throws SocketException {
		super(port);
	}

	public ContextShimmedDatagramSocket(SocketAddress bindaddr)
			throws SocketException {
		super(bindaddr);
	}
	
	@Override
	public void send(DatagramPacket p) throws IOException {
		byte[] shimmedBytes = contextShim.injectContextBytes(p.getData());
		p.setData(shimmedBytes);
		super.send(p);
	}
	
	@Override
	public synchronized void receive(DatagramPacket p) throws IOException {
		super.receive(p);
		byte[] originalBytes = contextShim.extractContextBytes(Arrays.copyOf(p.getData(), p.getLength())); // TODO: is this additional copy worth it? (simplicity of not sending length to extraction method)
		p.setData(originalBytes);
	}

	private static ContextShim contextShim = new ContextShim();
}
