import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;

public class ContextTestDriver implements Runnable {

    private static final int DEFAULT_FILL_ITEMS = 100;
    private static InetAddress BROADCAST_ADDRESS;

    private int numNodes;
    private float connectivityRadius;
    private long seed;
    private List<NodeInfo> nodes;

    private Random rand;
    private int runtime;
    private boolean skipFirst;
    private int fillItems;

    static {
        InetAddress broadcastAddress = null;
        Enumeration<NetworkInterface> interfaces;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new RuntimeException(
                                       "Could not retrieve list of interfaces from which to determine broadcast address",
                                       e);
        }

        if (interfaces == null) {
            throw new RuntimeException(
                                       "No interfaces exist from which a broadcast address can be determined");
        }

        broadcastAddress = null;
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            for (InterfaceAddress address : iface.getInterfaceAddresses()) {
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

    public ContextTestDriver(int numNodes, float connectivityRadius, int runtime, int fillItems,
                             long seed, boolean skipFirst) {
        this.numNodes = numNodes;
        this.connectivityRadius = connectivityRadius;
        this.runtime = runtime;
        this.fillItems = fillItems;
        this.seed = seed;
        this.skipFirst = skipFirst;

        rand = new Random(seed);
    }

    @Override
    public void run() {
        System.out.println("Simulating network of " + numNodes + " nodes with "
                           + connectivityRadius + " connectivity radius (randomization seed: "
                           + seed + ")");

        nodes = new ArrayList<NodeInfo>(numNodes);
        for (int i = 0; i < numNodes; i++) {
            nodes.add(new NodeInfo());
        }

        Collections.sort(nodes, new Comparator<NodeInfo>() {

            @Override
            public int compare(NodeInfo o1, NodeInfo o2) {
                return Double.compare(o1.x, o2.x);
            }
        });

        double[][] distances = new double[numNodes][numNodes];
        for (int i = numNodes - 1; i > 0; i--) {
            for (int j = 0; j < i; j++) {
                NodeInfo me = nodes.get(i);
                NodeInfo them = nodes.get(j);
                double distance = Math.sqrt(Math.pow(me.x - them.x, 2) + Math.pow(me.y - them.y, 2));
                distances[i][j] = distances[j][i] = distance;
            }
        }

        for (int i = 0; i < nodes.size(); i++) {
            NodeInfo node = nodes.get(i);
            node.id = i;

            List<Integer> nodesInRange = new ArrayList<Integer>();
            for (int j = 0; j < nodes.size(); j++) {
                if (distances[i][j] < connectivityRadius && i != j) {
                    nodesInRange.add(j);
                }
            }

            node.neighbors = new int[nodesInRange.size()];
            for (int k = 0; k < nodesInRange.size(); k++) {
                node.neighbors[k] = nodesInRange.get(k);
            }
        }

        System.out.println("Generated the following nodes");
        for (NodeInfo node : nodes) {
            System.out.println(node);
        }
        System.out.println();

        System.out.println("Starting beacon for each node");
        try {
            try {
                for (final NodeInfo node: nodes) {
                    List<String> command = new ArrayList<String>();
                    command.add("java");
                    command.add("-jar");
                    command.add("node.jar");
                    command.add(Integer.valueOf(node.id).toString());
                    command.add(Double.valueOf(node.x).toString());
                    command.add(Double.valueOf(node.x).toString());
                    command.add(Integer.valueOf(fillItems).toString());
                    command.add(BROADCAST_ADDRESS.toString().substring(1));

                    for (int neighbor: node.neighbors) {
                        command.add(Integer.valueOf(neighbor).toString());
                    }

                    // Handle skipFirst
                    if (skipFirst && node.id == 0) {
                        System.out.println("Skipping first node, command needed: " + command);
                        continue;
                    }

                    System.out.println("Starting process - command=" + command.toString());

                    final Process process = new ProcessBuilder(command).redirectErrorStream(true)
                                                                       .start();

                    node.process = process;
                    node.processIoHandlerThread = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                InputStream in = process.getInputStream();
                                InputStreamReader isr = new InputStreamReader(in);
                                BufferedReader br = new BufferedReader(isr);
                                String line;
                                while ((line = br.readLine()) != null) {
                                    System.out.printf("[%s] %s\n", node.id, line);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    node.processIoHandlerThread.setDaemon(true);
                    node.processIoHandlerThread.start();
                }
            } catch (IOException e) {
                throw new RuntimeException("Problem starting node", e);
            }
            try {
                Thread.sleep(runtime);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } finally {
            for (NodeInfo node: nodes) {
                if (node.process != null) {
                    node.process.destroy();
                }
            }
        }
    }

    private class NodeInfo {

        public NodeInfo() {
            x = rand.nextDouble();
            y = rand.nextDouble();
        }

        public String toString() {
            return "Node " + id + " @ (" + x + ":" + y + ") with neighbors: "
                   + Arrays.toString(neighbors);
        }

        public int id;
        public double x;
        public double y;
        public int[] neighbors;
        public Process process;
        public Thread processIoHandlerThread;

    }

    public static void main(String[] args) {

        int numNodes = 0;
        try {
            numNodes = Integer.valueOf(args[0]);
        } catch (Exception e) {
            System.err.println("Must specify the number of nodes to simulate");
            System.exit(-1);
        }

        float connectivityRadius = 0;
        try {
            connectivityRadius = Float.valueOf(args[1]);
        } catch (Exception e) {
            System.err.println("Must specify conectivity radius for nodes");
            System.exit(-1);
        }

        int runtime = 0;
        try {
            runtime = Integer.valueOf(args[2]);
        } catch (Exception e) {
            System.err.println("Must specify length of simulation (in milliseconds)");
            System.exit(-1);
        }

        int fillItems = DEFAULT_FILL_ITEMS;
        try {
            fillItems = Integer.valueOf(args[3]);
        } catch (Exception e) {
            // Ignore
        }

        long seed = 0;
        try {
            seed = Long.valueOf(args[4]);
        } catch (ArrayIndexOutOfBoundsException e) {
            seed = new Random().nextLong();
        } catch (Exception e) {
            System.err.println("Could not interpret provided seed");
            System.exit(-1);
        }

        boolean skipFirst = false;
        try {
            skipFirst = Boolean.valueOf(args[5]);
        } catch (ArrayIndexOutOfBoundsException e) {
            // Ignore (just use default value)
        } catch (Exception e) {
            System.err.println("Could not interpret skipFirst argument: " + e);
            System.exit(-1);
        }

        ContextTestDriver simulation = new ContextTestDriver(numNodes, connectivityRadius, runtime,
                                                             fillItems,
                                                             seed, skipFirst);
        simulation.run();
    }

}
