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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import edu.utexas.ece.mpc.context.ContextHandler.WireSummaryType;

public class ContextTestDriver {

    private static InetAddress BROADCAST_ADDRESS;
    private static Random rand = new Random();

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

    private static class NodeInfo {

        public NodeInfo() {
            x = rand.nextDouble();
            y = rand.nextDouble();
        }

        public NodeInfo(double x, double y) {
            this.x = x;
            this.y = y;
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

    public static class CLIOptions {
        public static final String NUM_NODES = "nodes";
        public static final String CONNECTIVITY_RADIUS = "connectivityRadius";
        public static final String RUN_LENGTH = "runLength";
        public static final String FILL_TO = "fillTo";
        public static final String TAU = "tau";
        public static final String SEND_INTERVAL = "sendInterval";
        public static final String WIRE_TYPE = "wireType";
        public static final String GROUP_TYPE = "groupType";
        public static final String SKIP_FIRST = "skipFirst";
        public static final String SEED = "seed";
        public static final String HASH_SEED_HINT = "hashSeedHint";
        public static final String GRID = "grid";

        public static final int DEFAULT_FILL_TO = 100;
        public static final int DEFAULT_TAU = 3;
        public static final int DEFAULT_SEND_INTERVAL = 300;
        public static final WireSummaryType DEFAULT_WIRE_TYPE = WireSummaryType.BLOOMIER;
        public static final Node.GroupType DEFAULT_GROUP_TYPE = Node.GroupType.NONE;
    }

    @SuppressWarnings("static-access")
    public static void main(String[] args) {
        Option nodeOption = OptionBuilder.withDescription("number of nodes").withType(int.class)
                                         .hasArg().withArgName("NUM").isRequired()
                                         .withLongOpt(CLIOptions.NUM_NODES).create();
        Option radiusOption = OptionBuilder.withDescription("radius of connectivity")
                                           .withType(int.class).hasArg().withArgName("RADIUS")
                                           .isRequired()
                                           .withLongOpt(CLIOptions.CONNECTIVITY_RADIUS).create();
        Option runtimeOption = OptionBuilder.withDescription("length to run (in ms)")
                                            .withType(int.class).hasArg().withArgName("TIME_MS")
                                            .isRequired().withLongOpt(CLIOptions.RUN_LENGTH)
                                            .create();
        Option fillToOption = OptionBuilder.withDescription("number of context items to include (0=no context)")
                                           .withType(int.class).hasArg().withArgName("FILLTO")
                                           .withLongOpt(CLIOptions.FILL_TO).create();
        Option tauOption = OptionBuilder.withDescription("upper limit on hopcount for forwarding context summaries")
                                        .withType(int.class).hasArg().withArgName("TAU")
                                        .withLongOpt(CLIOptions.TAU).create();
        Option sendIntervalOption = OptionBuilder.withDescription("internal (in ms) to send outgoing packets")
                                                 .withType(int.class).hasArg()
                                                 .withArgName("INTERVAL")
                                                 .withLongOpt(CLIOptions.SEND_INTERVAL).create();
        Option wireTypeOption = OptionBuilder.withDescription("context summary encoding")
                                             .withType(WireSummaryType.class).hasArg()
                                             .withArgName("WIRETYPE")
                                             .withLongOpt(CLIOptions.WIRE_TYPE).create();
        Option groupTypeOption = OptionBuilder.withDescription("group type")
                                              .withType(Node.GroupType.class).hasArg()
                                              .withArgName("GROUPTYPE")
                                              .withLongOpt(CLIOptions.GROUP_TYPE).create();
        Option skipFirstOption = OptionBuilder.withDescription("skip first node (useful for debugging)")
                                              .withType(boolean.class)
                                              .withLongOpt(CLIOptions.SKIP_FIRST).create();
        Option gridOption = OptionBuilder.withDescription("arrange in equidistant grid formation")
                                         .withType(boolean.class).withLongOpt(CLIOptions.GRID)
                                         .create();
        Option seedOption = OptionBuilder.withDescription("seed for randomization")
                                         .withType(long.class).hasArg().withArgName("SEED")
                                         .withLongOpt(CLIOptions.SEED).create();


        Options options = new Options();
        options.addOption(nodeOption);
        options.addOption(runtimeOption);
        options.addOption(radiusOption);
        options.addOption(fillToOption);
        options.addOption(tauOption);
        options.addOption(sendIntervalOption);
        options.addOption(wireTypeOption);
        options.addOption(groupTypeOption);
        options.addOption(skipFirstOption);
        options.addOption(seedOption);
        options.addOption(gridOption);

        int numNodes = 0;
        float connectivityRadius = 0;
        int runtime = 0;
        int fillTo = CLIOptions.DEFAULT_FILL_TO;
        int tau = CLIOptions.DEFAULT_TAU;
        int sendInterval = CLIOptions.DEFAULT_SEND_INTERVAL;
        WireSummaryType wireType = CLIOptions.DEFAULT_WIRE_TYPE;
        Node.GroupType groupType = CLIOptions.DEFAULT_GROUP_TYPE;
        boolean skipFirst = false;
        boolean grid = false;
        Long seed = null;

        try {
            CommandLineParser parser = new PosixParser();
            CommandLine cli = parser.parse(options, args);

            numNodes = Integer.valueOf(cli.getOptionValue(CLIOptions.NUM_NODES));
            connectivityRadius = Float.valueOf(cli.getOptionValue(CLIOptions.CONNECTIVITY_RADIUS));
            runtime = Integer.valueOf(cli.getOptionValue(CLIOptions.RUN_LENGTH));

            if (cli.hasOption(CLIOptions.FILL_TO)) {
                fillTo = Integer.valueOf(cli.getOptionValue(CLIOptions.FILL_TO));
            }

            if (cli.hasOption(CLIOptions.TAU)) {
                tau = Integer.valueOf(cli.getOptionValue(CLIOptions.TAU));
            }

            if (cli.hasOption(CLIOptions.SEND_INTERVAL)) {
                sendInterval = Integer.valueOf(cli.getOptionValue(CLIOptions.SEND_INTERVAL));
            }

            if (cli.hasOption(CLIOptions.WIRE_TYPE)) {
                wireType = WireSummaryType.valueOf(cli.getOptionValue(CLIOptions.WIRE_TYPE));
            }

            if (cli.hasOption(CLIOptions.GROUP_TYPE)) {
                groupType = Node.GroupType.valueOf(cli.getOptionValue(CLIOptions.GROUP_TYPE));
            }

            if (cli.hasOption(CLIOptions.SKIP_FIRST)) {
                skipFirst = true;
            }

            if (cli.hasOption(CLIOptions.GRID)) {
                grid = true;
            } else {
                grid = false;
            }

            if (cli.hasOption(CLIOptions.SEED)) {
                seed = Long.valueOf(cli.getOptionValue(CLIOptions.SEED));
            } else {
                seed = rand.nextLong();
            }

        } catch (ParseException e) {
            System.err.println("Could not parse options: " + e);
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Node", options, true);
            System.exit(-1);
        }

        rand = new Random(seed);

        System.out.printf("Simulating network with numNodes=%d connectivityRadius=%f fillTo=%d tau=%d sendInterval=%d wireType=%s groupType=%s skipFirst=%b seed=%d grid=%b\n",
                          numNodes, connectivityRadius, fillTo, tau, sendInterval,
                          wireType.toString(), groupType.toString(), skipFirst, seed, grid);

        List<NodeInfo> nodes = new ArrayList<NodeInfo>(numNodes);
        if (grid) {
            double sqrt = Math.sqrt(numNodes);
            int numRows = (int) Math.floor(sqrt);
            if (numRows != sqrt) {
                throw new IllegalArgumentException("Num nodes must be sqrt-able in grid formation");
            }

            for (int i = 0; i < numRows; i++) {
                for (int j = 0; j < numRows; j++) {
                    nodes.add(new NodeInfo(((double) i) / numRows, 1.0 * j / numRows));
                }
            }
        } else {
            for (int i = 0; i < numNodes; i++) {
                nodes.add(new NodeInfo());
            }
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
        for (NodeInfo node: nodes) {
            System.out.println(node);
        }
        System.out.println();

        Long hashSeedHint = null;
        if (nodes.size() > 0 && wireType == WireSummaryType.BLOOMIER) {
            System.out.println("Precalculating hash seed hint");

            final NodeInfo node = nodes.get(0);

            List<String> command = generateCommandList(node.id, node.x, node.y, fillTo, tau,
                                                       sendInterval, wireType, groupType,
                                                       node.neighbors, hashSeedHint);

            try {
                Process process = new ProcessBuilder(command).redirectErrorStream(true).start();

                InputStream in = process.getInputStream();
                InputStreamReader isr = new InputStreamReader(in);
                BufferedReader br = new BufferedReader(isr);
                String line;
                while ((line = br.readLine()) != null) {
                    Pattern pattern = Pattern.compile(".*hashSeed=(\\d+) .*");
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.matches()) {
                        hashSeedHint = Long.valueOf(matcher.group(1));
                        System.out.println("Hashseed hint retrieved: " + hashSeedHint);
                        break;
                    }
                }
                process.destroy();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        
        System.out.println("Starting beacon for each node");
        try {
            try {
                for (final NodeInfo node: nodes) {
                    List<String> command = generateCommandList(node.id, node.x, node.y, fillTo,
                                                               tau, sendInterval, wireType,
                                                               groupType, node.neighbors,
                                                               hashSeedHint);

                    // Handle skipFirst
                    if (skipFirst && node.id == 0) {
                        System.out.println("Skipping first node, command needed: " + command);
                        continue;
                    }

                    System.out.println("Starting process - command = " + command.toString());

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

    private static List<String> generateCommandList(int id, double x, double y, int fillTo,
                                                    int tau, int sendInterval,
                                                    WireSummaryType wireType,
                                                    Node.GroupType groupType, int[] neighbors,
                                                    Long hashSeedHint) {
        List<String> command = new ArrayList<String>();
        command.add("java");
        command.add("-jar");
        command.add("node.jar");
        command.add("--id");
        command.add(Integer.valueOf(id).toString());
        command.add("-x");
        command.add(Double.valueOf(x).toString());
        command.add("-y");
        command.add(Double.valueOf(y).toString());
        command.add("--fillTo");
        command.add(Integer.valueOf(fillTo).toString());
        command.add("--tau");
        command.add(Integer.valueOf(tau).toString());
        command.add("--sendInterval");
        command.add(Integer.valueOf(sendInterval).toString());
        command.add("--wireType");
        command.add(wireType.toString());
        command.add("--groupType");
        command.add(groupType.toString());
        command.add("--address");
        // command.add(BROADCAST_ADDRESS.toString().substring(1));
        command.add("localhost");
   
        if (neighbors.length > 0) {
            command.add("--connectedNodes");
            for (int neighbor: neighbors) {
                command.add(Integer.valueOf(neighbor).toString());
            }
        }

        if (hashSeedHint != null) {
            command.add("--hashSeedHint");
            command.add(hashSeedHint.toString());
        }

        return command;
    }
}
