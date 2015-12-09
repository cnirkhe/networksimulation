package com.ricketts;

public class SetupPacket extends Packet {
    private static final Integer SetupPacketSize = 8 * 64;

    private Integer maxPacketID;

    public Integer getMaxPacketID() { return this.maxPacketID; }

    public SetupPacket(Integer packetID, Host source, Host destination, Integer maxPacketID) {
        super(packetID, SetupPacketSize, source, destination);
        this.maxPacketID = maxPacketID;
    }
}