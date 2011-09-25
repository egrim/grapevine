import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import edu.utexas.ece.mpc.context.ContextHandler;
import edu.utexas.ece.mpc.context.ContextHandler.WireSummaryType;
import edu.utexas.ece.mpc.context.logger.SysoutLoggingDelegate;
import edu.utexas.ece.mpc.context.net.ContextShimmedDatagramSocket;
import edu.utexas.ece.mpc.context.summary.ContextSummary;
import edu.utexas.ece.mpc.context.summary.HashMapContextSummary;

public class Node {
    private static int BASE_PORT = 5000;

    public static void main(String[] args) throws Exception {
        int id = 0;
        double x = 0;
        double y = 0;
        int fillItems = 0;
        InetAddress address = null;
        try {
            id = Integer.valueOf(args[0]);
            x = Double.valueOf(args[1]);
            y = Double.valueOf(args[2]);
            fillItems = Integer.valueOf(args[3]);
            address = InetAddress.getByName(args[4]);
        } catch (Exception e) {
            System.out.println("Usage: <Node.class> <id> <x> <y> <fillItems> <sendAdress> <connectedToId> [<connectedToId]*");
            System.exit(-1);
        }

        String[] connectedToIdArgs;
        try {
            connectedToIdArgs = Arrays.copyOfRange(args, 5, args.length);
        } catch (ArrayIndexOutOfBoundsException e) {
            connectedToIdArgs = new String[0];
        }

        int[] connectedToIds = new int[connectedToIdArgs.length];
        for (int i = 0; i < connectedToIds.length; i++) {
            connectedToIds[i] = Integer.valueOf(connectedToIdArgs[i]);
        }

        System.out.println("Starting node with id=" + id + " position=(" + x + ":" + y
                           + ") fillItems=" + fillItems + " sending to " + address
                           + " connected to nodes " + Arrays.toString(connectedToIds));

        HashMapContextSummary summary = new HashMapContextSummary(id);
        summary.put("location: x", (int) (x * 100000));
        summary.put("location: y", (int) (y * 100000));
        for (int i = 0; i < connectedToIds.length; i++) {
            summary.put("neighbor: " + i, connectedToIds[i]);
        }

        Random rand = new Random(id);
        for (int i = 0; i < fillItems; i++) {
            summary.put("fill: " + i, rand.nextInt());
        }

        ContextHandler handler = ContextHandler.getInstance();
        handler.setWireSummaryType(WireSummaryType.LABELED);
        // handler.setWireSummaryType(WireSummaryType.BLOOMIER);
        handler.setLoggerDelegate(new SysoutLoggingDelegate());
        handler.putLocalSummary(summary);

        final DatagramSocket receiveSocket = new ContextShimmedDatagramSocket(BASE_PORT + id);
        Thread receiver = new Thread(new Runnable() {

            @Override
            public void run() {
                DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
                while (true) {
                    try {
                        receiveSocket.receive(packet);

                        System.out.println("Received packet: "
                                           + new String(packet.getData(), packet.getOffset(),
                                                        packet.getLength()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

        });
        receiver.setDaemon(true);
        receiver.start();

        DatagramSocket sendSocket = new ContextShimmedDatagramSocket();
        sendSocket.setBroadcast(true); // Just in case broadcast is desired

        DatagramPacket sendPacket = new DatagramPacket(new byte[1024], 1024);
        sendPacket.setAddress(address);

        int sequenceNum = 1;
        while (true) {
            for (int connectedToId : connectedToIds) {
                int sendPort = BASE_PORT + connectedToId;
                sendPacket.setData(("Packet burst number " + sequenceNum + " from " + id).getBytes());
                sendPacket.setPort(sendPort);

                sendSocket.send(sendPacket);
            }

            sequenceNum++;

            List<ContextSummary> receivedSummaries = handler.getReceivedSummaries();
            int[] receivedSummaryIds = new int[receivedSummaries.size()];
            for (int i = 0; i < receivedSummaries.size(); i++) {
                receivedSummaryIds[i] = receivedSummaries.get(i).getId();
            }
            System.out.println("Summaries received: " + Arrays.toString(receivedSummaryIds));

            Thread.sleep(1000);
        }
    }

}
