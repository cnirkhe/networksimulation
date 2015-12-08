package com.ricketts;

import com.sun.xml.internal.ws.api.streaming.XMLStreamReaderFactory;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;

import java.util.ArrayList;

/**
 * Created by Elaine on 11/27/2015.
 */
public class FlowAnalyticsCollector {
    /**
     * Flow rate, window size, and packet delay for analytics.
     * TODO: add in window size/packet delay when implemented
     */
    private XYSeries flowRates;
    private XYSeries windowSizes;
    private XYSeries packetDelays;
    private int flowId;
    private String name;

    public FlowAnalyticsCollector(int flowId, String name) {
        this.flowRates = new XYSeries("Flow " + flowId);
        this.windowSizes = new XYSeries("Flow " + flowId);
        this.packetDelays = new XYSeries("Flow " + flowId);
    }

    /**
     *  Add a rate to flow rates.
     */
    public void addToFlowRates(double rate, int time) {
        flowRates.add(time, rate);
    }

    /**
     * Add a window size to window sizes.
     */
    public void addToWindowSize(int size, int time) {
        windowSizes.add(time, size);
    }

    /**
     * Add a packet delay to packet delays.
     */
    public void addToPacketDelay(double delay, int time) {
        packetDelays.add(time, delay);
    }

    public ArrayList<XYSeries> getDatasets() {
        ArrayList<XYSeries> output = new ArrayList<>();
        output.add(flowRates);
        output.add(windowSizes);
        output.add(packetDelays);
        return output;
    }
}
