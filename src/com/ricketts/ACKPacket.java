package com.ricketts;

/**
 * Created by chinmay on 11/16/15.
 */
public class ACKPacket extends Packet {
    public ACKPacket(Flow parentFlow, Integer packetId) {
        super(64, parentFlow, packetId);
    }
}