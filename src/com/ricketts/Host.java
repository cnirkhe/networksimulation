package com.ricketts;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by chinmay on 11/16/15.
 */
public class Host extends Node
{
    private Integer address;
    private Link link;

    private Queue<Flow> flowsToStart;
    private Queue<Packet> packetsToSend;
    private Queue<ACKPacket> ackPacketsToProcess;

    private Integer windowSize;

    private Integer numbGeneratedPackets;
    private Integer maxACKReceived;

    public Host(int address, Link link)
    {
        this.address = address;
        this.link = link;
        flowsToStart = new LinkedList<>();
        packetsToSend = new LinkedList<>();
        ackPacketsToProcess = new LinkedList<>();
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

    public void update()
    {
        /**
         * If there are flows that have not started (i.e. flowsToStart isn't empty)
         * Then add the packets for them to the queue
         */
        if(!flowsToStart.isEmpty())
        {
            Flow flow = flowsToStart.remove();
            Collection<DataPacket> packetsToAdd = flow.generateDataPackets(numbGeneratedPackets);
            numbGeneratedPackets += packetsToAdd.size();
            packetsToSend.addAll(packetsToAdd);
        }

        /**
         * If there are acknowledgments received then they are processed
         */
        while(!ackPacketsToProcess.isEmpty())
        {
            ACKPacket ackPacket = ackPacketsToProcess.remove();
            if(maxACKReceived < ackPacket.getPackedId())
            {
                maxACKReceived = ackPacket.getPackedId();
            }
        }

        /**
         * Now we send packets if the packet id satisfies: n_t < n_a + w_t
         * Then we send it along to the link to handle
         */
        while( packetsToSend.peek().getPackedId() < maxACKReceived + windowSize)
        {
            //TODO Send the packet
            System.out.println("About to send packet " + packetsToSend.peek().getPackedId());
            link.addPacket(packetsToSend.remove());
        }

    }
}
