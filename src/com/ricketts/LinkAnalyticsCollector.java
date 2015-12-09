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
    private XYSeries bufferOccupancy;
    private XYSeries packetLoss;
    private XYSeries linkRates;

    public LinkAnalyticsCollector(int linkId, String name) {
        this.bufferOccupancy = new XYSeries("Link " + linkId);
        this.packetLoss = new XYSeries("Link " + linkId);
        this.linkRates = new XYSeries("Link " + linkId);
    }

    public void addToBuffer(double size, int time) {
        bufferOccupancy.add(time, size);
    }

    public void addToPacketLoss(int packets, int time) {
        packetLoss.add(time, packets);
    }

    public void addToLinkRates(double rate, int time) {
        linkRates.add(time, rate);
    }

    public ArrayList<XYSeries> getDatasets() {
        ArrayList<XYSeries> output = new ArrayList<>();
        output.add(bufferOccupancy);
        output.add(packetLoss);
        output.add(linkRates);
        return output;
    }
}