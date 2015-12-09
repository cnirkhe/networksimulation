package com.ricketts;

/**
 * An instance of an ACKnowledgement packet.
 * It has a fixed size of 64 bytes.
 */
public class ACKPacket extends Packet {
    public static final Integer ACKPacketSize = 8 * 64;

    public ACKPacket(Integer packetID, Host source, Host destination) {
        super(packetID, ACKPacketSize, source, destination);
    }
}