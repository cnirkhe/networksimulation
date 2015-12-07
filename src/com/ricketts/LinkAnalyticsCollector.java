package com.ricketts;

import javafx.scene.chart.LineChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import sun.applet.AppletIllegalArgumentException;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Created by Elaine on 11/27/2015.
 */
public class LinkAnalyticsCollector {
    private DefaultCategoryDataset leftBufferOccupancy;
    private DefaultCategoryDataset rightBufferOccupancy;
    private DefaultCategoryDataset packetLoss;
    private DefaultCategoryDataset linkRates;
    private int linkId;
    private String name;

    public LinkAnalyticsCollector(int linkId, String name) {
        this.linkId = linkId;
        this.leftBufferOccupancy = new DefaultCategoryDataset();
        this.rightBufferOccupancy = new DefaultCategoryDataset();
        this.packetLoss = new DefaultCategoryDataset();
        this.linkRates = new DefaultCategoryDataset();
        this.name = name;
    }

    public void addToLeftBuffer(double size, int time) {
        leftBufferOccupancy.addValue(size, "Link " + linkId, "" + time);
    }

    public void addToRightBuffer(double size, int time) {
        rightBufferOccupancy.addValue(size, "Link " + linkId, "" + time);
    }

    public void addToPacketLoss(int packets, int time) {
        packetLoss.addValue(packets, "Link " + linkId, "" + time);
    }

    public void addToLinkRates(double rate, int time) {
        linkRates.addValue(rate, "Link " + linkId, "" + time);
    }

    public void generateLinkGraphs() {
        LineChart_AWT leftBuff = new LineChart_AWT("Left Buffer", "Left Buffer Occupancy", "Buffer occupancy (pkts)",
                leftBufferOccupancy, "Link " + linkId + " Left Buffer Occupancy " + name + ".png", 888, 888);
        LineChart_AWT rightBuff = new LineChart_AWT("Right Buffer", "Right Buffer Occupancy", "Buffer occupancy (pkts)",
                rightBufferOccupancy, "Link " + linkId + " Right Buffer Occupancy " + name + ".png", 888, 888);
        LineChart_AWT packetLossGraph = new LineChart_AWT("Packet Loss", "Packet Loss", "Packet Loss (pkts)",
                packetLoss, "Link " + linkId + " Packet Loss " + name + ".png", 888, 888);
        LineChart_AWT linkRateGraph = new LineChart_AWT("Link Rates", "Link Rates", "Link Rate (Mbps)",
                linkRates, "Link " + linkId + " Link Rate " + name + ".png", 888, 888);
    }
}