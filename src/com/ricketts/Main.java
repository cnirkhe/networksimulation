package com.ricketts;

import com.sun.deploy.security.X509Extended7DeployTrustManager;
import com.sun.xml.internal.bind.annotation.OverrideAnnotationOf;
import com.sun.xml.internal.ws.api.streaming.XMLStreamReaderFactory;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYSeries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

public class Main {

    public static int currentTime = 0;
    public static final int intervalTime = 1;

    public static class Protocol {
        public static int RENO = 1;
        public static int FAST = 2;
    }
    public static int protocol = Protocol.RENO;

    public static void main(String[] args) {

        String filename = new String("h1.json");
        String filenameSubstring = filename.substring(0, filename.length() - ".json".length());
        InputParser ip = new InputParser();
        ip.parseJSON(filename);

        //First we derive all the links
        ArrayList<Link> links = ip.extractLinks(filenameSubstring, protocol);
        HashMap<Integer, Link> linkMap = InputParser.makeLinkMap(links);

        //But these links don't have their nodes linked

        // Get hosts and routers given links
        ArrayList<Host> hosts = ip.extractHosts(linkMap, protocol);
        ArrayList<Router> routers = ip.extractRouters(linkMap);

        ArrayList<Node> nodes = new ArrayList<>(hosts.size() + routers.size());
        nodes.addAll(hosts);
        nodes.addAll(routers);

        HashMap<String, Node> addressBook = InputParser.makeNodeMap(nodes);

        // Make flows
        ArrayList<Flow> flows = ip.extractFlows(addressBook, filenameSubstring, protocol);

        // Add nodes to links
        InputParser.addNodesToLinks(nodes);

        //After nodes are added to links, we can now setup routing tables
        //Have each router setup its routing table based on its neighbors
        for (Router router : routers) {
            router.initializeRoutingTable();
        }

        for(Flow flow : flows) {
            flow.getSource().addFlow(flow);
        }

        ArrayList<Updatable> updatableLinkedList = new ArrayList<>();
        updatableLinkedList.addAll(nodes);
        updatableLinkedList.addAll(links);

        //running of the simulation
        for (; currentTime < 30000; currentTime += intervalTime) {
            System.out.println("Time is currently: " + currentTime);
            if(currentTime % 100 == 0)
                System.out.println();
            for(Updatable u : updatableLinkedList) {
                u.update();
            }
       }


        // Get flow and link stats
        ArrayList<XYSeries> leftBuffer = new ArrayList<>();
        ArrayList<XYSeries> rightBuffer = new ArrayList<>();
        ArrayList<XYSeries> packetLoss = new ArrayList<>();
        ArrayList<XYSeries> linkRates = new ArrayList<>();
        for (Link l : links) {
            ArrayList<XYSeries> curr = l.getDatasets();
            leftBuffer.add(curr.get(0));
            rightBuffer.add(curr.get(1));
            packetLoss.add(curr.get(2));
            linkRates.add(curr.get(3));
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

        OverlaidPlot op1 = new OverlaidPlot("Left Buffer", "graphs/Left Buffer Occupancy " + filenameSubstring + ".png", leftBuffer,
                "Time (ms)", "Buffer occupancy (bits)", 888, 888);
        OverlaidPlot op2 = new OverlaidPlot("Right Buffer", "graphs/Right Buffer Occupancy " + filenameSubstring + ".png", rightBuffer,
                "Time (ms)", "Buffer occupancy (bits)", 888, 888);
        OverlaidPlot op3 = new OverlaidPlot("Packet Loss", "graphs/Packet Loss " + filenameSubstring + ".png", packetLoss,
                "Time (ms)", "Packet Loss (pkts)", 888, 888);
        OverlaidPlot op4 = new OverlaidPlot("Link Rates", "graphs/Link Rates " + filenameSubstring + ".png", linkRates,
                "Time (ms)", "Link Rate (Mbps)", 888, 888);
        OverlaidPlot op5 = new OverlaidPlot("Flow Rate", "graphs/Flow Rate " + filenameSubstring + ".png", flowRates,
                "Time (ms)", "Flow Rate (Mbps)", 888, 888);
        OverlaidPlot op6 = new OverlaidPlot("Window Size", "graphs/Window Size " + filenameSubstring + ".png", windowSizes,
                "Time (ms)", "Window Size (pkts)", 888, 888);
        OverlaidPlot op7 = new OverlaidPlot("Packet delay", "graphs/Packet Delay " + filenameSubstring + ".png", packetDelay,
                "Time (ms)", "Packet Delay (ms)", 888, 888);
    }
}