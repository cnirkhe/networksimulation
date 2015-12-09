package com.ricketts;

import org.jfree.data.xy.XYSeries;

import java.util.*;

/**
 * Flows are used to describe a desire to move Data from one Host to another.
 * They describe how much data is going to be moved, and also provide the logistics for
 * generating the necessary packets.
 */
public class Flow {

    public final static Integer initWindowSize = 1;
    public final static Integer initTimeoutLength = 1000;

    public Integer windowSize;
    public Integer timeoutLength;
    // In Reno CA phase, every ACK increases cwnd by 1/cwnd. We're keeping
    // track of these partial windows added and then adding 1 to windowSize once
    // once partialWindowSize == windowSize.
    public Integer partialWindowSize;
    public Integer maxPacketID;
    // Indicates whether or not we're in the slow start phase.
    public boolean slowStart;
    // Indicates whether or not we're waiting for a retransmit.
    public boolean awaitingRetransmit;
    // Slow start threshhold
    public int ssthresh;
    /**
     * Monotonically increasing count of last ACK received
     */
    public Integer numberOfLatestACKIDRecieved;
    public ArrayList<DataPacket> packets;
    public int mostRecentRetransmittedPacket;
    public int mostRecentQueued;
    public int windowOccupied;
    /**
     * A Hashmap of PacketID to the sendTime of that packet (in milliseconds)
     * Used to keep track of dropped packets
     */
    public HashMap<Integer, Integer> sendTimes;

    public Integer firstNotRecievedPacketIndex;

    /**
     * A set of information on round trip times of packets
     */
    public Integer minRoundTripTime;
    public Double avgRoundTripTime;
    public Double stdDevRoundTripTime;

    /**
     * Bits sent within this update session
     */
    public Integer currBitsSent;


    private Integer id;
    private Host source;
    private Host destination;
    public FlowAnalyticsCollector flowAnalyticsCollector;
    /**
     * dataSize is measured in bits. Total data sent over all packets.
     */
    private Integer dataSize;

    private int protocol;

    /**
     * Measured in milliseconds. Denotes when relative to the global time this flow should initiate.
     */
    private Integer startTime;

    public boolean activated;

    public Flow(Integer id, Host source, Host destination, Integer dataSize, Integer startTime,
                String name, int protocol) {
        this.id = id;
        this.source = source;
        this.destination = destination;
        this.dataSize = dataSize;
        this.startTime = startTime;
        this.flowAnalyticsCollector = new FlowAnalyticsCollector(this.id, name);
        this.protocol = protocol;

        activated = false;
    }

    public void activateFlow() {
        this.activated = true;
        this.windowSize = initWindowSize;
        this.timeoutLength = initTimeoutLength;
        this.packets = generateDataPackets(0);
        this.maxPacketID = packets.size() - 1;
        this.numberOfLatestACKIDRecieved = 0;
        this.sendTimes = new HashMap<>();
        this.minRoundTripTime = Integer.MAX_VALUE;
        this.avgRoundTripTime = null;
        this.stdDevRoundTripTime = null;
        this.currBitsSent = 0;
        this.partialWindowSize = 0;
        this.slowStart = true;
        this.awaitingRetransmit = false;
        this.ssthresh = Integer.MAX_VALUE;
        this.mostRecentRetransmittedPacket = 0;
        this.mostRecentQueued = -1;
        this.windowOccupied = 0;
        this.firstNotRecievedPacketIndex = 0;
    }

    public Host getSource() { return this.source; }
    public Host getDestination() { return this.destination; }
    public Integer getID() { return this.id; }
    public Integer getStartTime() {
        return startTime;
    }

    /**
     * This method generates a LinkedList of DataPackets corresponding to the size of data of the flow.
     * The packets are produced with sequential id numbers starting with the initialID number.
     * @param initID The first ID number for the corresponding sequence of DataPackets
     * @return LinkedList of all the data packets in sequential order.
     */
    private ArrayList<DataPacket> generateDataPackets(Integer initID) {
        Integer dataPacketSize = DataPacket.DataPacketSize;
        //We have to take the intValue or we're manipulating that data point itself (Objects not Primitives)
        Integer dataToPacketSize = this.dataSize.intValue();

        ArrayList<DataPacket> dataPackets = new ArrayList<>(dataToPacketSize / dataPacketSize + 1);
        Integer packetID = initID;
        while (dataToPacketSize - dataPacketSize > 0) {
            DataPacket newPacket = new DataPacket(packetID, this);
            dataPackets.add(newPacket);
            dataToPacketSize -= dataPacketSize;
            packetID++;
        }

        //In the case that the data cannot be evenly placed in all the packets
        //We need to send an extra packet
        //Note: Due to the parameters of the project, this should never be necessary but is good coding practice
        if (dataToPacketSize > 0) {
            DataPacket lastNewPacket = new DataPacket(packetID, this);
            dataPackets.add(lastNewPacket);
        }
        
        return dataPackets;
    }

    public ArrayList<XYSeries> getDatasets() {
        return flowAnalyticsCollector.getDatasets();
    }
}
