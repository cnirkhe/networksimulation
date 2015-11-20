package com.ricketts;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Main {

    public static void main(String[] args)
    {
        /**
         * For now I'm going to artifically construct the Network
         * TODO adjust to building the Network from a definition JSON
         */
        String filename = new String("h0.json");
        InputParser ip = new InputParser();
        ip.parse(filename);
        ArrayList<Link> links = ip.extractLinks();
        HashMap<Integer, Link> linkMap = ip.makeLinkMap(links);

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

        RunSim.run(links, hosts, 100, -1);

    }
}