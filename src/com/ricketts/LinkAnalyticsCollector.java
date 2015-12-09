package com.ricketts;

import org.jfree.data.xy.XYSeries;
import java.util.ArrayList;

/**
 * Collect statistics (buffer occupancy, link rate, and packet loss) for links.
 */
public class LinkAnalyticsCollector {
    /**
     * Series tracking buffer occupancy over time.
     */
    private XYSeries bufferOccupancy;
    /**
     * Series tracking packet loss.
     */
    private XYSeries packetLoss;
    /**
     * Series tracking link rate.
     */
    private XYSeries linkRates;

    /**
     * Create a new LinkAnalyticsCollector
     * @param linkId The ID of the current link
     */
    public LinkAnalyticsCollector(int linkId) {
        this.bufferOccupancy = new XYSeries("Link " + linkId);
        this.packetLoss = new XYSeries("Link " + linkId);
        this.linkRates = new XYSeries("Link " + linkId);
    }

    /**
     * Add a buffer size and the current time to the buffer series.
     * @param occupancy Buffer occupancy
     * @param time Current simulation time
     */
    public void addToBuffer(double occupancy, int time) {
        bufferOccupancy.add(time, occupancy);
    }

    /**
     * Add a packet loss amount and the current time to the packet loss series.
     * @param packetsDropped Number of packets lost in this interval
     * @param time Current simulation time
     */
    public void addToPacketLoss(int packetsDropped, int time) {
        packetLoss.add(time, packetsDropped);
    }

    /**
     * Add a link rate to the link rate series.
     * @param rate Link rate over the interval
     * @param time Current simulation time
     */
    public void addToLinkRates(double rate, int time) {
        linkRates.add(time, rate);
    }

    /**
     * Create a list of all 3 series.
     * @return ArrayList of the series.
     */
    public ArrayList<XYSeries> getDatasets() {
        ArrayList<XYSeries> output = new ArrayList<>();
        output.add(bufferOccupancy);
        output.add(packetLoss);
        output.add(linkRates);
        return output;
    }
}