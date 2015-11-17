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
    private final Integer packetId;
    private final Node sourceNode;
    private final Node destinationNode;

    public Packet(Integer packetSize, Flow parentFlow, Integer packedId)
    {
        this(packetSize,
            packedId,
            parentFlow.getFlowSource(),
            parentFlow.getFlowDestination());
    }

    public Packet(
        Integer packetSize,
        Node sourceNode,
        Node destinationNode,
        Integer packetId)
    {
        this(packetSize,
            sourceNode,
            destinationNode,
            packetId);
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