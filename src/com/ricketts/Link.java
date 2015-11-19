package com.ricketts;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by chinmay on 11/16/15.
 */
public class Link implements Updatable
{
    private enum Direction {LEFT, RIGHT}

    private final Integer linkID;
    private final Integer linkRate;     // link rate in bits per second
    private final Integer linkDelay;    // delay in milliseconds
    private final Integer linkBuffer;   // buffer size in bits

    private final Node leftNode, rightNode;

    // packets transmitting have a direction and start time associated with them
    private class TransmittingPacket
    {
        public Packet packet;
        public Direction direction;
        public Integer transmissionStartTime;

        public TransmittingPacket(Packet packet, Direction direction,
            Integer transmissionStartTime) {
            this.packet = packet;
            this.direction = direction;
            this.transmissionStartTime = transmissionStartTime;
        }
    }

    // packet buffers
    private Queue<TransmittingPacket> leftPacketBuffer;
    private Queue<TransmittingPacket> rightPacketBuffer;
    
    // remaining capacity on the buffers
    private Integer leftBufferRemainingCapacity;
    private Integer rightBufferRemainingCapacity;

    // Parker: I don't know what below variables are for. What is this for?
    //         Please reply in the group chat
    // private Queue<TransmittingPacket> currentlyTransmittingPackets;
    // private final Integer totalBitsTransmittable;
    // private Integer bitsInTransmission;

    public Link(
        Integer linkID,
        Integer linkRate,
        Integer linkDelay,
        Integer linkBuffer,
        Node leftNode,
        Node rightNode)
    {
        this(linkID, linkRate, linkDelay, linkBuffer, leftNode, rightNode);

        leftPacketBuffer = new LinkedList<TransmittingPacket>();
        rightPacketBuffer = new LinkedList<TransmittingPacket>();
        leftBufferRemainingCapacity = this.linkBuffer;
        rightBufferRemainingCapacity = this.linkBuffer;

        // Parker: Yeah I think we can just calculate these when needed. But
        //         feel free to add them back in if needed
        // currentlyTransmittingPackets = new LinkedList<>();
        // totalBitsTransmittable = (linkRate * linkDelay / 1000);
        // bitsInTransmission = 0;
    }

    // Constructor before setting left and right nodes
    public Link(
            Integer linkID,
            Integer linkRate,
            Integer linkDelay,
            Integer linkBuffer)
    {
        this(linkID, linkRate, linkDelay, linkBuffer, null, null);
    }

    public Integer getID() { return this.linkID; }

    // Parker: I didn't touch anything below. Link update functionality goes
    //         here. @Nailen you have that stuff right?

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
                packetBuffer.add(new TransmittingPacket(packet,Direction.RIGHT));
            }
            else if(sendingNode == rightNode)
            {
                packetBuffer.add(new TransmittingPacket(packet, Direction.LEFT));
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
            TransmittingPacket current = currentlyTransmittingPackets.peek();
            bitsInTransmission -= current.packet.getPacketSize();
            System.out.println("Moving " + (current.packet instanceof ACKPacket ? "ACK" : "Data") + " packet " + current.packet.getPacketID() + " out of link " + linkID);
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
            TransmittingPacket packetDirectionPair = packetBuffer.remove();
            packetDirectionPair.packet.setSendTime(overallTime);
            currentlyTransmittingPackets.add(packetDirectionPair);
            bitsInTransmission += packetDirectionPair.packet.getPacketSize();
            System.out.println("Adding " + (packetDirectionPair.packet instanceof ACKPacket ? "ACK" : "Data") + " packet " + packetDirectionPair.packet.getPacketID() + " to link " + linkID);
        }
    }
}