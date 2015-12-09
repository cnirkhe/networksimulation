package com.ricketts;

import com.sun.tools.javac.util.Pair;

import java.util.*;

/**
 * A Router is a type of Node who's job is to process incoming packets and forward them to the best neighbor.
 */
public class Router extends Node
{
    /**
     * Boolean indicating if routing tables have converged
     */
    private boolean routingTablesConverged = false;

    /**
     * The set of Links that this router is connected to
     */
    private final ArrayList<Link> links;
    /**
     * The set of Packets To Send to each of the respective Links
     */
    private HashMap<Link, Deque<Packet>> packetsToSend;
    /**
     * Maps a Host to the respective distance, link to send along
     */
    private HashMap<Node, Pair<Double, Link>> currentRoutingTable;

    /**
     * The next routing table. It is developed using routingtable packets
     */
    private HashMap<Node, Pair<Double, Link>> nextRoutingTable;

    private LinkedList<HashMap<Node, Pair<Double, Link>>> previousRoutingTables;

    public Router(String address, ArrayList<Link> links) {
        super(address);
        this.links = links;

        //Generate HashMap
        packetsToSend = new HashMap<>();
        for(Link l : links) { //Setup queues for each of the links in this router
            packetsToSend.put(l, new LinkedList<Packet>());
        }

        previousRoutingTables = new LinkedList<>();
    }

    public ArrayList<Link> getLinks() {
        return links;
    }
    public HashMap<Node, Pair<Double, Link>> getCurrentRoutingTable() {return currentRoutingTable; }

    public void initializeRoutingTable() {
        nextRoutingTable = new HashMap<>();

        //Add neighbors
        for(Link link : links) {
            Node neighbor = link.getOtherEnd(this);
            Pair<Double, Link> neighborInformation = Pair.of(link.getDelay(this), link);
            nextRoutingTable.put(neighbor, neighborInformation);
        }

        //Itself
        Pair<Double, Link> selfInformation = Pair.of(0.0, (Link) null);
        nextRoutingTable.put(this, selfInformation);

        if(currentRoutingTable == null) {
            currentRoutingTable = nextRoutingTable;
            initializeRoutingTable();
        }
    }

    /**
     * This method updates this routers next routing table given the following information by the Bellman-Ford algorithm
     * @param connectingLink index of their connecting link
     * @param neighborRoutingTable and the routing table of the neighbor
     */
    private void updateRoutingTable(Link connectingLink, HashMap<Node,Pair<Double,Link>> neighborRoutingTable) {
        for(Node node : neighborRoutingTable.keySet()) {
            Pair<Double, Link> neighborsKnowledge = neighborRoutingTable.get(node);

            Pair<Double, Link> myKnowledge = nextRoutingTable.get(node);
            Double distanceThroughNeighbor = connectingLink.getDelay(this) + neighborsKnowledge.fst;
            if(myKnowledge == null || distanceThroughNeighbor < myKnowledge.fst) {
                Pair<Double, Link> tableEntry = Pair.of(distanceThroughNeighbor, connectingLink);
                nextRoutingTable.put(node, tableEntry);
            }
        }
    }

    /**
     * When a non-routingtablepacket is received it is forwarded along the appropriate link in accordance to the routing table.
     * When a routingtablepacket is received it is used to recompute the routing table.
     * @param packet The packet being receiving
     * @param receivingLink The link that it was sent on
     */
    public void receivePacket(Packet packet, Link receivingLink) {
        if (packet instanceof RoutingTablePacket) {
            RoutingTablePacket rpacket = (RoutingTablePacket) packet;
            updateRoutingTable(receivingLink, rpacket.getRoutingTable());
        } else {
            Node destination = packet.getDestination();

            //Check the routing table for which link to send out these packets on
            Pair<Double, Link> bestPath = currentRoutingTable.get(destination);
            if (bestPath == null) {
                System.out.println(Main.currentTime);
                System.out.println("Destination unknown in routing table.");
            } else {
                Link bestLink = bestPath.snd;
                Deque<Packet> sendingQueue = packetsToSend.get(bestLink);
                if (sendingQueue == null) {
                    System.out.println("Sending Queue doesn't exist! Uh-oh");
                } else {
                    sendingQueue.add(packet);
                }
            }
        }
    }

    /**
     * Forwards all the packets queued up along the router along their way.
     * Pays no attention to any constraints.
     * Periodically send the routing table along all links. Also periodically update to the next routing table.
     */
    public void update() {

        if((routingTablesConverged && Main.currentTime % 4000 == 0) ||
                (!routingTablesConverged && Main.currentTime % 100 == 0)) {
            for(Link link : links) {
                Node otherEnd = link.getOtherEnd(this);
                RoutingTablePacket routingTablePacket = new RoutingTablePacket(this, otherEnd, currentRoutingTable);
                packetsToSend.get(link).addFirst(routingTablePacket);
            }
        }

        if((routingTablesConverged && Main.currentTime % 5000 == 0) ||
                (!routingTablesConverged && Main.currentTime % 100 == 0)) {
            //First we ensure that we have entries in every part of the newtable
            //If not, we get them from the old table
            Set<Node> currentNodesKnown = currentRoutingTable.keySet();

            if(previousRoutingTables.size() >= 3)
            {
                previousRoutingTables.removeLast();
                boolean agreement = true;
                if(!routingTablesConverged) {
                    for (Node node : currentNodesKnown) {
                        Pair<Double, Link> currentEntry = currentRoutingTable.get(node);
                        for (HashMap<Node, Pair<Double, Link>> prevRoutingTable : previousRoutingTables) {
                            Pair<Double, Link> previousEntry = prevRoutingTable.get(node);
                            if (previousEntry == null || currentEntry.snd != previousEntry.snd) {
                                agreement = false;
                            }
                        }
                    }
                    if (agreement)
                        routingTablesConverged = true;
                }
            }
            previousRoutingTables.addFirst(currentRoutingTable);

            //First we ensure that we have entries in every part of the newtable
            //If not, we get them from the old table
            for(Node node : currentNodesKnown) {
                if(!nextRoutingTable.containsKey(node)) {
                    Pair<Double, Link> entry = currentRoutingTable.get(node);
                    nextRoutingTable.put(node, entry);
                }
            }

            currentRoutingTable = nextRoutingTable;
            initializeRoutingTable();
        }


        for(Link link : links) {
            Deque<Packet> sendingQueue = packetsToSend.get(link);
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