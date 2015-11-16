package com.ricketts;

/**
 * Created by chinmay on 11/16/15.
 */
public abstract class Packet
{
    /**
     * Measured in bytes
     */
    private final Integer packetSize;
    /**
     * Which Flow it belongs to
     */
    private final Flow parentFlow;
    private final Integer packedId;

    public Packet(Integer packetSize, Flow parentFlow, Integer packedId)
    {
        this.packetSize = packetSize;
        this.parentFlow = parentFlow;
        this.packedId = packedId;
    }

    public Integer getPackedId() {return packedId; }
    public Integer getPacketSize() { return packetSize; }

}