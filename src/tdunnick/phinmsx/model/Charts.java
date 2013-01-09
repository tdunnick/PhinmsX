/*
 *  Copyright (c) 2012-2013 Thomas Dunnick (https://mywebspace.wisc.edu/tdunnick/web)
 *  
 *  This file is part of PhinmsX.
 *
 *  PhinmsX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  PhinmsX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with PhinmsX.  If not, see <http://www.gnu.org/licenses/>.
 */
package tdunnick.phinmsx.model;

import java.io.*;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.*;
import java.awt.*;
import java.awt.image.*;

import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.urls.*;
import org.jfree.chart.renderer.category.*;
import org.jfree.data.category.*;
import org.jfree.data.general.*;
import org.jfree.data.xy.*;

import tdunnick.phinmsx.domain.DashBoardData;

public class Charts
{		
	private final static long MS = 24*60*60*1000;

	/************************* bar charts *********************************/
	
	/**
	 * Generate and set a bar chart for the current table shown in the dashboard.
	 * 
	 * @param dash
	 */
	public void getBarChart (DashBoardData dash)
	{
		ArrayList r = dash.getStats();
		long ends = dash.getEnds();
		int days = dash.getDays();
		CategoryDataset dataset = createBarChartDataset (r, ends, days);
    JFreeChart chart = createBarChart("Activity", dataset);
    dash.setBarchart(getJFreeObject (chart, "bar", 350, 250));
	}
	
	/**
	 * Sort and setup chart data by "constraint"
	 * 
	 * @param r list of constraint, time pairs
	 * @param ends ending date for list
	 * @param days covered by list
	 * @return bar chart data
	 */
	private CategoryDataset createBarChartDataset (ArrayList r, long ends, long days)
	{
		ArrayList constraints = new ArrayList();
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		// first time through just collect the constraint names
		for (int i = 0; i < r.size(); i++)
		{
			String[] item = (String[]) r.get(i);
			String k = (String) item[0];
			if (!constraints.contains(k))
				constraints.add(k);
		}
		long interval = days * MS;
		long start = ends - interval;
		interval /= 5;
		int[] counts = new int[constraints.size()];
		for (int i = 0; i < constraints.size(); i++)
			counts[i] = 0;
		for (int i = 0; i < r.size(); i++)
		{
			String[] item = (String[]) r.get(i);
			long d = Long.parseLong(item[1]);
			if (d > start)
			{
				addBarData (dataset, constraints, start, counts);
				start += interval;
			}
			counts[constraints.indexOf(item[0])]++;
		}
		addBarData (dataset, constraints, start, counts);
		while ((start += interval) < ends)
			addBarData (dataset, constraints, start, counts);
		return dataset;
	}
	
	/**
	 * Adds data for one interval in the bar chart.  The date is set to MMM-yy
	 * format.
	 * 
	 * @param data to add interval to
	 * @param constraints for that data
	 * @param d time (date) for that interval
	 * @param values for each constraint
	 */
	private void addBarData (DefaultCategoryDataset data, ArrayList constraints, 
			long d, int[] values)
	{
		SimpleDateFormat f = new SimpleDateFormat("MMM-yy");
		String date = f.format (new Date(d));
		for (int i = 0; i < values.length; i++)
		{
			data.addValue(values[i], (String) constraints.get(i), date);
			values[i] = 0;
		}
	}

	/**
	 * Creates the bar chart.  Set the X-axis font small enough to accommodate
	 * dates and tidy up the bars.
	 * @param title for the chart
	 * @param dataset for the chart
	 * @return the chart
	 */
	private JFreeChart createBarChart(String title, CategoryDataset dataset) 
	{
		// create the chart...
	 JFreeChart chart = ChartFactory.createBarChart(
			  title,
				null, // domain axis label
				"Messages", // range axis label
				dataset, // data
				PlotOrientation.VERTICAL, // orientation
				true, // include legend
				true, // tooltips?
				false // URLs?
				);
	  CategoryPlot plot = chart.getCategoryPlot();
	  plot.getDomainAxis().setTickLabelFont(new Font("Arial", Font.BOLD, 9));
    BarRenderer renderer = (BarRenderer) chart.getCategoryPlot().getRenderer();
    renderer.setItemMargin(0.01);
    renderer.setDrawBarOutline(true);
    renderer.setShadowVisible(false);
		return chart;
	}
  
  /************************** line chart **************************************/
  
	public void getLineChart (DashBoardData dash)
	{
		ArrayList r = dash.getStats();
		long ends = dash.getEnds();
		int days = dash.getDays();
		String constraint = dash.getConstraint();		
		XYDataset dataset = createLineChartDataset (r, ends, days);
		String title;
		if (dash.isSender())
		{
			if (constraint == null)
				title = "Messages Sent by Route";
			else
				title = "Messages Sent to " + constraint;
		}
		else
		{
			if (constraint == null)
				title = "Messages Received by PartyID";
			else
				title = "Messages Received from " + constraint;
		}
		JFreeChart chart = createLineChart(title, constraint, dataset);
    dash.setLinechart(getJFreeObject (chart, "line", 300, 250));
	}

	private XYSeriesCollection createLineChartDataset (ArrayList r, 
			long ends, long days)
	{
		ArrayList constraints = new ArrayList();
		XYSeriesCollection dataset = new XYSeriesCollection();

		// first time through just collect the constraint names
		for (int i = 0; i < r.size(); i++)
		{
			String[] item = (String[]) r.get(i);
			String k = (String) item[0];
			if (!constraints.contains(k))
			{
				constraints.add(k);
				dataset.addSeries(new XYSeries(k));
			}
		}
		long interval = days * MS;
		long start = ends - interval;
		interval /= 5;
		int[] counts = new int[constraints.size()];
		for (int i = 0; i < constraints.size(); i++)
			counts[i] = 0;
		for (int i = 0; i < r.size(); i++)
		{
			String[] item = (String[]) r.get(i);
			long d = Long.parseLong(item[1]);
			if (d > start)
			{
				addLineData (dataset, start, counts);
				start += interval;
			}
			int j = constraints.indexOf(item[0]);
			if (j >= 0)
			  counts[j]++;
		}
		addLineData (dataset, start, counts);
		while ((start += interval) < ends)
			addLineData (dataset, start, counts);
		return dataset;
	}
	
	private void addLineData (XYSeriesCollection data, long d, int[] values)
	{
		for (int i = 0; i < values.length; i++)
		{
			data.getSeries (i).add (d, values[i]);
			values[i] = 0;
		}
	}
	
	private JFreeChart createLineChart(String title, String constraint, 
			XYDataset dataset) 
	{
		// create the chart...
		JFreeChart chart = ChartFactory.createXYLineChart(
				title, 
				"Date/Time", // x axis label
				null, // y axis label
				dataset, // data
				PlotOrientation.VERTICAL, 
				true, // include legend
				true, // tooltips
				false // urls
		);
		XYPlot plot = chart.getXYPlot();
		// X axis shows dates
		plot.setDomainAxis(new DateAxis ());
		// if data has a constraint, hide every thing else
		if (constraint != null)	
		{
			chart.removeLegend();
			XYDataset data = plot.getDataset();
			Paint bg = plot.getBackgroundPaint();
			for (int i = 0; i < data.getSeriesCount(); i++)
			{
				if (!constraint.equals (data.getSeriesKey(i)))
					plot.getRenderer().setSeriesPaint(i, bg);					
				else
				{
					// get a color match...
					plot.getRenderer().setSeriesPaint(i, 
							DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE[i]);
					// reset plot range for this maximum
					double d = 0;
					for (int j = 0; j < data.getItemCount(i); j++)
					{
						if (d < data.getYValue(i, j))
							d = data.getYValue(i, j);
					}
					// add a bit for top margin
					d += d * 0.04;
					plot.getRangeAxis().setUpperBound(d);
				}
			}
		}
		return chart;
	}
	
	/************************ PIE chart ***********************************/

	public void getPieChart (DashBoardData dash)
	{
		ArrayList r = dash.getStats();
		ArrayList categories = new ArrayList();
		HashMap counts = new HashMap ();
		
		// count entries for each category
		for (int i = 0; i < r.size(); i++)
		{
			String[] item = (String[]) r.get(i);
			int v = 0;
			String k = item[0];
			if (counts.containsKey(k))
				v = Integer.parseInt((String) counts.get(k));
			else
				categories.add(k);
			counts.put(k, Integer.toString(v+1));
		}
    DefaultPieDataset dataset = new DefaultPieDataset ();
		for (int i = 0; i < categories.size(); i++)
    {
			String k = categories.get(i).toString();
    	dataset.setValue(k,Integer.parseInt(counts.get(k).toString()));
    }
    JFreeChart chart = createPieChart("Summary", dataset);
    dash.setPiechart(getJFreeObject (chart, "pie", 200, 175));
	}
 
  private JFreeChart createPieChart(String title, PieDataset dataset) {
    
    JFreeChart chart = ChartFactory.createPieChart(
    		title,          // chart title
        dataset,        // data
        false,          // legend
        true,						// tool tips
        false);					// urls

    PiePlot plot = (PiePlot) chart.getPlot();
    // add a URL map to this image for selection o f constraints
    plot.setURLGenerator(new StandardPieURLGenerator ("", "constraint"));
    // simple labels use less horizontal space
    plot.setSimpleLabels(true);
    return chart;   
  }	

	/**
	 * Get a PNG image/HTML map pair for storing in the dashboard data
	 * @param chart to use
	 * @param id for the map
	 * @param width of the image
	 * @param height of the image
	 * @return the PNG/map pair
	 */
	private Object[] getJFreeObject (JFreeChart chart, String id, int width,int height)
	{
		ChartRenderingInfo info = new ChartRenderingInfo(); 
		BufferedImage img = getJFreeImage (chart, width, height, info);
		Object[] o = { getPNG (img), ChartUtilities.getImageMap (id, info) };
		return o;
	}
	
	private byte[] getPNG (BufferedImage img)
	{
		try
		{
		  return ChartUtilities.encodeAsPNG(img, true, 3);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;	
	}
	
	private BufferedImage getJFreeImage (JFreeChart image, int width, int height, ChartRenderingInfo info)
	{
		image.getTitle().setFont(new Font("Times New Roman", Font.BOLD, 16));
		image.setBackgroundPaint(new Color (255, 255, 255, 0));
		image.setBackgroundImageAlpha(0.0f);
		return (image.createBufferedImage(width, height, info));
	}
}
