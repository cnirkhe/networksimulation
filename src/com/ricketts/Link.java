package com.ricketts;

import org.jfree.data.xy.XYSeries;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * The Link is unlike a physical Link. Instead think of a Link as the physical link plus the buffers on either end.
 * A link is defined as LEFT to RIGHT (the naming is arbitrary) but packets are sent in 1 direction at a time.
 */
public class Link implements Updatable {

    private Integer timeSinceReBufferDelay;

    /**
     * Orientations for Packets flowing on the link
     */
    public enum Direction {LEFT, RIGHT}

    private final Integer linkID;
    /**
     * Link rate in bits per millisecond
     */
    private final Integer linkRate;
    /**
     * Link delay in milliseconds
     */
    private final Integer linkDelay;
    /**
     * Link buffer in bits
     */
    private final Integer linkBuffer;

    private Node leftNode, rightNode;

    private Integer numbLeftPktsThruBuffer;
    private Integer numbRightPktsThruBuffer;

    private Double sumLeftBufferTime;
    private Double sumRightBufferTime;

    private Double latestLeftBufferDelayEstimate;
    private Double latestRightBufferDelayEstimate;

    /**
     * Packet drops for current interval
     */
    private Integer packetDrops;
    private LinkAnalyticsCollector linkAnalyticsCollector;

    /**
     * Indicator for whether or not we should graph this link
     */
    public Boolean graph;

    /**
     * Data class associating a direction and start time with a packet being
     * transmitted on a link.
     */
    private class TransmittingPacket {
        public Packet packet;
        public Direction direction;
        public Integer transmissionStartTime;

        public TransmittingPacket(Packet packet, Direction direction, Integer transmissionStartTime) {
            this.packet = packet;
            this.direction = direction;
            this.transmissionStartTime = transmissionStartTime;
        }
    }

    /**
     * Packet buffers on either end of the link
     */
    private LinkedList<TransmittingPacket> leftPacketBuffer, rightPacketBuffer;
    /**
     * Remaining capacity in the two buffers, in bits
     */
    private Integer leftBufferRemainingCapacity, rightBufferRemainingCapacity;

    /**
     * Packet currently being transmitted
     */
    private Queue<TransmittingPacket> currentlyTransmittingPackets;

    /**
     * How many bits have been transmitted in the total period
     */
    private Integer totalBitsTransmitted;

    /**
     * Total buffer capacity and link rate over an interval so we can average for analytics.
     */
    private Integer sumTotalBitsTransmitted;
    private Integer sumBufferCapacity;

    /**
     * Constructor without nodes defined
     */
    public Link(Integer linkID, Integer linkRate, Integer linkDelay, Integer linkBuffer, String name,
                boolean graph) {
        this.linkID = linkID;
        this.linkRate = linkRate;
        this.linkDelay = linkDelay;
        this.linkBuffer = linkBuffer;

        this.leftPacketBuffer = new LinkedList<>();
        this.rightPacketBuffer = new LinkedList<>();
        this.leftBufferRemainingCapacity = linkBuffer;
        this.rightBufferRemainingCapacity = linkBuffer;
        this.packetDrops = 0;
        this.totalBitsTransmitted = 0;
        this.linkAnalyticsCollector = new LinkAnalyticsCollector(linkID, name);
        this.sumBufferCapacity = 0;
        this.sumTotalBitsTransmitted = 0;
        this.graph = graph;

        initializeBufferDelayEstimate();

        currentlyTransmittingPackets = new LinkedList<>();
    }

    public void initializeBufferDelayEstimate() {
        numbLeftPktsThruBuffer = 0;
        numbRightPktsThruBuffer = 0;
        sumLeftBufferTime = 0.0;
        sumRightBufferTime = 0.0;
        timeSinceReBufferDelay = 0;

        latestLeftBufferDelayEstimate = 0.0;
        latestRightBufferDelayEstimate = 0.0;
    }

    public Integer getID() { return this.linkID; }
    public Node getLeftNode() { return this.leftNode; }
    public Node getRightNode() { return this.rightNode; }
    public Integer getLinkDelay() { return this.linkDelay; }
    public void setLeftNode(Node node) { this.leftNode = node; }
    public void setRightNode(Node node) { this.rightNode = node; }


    public Double getBufferDelay(Direction direction) {
        if(direction == Direction.LEFT) {
            return latestLeftBufferDelayEstimate;
        }
        else {
            return latestRightBufferDelayEstimate;
        }
    }
    private Double getBufferDelay(Node node) {
        if(node == leftNode) {
            return getBufferDelay(Direction.LEFT);
        } else if (node == rightNode) {
            return getBufferDelay(Direction.RIGHT);
        } else
            return 0.0;
    }
    public Double getDelay(Node node) {
        //return getLinkDelay().doubleValue();
        return getLinkDelay() + getBufferDelay(node);
    }
    public Node getOtherEnd(Node oneEnd) {
        if (oneEnd == leftNode) {
            return rightNode;
        } else if (oneEnd == rightNode) {
            return leftNode;
        } else {
            return null;
        }
    }

    /**
     * Check if the packet can fit in the buffer otherwise drop it
     * Return a Boolean if the Packet was added to the buffer
     * @param packet the packet being sent across the node
     * @param sendingNode the node sending the packet
     * @return false if Dropped Packet or true if Successfully Added to Buffer
     */
    public Boolean addPacket(Packet packet, Node sendingNode) {
        Integer newRemainingCapacity;
        // If packet is coming from the left
        if (sendingNode == leftNode) {
            // Check if it fits in the buffer
            newRemainingCapacity = leftBufferRemainingCapacity - packet.getSize();
            if (newRemainingCapacity >= 0) {
                // If so, add it and update the remaining capacity
                leftPacketBuffer.add(new TransmittingPacket(packet, Direction.RIGHT, Main.currentTime));
                leftBufferRemainingCapacity = newRemainingCapacity;
                return true;
            }
        }
        // Likewise if coming from right
        else if (sendingNode == rightNode) {
            newRemainingCapacity = rightBufferRemainingCapacity - packet.getSize();
            if (newRemainingCapacity >= 0) {
                rightPacketBuffer.add(new TransmittingPacket(packet, Direction.LEFT, Main.currentTime));
                rightBufferRemainingCapacity = newRemainingCapacity;
                return true;
            }
        }
        // If it came from somewhere else, something is wrong
        else {
            System.out.println("addPacket() from unconnected node");
        }
        // We dropped this packet
        packetDrops = packetDrops + 1;
        return false;
    }

    /**
     * Clears the buffers (we want to do this if we have a timeout or retransmit to avoid sending
     * a lot of unnecessary packets).
     * @param sendingNode: the node we're sending from
     */
    public void clearBuffer(Node sendingNode) {
        // We want to clear the buffer we're sending from
        if (sendingNode == leftNode) {
            leftPacketBuffer.clear();
            leftBufferRemainingCapacity = linkBuffer;
        }
        else if (sendingNode == rightNode) {
            rightPacketBuffer.clear();
            rightBufferRemainingCapacity = linkBuffer;
        }
        else {
            System.out.println("Something went terribly wrong");
        }
    }

    /**
     * Updates the state of the Link. This involves any of the following:
     * (a) Updating the state of packets currently in transmission
     * (b) Removing transmitted packets
     * (c) Adding packets to the link from the link buffer

     */
    public void update() {

        //Buffer Estimate
        if(Main.currentTime % 100 == 0) {

            if(numbLeftPktsThruBuffer == 0)
                latestLeftBufferDelayEstimate = 0.0;
            else
                latestLeftBufferDelayEstimate = sumLeftBufferTime / numbLeftPktsThruBuffer;

            if(numbRightPktsThruBuffer == 0)
                latestRightBufferDelayEstimate = 0.0;
            else
                latestRightBufferDelayEstimate = sumRightBufferTime / numbRightPktsThruBuffer;

            numbLeftPktsThruBuffer = 0;
            numbRightPktsThruBuffer = 0;
            sumLeftBufferTime = 0.0;
            sumRightBufferTime = 0.0;
        }

        // Reset total bits transmitted for new interval
        totalBitsTransmitted = 0;

        while(!currentlyTransmittingPackets.isEmpty() &&
                Main.currentTime >= currentlyTransmittingPackets.peek().transmissionStartTime + linkDelay ) {
            //remove the packet
            TransmittingPacket transmittedPacket = currentlyTransmittingPackets.remove();
            Integer size = transmittedPacket.packet.getSize();
            totalBitsTransmitted += size;
            if(transmittedPacket.direction == Direction.LEFT) {
                leftNode.receivePacket(transmittedPacket.packet, this);
            } else {
                rightNode.receivePacket(transmittedPacket.packet, this);
            }
        }

        Integer bitsAddedToLink = 0;
        Integer bitsAddableToLink = Main.intervalTime * this.linkRate;

        boolean transmitPackets = true;
        while(transmitPackets && !this.leftPacketBuffer.isEmpty() && !this.rightPacketBuffer.isEmpty()) {
            if(leftPacketBuffer.peek().transmissionStartTime <= rightPacketBuffer.peek().transmissionStartTime &&
                    leftPacketBuffer.peek().packet.getSize() <= bitsAddableToLink - bitsAddedToLink) {
                //Remove leftpacket and put onto transmitting
                TransmittingPacket transmittingPacket = leftPacketBuffer.remove();
                sumLeftBufferTime += Main.currentTime - transmittingPacket.transmissionStartTime;
                numbLeftPktsThruBuffer++;
                this.leftBufferRemainingCapacity += transmittingPacket.packet.getSize();
                currentlyTransmittingPackets.add(transmittingPacket);
                bitsAddedToLink += transmittingPacket.packet.getSize();
                transmittingPacket.transmissionStartTime = Main.currentTime;
            } else if(leftPacketBuffer.peek().transmissionStartTime > rightPacketBuffer.peek().transmissionStartTime &&
                    rightPacketBuffer.peek().packet.getSize() <= bitsAddableToLink - bitsAddedToLink){
                //Remove rightpacket and put onto transmitting
                TransmittingPacket transmittingPacket = rightPacketBuffer.remove();
                sumRightBufferTime += Main.currentTime - transmittingPacket.transmissionStartTime;
                numbRightPktsThruBuffer++;
                this.rightBufferRemainingCapacity += transmittingPacket.packet.getSize();
                currentlyTransmittingPackets.add(transmittingPacket);
                bitsAddedToLink += transmittingPacket.packet.getSize();
                transmittingPacket.transmissionStartTime = Main.currentTime;
            } else {
                //Move neither
                transmitPackets = false;
            }
        }

        while(transmitPackets && !leftPacketBuffer.isEmpty()) {
            if(leftPacketBuffer.peek().packet.getSize() <= bitsAddableToLink - bitsAddedToLink) {
                //Remove leftpacket and put onto transmitting
                TransmittingPacket transmittingPacket = leftPacketBuffer.remove();
                sumLeftBufferTime += Main.currentTime - transmittingPacket.transmissionStartTime;
                numbLeftPktsThruBuffer++;
                this.leftBufferRemainingCapacity += transmittingPacket.packet.getSize();
                currentlyTransmittingPackets.add(transmittingPacket);
                bitsAddedToLink += transmittingPacket.packet.getSize();
                transmittingPacket.transmissionStartTime = Main.currentTime;
            } else {
                //Don't move
                transmitPackets = false;
            }
        }

        while(transmitPackets && !rightPacketBuffer.isEmpty()) {
            if(rightPacketBuffer.peek().packet.getSize() <= bitsAddableToLink - bitsAddedToLink) {
                TransmittingPacket transmittingPacket = rightPacketBuffer.remove();
                sumRightBufferTime += Main.currentTime - transmittingPacket.transmissionStartTime;
                numbRightPktsThruBuffer++;
                this.rightBufferRemainingCapacity += transmittingPacket.packet.getSize();
                currentlyTransmittingPackets.add(transmittingPacket);
                bitsAddedToLink += transmittingPacket.packet.getSize();
                transmittingPacket.transmissionStartTime = Main.currentTime;
            } else {
                //Don't move
                transmitPackets = false;
            }
        }

        // Want rates per second
        sumBufferCapacity += linkBuffer - leftBufferRemainingCapacity;
        sumTotalBitsTransmitted += totalBitsTransmitted;
        linkAnalyticsCollector.addToPacketLoss(packetDrops, Main.currentTime);
        packetDrops = 0;
        // Want link rates in Mbps
        if (Main.currentTime % 100 == 0) {
             linkAnalyticsCollector.addToBuffer(sumBufferCapacity / (100 / Main.intervalTime)
                    / ((double) Main.intervalTime), Main.currentTime);
             // Convert from bits / s to Mbps -> divide by 1048.57
             linkAnalyticsCollector.addToLinkRates(sumTotalBitsTransmitted / (100 / Main.intervalTime)
                            * Main.intervalTime / 1048.576, Main.currentTime);
             sumBufferCapacity = 0;
             sumTotalBitsTransmitted = 0;
        }
    }

    public ArrayList<XYSeries> getDatasets() {
        return linkAnalyticsCollector.getDatasets();
    }
}