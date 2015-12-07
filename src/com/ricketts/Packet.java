package com.ricketts;

/**
 * This object represents the Packets that are sent between Nodes.
 * Key properties of this simulated Packet include:
 *  (a) Containing information on the size of the packet, but no physical data
 *  (b) Containing information about the host and destination
 *
 *  Packets can be generated from Flows or individually
 *  Note that Packets are abstract. They must be instantiated.
 */
public abstract class Packet {
    private final Integer id;
    private final Integer size;   // packet size in bits
    private final Node source;
    private final Node destination;

    public Packet(Integer id, Integer size, Node source, Node destination) {
        this.id = id;
        this.size = size;
        this.source = source;
        this.destination = destination;
    }

    public Packet(Integer id, Integer size, Flow parentFlow) {
        this(id, size, parentFlow.getSource(), parentFlow.getDestination());
    }

    public Integer getID() { return id; }
    public Integer getSize() { return size; }
    public Node getSource() { return source; }
    public Node getDestination() { return destination; }
}