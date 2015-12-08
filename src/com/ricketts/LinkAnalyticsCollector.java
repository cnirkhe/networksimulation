package com.ricketts;

import javafx.scene.chart.LineChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.ui.ApplicationFrame;
import sun.applet.AppletIllegalArgumentException;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;

/**
 * Created by Elaine on 11/27/2015.
 */
public class LinkAnalyticsCollector {
    private XYSeries leftBufferOccupancy;
    private XYSeries rightBufferOccupancy;
    private XYSeries packetLoss;
    private XYSeries linkRates;

    public LinkAnalyticsCollector(int linkId, String name) {
        this.leftBufferOccupancy = new XYSeries("Link " + linkId);
        this.rightBufferOccupancy = new XYSeries("Link " + linkId);
        this.packetLoss = new XYSeries("Link " + linkId);
        this.linkRates = new XYSeries("Link " + linkId);
    }

    public void addToLeftBuffer(double size, int time) {
        leftBufferOccupancy.add(time, size);
    }

    public void addToRightBuffer(double size, int time) {
        rightBufferOccupancy.add(time, size);
    }

    public void addToPacketLoss(int packets, int time) {
        packetLoss.add(time, packets);
    }

    public void addToLinkRates(double rate, int time) {
        linkRates.add(time, rate);
    }

    public ArrayList<XYSeries> getDatasets() {
        ArrayList<XYSeries> output = new ArrayList<>();
        output.add(leftBufferOccupancy);
        output.add(rightBufferOccupancy);
        output.add(packetLoss);
        output.add(linkRates);
        return output;
    }

    /*
    public void generateLinkGraphs() {
        LineChart_AWT leftBuff = new LineChart_AWT("Left Buffer", "Left Buffer Occupancy", "Buffer occupancy (pkts)",
                leftBufferOccupancy, "Link " + linkId + " Left Buffer Occupancy " + name + ".png", 888, 888);
        LineChart_AWT rightBuff = new LineChart_AWT("Right Buffer", "Right Buffer Occupancy", "Buffer occupancy (pkts)",
                rightBufferOccupancy, "Link " + linkId + " Right Buffer Occupancy " + name + ".png", 888, 888);
        LineChart_AWT packetLossGraph = new LineChart_AWT("Packet Loss", "Packet Loss", "Packet Loss (pkts)",
                packetLoss, "Link " + linkId + " Packet Loss " + name + ".png", 888, 888);
        LineChart_AWT linkRateGraph = new LineChart_AWT("Link Rates", "Link Rates", "Link Rate (Mbps)",
                linkRates, "Link " + linkId + " Link Rate " + name + ".png", 888, 888);
    }*/
}