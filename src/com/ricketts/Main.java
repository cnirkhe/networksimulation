package com.ricketts;

import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Main {
    public static boolean DEBUG = false;

    public static void main(String[] args) {

        String filename = new String("h0.json");
        InputParser ip = new InputParser();
        ip.parseJSON(filename);

        //First we derive all the links
        ArrayList<Link> links = ip.extractLinks();
        HashMap<Integer, Link> linkMap = ip.makeLinkMap(links);

        //But these links don't have their nodes linked

        // Get hosts given links
        ArrayList<Host> hosts = ip.extractHosts(linkMap);
        HashMap<Integer, Node> addressBook = ip.makeNodeMap(hosts);

        // Make flows
        ArrayList<Flow> flows = ip.extractFlows(addressBook);
        for (Flow flow : flows)
            flow.getSource().addFlow(flow);

        // Add hosts to links
        Link link;
        for (Host host : hosts) {
            link = host.getLink();
            if (link.getLeftNode() == null) {
                link.setLeftNode(host);
            } else if (link.getRightNode() == null) {
                link.setRightNode(host);
            } else {
                System.out.println("Bad Network Definition.");
            }
        }

        ArrayList<Updatable> updatableLinkedList = new ArrayList<>();
        updatableLinkedList.addAll(hosts);
        updatableLinkedList.addAll(links);


        if (DEBUG) {
            Integer currentTime = RunSim.getCurrentTime(), nextTime;
            while (true) {
                nextTime = RunSim.getCurrentTime();
                for(Updatable u : updatableLinkedList) {
                    u.update(nextTime - currentTime, currentTime);
                }
                currentTime = nextTime;
            }
        }
        else
            RunSim.run(updatableLinkedList, 2, -1);
    }
}