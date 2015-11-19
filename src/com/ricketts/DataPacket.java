package com.ricketts;

/**
 * Created by chinmay on 11/16/15.
 */
public class DataPacket extends Packet {
    private static final Integer DataPacketSize = 1024;

    public DataPacket(Integer packetID, Integer datasize, Flow parentFlow) {
        super(packetID, datasize, parentFlow);
    }
}