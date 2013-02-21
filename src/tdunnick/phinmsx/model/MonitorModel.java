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

package tdunnick.phinmsx.model
;

import java.io.*;
import java.text.*;
import java.util.*;
import org.apache.log4j.*;
import java.sql.*;
import java.sql.Date;
import javax.servlet.http.*;

import tdunnick.phinmsx.domain.*;
import tdunnick.phinmsx.util.*;

/**
 * This is the model that prepares data for display in the dashboard and 
 * queue monitor.
 * Key items to track for display are:
 * <ul>
 * <li>table - current table</li>
 * <li>recordId - current record </li>
 * <li>top - current top record</li>
 * <li>route - current route for sender tables</li>
 * <li>partyId - current sender party ID for receiver tables<li>
 * <li>ends - date when dashboard data display ends</li>
 * <li>days - number of days shown in dashboard</li>
 * </ul>
 * 
 * 
 * @author Thomas Dunnick
 */
public class MonitorModel
{
	private String Version = "Monitor RO 0.10 08/08/2012";
	private Props props = null;
	private ArrayList tables = null;
	private Logger logger = null;
	
	private final static long MS = 24*60*60*1000;
	
	private final SimpleDateFormat dfmt = 				// PHIN-MS data format
		new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss");

	/*
	 * fields we show in the queue monitor table
	 */
	private static String[] routeFields =
	{ 
		"recordId", "payloadFile", "service", "action",
		"messageRecipient", "messageSentTime"
	};
	
	private static String[] workerFields = 
	{
		"recordId", "payloadName", "service", "action",
		"fromPartyId", "receivedTime" 
	};
	

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public MonitorModel ()
	{
		super();
	}
		
	public String getVersion()
	{
		return Version;
	}

	public void setVersion(String version)
	{
		Version = version;
	}

	/**
	 * do basic initialization... load and initialize properties and logging
	 * and refresh current configuration data
	 * 
	 * @param props properties for the monitor
	 * @return true if successful
	 */
	public boolean initialize (Props props)
	{
		boolean ok;
		this.props = props;
		logger = props.getLogger(null, false);
		ok = refresh ();
		return (ok);
	}

	/**
	 * clean up
	 */
	public void close ()
	{
		props.closeConnection ();
	}

	/**
	 * Generate the bean needed for the queue monitor view.
	 * 
	 * @param request received by the monitor
	 * @return a queue monitor bean
	 */
	public MonitorData getMonitorData (HttpServletRequest request)
	{
		setSession (request);
		MonitorData mon = new MonitorData (Version, Phinms.getVersion());		
		if (setMonitor (mon, setSession (request)))
		  return (mon);
		return null;
	}
	
	
	/**
	 * Generate the bean needed for the dashboard view.
	 * 
	 * @param request received by the monitor
	 * @return a dashboard bean
	 */
	public DashBoardData getDashBoardData (HttpServletRequest request)
	{
		DashBoardData dash = getDashBoardSession (request);
		if (setDashBoard (dash, setSession (request)))
		  return dash;
		return null;
	}
	
	/**
	 * Return an image found in the dashboard bean.  This make use of a 
	 * bean cache so that we don't continually regenerate the data used by
	 * the chart.  Instead generate it once (in getDashBoardData above) and
	 * look it up on the browser call back requests.
	 * 
	 * @param path of image from the browser
	 * @param request from the browser
	 * @return the image
	 */
	public byte[] getChart (String path, HttpServletRequest request)
	{
		DashBoardData dash = getDashBoardSession (request);
		Object[] c;
		if (path.indexOf ("bar") >= 0)
			c = dash.getBarchart ();	
		else if (path.indexOf ("line") >= 0)
			c = dash.getLinechart();
		else
			c = dash.getPiechart();
    if (c == null)
    	return (new byte[0]);
		return (byte[]) c[0];	
	}
	
	/************************** dashboard support *****************************/
	
	/**
	 * return a dashboard bean either from the cache, or a new one
	 * 
	 * @param request from browser used to search the cache
	 * @return a dashboard bean
	 */
	private DashBoardData getDashBoardSession (HttpServletRequest request)
	{
		DashBoardData dash = DashBoardCache.get(request.getSession());
		if (dash == null)
			dash = new DashBoardData(Version, Phinms.getVersion());
		return dash;
	}

	/**
	 * set a dashboard bean with all the data needed by the view, based on 
	 *  the session.  This includes the data, images, statistics, etc.
	 * 
	 * @param dash to set
	 * @param session used by this dash
	 * @return true if successful.
	 */
	private boolean setDashBoard (DashBoardData dash, HttpSession session)
	{
		// start out by pulling and setting session values
		String table = (String) session.getAttribute("table");
		if (table == null)
			table = getTransportName ();
		if (table == null)
			return false;
    dash.setSender(table.equals(getTransportName()));
		int days = getInt((String) session.getAttribute("days"));
		long ends = getDate((String) session.getAttribute("ends"));
		dash.setTables(tables);
		dash.setConstraint((String) session.getAttribute("constraint"));
		dash.setTable(table);
		if (days == 0)
			days = 365;
		dash.setDays(days);
		dash.setEnds(ends);
		// get our current stats and create the associate images
		setDashStats (dash, table, ends, days);
		Charts chart = new Charts();
		chart.getPieChart(dash);
		chart.getBarChart(dash);
		chart.getLineChart(dash);
		// finally save it in our cache for image recall
		DashBoardCache.put(session, dash);
		return true;
	}

	/**
	 * query the database to get the raw dashboard bean statistics
	 * 
	 * @param dash bean to set
	 * @param table to query
	 * @param ends when last item appears in statistics
	 * @param days statistics coveres
	 * @return
	 */
	private boolean setDashStats (DashBoardData dash, String table, 
			long ends, long days)
	{
		String constraintName = "routeInfo";
		String dateName = "messageReceivedTime";
		if (!table.equals(getTransportName()))
		{
			constraintName = "fromPartyId";
			dateName = "receivedTime";
		}
		try
		{
			StringBuffer buf = new StringBuffer();
			long interval = days * MS;
			long start = ends - interval;
			addConstraint (buf, dateName + " > ", dfmt.format (new Date(start)));
			addConstraint (buf, dateName + " <= ", dfmt.format(new Date(ends)));
			Statement stmt = props.query ("SELECT " + constraintName + "," 
					+ dateName + " FROM " + table	+ buf.toString() 
					+ " ORDER BY " + dateName + " ASC", 0);
			if (stmt == null)
				return false;
		  ResultSet res = stmt.getResultSet();
		  ArrayList data = new ArrayList();
		  String constraint = dash.getConstraint();
			int n = 0, min = Integer.MAX_VALUE, max = 0, total = 0;
			interval /= 5;
			if (interval < MS)
				dash.setInterval("" + (interval * 24 / MS) + " hour");
			else if (interval / MS < 30)
				dash.setInterval("" + (interval / MS) + " day");
			else if (interval / MS < 150)
				dash.setInterval("" + (interval / (MS * 7)) + " week");
			else
				dash.setInterval("" + (interval / (MS * 30)) + " month");
			start += interval;
		  while (res.next())
		  {
		  	try
		  	{
			  	String s = res.getString(1);
			  	long t = dfmt.parse(res.getString(2)).getTime();
			  	if (t > start)
			  	{
			  		if (min > n) min = n;
			  		if (max < n) max = n;
			  		total += n;
			  		n = 0;
			  		start += interval;
			  	}
			  	String d = "" + t;
			  	data.add (new String[]{s,d});
			  	if ((constraint == null) || constraint.equals(s))
			  	  n++;
		  	}
		  	catch (Exception e)
		  	{
		  		logger.error("result fetch " + e.getMessage());
		  	}
		  }
  		if (min > n) min = n;
  		if (max < n) max = n;
  		total += n;
		  res.close();
		  stmt.close();
		  dash.setStats(data);
		  dash.setMin (min);
		  dash.setMax(max);
		  dash.setTotal(total);
		}
		catch (SQLException e)
		{
			logger.error("ERROR: result set - " + e.getMessage());
			return false;
		}
		return true;
	}
	
  /************************* queue monitor ********************************/
	
	/**
	 * set the data in the queue monitor bean needed for display based on
	 * criteria found in the session.
	 * 
	 * @param mon bean to set
	 * @param session for the request
	 * @return true if successful
	 */
	private boolean setMonitor (MonitorData mon, HttpSession session)
	{
		String s;
		mon.setTables(tables);
		mon.setRecordId(getInt((String) session.getAttribute ("recordId")));
		mon.setTop(getInt((String) session.getAttribute ("top")));
		mon.setConstraint((String) session.getAttribute("constraint"));
		if ((s = (String) session.getAttribute("table")) == null)
			s = getTransportName(); 
		mon.setTable(s);
		return (getData (mon));
	}
	
	/**
	 * query the database and retrieve the information needed by the
	 * queue view for display including a summary table and details for the
	 * selected record.
	 * 
	 * @param mon to fill in with the data
	 * @return true if successful
	 */
	private boolean getData (MonitorData mon)
	{
		String constraintName = "routeInfo=";
		String t = mon.getTable();
		if (t == null)
			return false;
		if (t.equals(getTransportName()))
			mon.setRowfields (new ArrayList (Arrays.asList(routeFields)));
		else
		{
			mon.setRowfields (new ArrayList (Arrays.asList(workerFields)));
			constraintName = "fromPartyId=";
		}
		if (!setPrevTop (mon))
			return false;
		try
		{
			StringBuffer buf = new StringBuffer();
			int top = mon.getTop();
			if (top > 0)
			  addConstraint (buf, "recordId<=", Integer.toString (top));
			addConstraint (buf, constraintName, mon.getConstraint());
			logger.debug("constraint: " + buf.toString());
			Statement stmt = props.query ("SELECT * FROM " + mon.getTable()
					+ buf.toString() + " ORDER BY recordId DESC", 10);
			if (stmt == null)
				return false;
		  ResultSet res = stmt.getResultSet();
		  mon.setFields(getFieldNames (res));
		  setMonitorRows (mon, res);
		  res.close();
		  stmt.close();
		}
		catch (SQLException e)
		{
			logger.error("ERROR: result set - " + e.getMessage());
			return (false);
		}
	  return true;
	}
	
  /**
   * move data from a JDBC result set into a queue monitor bean
   * @param mon bean to update
   * @param res result set from JDBC
   * @return true if successful
   * @throws SQLException
   */
  private boolean setMonitorRows (MonitorData mon, ResultSet res)
  throws SQLException
  {
		ArrayList fields = mon.getFields();
		ArrayList rowfields = mon.getRowfields();
	  ArrayList rows = new ArrayList ();
	  ArrayList rowClass = new ArrayList ();
	  while (res.next ())
		{
	  	int recordId = 0;
	  	String status = null;
	  	ArrayList values = new ArrayList();
			ArrayList record = new ArrayList();
			for (int i = 0; i < fields.size(); i++)
			{
				String s = (String) fields.get(i);
				String v = res.getString(s);
				record.add(v);
				if (v != null) 
					{
					if ((status == null) && v.indexOf ("attempted") >= 0)
						status = "attempted";
						else if (v.indexOf ("fail") >= 0)
						status = "fail";
					}
				for (int j = 0; j < rowfields.size(); j++)
				{
					if (s.equals((String) rowfields.get(j)))
						values.add(v);
				}
				if (s.equalsIgnoreCase("recordid"))
				{
					recordId = Integer.parseInt(v);
				}
			}
		  if (mon.getRecordId() == 0)
		  	mon.setRecordId(recordId);
		  if (mon.getRecordId() == recordId)
		  	mon.setRecord(record);
			rows.add(values);
			if (status == null)
				status = "ok";
			rowClass.add(status);
		}
	  
	  mon.setRows(rows);
	  mon.setRowClass(rowClass);
  	return true;
  }
  
  /**
   * query the database to determine what the record ID should be for the
   * previous page based on the current location, and note that in the bean
   * for display.
   * 
   * @param mon bean to set
   * @return true if successful
   */
  private boolean setPrevTop (MonitorData mon)
  {
		int top = mon.getTop(),
		    prev = 0;
		mon.setPrev (0);
		if (top == 0)
			return true;
		StringBuffer buf = new StringBuffer();		
		addConstraint (buf, "recordId>=", Integer.toString (top));
		String constraintName = "routeInfo=";
		if (!mon.getTable().equals(getTransportName()))
			constraintName = "fromPartyId=";
		addConstraint (buf, constraintName, mon.getConstraint());
		addConstraint (buf, "fromPartyId=", null);
		try
		{
	  	Statement stmt = props.query ("SELECT recordId FROM " 
	    		+ mon.getTable() + buf.toString() + " ORDER by recordId ASC", 10);
			if (stmt == null)
				return true;
		  ResultSet res = stmt.getResultSet();
		  while (res.next())
		  	prev = Integer.parseInt(res.getString (1));
		  res.close();
		  stmt.close();
		}
		catch (SQLException e)
		{
			logger.error("ERROR: result set - " + e.getMessage());
			return false;
		}
		logger.debug("prev=" + prev);
		if (prev > top)
		  mon.setPrev(prev);
		return true;
  }
  	
	/**
	 * builds an SQL WHERE clause based on the given constraint as found in the
	 * session.
	 * 
	 * @param buf used to build the clause
	 * @param expr left side of clause
	 * @param constraint right side of clause
	 * @return true if successful
	 */
	private boolean addConstraint (StringBuffer buf, String expr, String constraint)
	{
		if ((constraint == null) || (constraint.length() == 0))
			return false;
		if (buf.length() == 0)
			buf.append(" WHERE " + expr);
		else
			buf.append(" AND " + expr);
		if (constraint.matches("[0-9]+") && (Integer.parseInt(constraint) > 0))
			buf.append(constraint);
		else
			buf.append("'" + constraint + "'");
		return true;
	}
	
	/**
	 * convert a JDBC result set to a list of column names.
	 * 
	 * @param res result set
	 * @return the column names
	 * @throws SQLException
	 */
	private ArrayList getFieldNames (ResultSet res)  throws SQLException
	{
	  ResultSetMetaData m = res.getMetaData();
	  ArrayList names = new ArrayList ();
	  for (int i = 1; i <= m.getColumnCount(); i++)
	  {
	  	if (m.isSearchable(i)) names.add (m.getColumnName(i));
	  	else logger.debug("skipping " + m.getColumnName(i));
	  }
	  return names;
	}
	
	/**
	 * re-open the dB connection and reload table found in the configuration
	 * files
	 * 
	 * @return true if successful
	 */
	public boolean refresh ()
	{
		if (!openConnection () || !setTables ())
		{
			logger.error("Failed initializing Monitor tables");
			return false;
		}
		return true;
	}

	/**
	 * gets a list of worker (receiver) queues from the receiver configuration
	 * 
	 * @return the list
	 */
	private ArrayList getWorkers ()
	{
		return getXMLprops (props.getProperty (Props.QUEUEMAP), 
				"QueueMap.workerQueue", "tableName");
	}
	
	/**
	 * get cheap object with the transport table name and route map XML
	 * properties.
	 * @return the object
	 */
	private Object[] getTransport ()
	{
		String s = props.getProperty (Props.SENDERXML);
		if (s == null)
			s = Phinms.getPath("config") + "/sender/sender.xml";
		XmlContent sxml = getXML (s);
		if (sxml == null)
			return null;
		Object[] transport = new Object[2];
		String dir = sxml.getValue("Sender.installDir");
		transport[1] = getXMLprops (dir + sxml.getValue("Sender.routeMap"),	
				"RouteMap.Route", "Name");
		transport[0] = sxml.getValue("Sender.messageTable");
		return (transport);
	}
	
	/**
	 * extract the sender's transport queue name from our cheap object above
	 * @return
	 */
	private String getTransportName ()
	{
		if (tables == null)
			return null;
		return (String) ((Object[]) tables.get(0))[0];
	}
	
	/**
	 * set our list of sender and receiver tables, including routes for
	 * the sender and party ID's for the receiver.
	 * @return true if successful
	 */
	private boolean setTables ()
	{
		tables = new ArrayList ();
		tables.add (getTransport());
		ArrayList workers = getWorkers ();
		for (int i = 0; i < workers.size(); i++)
		{
			Object[] entry = new Object[2];
			entry[0] = workers.get(i);
			entry[1] = getParties ((String) entry[0]);
			tables.add (entry);
		}
		return true;
	}
	
	/**
	 * query the database to get a list of incoming party ID for a receiver queue
	 * 
	 * @param table name of the queue
	 * @return list of party id's
	 */
	private ArrayList getParties (String table)
	{
		ArrayList parties = new ArrayList();
		try
		{
			Statement stmt = props.query("SELECT distinct (fromPartyId) from " 
				+ table, 0);
			ResultSet res = stmt.getResultSet();
			while (res.next ())
			{
				parties.add(res.getString (1));
			}
			res.close();
			stmt.close();
		}
		catch (SQLException e)
		{
			logger.error("Error reading party ID list");
		}
		return parties;
	}
	
	/**
	 * convert a set of properties in XML to a list
	 * 
	 * @param xname of XML file
	 * @param (repeated) prefix to that property
	 * @param suffix for specific property in the list
	 * @return
	 */
	private ArrayList getXMLprops (String xname, String prefix, String suffix)
	{
		XmlContent x = getXML (xname);
		if (x == null)
			return null;
		if ((prefix == null) || (suffix == null))
		{
			logger.error("null prefix or suffix for " + xname);
		  return null;
		}
		ArrayList l = new ArrayList ();
		for (int i = 0; i < x.getTagCount(prefix); i++)
			l.add (x.getValue(prefix + "[" + i + "]." + suffix));
		logger.debug("found " + l.size() + " entries for " + prefix + " in " + xname);
		return l;
	}

	/**
	 * reads an XML configuration
	 * 
	 * @param name of the configuration
	 * @return the XML
	 */
	private XmlContent getXML (String name)
	{
		if (name == null)
		{
			logger.error("null XML name");
			return null;
		}
		File f = new File (name);
		if (!f.canRead())
		{
			logger.error("can't read " + name);
			return null;
		}
		XmlContent x = new XmlContent ();
		if (!x.load(f))
			logger.error("failed parsing " + name);
		return x;
	}
	
	private boolean openConnection ()
	{
		props.closeConnection ();
		return (props.getConnection() != null);
	}
		
	/**
	 * safe integer parse
	 * @param value to parse
	 * @return 0 if fails
	 */
	private int getInt (String value)
	{
		try
		{
			return (Integer.parseInt(value));
		}
		catch (Exception e)
		{
			return 0;
		}
	}
	
	/**
	 * safe date parse
	 * @param value of date in seconds since the EPOCH
	 * @return the seconds since the EPOCH or now if fails
	 */
	private long getDate (String value)
	{
		java.util.Date d;
		try
		{
			d = new java.util.Date (Long.parseLong(value));
		}
		catch (Exception e)
		{
			logger.error("date value: " + value);
			d = new java.util.Date();
		}
		logger.debug("Date: " + d.toString());
		return d.getTime();
	}
	

	/******************** session data management *******************/
	
	private HttpSession setSession (HttpServletRequest request)
	{
		HttpSession session = request.getSession();
		setSessionAttribute (session, request, "table");
		setSessionAttribute (session, request, "constraint");
		setSessionAttribute (session, request, "recordId");
		setSessionAttribute (session, request, "top");
		setSessionAttribute (session, request, "days");
		setSessionAttribute (session, request, "ends");
		return (session);
	}
		
	private void setSessionAttribute (HttpSession s, HttpServletRequest r, String a)
	{
		String v = r.getParameter(a);
		if ((v == null) || (v.length() == 0))
			return;
		s.setAttribute (a, v);
		if (a.equals("days") || a.equals("ends") || a.equals("recordId"))
			return;
		if (a.equals("top"))
		{
			s.setAttribute ("recordId", v);
		}
		else
		{
			s.removeAttribute("recordId");
			s.removeAttribute("top");
			if (a.equals("table"))
				s.removeAttribute("constraint");
		}
	}
}
