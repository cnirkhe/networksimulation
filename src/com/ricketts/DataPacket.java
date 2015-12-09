package com.ricketts;

/**
 * An instance of a Data packet.
 * It contains no actual Data Itself.
 * It has a fixed size of 1024 bytes.
 */
public class DataPacket extends Packet {
    public static final Integer DataPacketSize = 8 * 1024;

    public DataPacket(Integer packetID, Flow parentFlow) {
        super(packetID, DataPacketSize, parentFlow);
    }
}