package com.ricketts;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Created by chinmay on 11/16/15.
 */
public class Flow
{
    private Integer flowId;
    private Node flowSource;
    private Node flowDestination;
    /**
     * Measured in MB
     */
    private Integer dataSize;
    /**
     * Measured in seconds
     */
    private Double startTime;

    public Flow(Integer flowId, Node flowSource, Node flowDestination, Integer dataSize, Double startTime)
    {
        this.flowId = flowId;
        this.flowSource = flowSource;
        this.flowDestination = flowDestination;
        this.dataSize = dataSize;
        this.startTime = startTime;
    }

    public Node getFlowSource() {return flowSource;}

    public LinkedList<DataPacket> generateDataPackets( Integer initalIndex )
    {
        Integer numbPackets = dataSize * 1024; //Convert to size of packets
        LinkedList<DataPacket> packets = new LinkedList<>();

        for(Integer i = initalIndex; i < numbPackets + initalIndex; i++)
        {
            DataPacket packet_i = new DataPacket(this, i);
            packets.add(packet_i);
        }
        return packets;
    }
}
