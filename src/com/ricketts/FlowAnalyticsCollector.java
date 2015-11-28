package com.ricketts;

import com.sun.xml.internal.ws.api.streaming.XMLStreamReaderFactory;
import org.jfree.data.category.DefaultCategoryDataset;

import java.util.ArrayList;

/**
 * Created by Elaine on 11/27/2015.
 */
public class FlowAnalyticsCollector {
    /**
     * Flow rate, window size, and packet delay for analytics.
     * TODO: add in window size/packet delay when implemented
     */
    private DefaultCategoryDataset flowRates;
    private DefaultCategoryDataset windowSizes;
    private DefaultCategoryDataset packetDelays;
    private int flowId;
    private String name;

    public FlowAnalyticsCollector(int flowId, String name) {
        this.flowId = flowId;
        this.flowRates = new DefaultCategoryDataset();
        this.windowSizes = new DefaultCategoryDataset();
        this.packetDelays = new DefaultCategoryDataset();
        this.name = name;
    }

    /**
     *  Add a rate to flow rates.
     */
    public void addToFlowRates(double rate, int time) {
        flowRates.addValue((Number) rate, "Flow " + flowId, time);
    }

    /**
     * Add a window size to window sizes.
     */
    public void addToWindowSize(int size, int time) {
        windowSizes.addValue((Number) size, "Flow " + flowId, time);
    }

    /**
     * Add a packet delay to packet delays.
     */
    public void addToPacketDelay(double delay, int time) {
        packetDelays.addValue((Number) delay, "Flow " + flowId, time);
    }

    public void generateFlowGraphs() {
        LineChart_AWT flowRateGraph = new LineChart_AWT("Flow Rates", "Flow Rates", "Flow Rate (Mbps)",
                flowRates, "graphs/Flow " + flowId + " Rate " + name + ".png", 888, 888);
        LineChart_AWT windowSizeGraph = new LineChart_AWT("Window Size", "Window Size", "Window Size (pkts)",
                windowSizes, "graphs/Flow " + flowId + " Window Size " + name + ".png", 888, 888);
        LineChart_AWT packetDelayGraph = new LineChart_AWT("Packet Delay", "Packet Delay", "Packets Delay (ms)",
                packetDelays, "graphs/Flow " + flowId + " Packet Delay " + name + ".png", 888, 888);
    }
}
