package com.ricketts;

/**
 * Created by chinmay on 11/16/15.
 */
public class ACKPacket extends Packet {
    private static final ACKPacketSize = 64;

    public ACKPacket(Node sourceNode, Node destinationNode)
    {
        super(-1, ACKPacketSize, sourceNode, destinationNode);
    }
}