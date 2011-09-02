import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;


public class ContextTestDriver implements Runnable {
	
	private int numNodes;
	private float connectivityPercentage;
	private long seed;
	private List<Node> nodes;

	private Random rand;

	public ContextTestDriver(int numNodes, float connectivityPercentage, long seed) {
		this.numNodes = numNodes;
		this.connectivityPercentage = connectivityPercentage;
		this.seed = seed;
		
		rand = new Random(seed);
	}

	@Override
	public void run() {
		System.out.println("Simulating network of " + numNodes + " nodes with "
				+ connectivityPercentage + " connectivity percentage (randomization seed: " + seed + ")");

		nodes = new ArrayList<Node>(numNodes);
		for (int i=0; i < numNodes; i++) {
			nodes.add(new Node());
		}
		
		Collections.sort(nodes, new Comparator<Node>() {

			@Override
			public int compare(Node o1, Node o2) {
				return Double.compare(o1.x, o2.x);
			}
		});
		
		double[][] distances = new double[numNodes][numNodes];
		for (int i=numNodes-1; i > 0; i--) {
			for (int j=0; j < i; j++) {
				Node me = nodes.get(i);
				Node them = nodes.get(j);
				double distance = Math.sqrt( Math.pow(me.x-them.x,2) + Math.pow(me.y-them.y,2) );
				distances[i][j] = distances[j][i] = distance;
			}
		}
		
		for (int i=0; i < nodes.size(); i++) {
			Node node = nodes.get(i);
			node.id = i;
			
			List<SimpleEntry<Integer, Double>> neighborDistances = new ArrayList<SimpleEntry<Integer, Double>>();
			for (int j=0; j < nodes.size(); j++) {
				neighborDistances.add(new SimpleEntry<Integer, Double>(j, distances[i][j]));
			}
			Collections.sort(neighborDistances, new Comparator<SimpleEntry<Integer, Double>>() {

				@Override
				public int compare(SimpleEntry<Integer, Double> o1,
						SimpleEntry<Integer, Double> o2) {
					return Double.compare(o1.getValue(), o2.getValue());
				}
			});
			
			int numNeighbors = Math.max((int) (connectivityPercentage * numNodes), 1);
			node.neighbors = new int[numNeighbors];
			for (int k=0; k < node.neighbors.length; k++) { // note: the closes neighbor is always itself, so skip that one
				node.neighbors[k] = neighborDistances.get(k+1).getKey();
			}
		}
		
		for (Node node: nodes) {
			System.out.println(node);
		}
		
	}

	private class Node {

		public Node() {
			x = rand.nextDouble();
			y = rand.nextDouble();
		}
		
		public String toString() {
			return "Node " + id + " @ (" + x + ":" + y + ") with neighbors: " + Arrays.toString(neighbors);
		}
		
		public int id;
		public double x;
		public double y;
		public int[] neighbors;
		
	}
	
	public static void main(String[] args) {
		
		int numNodes = 0;
		try {
			numNodes = Integer.valueOf(args[0]);
		} catch (Exception e) {
			System.err.println("Must specify the number of nodes to simulate");
			System.exit(-1);
		}
		
		float connectivityPercentage = 0;
		try {
			connectivityPercentage = Float.valueOf(args[1]);
		} catch (Exception e) {
			System.err.println("Must specify conectivity perscentage for nodes");
			System.exit(-1);
		}
		
		long seed = 0;
		try {
			seed = Long.valueOf(args[2]);
		} catch (ArrayIndexOutOfBoundsException e) {
			seed = new Random().nextLong();
		} catch (Exception e) {
			System.err.println("Could not interpret provided seed");
			System.exit(-1);
		}
		
		ContextTestDriver simulation = new ContextTestDriver(numNodes, connectivityPercentage, seed);
		simulation.run();
	}

}
