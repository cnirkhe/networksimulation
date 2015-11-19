package com.ricketts;

public class SetupPacket extends Packet {
    private static final Integer SetupPacketSize = 64;

    public Integer minPacketID;
    public Integer maxPacketID;

    public SetupPacket(Integer packetID, Host source, Host destination,
        Integer minPacketID, Integer maxPacketID) {
        super(packetID, SetupPacketSize, source, destination);
        this.minPacketID = minPacketID;
        this.maxPacketID = maxPacketID;
    }
}