package com.ricketts;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Created by chinmay on 11/16/15.
 */
public class Flow {
    // private information
    private static final Integer dataPacketSize = 1024;

    // private variables
    private Integer id;
    private Integer flowId;
    private Node flowSource;
    private Node flowDestination;
    private Integer dataSize;       // dataSize in bytes
    private Integer startTime;      // start time in milliseconds

    // constructor
    public Flow(Integer id, Integer flowId, Node flowSource,
        Node flowDestination, Integer dataSize, Integer startTime) {
        this.id = id;
        this.flowId = flowId;
        this.flowSource = flowSource;
        this.flowDestination = flowDestination;
        this.dataSize = dataSize;
        this.startTime = startTime;
    }

    // accessor methods
    public Node getFlowSource() { return this.flowSource; }
    public Node getFlowDestination() { return this.flowDestination; }
    public Integer getId() { return id; }

    // public methods below

    public LinkedList<DataPacket> generateDataPackets(Integer initID) {
        LinkedList<DataPacket> dataPackets = new LinkedList<DataPacket>();

        Integer dataToPacketize = this.dataSize;
        Integer packetID = initID;
        while (dataToPacketize - dataPacketSize > 0)
        {
            DataPacket newPacket =
                new DataPacket(packetID, dataPacketSize, this);
            dataPackets.add(newPacket);
            dataToPacketize -= dataPacketSize;
            packetID++;
        }
        if (dataToPacketize > 0)
        {
            DataPacket lastNewPacket =
                new DataPacket(packetID, dataPacketSize, this);
            dataPackets.add(lastNewPacket);
        }
        return dataPackets;
    }
}
