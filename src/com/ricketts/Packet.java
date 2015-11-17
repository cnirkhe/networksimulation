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

    private Integer sendTime;

    public Packet(Integer packetSize, Flow parentFlow, Integer packedId)
    {
        this(packetSize, parentFlow.getFlowSource(), parentFlow.getFlowDestination(), packedId);
    }

    public Packet(Integer packetSize, Node sourceNode, Node destinationNode, Integer packetId)
    {
        this.packetSize = packetSize;
        this.sourceNode = sourceNode;
        this.destinationNode = destinationNode;
        this.packetId = packetId;
    }

    public Integer getPacketId() {return packetId; }
    public Integer getPacketSize() { return packetSize; }

    public Node getSourceNode() {return sourceNode;}
    public Node getDestinationNode() {return destinationNode; }

    public Integer getSendTime() {return sendTime;}
    public void setSendTime(Integer sendTime) {this.sendTime = sendTime;}

}