package com.ricketts;

import com.sun.tools.javac.util.Pair;

import java.util.*;

/**
 * A Router is a type of Node who's job is to process incoming packets and forward them to the best neighbor.
 * A Router is primarily defined by its location in the network (i.e. its address and connecting links) and by its routing
 * table. It is the router's task to forward packets in accordance to the routing table.
 */
public class Router extends Node
{
    /**
     * Boolean indicating if routing tables have converged.
     */
    private boolean routingTablesConverged = false;

    /**
     * The set of Links that this router is connected to.
     */
    private final ArrayList<Link> links;
    /**
     * The set of Packets To send to each of the respective Links. Link -> Queue of Packets to Send
     */
    private HashMap<Link, Deque<Packet>> packetsToSend;
    /**
     * This is the representation of the current routing table. It is a map destination -> (Distance, Optimal Link to Use)
     * The current routing table is the one that is used to make decisions at the moment. It is updated periodically with
     * the next routing table.
     */
    private HashMap<Node, Pair<Double, Link>> currentRoutingTable;

    /**
     * The next routing table. It is developed using routing table packets sent by the neighboring routers.
     */
    private HashMap<Node, Pair<Double, Link>> nextRoutingTable;

    /**
     * A collection of previous iterations of the routing table (last 3). They are used to determine convergence.
     */
    private LinkedList<HashMap<Node, Pair<Double, Link>>> previousRoutingTables;

    /**
     * Generic constructor
     * @param address address given to the Router
     * @param links adjacent Links
     */
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

    /**
     * Initializes the next routing table to include information only about self and neighbors (everything the router
     * inherently knows). If the current routing table is null, it initializes that as well.
     */
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
     * The update is a standard triangle inequality update.
     * @param connectingLink connecting link
     * @param neighborRoutingTable and the routing table of the neighbor on that link
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
                System.out.println("Destination unknown in routing table.");
            } else {
                Link bestLink = bestPath.snd;
                Deque<Packet> sendingQueue = packetsToSend.get(bestLink);
                sendingQueue.add(packet);
            }
        }
    }

    /**
     * Periodically send the routing table along all links. Also periodically update to the next routing table.
     * The period is dependent on whether, convergence has occured.
     * Forwards all the packets queued up along the router along their way.
     * Pays no attention to any constraints.
     */
    public void update() {

        /*
         * Forward packets to the neighbors on a periodic interval
         */
        if((routingTablesConverged && Main.currentTime % 4000 == 0) ||
                (!routingTablesConverged && Main.currentTime % 100 == 0)) {
            for(Link link : links) {
                Node otherEnd = link.getOtherEnd(this);
                RoutingTablePacket routingTablePacket = new RoutingTablePacket(this, otherEnd, currentRoutingTable);
                packetsToSend.get(link).addFirst(routingTablePacket);
            }
        }

        /*
         * Periodically test for convergence and update to the next routing table
         */
        if((routingTablesConverged && Main.currentTime % 5000 == 0) ||
                (!routingTablesConverged && Main.currentTime % 100 == 0)) {

            /*
             * Test for convergence by comparing the latest routing table to the old routing tables and seeing
             * if they match for every node -> link pair.
             */
            Set<Node> currentNodesKnown = currentRoutingTable.keySet();
            if(previousRoutingTables.size() >= 3) {
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

            /*
             * As part of switching to the next routing table, we need to make sure that it has every entry that was
             * in the old routing table. Do this by copying over any entries that weren't.
             */
            for(Node node : currentNodesKnown) {
                if(!nextRoutingTable.containsKey(node)) {
                    Pair<Double, Link> entry = currentRoutingTable.get(node);
                    nextRoutingTable.put(node, entry);
                }
            }

            currentRoutingTable = nextRoutingTable;
            initializeRoutingTable();
        }

        /**
         * For every link, send all the packets if they exist
         */
        for(Link link : links) {
            Deque<Packet> sendingQueue = packetsToSend.get(link);
            while(!sendingQueue.isEmpty())
                link.addPacket(sendingQueue.remove(), this);
        }
    }
}