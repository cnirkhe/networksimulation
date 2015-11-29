package com.ricketts;

import com.sun.tools.javac.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class Router extends Node
{
    /**
     * The set of Links that this router is connected to
     */
    private final ArrayList<Link> links;
    /**
     * The set of Packets To Send to each of the respective Links
     */
    private HashMap<Link, Queue<Packet>> packetsToSend;
    /**
     * Maps a Host to the respective distance, rounting table index
     */
    private HashMap<Node, Pair<Integer, Integer>> routingTable;

    public Router(String address, ArrayList<Link> links) {
        super(address);
        this.links = links;

        //Generate HashMap
        packetsToSend = new HashMap<>();
        for(Link l : links) {
            packetsToSend.put(l, new LinkedList<Packet>());
        }

        routingTable = new HashMap<>();
    }

    public ArrayList<Link> getLinks() {
        return links;
    }

    public void setRoutingTable(HashMap<Node, Pair<Integer, Integer>> routingTable) {
        this.routingTable = routingTable;
    }

    /**
     * This method updates this routers routing table given the following information by the Bellman-Ford algorithm
     * @param neighbor The neighboring node
     * @param connectingLinkIndex index of their connecting link
     * @param neighborRoutingTable and the routing table of the neighbor
     */
    private void updateRoutingTable(Node neighbor, Integer connectingLinkIndex,
                                    HashMap<Node,Pair<Integer,Integer>> neighborRoutingTable) {
        //TODO move over
    }


    public void receivePacket(Packet packet, Link receivingLink) {

    }

    public void update(Integer intervalTime, Integer overallTime) {

    }
}