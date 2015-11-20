package com.ricketts;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.ListIterator;

/**
 * Created by chinmay on 11/16/15.
 */
public class Host extends Node {
    // Host properties
    private final static Integer initWindowSize = 50;
    private final static Integer timeoutLength = 3000;
    
    // Host address
    private final Integer address;
    // Attached link
    private Link link;
    // Total packets generated
    private Integer totalGenPackets;
    // Packets to send
    private LinkedList<Packet> packetsToSend;
    // Flows indexed by destination host
    private HashMap<Host, LinkedList<ActiveFlow>> flowsByDestination;
    // Downloads indexed by source host
    private HashMap<Host, LinkedList<Download>> downloadsBySource;

    /*
     * Data class for currently active flows.
     */
    private class ActiveFlow {
        // Flow
        public Flow flow;
        // Window size
        public Integer windowSize;
        // ID of the last packet
        public Integer maxPacketID;
        // Number of sequential identical ACKs received
        public Integer lastACKCount;
        // Packets left to send in the flow
        public LinkedList<DataPacket> packets;
        // Send times indexed by packet ID
        public HashMap<Integer, Integer> sendTimes;

        // Constructor
        public ActiveFlow(Host host, Flow flow) {
            this.flow = flow;
            this.windowSize = initWindowSize;
            this.packets = flow.generateDataPackets(host.totalGenPackets);
            host.totalGenPackets += this.packets.size();
            this.maxPacketID = host.totalGenPackets - 1;
            this.lastACKCount = 0;
            this.sendTimes = new HashMap<>();
        }
    }

    /*
     * Data class for currently active downloads.
     */
    private class Download {
        // Last packet ID in the download
        public Integer maxPacketID;
        // Next expected packet ID in the download
        public Integer nextPacketID;

        // Constructor
        public Download(Integer minPacketID, Integer maxPacketID) {
            this.nextPacketID = minPacketID;
            this.maxPacketID = maxPacketID;
        }
    }

    /* Full constructor for Host. */
    public Host(int address, Link link, Integer totalGenPackets,
        LinkedList<Packet> packetsToSend,
        HashMap<Host, LinkedList<ActiveFlow>> flowsByDestination,
        HashMap<Host, LinkedList<Download>> downloadsBySource) {
        this.address = address;
        this.link = link;
        this.totalGenPackets = totalGenPackets;
        this.packetsToSend = packetsToSend;
        this.flowsByDestination = flowsByDestination;
        this.downloadsBySource = downloadsBySource;
    }

    /* Constructs a Host connected a Link. */
    public Host(int address, Link link) {
        this(address, link, 0, new LinkedList<Packet>(),
            new HashMap<Host, LinkedList<ActiveFlow>>(),
            new HashMap<Host, LinkedList<Download>>());
    }

    /* Constructs a Host by itself. */
    public Host(int address) {
        this(address, null, 0, new LinkedList<Packet>(),
            new HashMap<Host, LinkedList<ActiveFlow>>(),
            new HashMap<Host, LinkedList<Download>>());
    }

    /* Accessor methods. */
    public int getAddress() { return this.address; }
    public Link getLink() { return this.link; }

    /* Connects the Host to a Link. */
    public void setLink(Link link) {
        this.link = link;
    }

    /* Adds a Flow from the Host to a different one. */
    public void addFlow(Flow flow) {
        // Look for the destination host in our HashMap
        LinkedList<ActiveFlow> flows = this.flowsByDestination.get(flow.getDestination());
        // If we have it, add a new flow to the queue
        if (flows != null) {
            ActiveFlow newFlow = new ActiveFlow(this, flow);
            flows.add(newFlow);
            this.packetsToSend.add(new SetupPacket(newFlow.packets.peek().getID(), this, flow.getDestination(),
                    newFlow.maxPacketID));
        }
        // Otherwise create a queue and add the flow to it
        else {
            flows = new LinkedList<ActiveFlow>();
            ActiveFlow newFlow = new ActiveFlow(this, flow);
            flows.add(newFlow);
            this.flowsByDestination.put(flow.getDestination(), flows);
            this.packetsToSend.add(new SetupPacket(newFlow.packets.peek().getID(), this, flow.getDestination(),
                    newFlow.maxPacketID));
        }
    }

    /* Handles the reception of an ACK packet. */
    private void receiveACKPacket(ACKPacket packet) {
        // Look for the source host in our HashMap
        LinkedList<ActiveFlow> flows = this.flowsByDestination.get(packet.getSource());
        Integer packetID = packet.getID();
        // If we have it...
        if (flows != null) {
            System.out.print("ACK packet " + packet.getID() + " received at host " + address);

            // Loop through all its active flows...
            for (ActiveFlow flow : flows) {
                Integer nextPacketID = flow.packets.peek().getID();
                // If the ACK is for a new packet, we know the destination has
                // received packets at least up to that one
                if (packetID > nextPacketID && packetID - 1 <= flow.maxPacketID) {
                    // If that was the last ACK, discard the flow
                    if (nextPacketID.equals(flow.maxPacketID))
                        flows.remove(flow);
                    // Remove all the packets we know to have be received from
                    // the flow's queue
                    while (flow.packets.peek().getID() < packetID) {
                        System.out.print(" - Removing packet " + flow.packets.peek().getID());
                        flow.sendTimes.remove(flow.packets.remove().getID());
                    }
                    System.out.println();
                    break;
                }
                // Otherwise the destination is still expecting the first
                //  packet in the queue
                else if (packetID.equals(nextPacketID)) {
                    // Increase the number of times the destination has reported
                    // a packet out of order
                    flow.lastACKCount++;
                    // If we get three in a row, TCP FAST kicks in and the
                    // packet is retransmitted
                    if (flow.lastACKCount == 3) {
                        packetsToSend.add(flow.packets.get(packetID - nextPacketID));
                        flow.lastACKCount = 0;
                    }
                    break;
                }
            }
        }
    }

    /* Handles the reception of setup packets. */
    private void receiveSetupPacket(SetupPacket packet) {
        System.out.println("Setup packet " + packet.getID() + " received at host " + address);

        // Look for the source host in our HashMap
        LinkedList<Download> downloads = this.downloadsBySource.get(packet.getSource());
        // If there have already been downloads from this host, add another to the queue
        if (downloads != null)
            downloads.add(new Download(packet.getID(), packet.getMaxPacketID()));
        // Otherwise create a queue and then add the download
        else {
            downloads = new LinkedList<Download>();
            downloads.add(new Download(packet.getID(), packet.getMaxPacketID()));
            this.downloadsBySource.put(packet.getSource(), downloads);
        }
    }

    /* Handles the reception of data packets. */
    private void receiveDataPacket(DataPacket packet) {
        System.out.println("Data packet " + packet.getID() + " received at host " + address);

        // Look for the source host in our HashMap
        LinkedList<Download> downloads = this.downloadsBySource.get(packet.getSource());
        Integer packetID = packet.getID();
        // If we have existing downloads from this host...
        if (downloads != null) {
            // Look through them all for the one this packet's a part of
            for (Download download : downloads) {
                if (download.nextPacketID <= packetID && packetID <= download.maxPacketID) {
                    // If this was the next packet in the download...
                    if (download.nextPacketID.equals(packetID)) {
                        // Start expecting the following one
                        download.nextPacketID++;
                        // Or if this was the last packet in the download, discard it
                        if (download.maxPacketID.equals(packetID))
                            downloads.remove(download);
                    }
                    // Add an ACK packet to the queue of packets to send immediately
                    packetsToSend.add(new ACKPacket(download.nextPacketID,
                        packet.getDestination(), packet.getSource()));
                    break;
                }
            }
        }
    }

    /* Generic packet receiver. */
    public void receivePacket(Packet packet) {
        if (packet instanceof ACKPacket)
            this.receiveACKPacket((ACKPacket) packet);
        else if (packet instanceof SetupPacket)
            this.receiveSetupPacket((SetupPacket) packet);
        else if (packet instanceof DataPacket)
            this.receiveDataPacket((DataPacket) packet);
    }

    /*
     * Updates a Host so that it sends the packets it currently has available
     * to the link buffer.
     */
    public void update(Integer intervalTime, Integer overallTime) {
        // If this host is connected
        if (this.link != null) {
            // While there are packets to send immediately (e.g. ACKs), add them
            while (this.packetsToSend.peek() != null)
                this.link.addPacket(this.packetsToSend.remove(), this);

            // For each set of flows, indexed by destination...
            for (LinkedList<ActiveFlow> flows : this.flowsByDestination.values()) {
                // For each flow...
                for (ActiveFlow flow : flows) {
                    // For each currently outstanding packet, check if the
                    // timeout time has elapsed since it was sent, and
                    // retransmit if so
                    for (Integer packetID : flow.sendTimes.keySet()) {
                        if (flow.sendTimes.get(packetID) + this.timeoutLength <
                            RunSim.getCurrentTime())
                        {
                            flow.sendTimes.put(packetID, RunSim.getCurrentTime());
                            this.link.addPacket(flow.packets.get(packetID -
                                flow.packets.peek().getID()), this);
                        }
                    }
                    
                    // Packets are ACKed sequentially, so the outstanding
                    // packets have to be at the front of the flow's queue. Thus
                    // we can jump past them and fill up the rest of the window.
                    ListIterator<DataPacket> it =
                        flow.packets.listIterator(flow.sendTimes.size());
                    if (it.hasNext()) {
                        DataPacket packet = it.next();
                        Integer nextPacketID = flow.packets.peek().getID();
                        while (packet.getID() < nextPacketID + flow.windowSize) {
                            this.link.addPacket(packet, this);
                            flow.sendTimes.put(packet.getID(), RunSim.getCurrentTime());
                            if (it.hasNext())
                                packet = it.next();
                            else
                                break;
                        }
                    }
                }
            }
        }
    }
}