package com.ricketts;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by chinmay on 11/16/15.
 */
public class Link
{
    private Integer linkId;
    /**
     * Measured in Mbps
     */
    private Integer linkRate;
    /**
     * Measured in ms
     */
    private Integer linkDelay;
    /**
     * Measured in bytes
     */
    private Integer linkBuffer;

    private Node leftNode, rightNode;

    /**
     * TODO This assumes that the links are one directional
     * This is a simplification that needs to be remedied
     */
    private Queue<Packet> packetBuffer;
    private Integer packetBufferFill;

    public Link(Integer linkId, Integer linkRate, Integer linkDelay, Integer linkBuffer, Node leftNode, Node rightNode)
    {
        this.linkId = linkId;
        this.linkRate = linkRate;
        this.linkDelay = linkDelay;
        this.linkBuffer = linkBuffer;
        this.leftNode = leftNode;
        this.rightNode = rightNode;

        packetBuffer = new LinkedList<>();
        packetBufferFill = 0;
    }

    /**
     * Check if the packet can fit in the buffer otherwise drop it
     * Return a Boolean if the Packet was added to the buffer
     * @param packet
     * @return
     */
    public Boolean addPacket(Packet packet)
    {
        if(packet.getPacketSize() + packetBufferFill < linkBuffer)
        {
            packetBuffer.add(packet);
            packetBufferFill += packet.getPacketSize();
            return true;
        }
        return false;
    }

    public void update()
    {

    }
}