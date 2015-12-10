package com.ricketts;

/**
 * An instance of a Data packet.
 * It contains no actual Data Itself. Size is simply virtually represented as an integral size value (in bits)
 * It has a fixed size of 1024 bytes.
 */
public class DataPacket extends Packet {
    /**
     * Data packets have sizes of 1024 bytes or 8 * 1024 bits (everything is measured in bits within the program)
     */
    public static final Integer DataPacketSize = 8 * 1024;

    /**
     * Construct a DataPacket from packetID and a flow that the new DataPacket belongs to
     * @param packetID ID of new DataPacket
     * @param parentFlow flow that the new DataPacket belongs to
     */
    public DataPacket(Integer packetID, Flow parentFlow) {
        super(packetID, DataPacketSize, parentFlow);
    }
}