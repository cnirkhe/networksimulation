package com.ricketts;

/**
 * Created by chinmay on 11/16/15.
 */
public class DataPacket extends Packet
{
    public DataPacket(Flow parentFlow, Integer packetId)
    {
        super(1024, parentFlow, packetId);
    }
}