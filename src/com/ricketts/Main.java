package com.ricketts;

import java.util.ArrayList;
import java.util.HashMap;

public class Main {
    public static boolean DEBUG = true;

    public static class Protocol {
        public static int RENO = 1;
        public static int FAST = 2;
    }
    public static int protocol = Protocol.RENO;

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
        for (Flow flow : flows)
            flow.getSource().addFlow(flow);

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


        if (DEBUG) {
            Integer initialTime = RunSim.getCurrentTime();
            Integer currentTime = RunSim.getCurrentTime(), nextTime;
            while (currentTime < initialTime + 90000) {

                System.out.println("loop");
                nextTime = RunSim.getCurrentTime();
                for(Updatable u : updatableLinkedList) {
                    u.update(1, currentTime);
                }
                while (RunSim.getCurrentTime() < nextTime + 5) {
                    // busy wait
                    // TODO: this is bad and we should change
                }
                currentTime = nextTime;
            }
            // Get flow and link stats
            for (Flow flowerino : flows) {
                flowerino.generateFlowGraphs();
            }
            for (Link linkerino : links) {
                linkerino.generateLinkGraphs();
            }
            System.out.println("generated graphs");
        }
        else {
            RunSim.run(updatableLinkedList, 2, 30000, links, flows);
        }
        System.out.println("hi");
    }
}