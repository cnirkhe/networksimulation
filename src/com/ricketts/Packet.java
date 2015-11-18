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
    private final Integer flowId;

    public Packet(Integer packedId, Integer packetSize, Flow parentFlow)
    {
        this(packedId,
            packetSize,
            parentFlow.getFlowSource(),
            parentFlow.getFlowDestination(),
            null); // flow id
    }

    public Packet(
        Integer packetId,
        Integer packetSize,
        Node sourceNode,
        Node destinationNode,
        Integer flowId)
    {
        this(packetId,
            packetSize,
            sourceNode,
            destinationNode,
            null);
    }

    // Fix this, not sure what it's supposed to look like since the other constructors don't work either
    public Packet(Integer packetId, Integer packetSize, Integer flowId) {
        this(packetId,
                packetSize,
                flowId);
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
    public Integer getFlowId() { return flowId; }
}