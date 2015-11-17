package com.ricketts;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by chinmay on 11/16/15.
 */
public class Link
{
    private final Integer linkId;
    /**
     * Measured in bps
     */
    private final Integer linkRate;
    /**
     * Measured in milliseconds
     */
    private final Integer linkDelay;
    /**
     * Measured in bytes
     */
    private final Integer linkBuffer;

    private final Node leftNode, rightNode;

    /**
     * TODO This assumes that the links are one directional
     * This is a simplification that needs to be remedied
     */
    private Queue<Packet> packetBuffer;
    /**
     * The number of bits in the packet buffer currently
     */
    private Integer packetBufferFill;

    private Queue<Packet> currentlyTransmittingPackets;

    private final Integer totalBitsTransmittable;
    private Integer bitsInTransmission;

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

        currentlyTransmittingPackets = new LinkedList<>();

        totalBitsTransmittable = (linkRate * linkDelay / 1000);
        bitsInTransmission = 0;
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

    public void update(Integer intervalTime, Integer overallTime)
    {
        /**
         * If the time has come to move the packet to the other side of the link
         */
        while(!currentlyTransmittingPackets.isEmpty() && overallTime - currentlyTransmittingPackets.peek().getSendTime() >= linkDelay)
        {
            bitsInTransmission -= currentlyTransmittingPackets.peek().getPacketSize();
            System.out.println("Moving packet " + currentlyTransmittingPackets.peek().getPacketId() + " out of link " + linkId);
            rightNode.receivePacket(currentlyTransmittingPackets.remove());
        }

        /** Add packets if there is space on the link */
        while(!packetBuffer.isEmpty() && totalBitsTransmittable - bitsInTransmission > packetBuffer.peek().getPacketSize())
        {
            Packet packet = packetBuffer.remove();
            packet.setSendTime(overallTime);
            currentlyTransmittingPackets.add(packet);
            bitsInTransmission += packet.getPacketSize();
            System.out.println("Adding packet " + packet.getPacketId() + " to link " + linkId);
        }
    }
}