package com.ricketts;

/**
 * Created by chinmay on 11/16/15.
 */
public class DataPacket extends Packet {
    public DataPacket(Integer packetId, Integer datasize, Flow parentFlow) {
        super(packetId, datasize, parentFlow);
    }
}