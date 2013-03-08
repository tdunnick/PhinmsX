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
package tdunnick.phinmsx.domain;

import java.io.*;
import java.text.*;
import java.util.*;
import java.net.URL;
import java.sql.*;
import java.util.logging.*;

import tdunnick.phinmsx.crypt.*;
import tdunnick.phinmsx.util.*;
/**
 * Definitions and shared methods for managing PHIN-MS properties.
 * This normally uses PHIN-MS configuration files, but may stand-alone.
 * However, it expects the configuration to mirror those found in
 * PHIN-MS (other than the root, which may be anything).
 * 
 * @author tld tdunnick@wisc.edu
 *
 */
public class Props
{
	private String propRoot = null;
	private XmlContent props = null;
	private Passwords passwords = null;
	private Logger logger = null;
	private String tableName = null;
	Connection conn = null;
		
	
	/**
	 * on start up we at least need a default logger
	 */
	public Props()
	{
		logger = Logger.getLogger("");
	}

	/**
	 * Load a configuration and initial the properties.  Will merge
	 * with PHIN-MS receiver properties.
	 * 
	 * @param conf name of configuration file.
	 * @return true if successful
	 */
	public boolean load (String conf)
	{
		return load (conf, null);
	}
	
	public boolean close ()
	{
		closeConnection ();
		closeLog ();
		return true;
	}
	
	/**
	 * Load a configuration and initial the properties and merge
	 * with given properties.  This does a lot of the initialization
	 * and auto-configuration as follows:
	 * <ul>
	 * <li>Get a temporary (console) logger and the calling Class name (owner)</li>
	 * <li>If a configuration is given, load it, otherwise create one</li>
	 * <li>Set up the path (and version) to PHIN-MS if possible</li>
	 * <li>Reconfigure and set up the permanent logger</li>
	 * <li>Merge in given, or default properties</li>
	 * <li>Make sure Tomcat has a temp directory</li>
	 * <li>Make sure we have a temp directory</li>
	 * <li>If we are a receiver, set specifics - this should probably
	 * be moved to the receiver</li>
	 * </ul>
	 * 
	 * @param conf name of configuration file.
	 * @param r properties to merge with
	 * @return true if successful
	 */
	public boolean load (String conf, XmlContent r)
	{
		String owner = getParentClassName ();
		String s = null;
		File d = null;
		
		logger.fine ("Loading configuration for " + owner);
		
		// if there is no configuration given, try the default
		if (conf == null)
		{
			d = new File (PhinmsX.getContextPath()
					+ PhinmsX.PHINMSXPATH + "/config/" + owner + ".xml");
		  logger.info("trying configuration " + d.getAbsolutePath());
			if (d.canRead()) 
				conf = d.getAbsolutePath();
		}
		// if no configuration then create one
		if (conf == null)
		{
			logger.info("auto configuring...");
			props = new XmlContent ();
			props.createDoc();
			propRoot = owner;
	  }
		else
		{
			logger.info("configuring from " + conf);
			if (((props = getProps(conf)) == null)
					|| ((propRoot = props.getRoot()) == null))
			{
				logger.severe("Unable to load properties from " + conf);
				return false;
			}
		}
		propRoot += ".";
		
		logger.finest ("Properties intialized");
		/*
		 * make sure we have a default file location
		 */
		if (getProperty (PhinmsX.PHINMSX) == null)
		{
			d = new File (PhinmsX.getContextPath() + PhinmsX.PHINMSXPATH);
			setProperty (PhinmsX.PHINMSX, d.getPath());
		}
		
		logger.finest ("Context set to " + getProperty (PhinmsX.PHINMSX));
		
		/*
		 * if PHIN-MS hasn't been identified, then try to set it
		 * up by looking at various configuration settings and finally
		 * from our own context.
		 */
		if (Phinms.getPath (null) == null)
		{
			String p = Phinms.setPath(getProperty (PhinmsX.PHINMS));
			if (p == null)
				p = Phinms.setPath(getProperty (PhinmsX.RECEIVERXML));
			if (p == null)
				p = Phinms.setPath(getProperty (PhinmsX.SENDERXML));
			if (p == null)
 			  Phinms.setPath(PhinmsX.getContextPath());
		}
		
		/*
		 * set up a logging - note done BEFORE merging so that each
		 * configuration will have it's own logging unless discretely
		 * specified with at least a LOGCONTEXT.
		 */
		if (getProperty (Phinms.LOGDIR) == null)
		{
			d = new File (getProperty (PhinmsX.PHINMSX) + "/logs");
			if (!(d.exists() || d.mkdirs ()))
				logger.severe ("Can't create " + d.getPath ());
			if (d.isDirectory())
				setProperty (Phinms.LOGDIR, d.getAbsolutePath());			
		}
		if (getProperty (Phinms.LOGCONTEXT) == null)
			setProperty (Phinms.LOGCONTEXT, owner);
		if (getProperty (Phinms.LOGNAME) == null)
			setProperty (Phinms.LOGNAME, owner);
		if (getProperty (Phinms.LOGLEVEL) == null)
			setProperty (Phinms.LOGLEVEL, "INFO");
		setLogger (null);
		
		/*
		 * if no properties were given, then merge by default 
		 * with receiver.xml
		 */
		if (r == null)
		{
			// get receiver.xml
			s = getProperty(PhinmsX.RECEIVERXML);
			if (s == null)
				s = Phinms.getPath("config") + "/receiver/receiver.xml";
			if ((r = getProps(s)) == null)
				logger.severe ("Can't load " + s);
		}
		if ((r != null) && !props.merge (r, false))
		{
		  logger.severe ("Failed merging properties from " + r.getRoot());
			return false;
		}
		passwords = loadPasswords ();

		/*
		 * make sure we have a temp folder for the JVM
		 * on tomcat this should be CATALINA_TMPDIR which often does not
		 * (yet) exist
		 */
		if ((s = System.getProperty("java.io.tmpdir")) == null)
		{
			logger.severe ("No temporary directory set for the JVM");
		}
		else
		{
			d = new File (s);
			if (!(d.exists() || d.mkdirs()))
			{
				logger.severe ("can't create " + d.getPath());
			}
		}

		/*
		 * set up a temp directory
		 */
		if ((s = getProperty (PhinmsX.TEMPDIR)) == null)
		{
			s = getProperty (PhinmsX.PHINMSX) + "/temp";
			setProperty (PhinmsX.TEMPDIR, s);
		}
		d = new File (s);
		if (!(d.exists() || d.mkdirs()))
		{
			logger.severe ("can't create " + d.getPath());
			setProperty (PhinmsX.TEMPDIR, null);
		}
		logger.finest("TEMPDIR " + s);
		
		/*
		 * finally the status cache and default queue for the receiver
		 */
		if (owner.equals("Receiver"))
		{
			if (getProperty(PhinmsX.STATUS) == null)
				setProperty(PhinmsX.STATUS, getProperty(Phinms.LOGDIR) + "/status.bin");
			if (getProperty(PhinmsX.QUEUENAME) == null)
				props.setValue(propRoot + PhinmsX.QUEUENAME, PhinmsX.DFLTQUEUE);
		}
		return true;
	}
	
	/**
	 * Load passwords for a given configuration.  Try various tags
	 * for the password file, key, and seed values before giving up.
	 * 
	 * @return the passwords or null if can't be loaded.
	 */
	public Passwords loadPasswords ()
	{
		Passwords pw = new Passwords ();
		if (!pw.load (props))
		{
			logger.severe ("Can't load passwords from " 
					+ getProperty (Phinms.PASSWORDFILE));
			return null;
		}
		return pw;
	}

	/**
   * get the class name of whomever created this instance to use
   * as the "owner" of this set of properties.
   * @return
   */
  private String getParentClassName ()
  {
  	String s = getClass().getName();
  	Exception e  = new Exception ();
  	StackTraceElement[] t = e.getStackTrace();
  	for (int i = 0; i < t.length; i++)
  	{
  		// System.out.println ("[" + i + "] " + t[i].getClassName());
  		if (!t[i].getClassName().equals(s))
  		{
  			s = t[i].getClassName();
  			i = s.lastIndexOf('.');
  			return (s.substring(i + 1));
  		}
  	}
  	return null;
  }
  
	/**
	 * get the name of the table for this QUEUENAME
	 * @return
	 */
	public String getTableName ()
	{
		if (tableName != null)
		  return tableName;
		String queue = getProperty (PhinmsX.QUEUENAME);
		String mapname = getProperty (Phinms.QUEUEMAP);
		if ((queue == null) || (mapname == null))
			return null;
		// logger().debug("Loading queue map " + mapname);
		XmlContent map = new XmlContent ();
		if (!map.load (new File (mapname)))
			return null;
		for (int i = 0; i < map.getTagCount("QueueMap.workerQueue"); i++)
		{
			String prefix = "QueueMap.workerQueue[" + i + "].";
			if (queue.equals(map.getValue(prefix + "queueId")))
				return tableName = map.getValue(prefix + "tableName");
		}
		return null;
	}
	
	/**
	 * Convenience to get the XML configuration - most likey never used.
	 * @return current XML properties
	 */
	public XmlContent getProps ()
	{
		return props;
	}
	
	public boolean merge (Props p, boolean replace)
	{
		return props.merge (p.getProps(), replace);
	}
	
	/**
	 * Retrieve a default property.  Note, most properties should
	 * come from the request environment which includes these...
	 * 
	 * @param name of property sans prefix
	 * @return value or null if not found
	 */
	public String getProperty (String name)
	{
		if ((props == null) || (propRoot == null) || (name == null))
			return (null);
		String s = props.getValue (propRoot + name);
		if ((s != null) && (s.length() == 0))
		  s = null;
		return s;
	}

	/**
	 * Get the password or userid for a given tag in a configuration
	 * @param name of the tag property
	 * @return
	 */
	public String getPassword (String name)
	{
		if (passwords == null)
		  return null;
		String s = getProperty (name);
		if (s == null)
			return null;
		return passwords.get(s);
	}
	
	/**
	 * Set a default property.  
	 * @param name of property sans prefix
	 * @param value to set
	 */
	public void setProperty (String name, String value)
	{
		logger.finest("Set " + name + " to " + value);
		if ((props != null) && (propRoot != null))
		{
		  if (!props.setValue (propRoot + name, value))
		  	logger.severe("Can't set " + propRoot + name + " to " + value);
		}
		else
			logger.warning ("Can't set " + name + " to " + value);
	}

	/**
	 * Load an XML properties files
	 * 
	 * @param name of file to load
	 * @return the properties
	 */
	private XmlContent getProps(String name)
	{
		if (name == null)
			return null;
		// System.out.println ("Reading " + name);
		File f = new File(name);
		if (!f.exists())
			return (null);
		XmlContent xml = new XmlContent ();
		if (!xml.load(f))
		{
			logger.severe("Failed loading " + name + " - " + xml.getError());
			return null;
		}
		return xml;
	}
	
	
	/**************************** logging ********************************/


	/**
	 * return our default logger
	 * @return
	 */
	public Logger getLogger ()
	{
		return logger;
	}
	
	/**
	 * close the handler(s) for this logger
	 * @return
	 */
	public boolean closeLog ()
	{
		if ((logger != null) && (logger != Logger.getLogger("")))
		{
			Handler[] h = logger.getHandlers();
			for (int i = 0; i < h.length; i++)
			{
				h[i].close();
				logger.removeHandler(h[i]);
			}
			logger.setLevel(Level.OFF);
		}
		return true;
	}

	/**
	 * Set up the file handler for this logger if needed.
	 * 
	 * @param l logger to set up
	 * @return true if successful
	 */
	public boolean setHandler (Logger l)
	{
		if (l == null)
			return false;
		String s = getProperty (Phinms.LOGDIR);

		// if no log dir or debug is set us the parent (root) handler
	  if (s == null)
	  {
	  	logger.warning ("No LOGDIR set");
      return false;
	  }
	  
	  // get or create the log name
		String logName = getProperty(Phinms.LOGNAME);
		if ((logName == null) || (logName.length() == 0))
			logName = "PhinmsX";
		logName = s + "/" + logName;
		
		int numlogs = 10; // archive 10 logs
		int limit = 0x100000; // 1M default size
		
	  // do we archive
	  if ((s = getProperty (Phinms.LOGARCHIVE))!= null)
		{
			if (!s.toLowerCase().equals("true"))
				numlogs = 0;
			else if (s.matches("[0-9]+"))
				numlogs = Integer.parseInt(s);
			if ((s = getProperty(Phinms.LOGSIZE)) != null)
			{
				if (s.matches("[0-9]+"))
					limit = Integer.parseInt(s);
			}
		}
	  try
		{
			FileHandler f;
			if (numlogs > 0)
				f = new FileHandler(logName + ".%g.txt", limit, numlogs, true);
			else
				f = new FileHandler(logName + ".txt", false);
			f.setFormatter(new PhinmsXFormatter ());
			l.addHandler(f);
			l.setLevel(getLevel());
			return true;
		}
	  catch (IOException e)
	  {
	  	return false;
	  }
	}
	
	/**
	 * force the use of this logger or create a new one if needed.
	 * We use the Level to determine if this logger has been previously
	 * set up and open
	 * 
	 * @param logger
	 * @return logger
	 */
	public Logger setLogger (Logger newlogger)
  {
		if (newlogger != null)
			return logger = newlogger;
		String s = getProperty (Phinms.LOGDEBUG);
		boolean debug = ((s != null) && s.equalsIgnoreCase("TRUE"));
		s = getProperty (Phinms.LOGCONTEXT);
		if (s == null)
		{
			logger.warning ("No LOGCONTEXT set - using root logger");
			return logger;
		}
		if ((newlogger = LogManager.getLogManager().getLogger(s)) != null)
		{
			if (!newlogger.getLevel().equals(Level.OFF))
			  return logger = newlogger;
		}
		else
		{
		  newlogger = Logger.getLogger(s);
		}
		newlogger.setLevel(Level.OFF);
  	newlogger.setUseParentHandlers(debug);
	  if (setHandler (newlogger))
	  	logger = newlogger;
		return logger;
	}
	
	/**
	 * convert logging designation to compatible Level using both
	 * log4j  and PHIN-MS conventions
	 * @return logging level to use
	 */
	public Level getLevel ()
	{
		String s = getProperty (Phinms.LOGLEVEL).toUpperCase();
		if (s == null)
			s = "INFO";
		else if (s.equals("DEBUG") || s.equals("DETAIL"))
			s = "FINEST";
		else if (s.equals("WARN"))
			s = "WARNING";
		else if (s.equals("FATAL") || s.equals("ERROR"))
			s = "SEVERE";
		try
		{
		  logger.info ("setting log level to " + s);
		  return Level.parse(s);
		}
		catch (Exception e)
		{
		  logger.severe("Log configuration: " + e.getMessage());
			return Level.INFO;
		}
	}
	/*************************** db management *************************/
	
	public Statement query (String q)
	{
		return query (q, 0);
	}
	
	/**
	 * Issues an SQL query.  Caller is responsible for closing the returned
	 * object to prevent memory leaks.
	 * 
	 * @param conn dB connection
	 * @param q query to issue
	 * @return resulting statement
	 */
	public Statement query (String q, int rowlimit)
	{
		if ((q == null) || (getConnection() == null))
			return (null);
		try
		{
		  Statement s = conn.createStatement();
		  if (rowlimit > 0)
		  	s.setMaxRows(rowlimit);
		  s.execute(q);
		  return (s);
		}
		catch (SQLException e)
		{
			logger.severe ("Unable to complete query " + q + " - " 
				+ e.getMessage());
		}
		return null;		
	}
	
	/**
	 * set up a dB connection
	 * 
	 * @param env for this connection
	 * @return new connnection
	 */
	public Connection getConnection ()
	{
		if (conn != null)
			return (conn);
		
		String jdbcDriver = getProperty(Phinms.JDBCDRIVER);
		String databaseUrl = getProperty(Phinms.DATABASEURL);
		String databaseUser = getProperty(Phinms.DATABASEUSER);
		String databasePasswd = getProperty(Phinms.DATABASEPASSWD);

		// get the dB password from the password store
		String key = getProperty(Phinms.KEY);
		String seed = getProperty (Phinms.SEED);
		String passfile = getProperty(Phinms.PASSWORDFILE);
		
		Passwords pw = new Passwords ();
		if (!pw.load(passfile, seed, key))
		{
			logger.severe ("Can't resolve passwords");
			return null;
		}
		databaseUser = pw.get(databaseUser);
		databasePasswd = pw.get(databasePasswd);
		
		try
		{
			if (Class.forName(jdbcDriver) == null)
				return null;
		}
		catch (ClassNotFoundException e)
		{
			logger.severe ("Unable to load " + jdbcDriver);
			return null;
		}
		try
		{
			conn = 
				DriverManager.getConnection(databaseUrl, databaseUser, databasePasswd);
		}
		catch (Exception e)
		{
			logger.severe ("Unable to connect to " 
					+ databaseUrl + " - " + e.getLocalizedMessage());
			return (null);
		}
		return conn;
	}
	
	/**
	 * closes a connection
	 * @param conn
	 */
	public void closeConnection ()
	{
		if (conn != null)
		{
			try
			{
				if (!conn.isClosed())
				  conn.close ();
			}
			catch (SQLException e)
			{
			  logger.severe ("Failed closing connection - " 
						+ e.getLocalizedMessage());
			}
		  conn = null;
		}
	}
}
