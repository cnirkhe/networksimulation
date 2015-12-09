package com.ricketts;

import org.jfree.data.xy.XYSeries;

import java.util.ArrayList;

/**
 * Collect statistics (flow rate, window size, packet delay) for a flow.
 */
public class FlowAnalyticsCollector {
    /**
     * Series for flow rate, window size, and packet delay.
     */
    private XYSeries flowRates;
    private XYSeries windowSizes;
    private XYSeries packetDelays;

    /**
     * Create a FlowAnalyticsCollector with XYSeries for the series we want to collect.
     * @param flowId The flow ID
     */
    public FlowAnalyticsCollector(int flowId) {
        this.flowRates = new XYSeries("Flow " + flowId);
        this.windowSizes = new XYSeries("Flow " + flowId);
        this.packetDelays = new XYSeries("Flow " + flowId);
    }

    /**
     * Add a rate to flow rates.
     * @param rate The flow rate over the interval
     * @param time Current simulation time
     */
    public void addToFlowRates(double rate, int time) {
        flowRates.add(time, rate);
    }

    /**
     * Add a window size to the window size series.
     * @param size The window size
     * @param time The current simulation time
     */
    public void addToWindowSize(int size, int time) {

        windowSizes.add(time, size);
    }

    /**
     * Add a packet delay to packet delays.
     * @param delay The current packet delay
     * @param time The simulation time
     */
    public void addToPacketDelay(double delay, int time) {
        packetDelays.add(time, delay);
    }

    /**
     * Create a list of the three series.
     * @return ArrayList of the three series.
     */
    public ArrayList<XYSeries> getDatasets() {
        ArrayList<XYSeries> output = new ArrayList<>();
        output.add(flowRates);
        output.add(windowSizes);
        output.add(packetDelays);
        return output;
    }
}