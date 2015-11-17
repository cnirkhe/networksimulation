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
     * Measured in seconds
     */
    private final Double linkDelay;
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
    private Integer packetBufferFill;

    public class PacketTimePair
    {
        public PacketTimePair(Packet packet, Integer time)
        {
            this.packet = packet;
            this.time = time;
        }

        public Packet packet;
        public Integer time;
    }

    private Queue<PacketTimePair> currentlyTransmittingPackets;

    private final Integer totalBitsTransmittable;
    private Integer bitsInTransmission;

    public Link(Integer linkId, Integer linkRate, Double linkDelay, Integer linkBuffer, Node leftNode, Node rightNode)
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

        totalBitsTransmittable = (int)(linkRate * linkDelay);
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
        while( overallTime - currentlyTransmittingPackets.peek().time > linkDelay)
        {
            bitsInTransmission -= currentlyTransmittingPackets.peek().packet.getPacketSize();
            rightNode.receivePacket(currentlyTransmittingPackets.remove().packet);
        }

        /** Add packets if there is space on the link */
        while(totalBitsTransmittable - bitsInTransmission > packetBuffer.peek().getPacketSize())
        {
            PacketTimePair ptpair = new PacketTimePair(packetBuffer.remove(), overallTime);
            currentlyTransmittingPackets.add(ptpair);
            bitsInTransmission += ptpair.packet.getPacketSize();
        }
    }
}