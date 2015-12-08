package com.ricketts;

import java.util.LinkedList;

/**
 * Flows are used to describe a desire to move Data from one Host to another.
 * They describe how much data is going to be moved, and also provide the logistics for
 * generating the necessary packets.
 */
public class Flow {

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

    public Flow(Integer id, Host source, Host destination, Integer dataSize, Integer startTime,
                String name, int protocol) {
        this.id = id;
        this.source = source;
        this.destination = destination;
        this.dataSize = dataSize;
        this.startTime = startTime;
        this.flowAnalyticsCollector = new FlowAnalyticsCollector(this.id, name);
        this.protocol = protocol;
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
    public LinkedList<DataPacket> generateDataPackets(Integer initID) {
        Integer dataPacketSize = DataPacket.DataPacketSize;
        LinkedList<DataPacket> dataPackets = new LinkedList<>();

        //We have to take the intValue or we're manipulating that data point itself (Objects not Primitives)
        Integer dataToPacketSize = this.dataSize.intValue();
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

    public void generateFlowGraphs() {
        flowAnalyticsCollector.generateFlowGraphs();
    }
}
