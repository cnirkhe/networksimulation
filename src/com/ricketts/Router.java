package com.ricketts;

import com.sun.tools.javac.util.Pair;

import java.util.*;


/**
 * A Router is a type of Node who's job is to process incoming packets and forward them to the best neighbor.
 */
public class Router extends Node {
    /**
     * The period at which the Routing Table is broadcast to all of its neighbors
     */
    private static final Integer TABLE_BROADCAST_PERIOD = 100;

    /**
     * The minimum number of identical routing tables before the router has declared convergence
     */
    private static final Integer MINIMUM_IDENTICAL_TABLES = 4;

    /**
     * The time left in the period before rebroadcast
     */
    private Integer timeLeftInPeriod;

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
     * Over the TABLE_BROADCAST_PERIOD a new routing table is built (dynamically)
     */
    private HashMap<Node, Pair<Double, Link>> developingRoutingTable;

    /**
     * The collection of old RoutingTables from previous constructions
     * This is used to check convergence of routing tables
     */
    private LinkedList<HashMap<Node, Pair<Double, Link>>> oldRoutingTables;

    /**
     * Boolean to indicate if the routing tables have converged to a single value.
     */
    private Boolean convergedRoutingTable;

    public Router(String address, ArrayList<Link> links) {
        super(address);
        this.links = links;

        //Generate HashMap
        packetsToSend = new HashMap<>();
        for(Link l : links) { //Setup queues for each of the links in this router
            packetsToSend.put(l, new LinkedList<Packet>());
        }

        timeLeftInPeriod = 0;
    }

    public ArrayList<Link> getLinks() {
        return links;
    }

    /**
     * Setups minimum distance for its neighbors and itself
     */
    public void initializeRoutingTable() {
        currentRoutingTable = new HashMap<>();

        //Neighbors
        for(Link link : links) {
            Node neighbor = link.getOtherEnd(this);
            Pair<Double, Link> neighborInformation = Pair.of(link.getLinkDelay().doubleValue(), link);
            currentRoutingTable.put(neighbor, neighborInformation);
        }

        //Itself
        Pair<Double, Link> selfInformation = Pair.of(Double.valueOf(0.0), (Link) null);
        currentRoutingTable.put(this, selfInformation);

        developingRoutingTable = new HashMap<>();

        oldRoutingTables = new LinkedList<>();
        convergedRoutingTable = false;
    }

    /**
     * This method updates this router's developing routing table given the following information by the Bellman-Ford algorithm
     * @param connectingLink index of their connecting link
     * @param neighborRoutingTable and the routing table of the neighbor
     */
    private void updateRoutingTable(Link connectingLink,
                                    HashMap<Node,Pair<Double,Link>> neighborRoutingTable) {
        //Calculate who sent it
        Node neighbor = connectingLink.getOtherEnd(this);

        Pair<Double, Link> neighborInformation = Pair.of(connectingLink.getLinkDelay() + connectingLink.getEstimatedBufferDelay(this), connectingLink);

        // Now we update our routing table using the triangle inequality and all of the information in the neighbor's routing table
        for( Node router : neighborRoutingTable.keySet()) {
            Pair<Double, Link> routerInformation = neighborRoutingTable.get(router);
            Pair<Double, Link> myCurrentInformation = developingRoutingTable.getOrDefault(router, Pair.of(Double.MAX_VALUE, (Link) null));
            //If our ideal path already goes through the neighbor router or the path through the neighbor is better
            if(myCurrentInformation.snd == connectingLink || routerInformation.fst + neighborInformation.fst < myCurrentInformation.fst) {
                //Change the routing to go through neighbor router
                myCurrentInformation = Pair.of(routerInformation.fst + neighborInformation.fst, connectingLink);
                developingRoutingTable.put(router, myCurrentInformation);
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

            //Check the routingtable for which link to send out these packets on
            Pair<Double, Link> bestPath = currentRoutingTable.get(destination);
            if (bestPath == null) {
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
     * Method for updating the developing routing table into the current routing table.
     */
    private void updateRoutingTable() {

        //First need to make sure each location in the old routing table is in the new routing table
        //in some capacity
        Set<Node> destinationKeySet = currentRoutingTable.keySet();
        for(Node destination : destinationKeySet) {
            if(!developingRoutingTable.containsKey(destination)) {
                developingRoutingTable.put(destination, currentRoutingTable.get(destination));
            }
        }

        //Add the current routing table to the set of old routing tables
        //Then verify that we don't have MINIMUM_IDENTICAL_TABLES identical routing tables
        //If we do, then the routing tables have converges

        if(oldRoutingTables.size() == MINIMUM_IDENTICAL_TABLES ) {
            oldRoutingTables.remove(0); //Remove the first one
        }

        //Test if they are identical on all destination (in the sense the link that they send it on)
        boolean identical = true;

        Iterator<Node> destinationIterator = destinationKeySet.iterator();
        while(identical && destinationIterator.hasNext()) {
            Node destination = destinationIterator.next();
            Link linkUsed = currentRoutingTable.get(destination).snd;

            Iterator<HashMap<Node, Pair<Double, Link>>> routingTableIterator = oldRoutingTables.iterator();
            HashMap<Node, Pair<Double,Link>> oldRoutingTable;
            while(identical && routingTableIterator.hasNext()) {
                oldRoutingTable = routingTableIterator.next();
                Link oldLink = oldRoutingTable.get(destination).snd;
                if(oldLink != linkUsed) {
                    identical = false;
                }
            }
        }

        //If identical, then routing tables have converged
        if(identical) {
            convergedRoutingTable = true;
        }
        else {
            oldRoutingTables.add(currentRoutingTable);
            currentRoutingTable = developingRoutingTable;
            developingRoutingTable = new HashMap<>();
        }
    }

    /**
     * Forwards all the packets queued up along the router along their way.
     * Pays no attention to any constraints.
     * Periodically send the routing table along all links.
     * @param intervalTime The time step of the simulation
     * @param overallTime Overall simulation time
     */
    public void update(Integer intervalTime, Integer overallTime) {

        //Broadcasting the Routing Table and update our own routing table
        if(timeLeftInPeriod <= 0) {
            //Broadcast Routing Table
            System.out.println("Router " + address + " is broadcasting its table.");
            timeLeftInPeriod = TABLE_BROADCAST_PERIOD;
            for(Link link : links) {
                Node otherEnd = link.getOtherEnd(this);
                RoutingTablePacket routingTablePacket = new RoutingTablePacket(this, otherEnd, currentRoutingTable);
                packetsToSend.get(link).addFirst(routingTablePacket);
            }

            //Update own routing table if we have not converged yet
            if(!convergedRoutingTable)
                updateRoutingTable();

        } else {
            timeLeftInPeriod -= intervalTime;
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