package com.ricketts;

import java.lang.Math;
import java.util.LinkedList;

/**
 * Created by chinmay on 11/16/15.
 */
public class Link implements Updatable {
    // Arbitrary orientations
    private enum Direction {LEFT, RIGHT}

    // Link ID
    private final Integer linkID;
    // Link rate in bits per millisecond
    private final Integer linkRate;
    // Link delay in milliseconds
    private final Integer linkDelay;
    // Link buffer in bits
    private final Integer linkBuffer;

    // Nodes this link connects
    private Node leftNode, rightNode;

    /*
     * Data class associating a direction and start time with a packet being
     * transmitted on a link.
     */
    private class TransmittingPacket {
        public Packet packet;
        public Direction direction;
        public Integer transmissionStartTime;

        // Constructor
        public TransmittingPacket(Packet packet, Direction direction,
            Integer transmissionStartTime) {
            this.packet = packet;
            this.direction = direction;
            this.transmissionStartTime = transmissionStartTime;
        }
    }

    // Packet buffers on either end of the link
    private LinkedList<TransmittingPacket> leftPacketBuffer, rightPacketBuffer;
    // Remaining capacity in the two buffers, in bits
    private Integer leftBufferRemainingCapacity, rightBufferRemainingCapacity;

    // Packet currently being transmitted
    private TransmittingPacket currentlyTransmittingPacket;
    // How much of the packet has been transmitted
    private Integer bitsTransmitted;

    /* Constructs a Link with empty buffers. */
    public Link(Integer linkID, Integer linkRate, Integer linkDelay,
        Integer linkBuffer, Node leftNode, Node rightNode) {
        this.linkID = linkID;
        this.linkRate = linkRate;
        this.linkDelay = linkDelay;
        this.linkBuffer = linkBuffer;
        this.leftNode = leftNode;
        this.rightNode = rightNode;

        this.leftPacketBuffer = new LinkedList<TransmittingPacket>();
        this.rightPacketBuffer = new LinkedList<TransmittingPacket>();
        this.leftBufferRemainingCapacity = linkBuffer;
        this.rightBufferRemainingCapacity = linkBuffer;
    }

    /* Constructs a disconnected Link. */
    public Link(Integer linkID, Integer linkRate, Integer linkDelay,
            Integer linkBuffer) {
        this.linkID = linkID;
        this.linkRate = linkRate;
        this.linkDelay = linkDelay;
        this.linkBuffer = linkBuffer;

        this.leftPacketBuffer = new LinkedList<TransmittingPacket>();
        this.rightPacketBuffer = new LinkedList<TransmittingPacket>();
        this.leftBufferRemainingCapacity = linkBuffer;
        this.rightBufferRemainingCapacity = linkBuffer;
    }

    /* Accessor methods */
    public Integer getID() { return this.linkID; }
    public Node getLeftNode() { return this.leftNode; }
    public Node getRightNode() { return this.rightNode; }

    /* Modifier methods. */
    public void setLeftNode(Node node) { this.leftNode = node; }
    public void setRightNode(Node node) { this.rightNode = node; }

    /**
     * Check if the packet can fit in the buffer otherwise drop it
     * Return a Boolean if the Packet was added to the buffer
     * @param packet
     * @return false -> Dropped Packet, true -> Successfully Added to Buffer
     */
    public Boolean addPacket(Packet packet, Node sendingNode) {
        Integer newRemainingCapacity;
        // If packet is coming from the left
        if (sendingNode == leftNode) {
            // Check if it fits in the buffer
            newRemainingCapacity = leftBufferRemainingCapacity - packet.getSize();
            if (newRemainingCapacity >= 0) {
                // If so, add it and update the remaining capacity
                leftPacketBuffer.add(new TransmittingPacket(packet, Direction.RIGHT,
                    RunSim.getCurrentTime()));
                leftBufferRemainingCapacity = newRemainingCapacity;
                return true;
            }
        }
        // Likewise if coming from right
        else if (sendingNode == rightNode) {
            newRemainingCapacity = rightBufferRemainingCapacity - packet.getSize();
            if (newRemainingCapacity >= 0) {
                rightPacketBuffer.add(new TransmittingPacket(packet, Direction.LEFT,
                    RunSim.getCurrentTime()));
                rightBufferRemainingCapacity = newRemainingCapacity;
                return true;
            }
        }
        // If it came from somewhere else, something is wrong
        else
            System.out.println("addPacket() from unconnected node");

        return false;
    }

    /**
     * Updates a Link so that it performs all the transfers it should during a
     * given timestep.
     */
    public void update(Integer intervalTime, Integer overallTime) {
        // While there's time left in the interval,,,
        Integer usageLeft = intervalTime * this.linkRate, packetBits, endOfDelay;

        while (usageLeft > 0) {
            // If there's no packet being currently transmitted, fetch one from
            // the left or right buffer. Preference is given to whichever buffer
            // has the packet at the front of its queue that's been waiting
            // longer.
            if (this.currentlyTransmittingPacket == null) {
                TransmittingPacket leftPacket = leftPacketBuffer.peek();
                TransmittingPacket rightPacket = rightPacketBuffer.peek();
                if (leftPacket == null && rightPacket == null)
                    break;
                else if (rightPacket == null || (leftPacket == null &&
                    leftPacket.transmissionStartTime < rightPacket.transmissionStartTime))
                {    
                    this.currentlyTransmittingPacket = leftPacketBuffer.remove();
                    this.leftBufferRemainingCapacity += leftPacket.packet.getSize();
                }
                else {
                    this.currentlyTransmittingPacket = rightPacketBuffer.remove();
                    this.rightBufferRemainingCapacity += rightPacket.packet.getSize();
                }

                this.currentlyTransmittingPacket.transmissionStartTime = RunSim.getCurrentTime();
                this.bitsTransmitted = 0;
            }

            // Figure out when this packet's propagation delay would be over and
            // it could start transferring to the node
            endOfDelay = this.currentlyTransmittingPacket.transmissionStartTime + linkDelay;
            if (endOfDelay > overallTime)
                usageLeft = (overallTime + intervalTime - endOfDelay) * this.linkRate;

            // If it reaches the node before the timestep is over, transmit as
            // much as possible
            if (usageLeft > 0) {
                // We can either transmit a chunk of the packet, or all that
                // remains of it
                packetBits = Math.min(usageLeft,
                    this.currentlyTransmittingPacket.packet.getSize() - this.bitsTransmitted);
                this.bitsTransmitted += packetBits;
                // If we've transmitted the entire packet, transfer it to the
                // host
                if (this.bitsTransmitted.equals(this.currentlyTransmittingPacket.packet.getSize())) {
                    if (this.currentlyTransmittingPacket.direction == Direction.RIGHT)
                        this.rightNode.receivePacket(this.currentlyTransmittingPacket.packet);
                    else
                        this.leftNode.receivePacket(this.currentlyTransmittingPacket.packet);
                    
                    // We're done transmitting this packet
                    this.currentlyTransmittingPacket = null;
                    this.bitsTransmitted = 0;

                    // Presumably we're in the "remainder" case, and we have to
                    // figure out how long transferring the end of the packet
                    // would take
                    usageLeft -= packetBits;
                }
            }
        }
    }
}