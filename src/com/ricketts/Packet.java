package com.ricketts;

/**
 * Created by chinmay on 11/16/15.
 */
public abstract class Packet
{
    private final Integer packetId;
    private final Integer packetSize;   // packet size in bytes
    private final Node sourceNode;
    private final Node destinationNode;

    public Packet(Integer packedId, Integer packetSize, Flow parentFlow)
    {
        this(packedId,
            packetSize,
            parentFlow.getFlowSource(),
            parentFlow.getFlowDestination());
    }

    public Packet(
        Integer packetId,
        Integer packetSize,
        Node sourceNode,
        Node destinationNode)
    {
        this(packetId,
            packetSize,
            sourceNode,
            destinationNode);
    }

    public Integer getPacketId()
    {
        return packetId;
    }
    public Integer getPacketSize()
    {
        return packetSize;
    }
    public Node getSourceNode()
    {
        return sourceNode;
    }
    public Node getDestinationNode()
    {
        return destinationNode;
    }
}