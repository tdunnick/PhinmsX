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
import java.util.*;
import java.util.logging.*;
import java.sql.*;
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
		logger = props.getLogger ();
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
		return getChart (path, getDashBoardSession (request));
	}
	
	/**
	 * Return an image found in the dashboard bean.  This make use of a 
	 * bean cache so that we don't continually regenerate the data used by
	 * the chart.  Instead generate it once (in getDashBoardData above) and
	 * look it up on the browser call back requests. Here assume session id
	 * is embedded in img path.
	 * 
	 * @param path to image
	 * @return the image
	 */
	public byte[] getChart (String path)
	{
		String id = path.replaceFirst("^.*_(.*)[.]png$", "$1");
		return getChart (path, getDashBoardSession (id));
	}
	
	/**
	 * Return an image found in a dashboard bean. 
	 * @param path to image
	 * @param dash bean
	 * @return the image
	 */
	public byte[] getChart (String path, DashBoardData dash)
	{
		Object[] c;
		if (path.indexOf ("bar") >= 0)
			c = dash.getBarchart ();	
		else if (path.indexOf ("line") >= 0)
			c = dash.getLinechart();
		else
			c = dash.getPiechart();
    if (c == null)
    {
    	return (new byte[0]);
    }
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
		return getDashBoardSession (request.getSession().getId());
	}

	/**
	 * return a dashboard bean either from the cache, or a new one
	 * 
	 * @param id from browser used to search the cache
	 * @return a dashboard bean
	 */
	private DashBoardData getDashBoardSession (String id)
	{
		DashBoardData dash = DashBoardCache.get(id);
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
		{
			logger.severe("No transport table specified or found");
			return false;
		}
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
		Charts chart = new Charts(logger);
		chart.getPieChart(dash);
		chart.getBarChart(dash);
		chart.getLineChart(dash);
		// finally save it in our cache for image recall
		DashBoardCache.put(session.getId(), dash);
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
			addConstraint (buf, dateName + " > ", Phinms.fmt_date (start));
			addConstraint (buf, dateName + " <= ", Phinms.fmt_date (ends));
			Statement stmt = props.query ("SELECT recordId," + constraintName + "," 
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
			  	String s = res.getString(2);
			  	if (s == null)
			  	{
			  		logger.warning ("Record " + res.getString(1) + " missing " 
			  				+ constraintName + " in " + table);
			  		continue;
			  	}
			  	long t = Phinms.get_time (res.getString(3));
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
		  		logger.severe("result fetch exception - " + e.getMessage());
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
			logger.severe("result set exception - " + e.getMessage());
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
		if ((s = (String) session.getAttribute("table")) == null)
			s = getTransportName(); 
		mon.setTable(s);
		mon.setConstraint((String) session.getAttribute("constraint"));
		mon.setTop(getInt((String) session.getAttribute ("top")));
		mon.setRecordId(getInt((String) session.getAttribute ("recordId")));
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
		{
			logger.severe ("Can't get data - No table set");
			return false;
		}
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
			logger.finest("constraint: " + buf.toString());
			t = "SELECT * FROM " + t + buf.toString() + " ORDER BY recordId DESC";
			Statement stmt = props.query (t, 10);
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
			logger.severe("result set exception for " + t + " - " + e.getMessage());
			return (false);
		}
	  return true;
	}
	
  /**
   * move data from a JDBC result set into a queue monitor bean
   * @param mon bean to update
   * @param res result set from JDBC
   * @return true if successful
   */
  private boolean setMonitorRows (MonitorData mon, ResultSet res)
  {
		ArrayList fields = mon.getFields();
		ArrayList rowfields = mon.getRowfields();
	  ArrayList rows = new ArrayList ();
	  ArrayList rowClass = new ArrayList ();
	  int rownum = 0;
	  String colname = "";
	  try
	  {
		  while (res.next ())
			{
		  	int recordId = 0;
		  	String status = null;
		  	ArrayList values = new ArrayList();
				ArrayList record = new ArrayList();
				rownum++;
				for (int i = 0; i < fields.size(); i++)
				{
					colname = (String) fields.get(i);
					Object o = res.getObject(colname);
					String v = null;
					// skip null or binary data
					if ((o != null)	&& !o.getClass().getName().equals("[B"))
						v = o.toString();
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
						if (colname.equalsIgnoreCase ((String) rowfields.get(j)))
							values.add(v);
					}
					if (colname.equalsIgnoreCase("recordid"))
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
	  }
	  catch (SQLException e)
	  {
	  	logger.severe("failed setting monitor row " + rownum + " " + colname 
	  			+ " - " + e.getLocalizedMessage());
	  	return false;
	  }
	  logger.finest("set " + rows.size() + " monitor queue rows");
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
		String s = "routeInfo=";
		if (!mon.getTable().equals(getTransportName()))
			s = "fromPartyId=";
		addConstraint (buf, s, mon.getConstraint());
		addConstraint (buf, "fromPartyId=", null);
		try
		{
			s = "SELECT recordId FROM " + mon.getTable() + buf.toString() 
			  + " ORDER by recordId ASC";
	  	Statement stmt = props.query (s, 10);
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
			logger.severe("result set exception for " + s + " - " + e.getMessage());
			return false;
		}
		logger.finest("prev=" + prev);
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
		{
			return false;
		}
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
	  	else logger.finest("skipping " + m.getColumnName(i));
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
			logger.severe("Failed initializing Monitor tables");
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
		return getXMLprops (props.getProperty (Phinms.QUEUEMAP), 
				"QueueMap.workerQueue", "tableName");
	}
	
	/**
	 * get cheap object with the transport table name and route map XML
	 * properties.
	 * @return the object
	 */
	private Object[] getTransport ()
	{
		String s = props.getProperty (PhinmsX.SENDERXML);
		if (s == null)
			s = Phinms.getPath("config") + "/sender/sender.xml";
		XmlContent sxml = getXML (s);
		if (sxml == null)
		{
			logger.severe("Couldn't load " + s);
			return null;
		}
		Object[] transport = new Object[2];
		String dir = sxml.getValue("Sender.installDir");
		// 2.7.0
		s = sxml.getValue("Sender.messageTable");
		// 2.8.x
		if (s == null)
			s = sxml.getAttribute("Sender.transportDatabasePool.transportDatabase.transportQueue", "tableName");
		if (s == null)
		{
			logger.severe ("Can't find transport table name");
			return null;
		}
		transport[0] = s;
		transport[1] = getXMLprops (dir + sxml.getValue("Sender.routeMap"),	
				"RouteMap.Route", "Name");
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
		String s = "SELECT distinct (fromPartyId) from " + table;
		try
		{
			Statement stmt = props.query(s, 0);
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
			logger.severe("Failed " + s + " - " + e.getMessage());
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
			logger.severe("null prefix or suffix for " + xname);
		  return null;
		}
		ArrayList l = new ArrayList ();
		for (int i = 0; i < x.getTagCount(prefix); i++)
			l.add (x.getValue(prefix + "[" + i + "]." + suffix));
		logger.finest("found " + l.size() + " entries for " + prefix + " in " + xname);
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
			logger.severe("null XML name");
			return null;
		}
		File f = new File (name);
		if (!f.canRead())
		{
			logger.severe("can't read " + name);
			return null;
		}
		XmlContent x = new XmlContent ();
		if (!x.load(f))
			logger.severe("failed parsing " + name);
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
		java.util.Date d = new java.util.Date();
;
		if ((value != null) && value.matches("[0-9]+"))	try
		{
			d = new java.util.Date (Long.parseLong(value));
		}
		catch (Exception e)
		{
			logger.severe ("date value: " + value);
		}
		return d.getTime();
	}
	

	/******************** session data management *******************/
	
	/**
	 * Add any request parameters to the session data.  Reset sub-parameter -
	 * for example a change in the table voids everything else.  However,
	 * we set sub-paramters last so they can still be included on a RESTful
	 * URL
	 * 
	 * @param request
	 * @return
	 */
	private HttpSession setSession (HttpServletRequest request)
	{
		HttpSession session = request.getSession();
		String[] parm = { "table", "constraint", "top", "recordId", "days", "ends" };
		for (int i = 0; i < parm.length; i++)			
		{
			String v = request.getParameter(parm[i]);
			if ((v == null) || (v.length() == 0))
				continue;
			logger.finest("setting session " + parm[i] + "=" + v);
			session.setAttribute (parm[i], v);
			if (i == 2) // "top"
			{
				session.setAttribute ("recordId", v);
			}
			else if (i < 2) // "table" or "constraint"
			{
				session.removeAttribute("recordId");
				session.removeAttribute("top");
				if (i == 0) // "table"
					session.removeAttribute("constraint");
			}
		}
		return session;
	}
}
