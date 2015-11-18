package com.ricketts;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Queue;

/**
 * Created by chinmay on 11/16/15.
 */
public class Host extends Node
{
    // private information
    private final static Integer initWindowSize = 50;
    // active flows
    private class ActiveFlows
    {
        public static Integer totalSentPackets = 0;

        public Flow flow;
        public Queue<Packet> packets;
        public Integer numSentPackets;

        public ActiveFlows(Host host, Flow flow)
        {
            this.flow = flow;
            this.packet = flow.generateDataPackets(host.totalGenPackets);
            host.totalGenPackets += packets.size();
            this.numSentPackets = 0;
        }
    }
    // sent packets
    private class SentPacket
    {
        public Packet packet;
        public Integer sendTime;    // sendTime in milliseconds

        public SentPacket(Packet packet, Integer sendTime)
        {
            this(packet, sendTime);
        }
    }

    // private variables
    private final Integer address;
    private Link link;
    private Integer windowSize;
    private Integer totalGenPackets;
    private ArrayList<ActiveFlows> flows;
    private Queue<SentPacket> sentPackets;

    // constructors
    public Host(int address, Link link)
    {
        this(address,
            link,
            initWindowSize,
            0,
            new HashMap<Flow, Queue<Packet> >(),
            new Queue<SentPacket>());
    }

    public Host(int address)
    {
        this(address,
            null,
            initWindowSize,
            0,
            new HashMap<Flow, Queue<Packet> >(),
            new Queue<SentPacket>());
    }

    // Accessor Methods
    public int getAddress()
    {
        return this.address;
    }
    public Link getLink()
    {
        return this.link;
    }

    // public methods below

    public void setLink(Link link)
    {
        this.link = link;
    }
    public void addFlow(Flow flow)
    {
        this.flows.add(new ActiveFlows(this, flow));
    }


    // below in process of being updated


    // receive a packet and return an ACK packet
    public void receivePacket(Packet packet)
    {
        if(packet instanceof ACKPacket)
        {
            if(maxACKReceived < packet.getPacketId())
            {
                maxACKReceived = packet.getPacketId();
            }
            System.out.println("ACK packet " + packet.getPacketId() +
                " received at host " + address);
        }
        else if(packet instanceof DataPacket)
        {
            System.out.println("Data packet " + packet.getPacketId() + " received at host " + address);
            //TODO Add analytics here

            /**
             * Send an ACKPacket back
             */
            ACKPacket ackPacket = new ACKPacket(packet.getDestinationNode(), packet.getSourceNode(), numbGeneratedPackets);
            numbGeneratedPackets++;

            packetsToSend.add(ackPacket);
        }
    }

    public void update(Integer intervalTime, Integer overallTime)
    {
        /**
         * If there are flows that have not started (i.e. flows isn't empty)
         * Then add the packets for them to the queue
         */
        while(!flows.isEmpty())
        {
            Flow flow = flows.remove();
            Collection<DataPacket> packetsToAdd = flow.generateDataPackets(numbGeneratedPackets);
            numbGeneratedPackets += packetsToAdd.size();
            packetsToSend.addAll(packetsToAdd);
        }

        /**
         * Now we send packets if the packet id n_t satisfies: n_t < n_a (maxACKReceived) + w_t
         * Then we send it along to the link to handle
         */
        while(!packetsToSend.isEmpty() && packetsToSend.peek().getPacketId() <= maxACKReceived + windowSize)
        {
            Packet packet = packetsToSend.peek();
            System.out.println("About to send " + (packet instanceof ACKPacket ? "ACK" : "Data") + " packet " + packet.getPacketId());
            link.addPacket(packet, this);
            packetsToSend.remove();
        }

    }
}