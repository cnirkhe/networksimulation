package com.ricketts;

import com.sun.tools.javac.util.Pair;

import java.util.HashMap;

/**
 * A Packet containing the physical data of a Routing Table.
 * Made of variable size.
 */
public class RoutingTablePacket extends Packet {

    private HashMap<Node, Pair<Double, Link>> routingTable;

    /**
     * Create a RoutingTablePacket
     * @param source Where the packet is coming from
     * @param destination Where the packet is going
     * @param routingTable What the packet currently thinks the routing table is
     */
    public RoutingTablePacket(Node source, Node destination, HashMap<Node, Pair<Double, Link>> routingTable) {
        super(0, calculateTableSize(routingTable) ,source, destination);
        this.routingTable = routingTable;
    }

    /**
     * The size of the routing table.
     * A Hashmap has size 32 * SIZE + 4 * CAPACITY
     * http://java-performance.info/memory-consumption-of-java-data-types-2/
     * But we are forgetting that we are using a Pair object in the simulation which
     * in reality we would send the physical integer, so we need another 4 bytes per SIZE
     * Therfore 36 * SIZE + 4 * CAPACITY
     * @param routingTable the routing table being used
     * @return int of the size of routing table in bytes
     */
    private static int calculateTableSize(HashMap<Node, Pair<Double, Link>> routingTable) {
        int initialCapacity = (routingTable.size() > 16 ? routingTable.size() : 16);
        return 8 * (36 * routingTable.size() + 4 * initialCapacity);
    }

    public HashMap<Node, Pair<Double, Link>> getRoutingTable() {
        return this.routingTable;
    }
}