package com.ricketts;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Set;

/**
 * A Host is a Node meant to simulate a source or sink of data. Hosts have only one Link. Flows begin at Hosts.
 */
public class Host extends Node {

    public final static Double timeoutLengthCatchupFactor = 0.1;

    private Link link;

    /**
     * A LinkedList of Packets that have been scheduled to send out but have yet to be sent.
     * These are the priority packets to send out (generally ACKs).
     */
    private LinkedList<Packet> immediatePacketsToSend;

    /**
     * Downloads indexed by Source Host
     */
    private Download download;
    private Flow flow;

    /**
     * Protocol we're using
     */
    private int protocol;

    /**
     * A Download represents a Flow incoming from another Host
     * As we are only simulating, no track of the actual packets is kept, just the packetIds
     */
    private class Download {

        public Host source;
        /**
         * Last packet ID in the download
         */
        public Integer maxPacketID;
        /**
         * Next expected packet ID in the download
         * */
        public Integer nextPacketID;

        public Download(Host source, Integer minPacketID, Integer maxPacketID) {
            this.source = source;
            this.nextPacketID = minPacketID;
            this.maxPacketID = maxPacketID;
        }
    }

    /**
     * Complete Constructor
     */
    public Host(String address, Link link, LinkedList<Packet> immediatePacketsToSend, Flow flow, Download download, int protocol) {
        super(address);
        this.link = link;
        this.immediatePacketsToSend = immediatePacketsToSend;
        this.flow = flow;
        this.download = download;
        this.protocol = protocol;
    }

    /**
     * Construct a Host from a link
     */
    public Host(String address, Link link, int protocol) {
        this(address, link, new LinkedList<Packet>(), null, null, protocol);
    }

    public Link getLink() { return this.link; }

    /**
     * Add a flow starting from this Host
     * This involves checking if we already have flows going to that destination,
     * and getting the flowsByDestination HashMap set accordingly
     * Lastly, we send a setup packet to initiate the flow
     * @param flow The flow to be added
     */
    public void addFlow(Flow flow) {
        this.flow = flow;
    }

    /**
     * Handles the reception of an ACK packet.
     * @param ackPacket the ACK received
     */
    private void receiveACKPacket(ACKPacket ackPacket) {
        System.out.println("ACK packet " + ackPacket.getID() + " received at host " + address + " at time " + Main.currentTime);
        Integer ackPacketID = ackPacket.getID();
        //Check to make sure the source of the ACK is from one which we are sending flows to
        if(flow != null && flow.activated) {
            // If the ACK is for a new packet, we know the destination has
            // received packets at least up to that one
            if (ackPacketID > flow.firstNotRecievedPacketIndex) {
                flow.windowOccupied--;
                flow.numberOfLatestACKIDRecieved = 0;
                if (protocol == Main.Protocol.RENO) {
                    if (flow.slowStart) {
                        // If we're in slow start & Reno, cwnd <- cwnd + 1
                        flow.windowSize++;
                        if (flow.windowSize > flow.ssthresh) {
                            flow.slowStart = false;
                        }
                    }
                    // If we're in CA phase for Reno, cwnd <- cwnd + 1/cwnd. In out
                    // implementation we add to partialWindowSize.
                    else {
                        flow.partialWindowSize++;
                        // If we've received enough acks to increment the window size, do so.
                        if (flow.partialWindowSize >= flow.windowSize) {
                            flow.windowSize++;
                            flow.partialWindowSize = 0;
                        }
                    }
                }
                // If that was the last ACK, discard the flow
                if (flow.firstNotRecievedPacketIndex.equals(flow.maxPacketID)) {
                    System.out.println("flow finished. gg no re");
                }
                // Increment the firstNOTACKedPacketIndex to 1 past the ack that was just recieved
                //Furthermore update the round trip times accordingly
                else {
                    for(int i = flow.firstNotRecievedPacketIndex; i < ackPacketID; ++i) {
                        Integer newRoundTripTime =
                                Main.currentTime - flow.sendTimes.get(i);
                        // update minRoundTripTime
                        flow.minRoundTripTime = Math.min(flow.minRoundTripTime, newRoundTripTime);
                        // update avgRoundTripTime
                        if (flow.avgRoundTripTime == null) {
                            flow.avgRoundTripTime = newRoundTripTime * 1.0;
                        } else {
                            flow.avgRoundTripTime = flow.avgRoundTripTime * (1 - timeoutLengthCatchupFactor)
                                    + newRoundTripTime * timeoutLengthCatchupFactor;
                        }
                        // update stdDevRoundTripTime
                        if (flow.stdDevRoundTripTime == null) {
                            flow.stdDevRoundTripTime = newRoundTripTime * 1.0;
                        } else {
                            flow.stdDevRoundTripTime = flow.stdDevRoundTripTime * (1 - timeoutLengthCatchupFactor)
                                    + Math.abs(newRoundTripTime - flow.avgRoundTripTime) * timeoutLengthCatchupFactor;
                        }
                        flow.sendTimes.remove(i);
                    }
                    flow.firstNotRecievedPacketIndex = ackPacketID;
                }
            }
            // Otherwise the destination is still expecting the first packet in the queue
            // If it does actually receive it
            else if (ackPacketID.equals(flow.firstNotRecievedPacketIndex)) {
                // Increase the number of times the destination has reported
                // a packet out of order
                flow.numberOfLatestACKIDRecieved++;
                // If this packet has been ACKed three or more time, assume
                // it's been dropped and retransmit (TCP FAST)

                //TODO QUESTION @ ELAINE
                //TODO why keep track of flow.mostRecentRetransmittedPacket?
                if (flow.numberOfLatestACKIDRecieved >= 3 && flow.mostRecentRetransmittedPacket != ackPacketID) {
                    flow.mostRecentRetransmittedPacket = ackPacketID;
                    DataPacket packet = flow.packets.get(flow.firstNotRecievedPacketIndex);
                    flow.sendTimes.put(flow.firstNotRecievedPacketIndex, Main.currentTime);
                    this.link.clearBuffer(this);
                    this.link.addPacket(packet, this);
                    System.out.println("FASTRETransmission of Packet " + packet.getID() + " at time " + Main.currentTime);
                    flow.currBitsSent += packet.getSize();
                    // Since everything we sent won't go through, reset the window size to
                    // 1 (since we just retransmitted a packet).
                    flow.windowOccupied = 1;
                    flow.mostRecentQueued = packet.getID();
                    if (protocol == Main.Protocol.RENO && !flow.awaitingRetransmit) {
                        // Enter FR/FR.
                        if (flow.windowSize / 2 < 2) {
                            flow.ssthresh = 2;
                        }
                        else {
                            flow.ssthresh = flow.windowSize / 2;
                        }
                        // Wait for packet retransmit, at that point we will deflate the
                        // window.
                        flow.awaitingRetransmit = true;
                        // cwnd <- ssthresh + ndup (temp window inflation)
                        flow.windowSize = flow.ssthresh + flow.numberOfLatestACKIDRecieved;
                        flow.slowStart = false;
                    }
                    flow.numberOfLatestACKIDRecieved = 0;
                }
            }
        }
    }

    /**
     * Handles the setup of receiving a flow upon reach of a setup packet
     * @param packet The Setup packet
     */
    private void receiveSetupPacket(SetupPacket packet) {
        download = new Download((Host) packet.getSource(), 0, packet.getMaxPacketID());
    }

    /**
     * Handles the reception and resending of an ACK packet upon recieving a DataPacket
     * @param packet The Setup packet
     */
    private void receiveDataPacket(DataPacket packet) {
        System.out.println("Data packet " + packet.getID() + " received at host " + address + " at time " + Main.currentTime);
        Integer packetID = packet.getID();
        if (download != null && download.source == packet.getSource()) {
            if (download.nextPacketID <= packetID && packetID <= download.maxPacketID) {
                // If this was the next packet in the download...
                if (download.nextPacketID.equals(packetID)) {
                    // Start expecting the following one
                    download.nextPacketID++;
                }
                // Add an ACK packet to the queue of packets to send immediately
                immediatePacketsToSend.add(new ACKPacket(download.nextPacketID,
                        (Host) packet.getDestination(), (Host) packet.getSource()));
            }
        }
    }

    /**
     * Calls the appropriate Packet Receiving subroutine based on packet data type
     * @param packet Packet received
     * @param receivingLink The link that the packet came on
     */
    public void receivePacket(Packet packet, Link receivingLink) {
        if (packet instanceof ACKPacket)
            this.receiveACKPacket((ACKPacket) packet);
        else if (packet instanceof SetupPacket)
            this.receiveSetupPacket((SetupPacket) packet);
        else if (packet instanceof DataPacket)
            this.receiveDataPacket((DataPacket) packet);
        //else if (packet instanceof RoutingTablePacket)
            //Do nothing
    }

    /**
     * Updates a Host so that it sends the packets it currently has available
     * to the link buffer.
     */
    public void update() {

        //Activate the flow if the time is ready
        if(flow != null && flow.getStartTime() <= Main.currentTime && flow.activated == false) {
            flow.activateFlow();
            this.immediatePacketsToSend.add(new SetupPacket(0, this, flow.getDestination(), flow.maxPacketID));
            System.out.println("Regular Transmission of Setup Packet at time " + Main.currentTime);
        }

        // If this host is connected
        if (this.link != null) {
            // While there are packets to send immediately (e.g. ACKs), add them
            while (this.immediatePacketsToSend.peek() != null) {
                this.link.addPacket(this.immediatePacketsToSend.remove(), this);
            }

            //If there is an active flow
            if(flow != null && flow.activated) {
                flow.currBitsSent = 0;
                // For each currently outstanding packet, check if the
                // timeout time has elapsed since it was sent, and
                // retransmit if so
                Integer minTimedOutPacketID = Integer.MAX_VALUE;

                Set<Integer> sentPacketIDs = flow.sendTimes.keySet();
                for(Integer sentPacketID : sentPacketIDs) {
                    Integer sendTime = flow.sendTimes.get(sentPacketID);
                 if (sendTime + flow.timeoutLength < Main.currentTime) {
                        //Flow has timed out
                        if (minTimedOutPacketID > sentPacketID)
                            minTimedOutPacketID = sentPacketID;
                    }
                }

                //Now for the minTimedOutPackedID (assuming sentPacketIDs wasn't empty)
                if(minTimedOutPacketID != Integer.MAX_VALUE) {
                    if (protocol == Main.Protocol.RENO) {
                        if (flow.windowSize / 2 < 2) {
                            flow.ssthresh = 2;
                        } else {
                            flow.ssthresh = flow.windowSize / 2;
                        }
                        flow.slowStart = true;
                        flow.windowSize = Flow.initWindowSize;
                    }
                    flow.sendTimes.put(minTimedOutPacketID, Main.currentTime);
                    flow.windowOccupied = 1;
                    flow.mostRecentQueued = minTimedOutPacketID;
                    link.clearBuffer(this);
                    DataPacket packetToResend = flow.packets.get(minTimedOutPacketID);
                    this.link.addPacket(packetToResend, this);
                    System.out.println("RETransmission of Packet " + packetToResend.getID() + " at time " + Main.currentTime);
                    flow.currBitsSent += packetToResend.getSize();
                }

                // Packets are ACKed sequentially, so the outstanding
                // packets are just the ones from mostRecentQueued onwards. Thus
                // we can jump past them and fill up the rest of the window.
                int next = flow.mostRecentQueued + 1;
                if(flow.packets.size() > next) {
                    ListIterator<DataPacket> it = flow.packets.listIterator(next);
                    if (it.hasNext()) {
                        DataPacket packet = it.next();
                        while (flow.windowSize > flow.windowOccupied) {
                            // If we're in FR/FR and we're retransmitting, we need to deflate the window.
                            if (protocol == Main.Protocol.RENO && flow.awaitingRetransmit) {
                                flow.windowSize = flow.ssthresh;
                                flow.awaitingRetransmit = false;
                            }
                            flow.windowOccupied++;
                            this.link.addPacket(packet, this);
                            System.out.println("Regular Transmission of Packet " + packet.getID() + " at time " + Main.currentTime);
                            flow.sendTimes.put(packet.getID(), Main.currentTime);
                            flow.mostRecentQueued = packet.getID();
                            flow.currBitsSent += packet.getSize();
                            if (it.hasNext())
                                packet = it.next();
                            else
                                break;
                        }
                    }
                }
                flow.flowAnalyticsCollector.addToFlowRates(((double) flow.currBitsSent / 100000) / ((double) Main.intervalTime / 1000), Main.currentTime);
                flow.flowAnalyticsCollector.addToWindowSize(flow.windowSize, Main.currentTime);
            }
        }
    }
}