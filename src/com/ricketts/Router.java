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
     * Maps a Host to the respective distance, link to send along
     */
    private HashMap<Node, Pair<Integer, Link>> routingTable;

    public Router(String address, ArrayList<Link> links) {
        super(address);
        this.links = links;

        //Generate HashMap
        packetsToSend = new HashMap<>();
        for(Link l : links) { //Setup queues for each of the links in this router
            packetsToSend.put(l, new LinkedList<Packet>());
        }

        routingTable = new HashMap<>();
    }

    public ArrayList<Link> getLinks() {
        return links;
    }

    public void setRoutingTable(HashMap<Node, Pair<Integer, Link>> routingTable) {
        this.routingTable = routingTable;
    }

    /**
     * This method updates this routers routing table given the following information by the Bellman-Ford algorithm
     * @param neighbor The neighboring node
     * @param connectingLink index of their connecting link
     * @param neighborRoutingTable and the routing table of the neighbor
     */
    private void updateRoutingTable(Node neighbor, Link connectingLink,
                                    HashMap<Node,Pair<Integer,Link>> neighborRoutingTable) {
        Pair<Integer,Link> neighborInformation = Pair.of(connectingLink.getLinkDelay(), connectingLink);
        routingTable.put(neighbor, neighborInformation); //If data exists, overwrites but equivalent time as check and rewrite

        // Now we update our routing table using the triangle inequality and all of the information in the neighbor's routing table
        for( Node router : neighborRoutingTable.keySet()) {
            Pair<Integer, Link> routerInformation = neighborRoutingTable.get(router);
            Pair<Integer, Link> myCurrentInformation = routingTable.getOrDefault(router, Pair.of(Integer.MAX_VALUE, (Link) null));
            if(routerInformation.fst + neighborInformation.fst < myCurrentInformation.fst) {
                //Change the routing to go through neighbor router
                myCurrentInformation = Pair.of(routerInformation.fst + neighborInformation.fst, connectingLink);
                routingTable.put(router, myCurrentInformation);
            }
        }
    }


    public void receivePacket(Packet packet, Link receivingLink) {
        Node destination = packet.getDestination();

        //Check the routingtable for which link to send out these packets on
        Pair<Integer, Link> bestPath = routingTable.get(destination);
        if(bestPath == null) {
            System.out.println("Destination unknown in routing table.");
        } else {
            Link bestLink = bestPath.snd;
            Queue<Packet> sendingQueue = packetsToSend.get(bestLink);
            if (sendingQueue == null) {
                System.out.println("Sending Queue doesn't exist! Uh-oh");
            } else {
                sendingQueue.add(packet);
            }
        }
    }

    public void update(Integer intervalTime, Integer overallTime) {
        for(Link link : links) {
            Queue<Packet> sendingQueue = packetsToSend.get(link);
            if(sendingQueue == null) {
                System.out.println("Sending Queue doesn't exist! Uh-oh");
            }
            else {
                while (!sendingQueue.isEmpty()) {
                    link.addPacket(sendingQueue.remove(), this);
                }
            }
        }
    }
}