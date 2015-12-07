package com.ricketts;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Link is unlike a physical Link. Instead think of a Link as the physical link plus the buffers on either end.
 * A link is defined as LEFT to RIGHT (the naming is arbitrary) but packets are sent in 1 direction at a time.
 */
public class Link implements Updatable {

    /**
     * This is the number of ms before the buffer delay is reestimated
     */
    private static final Integer ESTIMATE_BUFFER_DELAY_PERIOD = 200;

    /**
     * The time left in the period before reestimation
     */
    private Integer timeLeftInEstimationPeriod;

    /**
     * Orientations for Packets flowing on the link
     */
    private enum Direction {LEFT, RIGHT}

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

    /**
     * Packet drops for current interval
     */
    private AtomicInteger packetDrops;
    private LinkAnalyticsCollector linkAnalyticsCollector;

    /**
     * This is a count of the number of packets that passed through the buffer since the last time
     * that the buffer delay was estimated. This number is used in said evaluation.
     * In addition, it is refreshed after the estimation is finished.
     */
    private Integer numbLeftPacketsThroughBuffer, numbRightPacketsThroughBuffer;

    /**
     * This is a sum of the number of ms that packets have spent in the buffer before being sent along
     * the link since the last time that the buffer delay was estimated. This number is used in said evaluation.
     * In addition, it is refreshed after the estimation is finished.
     */
    private Integer sumLeftBufferTime, sumRightBufferTime;

    /**
     * The current best estimate at BufferDelay in ms.
     */
    private Double leftEstimatedBufferDelay, rightEstimatedBufferDelay;

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
    private TransmittingPacket currentlyTransmittingPacket;
    /**
     * How much of the packet has been transmitted
     */
    private Integer bitsTransmitted;

    /**
     * How many bits have been transmitted in the total period
     */
    private AtomicInteger totalBitsTransmitted;

    /**
     * Complete Constructor
     * @param linkID
     * @param linkRate
     * @param linkDelay
     * @param linkBuffer
     * @param leftNode
     * @param rightNode
     * @param name
     */
    public Link(Integer linkID, Integer linkRate, Integer linkDelay,
        Integer linkBuffer, Node leftNode, Node rightNode, String name) {
        this.linkID = linkID;
        this.linkRate = linkRate;
        this.linkDelay = linkDelay;
        this.linkBuffer = linkBuffer;
        this.leftNode = leftNode;
        this.rightNode = rightNode;

        this.leftPacketBuffer = new LinkedList<>();
        this.rightPacketBuffer = new LinkedList<>();
        this.leftBufferRemainingCapacity = linkBuffer;
        this.rightBufferRemainingCapacity = linkBuffer;
        this.packetDrops = new AtomicInteger(0);
        this.totalBitsTransmitted = new AtomicInteger(0);
        this.linkAnalyticsCollector = new LinkAnalyticsCollector(linkID, name);

        //Initializations for estimating Buffer Delay
        numbLeftPacketsThroughBuffer = 0;
        numbRightPacketsThroughBuffer = 0;
        sumLeftBufferTime = 0;
        sumRightBufferTime = 0;
        timeLeftInEstimationPeriod = ESTIMATE_BUFFER_DELAY_PERIOD;

        leftEstimatedBufferDelay = 0.0;
        rightEstimatedBufferDelay = 0.0;
    }

    /**
     * Constructor without nodes defined
     * @param linkID
     * @param linkRate
     * @param linkDelay
     * @param linkBuffer
     * @param name
     */
    public Link(Integer linkID, Integer linkRate, Integer linkDelay,
            Integer linkBuffer, String name) {
        this(linkID,linkRate, linkDelay, linkBuffer, null, null, name);
    }

    public Integer getID() { return this.linkID; }
    public Node getLeftNode() { return this.leftNode; }
    public Node getRightNode() { return this.rightNode; }
    public Integer getLinkDelay() { return this.linkDelay; }
    public void setLeftNode(Node node) { this.leftNode = node; }
    public void setRightNode(Node node) { this.rightNode = node; }


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

        // We dropped this packet
        packetDrops.incrementAndGet();
        return false;
    }

    /**
     * Updates the state of the Link. This involves any of the following:
     * (a) Updating the state of packets currently in transmission
     * (b) Removing transmitted packets
     * (c) Adding packets to the link from the link buffer
     * @param intervalTime The time step of the simulation
     * @param overallTime Overall simulation time
     */
    public void update(Integer intervalTime, Integer overallTime) {
        System.out.println("updating");
        // Reset packets drops and total bits transmitted for new interval
        packetDrops.set(0);
        totalBitsTransmitted.set(0);
        // While there's time left in the interval,,,
        Integer usageLeft = intervalTime * this.linkRate, packetBits, endOfDelay;

        while (usageLeft > 0) {
            // If there's no packet being currently transmitted, fetch one from
            // the left or right buffer. Preference is given to whichever buffer
            // has the packet at the front of its queue that's been waiting
            // longer.
            if (this.currentlyTransmittingPacket == null) {
                TransmittingPacket leftPacket = this.leftPacketBuffer.peek();
                TransmittingPacket rightPacket = this.rightPacketBuffer.peek();
                if (leftPacket == null) {
                    if (rightPacket == null) {
                        System.out.println("null shit");
                        return;
                    }
                    else {
                        this.currentlyTransmittingPacket = rightPacketBuffer.remove();
                        this.rightBufferRemainingCapacity += rightPacket.packet.getSize();
                    }
                }
                else if (rightPacket == null ||
                    leftPacket.transmissionStartTime < rightPacket.transmissionStartTime)
                {    
                    this.currentlyTransmittingPacket = leftPacketBuffer.remove();
                    this.leftBufferRemainingCapacity += leftPacket.packet.getSize();
                }
                else {
                    this.currentlyTransmittingPacket = rightPacketBuffer.remove();
                    this.rightBufferRemainingCapacity += rightPacket.packet.getSize();
                }

                this.currentlyTransmittingPacket.transmissionStartTime =
                    RunSim.getCurrentTime();
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

                    //Calculation of time spent in buffer
                    Integer timeSpent = RunSim.getCurrentTime() - currentlyTransmittingPacket.transmissionStartTime;

                    if (this.currentlyTransmittingPacket.direction == Direction.RIGHT) {
                        this.rightNode.receivePacket(this.currentlyTransmittingPacket.packet, this);
                        numbRightPacketsThroughBuffer++;
                        sumRightBufferTime += timeSpent;
                    } else {
                        this.leftNode.receivePacket(this.currentlyTransmittingPacket.packet, this);
                        numbLeftPacketsThroughBuffer++;
                        sumLeftBufferTime += timeSpent;
                    }
                    
                    // We're done transmitting this packet
                    this.totalBitsTransmitted.addAndGet(this.bitsTransmitted);
                    this.currentlyTransmittingPacket = null;
                    this.bitsTransmitted = 0;

                    // Presumably we're in the "remainder" case, and we have to
                    // figure out how long transferring the end of the packet
                    // would take
                    usageLeft -= packetBits;
                }
            }
        }
        System.out.println("done updooting");
        // Want rates per second
        linkAnalyticsCollector.addToLeftBuffer((linkBuffer - leftBufferRemainingCapacity) / ((double) intervalTime / 1000), intervalTime);
        System.out.println((linkBuffer - leftBufferRemainingCapacity));
        linkAnalyticsCollector.addToRightBuffer((linkBuffer - rightBufferRemainingCapacity) / ((double) intervalTime / 1000), intervalTime);
        linkAnalyticsCollector.addToPacketLoss(packetDrops.get(), intervalTime);
        // Want link rates in Mbps
        linkAnalyticsCollector.addToLinkRates(totalBitsTransmitted.get() * 100000 / ((double) intervalTime / 1000), intervalTime);

        //estimation of sent packets
        if (timeLeftInEstimationPeriod <= 0 ) {
            leftEstimatedBufferDelay = ((double) sumRightBufferTime) / numbLeftPacketsThroughBuffer;
            rightEstimatedBufferDelay = ((double) sumLeftBufferTime) / numbRightPacketsThroughBuffer;

            numbLeftPacketsThroughBuffer = 0;
            numbRightPacketsThroughBuffer = 0;
            sumLeftBufferTime = 0;
            sumRightBufferTime = 0;
            timeLeftInEstimationPeriod = ESTIMATE_BUFFER_DELAY_PERIOD;
        } else {
            timeLeftInEstimationPeriod -= intervalTime;
        }
    }

    public void generateLinkGraphs() {
        linkAnalyticsCollector.generateLinkGraphs();
    }

    /**
     * Calculate the estimated buffer delay in ms for given direciton
     * @param direction
     * @return in ms
     */
    public Double getEstimatedBufferDelay(Direction direction) {
        if(direction == Direction.LEFT) {
            return leftEstimatedBufferDelay;
        } else {
            return rightEstimatedBufferDelay;
        }
    }

    /**
     * Calculate the estimated buffer delay in ms eminating from the Node
     * If the node is not part of the link, return -1.0;
     * @param node
     * @return
     */
    public Double getEstimatedBufferDelay(Node node) {
        if (node == leftNode) {
            return getEstimatedBufferDelay(Direction.RIGHT);
        } else if (node == rightNode) {
            return getEstimatedBufferDelay(Direction.LEFT);
        } else {
            return -1.0;
        }
    }
}