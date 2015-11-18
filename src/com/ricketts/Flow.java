package com.ricketts;

import java.util.Collection;
import java.util.Queue;

/**
 * Created by chinmay on 11/16/15.
 */
public class Flow
{
    // private information
    private static final Integer dataPacketSize = 1024

    // private variables
    private Integer flowId;
    private Node flowSource;
    private Node flowDestination;
    private Integer dataSize;       // dataSize in bytes
    private Integer startTime;      // start time in milliseconds

    // constructor
    public Flow(
        Integer flowId,
        Node flowSource,
        Node flowDestination,
        Integer dataSize,
        Double startTime)
    {
        this(flowId,
            flowSource,
            flowDestination,
            dataSize,
            startTime);
    }

    // accessor methods
    public Node getFlowSource()
    {
        return this.flowSource;
    }
    public Node getFlowDestination()
    {
        return this.flowDestination;
    }

    // public methods below

    public Queue<DataPacket> generateDataPackets(Integer initID)
    {
        Queue<DataPacket> dataPackets = new Queue<DataPacket>();

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
