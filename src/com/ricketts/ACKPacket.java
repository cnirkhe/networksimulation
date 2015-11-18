package com.ricketts;

/**
 * Created by chinmay on 11/16/15.
 */
public class ACKPacket extends Packet {
    private static final ACKPacketSize = 64;

    public ACKPacket(Integer packetID, Node sourceNode, Node destinationNode)
    {
        super(packetID, ACKPacketSize, sourceNode, destinationNode);
    }
}