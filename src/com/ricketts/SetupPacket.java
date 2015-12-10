package com.ricketts;

/**
 * SetupPacket: packet sent first to tell the destination to expect packets.
 */
public class SetupPacket extends Packet {
    /**
     * Setup packet is 64 bytes.
     */
    private static final Integer SetupPacketSize = 8 * 64;

    /**
     * Index of last packet to arrive--tells us how many packets we're supposed to get.
     */
    private Integer maxPacketID;

    public Integer getMaxPacketID() { return this.maxPacketID; }

    /**
     * Create a setup packet
     * @param packetID ID
     * @param source Where the packet is coming from
     * @param destination Where the packet is going to
     * @param maxPacketID Number of packets that will be arriving
     */
    public SetupPacket(Integer packetID, Host source, Host destination, Integer maxPacketID) {
        super(packetID, SetupPacketSize, source, destination);
        this.maxPacketID = maxPacketID;
    }
}