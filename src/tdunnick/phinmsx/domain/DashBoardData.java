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
 *  Foobar is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package tdunnick.phinmsx.domain;

import java.util.*;
import java.text.*;
import java.io.*;

/**
 * @author user
 *
 */
public class DashBoardData
{
	String version = null;
	String phinmsVersion = null;
	ArrayList tables = null;					// phinms table names
	String table = null;					// current table
	boolean isSender = true;		// true if table is a send Queue
	String constraint = null;					// table route or partyid constraint
	int days = 7;									// interval for statistics
	long ends = 0;          // ending date for interval
	String interval;				// approximate for reporting
	ArrayList stats = null;
	// reported stats
	int max = 0,						// messages per period
	    min = 0,
      total = 0;
	Object[] barchart, piechart, linechart;
  
  public DashBoardData (String version, String phinmsVersion)
	{
		super();
		this.version = version;
		this.phinmsVersion = phinmsVersion;
	}

	public String getVersion ()
  {
  	return version;
  }
  
  public String getPhinmsVersion ()
  {
  	return phinmsVersion;
  }
  
  public ArrayList getTables()
	{
		return tables;
	}

	public void setTables(ArrayList tables)
	{
		this.tables = tables;
	}

	public boolean isSender()
	{
		return isSender;
	}

	public void setSender(boolean isSender)
	{
		this.isSender = isSender;
	}

	public String getTable()
	{
		return table;
	}

	public void setTable(String table)
	{
		this.table = table;
	}

	public String getConstraint()
	{
		return constraint;
	}

	public void setConstraint(String constraint)
	{
		this.constraint = constraint;
	}

	public int getDays()
	{
		return days;
	}

	public void setDays (int days)
	{
		this.days = days;
	}

	public long getEnds()
	{
		return ends;
	}

	public String getDate ()
	{
		DateFormat fmt = new SimpleDateFormat ("MM/dd/yyyy");
		return fmt.format (new Date (ends));
	}
	
	public String getInterval()
	{
		return interval;
	}

	public void setInterval(String interval)
	{
		this.interval = interval;
	}

	public void setEnds (long ends)
	{
		this.ends = ends;
	}

	public ArrayList getStats()
	{
		return stats;
	}

	public void setStats(ArrayList stats)
	{
		this.stats = stats;
	}

	public int getTotal()
	{
		return total;
	}

	public void setTotal(int total)
	{
		this.total = total;
	}

	public int getMin()
	{
		return min;
	}

	public void setMin(int min)
	{
		this.min = min;
	}

	public int getMax()
	{
		return max;
	}

	public void setMax(int max)
	{
		this.max = max;
	}

	public void setVersion(String version)
	{
		this.version = version;
	}

	public void setPhinmsVersion(String phinmsVersion)
	{
		this.phinmsVersion = phinmsVersion;
	}

	public Object[] getBarchart()
	{
		return barchart;
	}

	public void setBarchart(Object[] barchart)
	{
		this.barchart = barchart;
	}

	public Object[] getPiechart()
	{
		return piechart;
	}

	public void setPiechart(Object[] piechart)
	{
		this.piechart = piechart;
	}

	public Object[] getLinechart()
	{
		return linechart;
	}

	public void setLinechart(Object[] linechart)
	{
		this.linechart = linechart;
	}
}
