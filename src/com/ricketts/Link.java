package com.ricketts;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by chinmay on 11/16/15.
 */
public class Link implements Updateable
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

    private enum Direction { LEFT, RIGHT }

    private class PacketDirectionPair
    {
        PacketDirectionPair(Packet packet, Direction direction)
        {
            this.packet = packet;
            this.direction = direction;
        }

        Packet packet;
        Direction direction;
    }

    /**
     * TODO This assumes that the links are one directional
     * This is a simplification that needs to be remedied
     */
    private Queue<PacketDirectionPair> packetBuffer;
    /**
     * The number of bits in the packet buffer currently
     */
    private Integer packetBufferFill;

    private Queue<PacketDirectionPair> currentlyTransmittingPackets;

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
     * @return false -> Dropped Packet, true -> Successfully Added to Buffer
     */
    public Boolean addPacket(Packet packet, Node sendingNode)
    {
        if(packet.getPacketSize() + packetBufferFill < linkBuffer)
        {
            if(sendingNode == leftNode)
            {
                packetBuffer.add(new PacketDirectionPair(packet,Direction.RIGHT));
            }
            else if(sendingNode == rightNode)
            {
                packetBuffer.add(new PacketDirectionPair(packet, Direction.LEFT));
            }
            else //Not coming from a sending node
            {
                return false;
            }
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
        while(!currentlyTransmittingPackets.isEmpty() && overallTime - currentlyTransmittingPackets.peek().packet.getSendTime() >= linkDelay)
        {
            PacketDirectionPair current = currentlyTransmittingPackets.peek();
            bitsInTransmission -= current.packet.getPacketSize();
            System.out.println("Moving " + (current.packet instanceof ACKPacket ? "ACK" : "Data") + " packet " + current.packet.getPacketId() + " out of link " + linkId);
            if(current.direction == Direction.RIGHT) {
                rightNode.receivePacket(currentlyTransmittingPackets.remove().packet);
            }
            else if( current.direction == Direction.LEFT)
            {
                leftNode.receivePacket(currentlyTransmittingPackets.remove().packet);
            }
        }

        /** Add packets if there is space on the link */
        while(!packetBuffer.isEmpty() && totalBitsTransmittable - bitsInTransmission > packetBuffer.peek().packet.getPacketSize())
        {
            PacketDirectionPair packetDirectionPair = packetBuffer.remove();
            packetDirectionPair.packet.setSendTime(overallTime);
            currentlyTransmittingPackets.add(packetDirectionPair);
            bitsInTransmission += packetDirectionPair.packet.getPacketSize();
            System.out.println("Adding " + (packetDirectionPair.packet instanceof ACKPacket ? "ACK" : "Data") + " packet " + packetDirectionPair.packet.getPacketId() + " to link " + linkId);
        }
    }
}