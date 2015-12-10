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
    private final Integer size;
    private final Node source;
    private final Node destination;

    /**
     * Constructor based on the packet's source and destination
     * @param id Packet id
     * @param size Size in bits
     * @param source Where the packet is coming from
     * @param destination Where the packet is going to
     */
    public Packet(Integer id, Integer size, Node source, Node destination) {
        this.id = id;
        this.size = size;
        this.source = source;
        this.destination = destination;
    }

    /**
     * Constructor based on a parent flow.
     * @param id Packet id
     * @param size Size in bits
     * @param parentFlow The flow this packet is being sent on
     */
    public Packet(Integer id, Integer size, Flow parentFlow) {
        this(id, size, parentFlow.getSource(), parentFlow.getDestination());
    }

    public Integer getID() { return id; }
    public Integer getSize() { return size; }
    public Node getSource() { return source; }
    public Node getDestination() { return destination; }
}