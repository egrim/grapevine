package edu.utexas.ece.mpc.context;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.junit.Assert;
import org.junit.Test;

import com.esotericsoftware.minlog.Log;

public class ContextTest {
	public final static InetAddress BROADCAST_ADDRESS;
	public final static int PING_PORT = 4498;
	
	static {
		InetAddress broadcastAddress = null;
		Enumeration<NetworkInterface> interfaces;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			throw new RuntimeException("Could not retrieve list of interfaces from which to determine broadcast address", e);
		}
		
		if (interfaces == null) {
			throw new RuntimeException("No interfaces exist from which a broadcast address can be determined");
		}
		
		broadcastAddress = null;
		while (interfaces.hasMoreElements()) {
			NetworkInterface iface = interfaces.nextElement();
			for (InterfaceAddress address: iface.getInterfaceAddresses()) {
				broadcastAddress = address.getBroadcast();
				if (broadcastAddress != null) {
					break;
				}
			}
			
			if (broadcastAddress != null) {
				break;
			}
		}
		
		if (broadcastAddress == null) {
			throw new RuntimeException("No interface provided a valid broadcast address");
		}
		
		BROADCAST_ADDRESS = broadcastAddress;
	}

	@Test
	public void test() throws Exception {
		Log.set(Log.LEVEL_TRACE);
		DatagramSocket socket = new ContextShimmedDatagramSocket(PING_PORT);

		PingerThread pinger = new PingerThread();
		pinger.start();
		//pinger.join(); // TODO: remove to make concurrent
		
		DatagramPacket packet = new DatagramPacket(new byte[256], 256);
		socket.receive(packet);
		
		Assert.assertArrayEquals("PING MESSAGE".getBytes(), packet.getData());
		
		ContextHandler handler = ContextHandler.getInstance();
		Assert.assertEquals(Integer.valueOf(1), handler.get(pinger.id, "test value 1"));
		Assert.assertEquals(Integer.valueOf(2), handler.get(pinger.id, "test value 2"));
		Assert.assertNull(handler.get(pinger.id, "test value 3"));
		
	}

	private class PingerThread extends Thread {
		public int id;
		
		@Override
		public void run() {
			HashMapContextSummary summary = new HashMapContextSummary();
			summary.put("test value 1", 1);
			summary.put("test value 2", 2);
			
			id = summary.getId();

			ContextHandler handler = ContextHandler.getInstance();
			handler.addContextSummary(summary);
			
			byte[] message = "PING MESSAGE".getBytes();
			DatagramPacket packet = new DatagramPacket(message, message.length, BROADCAST_ADDRESS, PING_PORT);
			
			try {
				DatagramSocket socket = new ContextShimmedDatagramSocket();
				socket.send(packet);
			} catch (IOException e) {
				throw new RuntimeException("Could not send ping", e);
			}
		}
	}
}
