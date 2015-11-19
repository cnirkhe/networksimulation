package com.ricketts;

/**
 * Created by chinmay on 11/16/15.
 */
public abstract class Packet {
    private final Integer id;
    private final Integer size;   // packet size in bits
    private final Host source;
    private final Host destination;

    public Packet(Integer id, Integer size, Host source, Host destination) {
        this.id = id;
        this.size = size;
        this.source = source;
        this.destination = destination;
    }

    public Packet(Integer id, Integer size, Flow parentFlow) {
        this(id, size, parentFlow.getSource(), parentFlow.getDestination());
    }

    // Fix this, not sure what it's supposed to look like since the other
    // constructors don't work either
    public Packet(Integer id, Integer size) {
        this(id, size, null, null);
    }

    public Integer getID() { return id; }
    public Integer getSize() { return size; }
    public Host getSource() { return source; }
    public Host getDestination() { return destination; }
}