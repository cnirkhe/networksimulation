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
    private final Integer linkBufferSize;

    private Node leftNode, rightNode;

    /**
     * Used to calculate average buffer delay
     */
    private Integer numbLeftPktsThruBuffer, numbRightPktsThruBuffer;

    /**
     * Used to calculate average buffer delay
     */
    private Double sumLeftBufferTime, sumRightBufferTime;

    private Double latestLeftBufferDelayEstimate, latestRightBufferDelayEstimate;

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
     * Packet currently being transmitted ordered by time entered in transmission
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
    /**
     * Total buffer capacity over the interval that buffer capacity is being averaged over.
     */
    private Integer sumBufferCapacity;

    /**
     * initializes the link as empty and initializes all the analytics variables
     * @param linkID linkID
     * @param linkRate linkRate
     * @param linkDelay linkDelay
     * @param linkBufferSize linkBufferSize
     * @param graph whether to graph or not
     */
    public Link(Integer linkID, Integer linkRate, Integer linkDelay, Integer linkBufferSize, boolean graph) {
        this.linkID = linkID;
        this.linkRate = linkRate;
        this.linkDelay = linkDelay;
        this.linkBufferSize = linkBufferSize;

        this.leftPacketBuffer = new LinkedList<>();
        this.rightPacketBuffer = new LinkedList<>();
        this.leftBufferRemainingCapacity = linkBufferSize;
        this.rightBufferRemainingCapacity = linkBufferSize;
        this.packetDrops = 0;
        this.totalBitsTransmitted = 0;
        this.linkAnalyticsCollector = new LinkAnalyticsCollector(linkID);
        this.sumBufferCapacity = 0;
        this.sumTotalBitsTransmitted = 0;
        this.graph = graph;

        initializeBufferDelayEstimate();
        latestLeftBufferDelayEstimate = 0.0;
        latestRightBufferDelayEstimate = 0.0;

        currentlyTransmittingPackets = new LinkedList<>();
    }

    /**
     * Initializes the variables for buffer delay estimation
     */
    private void initializeBufferDelayEstimate() {
        numbLeftPktsThruBuffer = 0;
        numbRightPktsThruBuffer = 0;
        sumLeftBufferTime = 0.0;
        sumRightBufferTime = 0.0;
    }

    public Integer getID() { return this.linkID; }
    public Node getLeftNode() { return this.leftNode; }
    public Node getRightNode() { return this.rightNode; }
    public Integer getLinkDelay() { return this.linkDelay; }
    public void setLeftNode(Node node) { this.leftNode = node; }
    public void setRightNode(Node node) { this.rightNode = node; }

    /**
     * Returns the buffer delay estimate for the given direction
     * @param direction
     * @return
     */
    private Double getBufferDelay(Direction direction) {
        if(direction == Direction.LEFT) {
            return latestLeftBufferDelayEstimate;
        }
        else {
            return latestRightBufferDelayEstimate;
        }
    }

    /**
     * If the node is connecting, gives the delay on its side of the link
     * @param node node adjacent to link
     * @return
     */
    private Double getBufferDelay(Node node) {
        if(node == leftNode) {
            return getBufferDelay(Direction.LEFT);
        } else if (node == rightNode) {
            return getBufferDelay(Direction.RIGHT);
        } else
            return 0.0;
    }

    /**
     * Sum of buffer and link delay
     * @param node tells the side that its coming from
     * @return
     */
    public Double getDelay(Node node) {
        return getLinkDelay() + getBufferDelay(node);
    }

    /**
     * Returns the other end of the link if this node is one of them
     * @param oneEnd
     * @return the other end
     */
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
        packetDrops++;
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
            leftBufferRemainingCapacity = linkBufferSize;
        }
        else if (sendingNode == rightNode) {
            rightPacketBuffer.clear();
            rightBufferRemainingCapacity = linkBufferSize;
        }
        else {
            System.out.println("Something went terribly wrong");
        }
    }

    /**
     * Updates the state of the Link. This involves any of the following:
     * Calculate the buffer delay (periodically)
     * Updating the state of packets currently in transmission
     * Removing transmitted packets
     * Adding packets to the link from the link buffer
     */
    public void update() {

        //Buffer Estimate
        if(Main.currentTime % 1000 == 900) {

            if(numbLeftPktsThruBuffer == 0)
                latestLeftBufferDelayEstimate = 0.0;
            else
                latestLeftBufferDelayEstimate = sumLeftBufferTime / numbLeftPktsThruBuffer;

            if(numbRightPktsThruBuffer == 0)
                latestRightBufferDelayEstimate = 0.0;
            else
                latestRightBufferDelayEstimate = sumRightBufferTime / numbRightPktsThruBuffer;

            initializeBufferDelayEstimate();
        }

        // Reset total bits transmitted for the current interval
        totalBitsTransmitted = 0;

        /*
         * If we have packets that are in transmission but should have reached the other end by now,
         * send them along their way
         */
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

        /*
         * Calculate how many bits can be added to the link in this interval and then add them accordingly
         */
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
        sumBufferCapacity += linkBufferSize - leftBufferRemainingCapacity;
        sumTotalBitsTransmitted += totalBitsTransmitted;
        linkAnalyticsCollector.addToPacketLoss(packetDrops, Main.currentTime);
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
        //packetDrops = 0;
    }

    public ArrayList<XYSeries> getDatasets() {
        return linkAnalyticsCollector.getDatasets();
    }
}