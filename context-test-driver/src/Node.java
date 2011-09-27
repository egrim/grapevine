import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import edu.utexas.ece.mpc.context.ContextHandler;
import edu.utexas.ece.mpc.context.ContextHandler.WireSummaryType;
import edu.utexas.ece.mpc.context.logger.SysoutLoggingDelegate;
import edu.utexas.ece.mpc.context.net.ContextShimmedDatagramSocket;
import edu.utexas.ece.mpc.context.summary.ContextSummary;
import edu.utexas.ece.mpc.context.summary.HashMapContextSummary;
import edu.utexas.ece.mpc.context.util.GroupUtils;


public class Node {
    public enum GroupType {
        NONE, LABELED
    }

    private static int BASE_PORT = 5000;
    private static int id;
    private static HashMapContextSummary summary;
    private static ContextHandler handler = ContextHandler.getInstance();

    public static void setupLabeledGroups() {
        // // Create some labeled groups based upon the divisibility of the id
        // int groupLabelNumber = 0;
        // Collection<WireContextSummary> groupSummaries = new ArrayList<WireContextSummary>();
        // for (int i = 1; i <= 3; i++) {
        // if (id % i == 0) {
        // int gId = GroupUtils.GROUP_ID_OFFSET + i;
        // summary.put(GroupUtils.GROUP_DECLARATION_PREFIX + groupLabelNumber, gId);
        // groupLabelNumber++;
        // LabeledContextSummary groupSummary = GroupUtils.createGroupAgg(gId);
        // GroupUtils.addGroupMember(groupSummary, id);
        // groupSummaries.add(groupSummary);
        // }
        // }
        // summary.put(GroupUtils.GROUP_DECLARATIONS_ENUMERATED, groupLabelNumber);
        //
        // handler.seedGroupSummaries(groupSummaries);
        // handler.addPreReceivedSummariesUpdateObserver(new Observer() {
        // @Override
        // public void update(Observable o, Object object) {
        // @SuppressWarnings("unchecked")
        // Collection<WireContextSummary> summariesToUpdate = (Collection<WireContextSummary>) object;
        // List<WireContextSummary> summariesReplaced = new ArrayList<WireContextSummary>();
        // List<WireContextSummary> replacementSummaries = new ArrayList<WireContextSummary>();
        // for (WireContextSummary summaryToUpdate: summariesToUpdate) {
        // Set<Integer> members = GroupUtils.getGroupMembers(summaryToUpdate);
        // if (!members.isEmpty()) {
        // int gId = summaryToUpdate.getId();
        //
        // LabeledContextSummary groupSummary;
        // try {
        // ContextSummary summary = handler.get(gId);
        // if (summary == null) {
        // throw new Exception();
        // }
        // groupSummary = (LabeledContextSummary) summary;
        // } catch (Exception e) {
        // groupSummary = new LabeledContextSummary(gId);
        // }
        //
        // for (Integer member: members) {
        // GroupUtils.addGroupMember(groupSummary, member);
        // }
        //
        // summariesReplaced.add(summaryToUpdate);
        // replacementSummaries.add(groupSummary);
        // }
        // }
        //
        // summariesToUpdate.removeAll(summariesReplaced);
        // summariesToUpdate.addAll(replacementSummaries);
        // }
        // });
    }

    private static void reportGroups() {
        for (ContextSummary summary: handler.getReceivedSummaries()) {
            int id = summary.getId();
            if (id >= GroupUtils.GROUP_ID_OFFSET) {
                handler.logDbg("  Group " + id + " membership: "
                        + GroupUtils.getGroupMembers(summary));
            }
        }
    }

    private static class CLIOptions {
        public static final String ID = "id";
        public static final String X = "x";
        public static final String Y = "y";
        public static final String FILL_TO = "fillTo";
        public static final String TAU = "tau";
        public static final String SEND_INTERVAL = "sendInterval";
        public static final String WIRE_TYPE = "wireType";
        public static final String GROUP_TYPE = "groupType";
        public static final String ADDRESS = "address";
        public static final String CONNECTED_NODES = "connectedNodes";
        public static final String SEED = "seed";
        public static final String HASH_SEED_HINT = "hashSeedHint";

        public static final int DEFAULT_FILL_TO = 100;
        public static final int DEFAULT_TAU = 3;
        public static final int DEFAULT_SEND_INTERVAL = 300;
        public static final WireSummaryType DEFAULT_WIRE_TYPE = WireSummaryType.BLOOMIER;
        public static final GroupType DEFAULT_GROUP_TYPE = GroupType.NONE;
        public static final InetAddress DEFAULT_ADDRESS;
        static {
            try {
                DEFAULT_ADDRESS = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                throw new RuntimeException("Could not get local host address", e);
            }
        }
    }
    
    @SuppressWarnings("static-access")
    public static void main(String[] args) throws Exception {

        Option idOption = OptionBuilder.withDescription("node id").withType(int.class).hasArg()
                                       .withArgName("ID").isRequired().withLongOpt(CLIOptions.ID)
                                       .create();
        Option xOption = OptionBuilder.withDescription("location (x coordinant)")
                                      .withType(double.class).hasArg().withArgName("X")
                                      .create(CLIOptions.X);
        Option yOption = OptionBuilder.withDescription("location (y coordinant)")
                                      .withType(double.class).hasArg().withArgName("Y")
                                      .create(CLIOptions.Y);
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
                                              .withType(GroupType.class).hasArg()
                                              .withArgName("GROUPTYPE")
                                              .withLongOpt(CLIOptions.GROUP_TYPE).create();
        Option addressOption = OptionBuilder.withDescription("target address")
                                            .withType(InetAddress.class).hasArg()
                                            .withArgName("ADDRESS").withLongOpt(CLIOptions.ADDRESS)
                                            .create();
        Option connectedToIdsOption = OptionBuilder.withDescription("reachable node ids")
                                                   .withType(int.class).hasArgs()
                                                   .withArgName("ID")
                                                   .withLongOpt(CLIOptions.CONNECTED_NODES)
                                                   .create();
        Option seedOption = OptionBuilder.withDescription("seed for randomization")
                                         .withType(long.class).hasArg().withArgName("SEED")
                                         .withLongOpt(CLIOptions.SEED).create();
        
        Option hashSeedHintOption = OptionBuilder.withDescription("hash seed hint for nodes")
                                                 .withType(long.class).hasArg()
                                                 .withArgName("HASHSEED")
                                                 .withLongOpt(CLIOptions.HASH_SEED_HINT).create();

        Options options = new Options();
        options.addOption(idOption);
        options.addOption(xOption);
        options.addOption(yOption);
        options.addOption(fillToOption);
        options.addOption(tauOption);
        options.addOption(sendIntervalOption);
        options.addOption(wireTypeOption);
        options.addOption(groupTypeOption);
        options.addOption(addressOption);
        options.addOption(connectedToIdsOption);
        options.addOption(seedOption);
        options.addOption(hashSeedHintOption);
        
        double x = 0;
        double y = 0;
        int fillTo = CLIOptions.DEFAULT_FILL_TO;
        int tau = CLIOptions.DEFAULT_TAU;
        int sendInterval = CLIOptions.DEFAULT_SEND_INTERVAL;
        WireSummaryType wireType = CLIOptions.DEFAULT_WIRE_TYPE;
        GroupType groupType = CLIOptions.DEFAULT_GROUP_TYPE;
        InetAddress address = CLIOptions.DEFAULT_ADDRESS;
        List<Integer> connectedToIds = new ArrayList<Integer>();
        Long hashSeedHint = null;

        long seed = 0;
        Random rand = new Random();

        try {
            CommandLineParser parser = new PosixParser();
            CommandLine cli = parser.parse(options, args);

            if (cli.hasOption(CLIOptions.SEED)) {
                seed = Long.valueOf(cli.getOptionValue(CLIOptions.SEED));
            } else {
                seed = rand.nextLong();
            }

            rand = new Random(seed);

            id = Integer.valueOf(cli.getOptionValue(CLIOptions.ID));

            if (cli.hasOption(CLIOptions.X)) {
                x = Double.valueOf(cli.getOptionValue(CLIOptions.X));
            } else {
                x = rand.nextDouble();
            }

            if (cli.hasOption(CLIOptions.Y)) {
                y = Double.valueOf(cli.getOptionValue(CLIOptions.Y));
            } else {
                y = rand.nextDouble();
            }

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
                groupType = GroupType.valueOf(cli.getOptionValue(CLIOptions.GROUP_TYPE));
            }

            if (cli.hasOption(CLIOptions.ADDRESS)) {
                address = InetAddress.getByName(cli.getOptionValue(CLIOptions.ADDRESS));
            }

            if (cli.hasOption(CLIOptions.CONNECTED_NODES)) {
                String[] values = cli.getOptionValues(CLIOptions.CONNECTED_NODES);
                connectedToIds = new ArrayList<Integer>(values.length);
                for (String value: values) {
                    connectedToIds.add(Integer.valueOf(value));
                }
            }

            if (cli.hasOption(CLIOptions.HASH_SEED_HINT)) {
                hashSeedHint = Long.valueOf(cli.getOptionValue(CLIOptions.HASH_SEED_HINT));
            }

        } catch (ParseException e) {
            System.err.println("Could not parse options: " + e);
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Node", options, true);
            System.exit(-1);

        }

        System.out.printf("Starting node with id=%d position=(%.4f:%.4f) fillItems=%d tau=%d sendInterval=%d wireType=%s groupType=%s seed=%d - sending to %s connected to nodes %s\n",
                          id, x, y, fillTo, tau, sendInterval, wireType.toString(),
                          groupType.toString(), seed, address.toString(), connectedToIds.toString());

        if (hashSeedHint != null) {
            summary = new HashMapContextSummary(id, hashSeedHint);
        } else {
            summary = new HashMapContextSummary(id);
        }

        if (fillTo != 0) {
            summary.put("location: x", (int) (x * 100000));
            summary.put("location: y", (int) (y * 100000));

            // for (int i = 0; i < connectedToIds.length; i++) {
            // summary.put("neighbor: " + i, connectedToIds[i]);
            // }

            int fillNeeded = fillTo - summary.size();
            if (fillNeeded > 0) {
                Random fillRand = new Random(id);
                for (int i = 0; i < fillNeeded; i++) {
                    summary.put("fill: " + i, fillRand.nextInt());
                }
            }

            handler.setTau(tau);
            handler.setWireSummaryType(wireType);
            handler.setLoggerDelegate(new SysoutLoggingDelegate());
            handler.updateLocalSummary(summary);

            switch (groupType) {
                case LABELED:
                    setupLabeledGroups();
                    break;
                case NONE:
                    break;
            }
        }

        final DatagramSocket receiveSocket = new ContextShimmedDatagramSocket(BASE_PORT + id);
        Thread receiver = new Thread(new Runnable() {

            @Override
            public void run() {
                DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
                while (true) {
                    try {
                        receiveSocket.receive(packet);

                        handler.logDbg("Received packet: "
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
            long start = System.currentTimeMillis();
            for (int connectedToId : connectedToIds) {
                int sendPort = BASE_PORT + connectedToId;
                sendPacket.setData(("Packet burst number " + sequenceNum + " from " + id).getBytes());
                sendPacket.setPort(sendPort);

                handler.logDbg("Sending packet number " + sequenceNum + " to " + connectedToId);
                sendSocket.send(sendPacket);
            }

            sequenceNum++;

            List<ContextSummary> receivedSummaries = handler.getReceivedSummaries();
            int[] receivedSummaryIds = new int[receivedSummaries.size()];
            for (int i = 0; i < receivedSummaries.size(); i++) {
                receivedSummaryIds[i] = receivedSummaries.get(i).getId();
            }
            handler.logDbg("Summaries received: " + Arrays.toString(receivedSummaryIds));
            
            if (groupType != GroupType.NONE) {
                reportGroups();
            }

            long sleepNeeded = sendInterval - (System.currentTimeMillis() - start);
            if (sleepNeeded > 0) {
                Thread.sleep(sleepNeeded);
            }
        }
    }

}
