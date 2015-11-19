package com.ricketts;

/**
 * Created by chinmay on 11/16/15.
 */
public class ACKPacket extends Packet {
    private static final Integer ACKPacketSize = 64;

    public ACKPacket(Integer packetID, Host source, Host destination) {
        super(packetID, ACKPacketSize, source, destination);
    }
}