package com.ricketts;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLine3DRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Elaine on 12/7/2015.
 */
public class OverlaidPlot extends ApplicationFrame
{
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

    private XYSeriesCollection createDataset(ArrayList<XYSeries> series) {
        final XYSeriesCollection dataset = new XYSeriesCollection();
        for (XYSeries s : series) {
            dataset.addSeries(s);
        }
        return dataset;
    }

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
        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        final NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        return chart;
    }


}