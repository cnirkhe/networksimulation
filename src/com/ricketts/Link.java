package com.ricketts;

import java.lang.Math;
import java.util.LinkedList;

/**
 * Created by chinmay on 11/16/15.
 */
public class Link implements Updatable {
    private enum Direction {LEFT, RIGHT}

    private final Integer linkID;
    private final Integer linkRate;     // link rate in bits per second
    private final Integer linkDelay;    // delay in milliseconds
    private final Integer linkBuffer;   // buffer size in bits

    private Node leftNode, rightNode;

    // packets transmitting have a direction and start time associated with them
    private class TransmittingPacket {
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
    private LinkedList<TransmittingPacket> leftPacketBuffer, rightPacketBuffer;
    
    // remaining capacity on the buffers
    private Integer leftBufferRemainingCapacity, rightBufferRemainingCapacity;

    private TransmittingPacket currentlyTransmittingPacket;
    private Integer bitsTransmitted;

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
        this.leftBufferRemainingCapacity = this.linkBuffer;
        this.rightBufferRemainingCapacity = this.linkBuffer;
    }

    // Constructor before setting left and right nodes
    public Link(Integer linkID, Integer linkRate, Integer linkDelay,
            Integer linkBuffer) {
        this.linkID = linkID;
        this.linkRate = linkRate;
        this.linkDelay = linkDelay;
        this.linkBuffer = linkBuffer;
    }

    public Integer getID() { return this.linkID; }

    public Node getLeftNode() { return this.leftNode; }
    public Node getRightNode() { return this.rightNode; }
    public void setLeftNode(Node node) { this.leftNode = node; }
    public void setRightNode(Node node) { this.rightNode = node; }

    // Parker: I didn't touch anything below. Link update functionality goes
    //         here. @Nailen you have that stuff right?

    /**
     * Check if the packet can fit in the buffer otherwise drop it
     * Return a Boolean if the Packet was added to the buffer
     * @param packet
     * @return false -> Dropped Packet, true -> Successfully Added to Buffer
     */
    public Boolean addPacket(Packet packet, Node sendingNode) {
        Integer newRemainingCapacity;
        if (sendingNode == leftNode) {
            newRemainingCapacity =
                leftBufferRemainingCapacity - packet.getSize();
            if (newRemainingCapacity >= 0) {
                leftPacketBuffer.add(new TransmittingPacket(packet,
                        Direction.RIGHT, RunSim.getCurrentTime()));
                leftBufferRemainingCapacity = newRemainingCapacity;
            }
        }
        else if (sendingNode == rightNode) {
            newRemainingCapacity =
                rightBufferRemainingCapacity - packet.getSize();
            if (newRemainingCapacity >= 0) {
                rightPacketBuffer.add(new TransmittingPacket(packet,
                        Direction.LEFT, RunSim.getCurrentTime()));
                rightBufferRemainingCapacity = newRemainingCapacity;
            }
        }
        else {
            System.out.println("addPacket() from unconnected node");
            return false;
        }

        return true;
    }

    public void update(Integer intervalTime, Integer overallTime) {
        Integer timeLeft = intervalTime;
        Integer packetBits, endOfDelay;
        while (timeLeft > 0) {
            if (this.currentlyTransmittingPacket == null) {
                TransmittingPacket leftPacket = leftPacketBuffer.peek();
                TransmittingPacket rightPacket = rightPacketBuffer.peek();
                if (leftPacket == null) {
                    if (rightPacket == null)
                        break;
                    this.currentlyTransmittingPacket = rightPacketBuffer.remove();
                }
                else if (rightPacket == null)
                    this.currentlyTransmittingPacket = leftPacketBuffer.remove();
                else if (leftPacket.transmissionStartTime < rightPacket.transmissionStartTime)
                    this.currentlyTransmittingPacket = leftPacketBuffer.remove();
                else
                    this.currentlyTransmittingPacket = rightPacketBuffer.remove();

                this.currentlyTransmittingPacket.transmissionStartTime = RunSim.getCurrentTime();
            }

            endOfDelay = this.currentlyTransmittingPacket.transmissionStartTime + linkDelay;
            if (endOfDelay > overallTime)
                timeLeft = overallTime + intervalTime - endOfDelay;

            if (timeLeft > 0) {
                packetBits = Math.min(timeLeft * linkRate, 
                    this.currentlyTransmittingPacket.packet.getSize() - this.bitsTransmitted);
                this.bitsTransmitted += packetBits;
                if (this.bitsTransmitted == this.currentlyTransmittingPacket.packet.getSize()) {
                    if (this.currentlyTransmittingPacket.direction == Direction.RIGHT)
                        this.rightNode.receivePacket(this.currentlyTransmittingPacket.packet);
                    else
                        this.leftNode.receivePacket(this.currentlyTransmittingPacket.packet);
                    
                    this.currentlyTransmittingPacket = null;
                    this.bitsTransmitted = 0;

                    timeLeft -= packetBits / linkRate;
                }
            }
            else
                break;
        }
    }
}