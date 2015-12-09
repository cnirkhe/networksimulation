package com.ricketts;

/**
 * An abstract class to represent both Routers and Hosts.
 */
public abstract class Node implements Updatable
{
    protected final String address;

    public Node(String address)
    {
        this.address = address;
    }

    /**
     * This method is called when a packet is received by the node
     * It details how to handle the packets reception for every class
     * @param packet The packet being receiving
     * @param receivingLink The link that it was sent on
     */
    public abstract void receivePacket(Packet packet, Link receivingLink);

    /**
     * Update along the simulation.
     */
    public abstract void update();

    public String getAddress() {
        return this.address;
    }
}