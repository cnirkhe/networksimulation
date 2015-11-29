package com.ricketts;

import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    public static boolean DEBUG = true;

    public static void main(String[] args) {

        String filename = new String("h0.json");
        String f2 = filename.substring(0, filename.length() - 5);
        InputParser ip = new InputParser();
        ip.parseJSON(filename);

        //First we derive all the links
        ArrayList<Link> links = ip.extractLinks(f2);
        HashMap<Integer, Link> linkMap = InputParser.makeLinkMap(links);

        //But these links don't have their nodes linked

        // Get hosts and routers given links
        ArrayList<Host> hosts = ip.extractHosts(linkMap);
        ArrayList<Router> routers = ip.extractRouters(linkMap);

        ArrayList<Node> nodes = new ArrayList<>(hosts.size() + routers.size());
        nodes.addAll(hosts);
        nodes.addAll(routers);

        HashMap<String, Node> addressBook = InputParser.makeNodeMap(nodes);

        // Make flows
        ArrayList<Flow> flows = ip.extractFlows(addressBook, f2);
        for (Flow flow : flows)
            flow.getSource().addFlow(flow);

        // Add nodes to links
        InputParser.addNodesToLinks(nodes);

        ArrayList<Updatable> updatableLinkedList = new ArrayList<>();
        updatableLinkedList.addAll(hosts);
        updatableLinkedList.addAll(links);


        if (DEBUG) {
            Integer initialTime = RunSim.getCurrentTime();
            Integer currentTime = RunSim.getCurrentTime(), nextTime;
            while (currentTime < initialTime + 30000) {
                System.out.println("loop");
                nextTime = RunSim.getCurrentTime();
                for(Updatable u : updatableLinkedList) {
                    u.update(nextTime - currentTime, currentTime);
                }
                while (RunSim.getCurrentTime() < nextTime + 10) {
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