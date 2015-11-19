package com.ricketts;

/**
 * Created by chinmay on 11/16/15.
 */
public abstract class Packet {
    private final Integer packetID;
    private final Integer size;   // packet size in bits
    private final Host source;
    private final Host destination;

    public Packet(Integer packetID, Integer size, Host source,
        Host destination) {
        this.packetID = packetID;
        this.size = size;
        this.source = source;
        this.destination = destination;
    }

    public Packet(Integer packetID, Integer size, Flow parentFlow) {
        this.packetID = packetID;
        this.size = size;
        this.source = parentFlow.getSource();
        this.destination = parentFlow.getDestination();
    }

    // Fix this, not sure what it's supposed to look like since the other
    // constructors don't work either
    public Packet(Integer packetID, Integer size) {
        this(packetID, size, null, null);
    }

    public Integer getPacketID() { return packetID; }
    public Integer getSize() { return size; }
    public Host getSource() { return source; }
    public Host getDestination() { return destination; }
}