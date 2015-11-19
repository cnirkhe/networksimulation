package com.ricketts;

import java.util.Collection;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.ListIterator;

/**
 * Created by chinmay on 11/16/15.
 */
public class Host extends Node {
    // private information
    private final static Integer initWindowSize = 50;
    private final static Integer timeoutLength = 1000;
    
    // private variables
    private final Integer address;
    private Link link;
    private Integer totalGenPackets;
    private LinkedList<Packet> packetsToSend;
    private HashMap<Host, LinkedList<ActiveFlow>> sendingFlows;
    private HashMap<Host, LinkedList<Download>> downloadsByHost;

    private class ActiveFlow {
        public Flow flow;
        public Integer windowSize;
        public Integer maxPacketID;
        public Integer nextPacketID;
        public Integer lastACKCount;
        public LinkedList<DataPacket> packets;
        public LinkedList<SentPacket> sentPackets;

        public ActiveFlow(Host host, Flow flow) {
            this.flow = flow;
            this.windowSize = initWindowSize;
            this.packets = flow.generateDataPackets(host.totalGenPackets);
            host.totalGenPackets += packets.size();
            this.maxPacketID = host.totalGenPackets - 1;
            this.lastACKCount = 0;
            this.sentPackets = new LinkedList<SentPacket>();
        }
    }

    private class Download {
        public Integer maxPacketID;
        public Integer nextPacketID;

        public Download(Integer minPacketID, Integer maxPacketID) {
            this.nextPacketID = minPacketID;
            this.maxPacketID = maxPacketID;
        }
    }

    private class SentPacket {
        public Packet packet;
        public Integer sendTime;

        public SentPacket(Packet packet, Integer sendTime) {
            this.packet = packet;
            this.sendTime = sendTime;
        }
    }

    // constructors
    public Host(int address, Link link, Integer totalGenPackets,
        LinkedList<Packet> packetsToSend,
        HashMap<Host, LinkedList<ActiveFlow>> sendingFlows,
        HashMap<Host, LinkedList<Download>> downloadsByHost) {
        this.address = address;
        this.link = link;
        this.totalGenPackets = totalGenPackets;
        this.packetsToSend = packetsToSend;
        this.sendingFlows = sendingFlows;
        this.downloadsByHost = downloadsByHost;
    }

    public Host(int address, Link link) {
        this(address, link, 0, new LinkedList<Packet>(),
            new HashMap<Host, LinkedList<ActiveFlow>>(),
            new HashMap<Host, LinkedList<Download>>());
    }

    public Host(int address) {
        this(address, null, 0, new LinkedList<Packet>(),
            new HashMap<Host, LinkedList<ActiveFlow>>(),
            new HashMap<Host, LinkedList<Download>>());
    }

    // Accessor Methods
    public int getAddress() { return this.address; }
    public Link getLink() { return this.link; }

    // public methods below
    public void setLink(Link link) {
        this.link = link;
    }

    public void addFlow(Flow flow) {
        LinkedList<ActiveFlow> flows =
            this.sendingFlows.get(flow.getDestination());
        if (flows != null)
            flows.add(new ActiveFlow(this, flow));
        else {
            flows = new LinkedList<ActiveFlow>();
            flows.add(new ActiveFlow(this, flow));
            this.sendingFlows.put(flow.getDestination(), flows);
        }
    }

    // below in process of being updated

    private void receiveACKPacket(ACKPacket packet) {
        LinkedList<ActiveFlow> flows =
            this.sendingFlows.get(packet.getSource());
        Integer packetID = packet.getID();
        if (flows != null) {
            for (ActiveFlow flow : flows) {
                Integer nextPacketID = flow.packets.peek().getID();
                if (packetID - 1 >= nextPacketID && packetID - 1 <= flow.maxPacketID) {
                    if (nextPacketID == flow.maxPacketID)
                        flows.remove(flow);
                    else {
                        while (flow.packets.peek().getID() < packetID)
                            flow.packets.remove();
                    }

                    break;
                }
                else if (packetID == nextPacketID) {
                    flow.lastACKCount++;
                    if (flow.lastACKCount == 3) {
                        packetsToSend.add(flow.packets.get(packetID - nextPacketID));
                        flow.lastACKCount = 0;
                    }

                    break;
                }
            }
        }

        System.out.println("ACK packet " + packet.getID() +
            " received at host " + address);
    }

    private void receiveSetupPacket(SetupPacket packet) {
        LinkedList<Download> downloads =
            this.downloadsByHost.get(packet.getSource());
        if (downloads != null)
            downloads.add(new Download(packet.minPacketID, packet.maxPacketID));
        else {
            downloads = new LinkedList<Download>();
            downloads.add(new Download(packet.minPacketID, packet.maxPacketID));
            this.downloadsByHost.put(packet.getSource(), downloads);
        }

        System.out.println("Setup packet " + packet.getID() +
            " received at host " + address);
    }

    private void receiveDataPacket(DataPacket packet) {
        //TODO Add analytics here

        // Send an ACKPacket back
        LinkedList<Download> downloads =
            this.downloadsByHost.get(packet.getSource());
        Integer packetID = packet.getID();
        if (downloads != null) {
            for (Download download : downloads) {
                if (download.nextPacketID <= packetID && packetID <= download.maxPacketID) {
                    if (download.maxPacketID == packetID)
                        downloads.remove(download);
                    if (download.nextPacketID == packetID)
                        download.nextPacketID++;

                    packetsToSend.add(new ACKPacket(download.nextPacketID,
                        packet.getDestination(), packet.getSource()));
                }
            }
        }

        System.out.println("Data packet " + packet.getID() +
            " received at host " + address);
    }

    // receive a packet and return an ACK packet
    public void receivePacket(Packet packet) {
        if (packet instanceof ACKPacket)
            this.receiveACKPacket((ACKPacket) packet);
        else if (packet instanceof SetupPacket)
            this.receiveSetupPacket((SetupPacket) packet);
        else if (packet instanceof DataPacket)
            this.receiveDataPacket((DataPacket) packet);
    }

    public void update(Integer intervalTime, Integer overallTime) {
        if (this.link != null) {
            while (this.packetsToSend.peek() != null)
                this.link.addPacket(this.packetsToSend.remove(), this);

            LinkedList<SentPacket> timedOutPackets = new LinkedList<SentPacket>();
            for (LinkedList<ActiveFlow> flows : this.sendingFlows.values()) {
                for (ActiveFlow flow : flows) {
                    timedOutPackets.clear();
                    for (SentPacket sentPacket : flow.sentPackets) {
                        if (sentPacket.sendTime + this.timeoutLength > RunSim.getCurrentTime()) {
                            flow.sentPackets.remove(sentPacket);
                            this.link.addPacket(sentPacket.packet, this);
                            timedOutPackets.add(new SentPacket(sentPacket.packet,
                                RunSim.getCurrentTime()));
                        }
                    }
                    flow.sentPackets.addAll(timedOutPackets);
                    
                    ListIterator<DataPacket> it =
                        flow.packets.listIterator(flow.sentPackets.size());
                    DataPacket packet = it.next();
                    while (packet.getID() < flow.nextPacketID + flow.windowSize) {
                        this.link.addPacket(packet, this);
                        flow.sentPackets.add(new SentPacket(packet, RunSim.getCurrentTime()));
                        packet = it.next();
                    }
                }
            }
        }
    }
}