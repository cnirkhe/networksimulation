package com.ricketts;

import org.jfree.data.xy.XYSeries;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Main class: runs the simulation for T0, T1, and T2.
 */
public class Main {
    /**
     * Set the initial time which will be updated as the simulation progresses.
     */
    public static int currentTime = 0;
    /**
     * Set the interval rate we update at.
     */
    public static final int intervalTime = 1;

    /**
     * The congesetion avoidance protocol we're using.
     */
    public static class Protocol {
        public static int RENO = 1;
        public static int FAST = 2;
    }

    /**
     * Run the simulation!!!!!
     * @param args Args
     */
    public static void main(String[] args) {
        ArrayList<String> fileList = new ArrayList<>();
        fileList.add("t0.json");
        fileList.add("t1.json");
        fileList.add("t2.json");

        ArrayList<Integer> protocols = new ArrayList<>();
        protocols.add(Protocol.FAST);
        protocols.add(Protocol.RENO);

        for (String filename : fileList) {
            for (int protocol : protocols) {
                currentTime = 0;
                String filenameSubstring = filename.substring(0, filename.length() - ".json".length());
                // Parse the network from the json using the an InputParser.
                InputParser ip = new InputParser();
                ip.parseJSON(filename);

                ArrayList<Link> links = ip.extractLinks(filenameSubstring, protocol);
                HashMap<Integer, Link> linkMap = InputParser.makeLinkMap(links);

                // Get hosts and routers given links
                ArrayList<Host> hosts = ip.extractHosts(linkMap, protocol);
                ArrayList<Router> routers = ip.extractRouters(linkMap);

                // Make map of addresses to nodes
                ArrayList<Node> nodes = new ArrayList<>(hosts.size() + routers.size());
                nodes.addAll(hosts);
                nodes.addAll(routers);
                HashMap<String, Node> addressBook = InputParser.makeNodeMap(nodes);

                // Make flows given the address book
                ArrayList<Flow> flows = ip.extractFlows(addressBook, filenameSubstring, protocol);

                // Add nodes to links
                InputParser.addNodesToLinks(nodes);

                // After nodes are added to links, we can now setup routing tables
                // Have each router setup its routing table based on its neighbors
                for (Router router : routers) {
                    router.initializeRoutingTable();
                }

                for (Flow flow : flows) {
                    flow.getSource().addFlow(flow);
                }

                ArrayList<Updatable> updatableLinkedList = new ArrayList<>();
                updatableLinkedList.addAll(nodes);
                updatableLinkedList.addAll(links);

                // In every interval, update the Updatables (Hosts, Routers, Flows).
                for (; currentTime < ip.extractRuntime(); currentTime += intervalTime) {
                    System.out.println("Time is currently: " + currentTime);
                    if (currentTime % 100 == 0)
                        System.out.println();
                    for (Updatable u : updatableLinkedList) {
                        u.update();
                    }
                }

                // After simulation ends, get the host and link stats.
                ArrayList<XYSeries> buffer = new ArrayList<>();
                ArrayList<XYSeries> packetLoss = new ArrayList<>();
                ArrayList<XYSeries> linkRates = new ArrayList<>();
                for (Link l : links) {
                    if (l.graph) {
                        ArrayList<XYSeries> curr = l.getDatasets();
                        buffer.add(curr.get(0));
                        packetLoss.add(curr.get(1));
                        linkRates.add(curr.get(2));
                    }
                }

                ArrayList<XYSeries> flowRates = new ArrayList<>();
                ArrayList<XYSeries> windowSizes = new ArrayList<>();
                ArrayList<XYSeries> packetDelay = new ArrayList<>();
                for (Flow f : flows) {
                    ArrayList<XYSeries> curr = f.getDatasets();
                    flowRates.add(curr.get(0));
                    windowSizes.add(curr.get(1));
                    packetDelay.add(curr.get(2));
                }

                // Plot the graphs and output to .png files.
                String protocolName = (protocol == Protocol.RENO) ? "Reno" : "Fast";
                OverlaidPlot op1 = new OverlaidPlot("Buffer", "Buffer Occupancy " + filenameSubstring + " " +
                        protocolName + ".jpeg", buffer, "Time (ms)", "Buffer occupancy (bits)", 888, 188);
                OverlaidPlot op3 = new OverlaidPlot("Packet Loss", "Packet Loss " + filenameSubstring + " " +
                        protocolName + ".jpeg", packetLoss, "Time (ms)", "Packet Loss (pkts)", 888, 188);
                OverlaidPlot op4 = new OverlaidPlot("Link Rates", "Link Rates " + filenameSubstring + " " +
                        protocolName + ".jpeg", linkRates, "Time (ms)", "Link Rate (Mbps)", 888, 188);
                OverlaidPlot op5 = new OverlaidPlot("Flow Rate", "Flow Rate " + filenameSubstring + " " +
                        protocolName + ".jpeg", flowRates, "Time (ms)", "Flow Rate (Mbps)", 888, 188);
                OverlaidPlot op6 = new OverlaidPlot("Window Size", "Window Size " + filenameSubstring + " " +
                        protocolName + ".jpeg", windowSizes, "Time (ms)", "Window Size (pkts)", 888, 188);
                OverlaidPlot op7 = new OverlaidPlot("Packet delay", "Packet Delay " + filenameSubstring + " " +
                        protocolName + ".jpeg", packetDelay, "Time (ms)", "Packet Delay (ms)", 888, 188);
            }
        }
    }
}