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
    private Host source;
    private Host destination;
    private Integer dataSize;       // dataSize in bits
    private Integer startTime;      // start time in milliseconds

    // constructor
    public Flow(Integer id, Host source, Host destination, Integer dataSize,
        Integer startTime) {
        this.id = id;
        this.source = source;
        this.destination = destination;
        this.dataSize = dataSize;
        this.startTime = startTime;
    }

    // accessor methods
    public Host getSource() { return this.source; }
    public Host getDestination() { return this.destination; }
    public Integer getID() { return id; }

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
