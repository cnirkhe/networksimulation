package com.ricketts;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;

import java.io.File;
import java.io.IOException;

/**
 * Created by Elaine on 11/27/2015.
 */
public class LineChart_AWT extends ApplicationFrame {
    public LineChart_AWT(String applicationTitle, String chartTitle, String yAxis, DefaultCategoryDataset dataset,
                         String filename, int width, int height) {
        super(applicationTitle);
        JFreeChart lineChart = ChartFactory.createLineChart(
                chartTitle,
                "Time (ms)",
                yAxis,
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);
        File lineChartFile = new File(filename);
        try {
            ChartUtilities.saveChartAsJPEG(lineChartFile, lineChart, width, height);
        } catch (IOException e) {
            System.out.println("Error in creating chart " + e);
        }
    }
}
