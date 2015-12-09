package com.ricketts;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Utilities for creating a line plot of multiple series and save as JPEG.
 */
public class OverlaidPlot extends ApplicationFrame
{
    /**
     * Create a lineplot of the specified dataset and save as a .jpeg.
     * @param title Plot title
     * @param filename Output filename the plot is saved as
     * @param series List of data series to plot
     * @param xAxis X axis title
     * @param yAxis Y axis title
     * @param width Plot width (pixels0
     * @param height Plot height (pixels)
     */
    public OverlaidPlot(final String title, final String filename, ArrayList<XYSeries> series,
                        final String xAxis, final String yAxis, int width, int height) {
        super(title);
        final XYDataset dataset = createDataset(series);
        final JFreeChart chart = createChart(dataset, title, xAxis, yAxis);

        File lineChartFile = new File(filename);
        try {
            ChartUtilities.saveChartAsJPEG(lineChartFile, chart, width, height);
        } catch (IOException e) {
            System.out.println("Error in creating chart " + e);
        }
    }

    /**
     * Create a dataset given a list of series.
     * @param series The list of XYSeries we collected
     * @return A XYSeriesCollection consisting of all the input series
     */
    private XYSeriesCollection createDataset(ArrayList<XYSeries> series) {
        final XYSeriesCollection dataset = new XYSeriesCollection();
        for (XYSeries s : series) {
            dataset.addSeries(s);
        }
        return dataset;
    }

    /**
     * Create a JFreeChart given a dataset.
     * @param dataset The collection of series we want to graph.
     * @param title Chart title
     * @param xAxis X axis title
     * @param yAxis Y axis title
     * @return A JFreeChart object representing the output graph
     */
    private JFreeChart createChart(final XYDataset dataset, final String title, final String xAxis,
                                   final String yAxis) {
        final JFreeChart chart = ChartFactory.createXYLineChart(
                title,
                xAxis,
                yAxis,
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        final XYPlot plot = chart.getXYPlot();
        // Make integer ticks for both x and y axes.
        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        final NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        return chart;
    }
}