package com.ricketts;

/**
 * An instance of an ACKnowledgement packet.
 * It has a fixed size of 64 bytes.
 */
public class ACKPacket extends Packet {
    /**
     * ACK packet size is set to 64 bytes or 8 * 64 bits (everything is measured in bits inside the program)
     */
    public static final Integer ACKPacketSize = 8 * 64;

    /**
     * Construct an ACKPacket from packetID, a source Host and a destination Host
     * @param packetID ID to create the new ACK packet with
     * @param source source Host
     * @param destination destination Host
     */
    public ACKPacket(Integer packetID, Host source, Host destination) {
        super(packetID, ACKPacketSize, source, destination);
    }
}