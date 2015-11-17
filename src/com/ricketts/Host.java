package com.ricketts;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by chinmay on 11/16/15.
 */
public class Host extends Node
{
    private final Integer address;
    private Link link;

    private Queue<Flow> flowsToStart;
    private Queue<Packet> packetsToSend;
    private Queue<Packet> packetsToProcess;

    private Integer windowSize;

    private Integer numbGeneratedPackets;
    private Integer maxACKReceived;

    public Host(int address, Link link)
    {
        this.address = address;
        this.link = link;
        flowsToStart = new LinkedList<>();
        packetsToSend = new LinkedList<>();
        packetsToProcess = new LinkedList<>();
        windowSize = 500; //TODO Make this Dynamic with Congestion Control

        numbGeneratedPackets = 0;
        maxACKReceived = 0;

    }

    public Host(int address)
    {
        this(address, null);
    }

    /**
     * Accessor Methods
     */
    public int getAddress() { return address; }
    public Link getLink() { return link; }
    public void setLink(Link link) { this.link = link; }

    public void addFlow(Flow flow)
    {
        flowsToStart.add(flow);
    }

    public void receivePacket(Packet packet)
    {
        packetsToProcess.add(packet);
    }

    public void update(Integer intervalTime, Integer overallTime)
    {
        /**
         * If there are flows that have not started (i.e. flowsToStart isn't empty)
         * Then add the packets for them to the queue
         */
        while(!flowsToStart.isEmpty())
        {
            Flow flow = flowsToStart.remove();
            Collection<DataPacket> packetsToAdd = flow.generateDataPackets(numbGeneratedPackets);
            numbGeneratedPackets += packetsToAdd.size();
            packetsToSend.addAll(packetsToAdd);
        }

        /**
         * Process all received packets
         * This involves checking if they are an ACK or a DataPacket
         */
        while(!packetsToProcess.isEmpty())
        {
            Packet packet = packetsToProcess.remove();

            if(packet instanceof ACKPacket)
            {
                if(maxACKReceived < packet.getPacketId())
                {
                    maxACKReceived = packet.getPacketId();
                }
                System.out.println("ACK packet " + packet.getPacketId() + " received at host " + address);
            }
            else if(packet instanceof DataPacket)
            {
                System.out.println("Data packet " + packet.getPacketId() + " received at host " + address);
                //TODO Add analytics here

                ACKPacket ackPacket = new ACKPacket(packet.getDestinationNode(), packet.getSourceNode(), numbGeneratedPackets);
                numbGeneratedPackets++;

                packetsToSend.add(ackPacket);
            }

        }

        /**
         * Now we send packets if the packet id n_t satisfies: n_t < n_a (maxACKReceived) + w_t
         * Then we send it along to the link to handle
         */
        while(!packetsToSend.isEmpty() && packetsToSend.peek().getPacketId() <= maxACKReceived + windowSize)
        {
            System.out.println("About to send packet " + packetsToSend.peek().getPacketId());
            link.addPacket(packetsToSend.remove());
        }

    }
}