package com.ricketts;

import java.util.Collection;
import java.util.LinkedList;
import java.util.HashMap;

/**
 * Created by chinmay on 11/16/15.
 */
public class Host extends Node {
    // private information
    private final static Integer initWindowSize = 50;

    public static Integer totalSentPackets = 0;
    
    // private variables
    private final Integer address;
    private Link link;
    private Integer windowSize;
    private Integer totalGenPackets;
    private LinkedList<Packet> packetsToSend;
    private LinkedList<Host> flowRecipients;
    private HashMap<Host, LinkedList<ActiveFlow>> sendingFlows;
    private HashMap<Host, LinkedList<Download>> downloadsByHost;

    private class ActiveFlow {
        public Flow flow;
        public Integer maxPacketID;
        public Integer nextPacketID;
        public LinkedList<DataPacket> packets;

        public ActiveFlow(Host host, Flow flow) {
            this.flow = flow;
            this.nextPacketID = host.totalGenPackets;
            this.packets = flow.generateDataPackets(host.totalGenPackets);
            host.totalGenPackets += packets.size();
            this.maxPacketID = host.totalGenPackets - 1;
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
        public Integer sendTime;    // sendTime in milliseconds

        public SentPacket(Packet packet, Integer sendTime) {
            this.packet = packet;
            this.sendTime = sendTime;
        }
    }

    // constructors
    public Host(int address, Link link, Integer windowSize,
        Integer totalGenPackets, LinkedList<Packet> packetsToSend,
        LinkedList<Host> flowRecipients,
        HashMap<Host, LinkedList<ActiveFlow>> sendingFlows,
        HashMap<Host, LinkedList<Download>> downloadsByHost) {
        this.address = address;
        this.link = link;
        this.windowSize = windowSize;
        this.totalGenPackets = totalGenPackets;
        this.packetsToSend = packetsToSend;
        this.flowRecipients = flowRecipients;
        this.sendingFlows = sendingFlows;
        this.downloadsByHost = downloadsByHost;
    }

    public Host(int address, Link link) {
        this(address, link, initWindowSize, 0, new LinkedList<Packet>(),
            new LinkedList<Host>(), new HashMap<Host, LinkedList<ActiveFlow>>(),
            new HashMap<Host, LinkedList<Download>>());
    }

    public Host(int address) {
        this(address, null, initWindowSize, 0, new LinkedList<Packet>(),
            new LinkedList<Host>(), new HashMap<Host, LinkedList<ActiveFlow>>(),
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
        if (flows != null) {
            for (ActiveFlow flow : flows) {
                if (flow.nextPacketID == packet.getPacketID()) {
                    if (flow.nextPacketID == flow.maxPacketID)
                        flows.remove(flow);
                    else
                        flow.nextPacketID++;

                    break;
                }
            }
        }

        System.out.println("ACK packet " + packet.getPacketID() +
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

        System.out.println("Setup packet " + packet.getPacketID() +
            " received at host " + address);
    }

    private void receiveDataPacket(DataPacket packet) {
        //TODO Add analytics here

        // Send an ACKPacket back
        LinkedList<Download> downloads =
            this.downloadsByHost.get(packet.getSource());
        if (downloads != null) {
            for (Download download : downloads) {
                if (download.maxPacketID == packet.getPacketID()) {
                    downloads.remove(download);
                    packetsToSend.add(new ACKPacket(packet.getPacketID(),
                        packet.getDestination(), packet.getSource()));
                }
                else if (download.nextPacketID == packet.getPacketID()) {
                    download.nextPacketID++;
                    packetsToSend.add(new ACKPacket(packet.getPacketID(),
                        packet.getDestination(), packet.getSource()));
                }
            }
        }

        System.out.println("Data packet " + packet.getPacketID() +
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
    }
}