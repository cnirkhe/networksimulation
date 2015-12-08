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

    public static Integer currentTime = 0;

    public static class Protocol {
        public static int RENO = 1;
        public static int FAST = 2;
    }
    public static int protocol = Protocol.FAST;

    public static void main(String[] args) {

        String filename = new String("h0.json");
        String f2 = filename.substring(0, filename.length() - 5);
        InputParser ip = new InputParser();
        ip.parseJSON(filename);

        //First we derive all the links
        ArrayList<Link> links = ip.extractLinks(f2, protocol);
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
        ArrayList<Flow> flows = ip.extractFlows(addressBook, f2, protocol);

        // Add nodes to links
        InputParser.addNodesToLinks(nodes);

        //After nodes are added to links, we can now setup routingtables
        //Have each router setup its routing table based on its neighbors
        for (Router router : routers) {
            router.initializeRoutingTable();
        }

        ArrayList<Updatable> updatableLinkedList = new ArrayList<>();
        updatableLinkedList.addAll(nodes);
        updatableLinkedList.addAll(links);

        Integer intervalStep = 10;

        while (currentTime < 150000) {

            Iterator<Flow> flowIterator = flows.iterator();
            while(flowIterator.hasNext()) {
                Flow flow = flowIterator.next();
                if(flow.getStartTime().equals(currentTime)) {
                    flow.getSource().addFlow(flow);
                }
            }

            for(Updatable u : updatableLinkedList) {
                u.update(intervalStep, currentTime);
            }

            if(currentTime % 2000 == 0)
                System.out.println("pause at " + currentTime);

            currentTime += intervalStep;
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

        if (protocol == Protocol.RENO) {
            f2 += "RENO";
        } else if (protocol == Protocol.FAST) {
            f2 += "FAST";
        }
        OverlaidPlot op1 = new OverlaidPlot("Left Buffer", "Left Buffer Occupancy " + f2 + ".png", leftBuffer,
                "Time (ms)", "Buffer occupancy (pkts)", 888, 888);
        OverlaidPlot op2 = new OverlaidPlot("Right Buffer", "Right Buffer Occupancy " + f2 + ".png", rightBuffer,
                "Time (ms)", "Buffer occupancy (pkts)", 888, 888);
        OverlaidPlot op3 = new OverlaidPlot("Packet Loss", "Packet Loss " + f2 + ".png", packetLoss,
                "Time (ms)", "Packet Loss (pkts)", 888, 888);
        OverlaidPlot op4 = new OverlaidPlot("Link Rates", "Link Rates " + f2 + ".png", linkRates,
                "Time (ms)", "Link Rate (Mbps)", 888, 888);
        OverlaidPlot op5 = new OverlaidPlot("Flow Rate", "Flow Rate " + f2 + ".png", flowRates,
                "Time (ms)", "Flow Rate (Mbps)", 888, 888);
        OverlaidPlot op6 = new OverlaidPlot("Window Size", "Window Size " + f2 + ".png", windowSizes,
                "Time (ms)", "Window Size (pkts)", 888, 888);
        OverlaidPlot op7 = new OverlaidPlot("Packet delay", "Packet Delay " + f2 + ".png", packetDelay,
                "Time (ms)", "Packet Delay (ms)", 888, 888);
    }
}