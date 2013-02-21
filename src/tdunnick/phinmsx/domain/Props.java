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
import java.util.Map;
import java.net.URL;
import java.sql.*;
import org.apache.log4j.*;

import tdunnick.phinmsx.crypt.*;
import tdunnick.phinmsx.util.Phinms;
import tdunnick.phinmsx.util.StrUtil;
import tdunnick.phinmsx.util.XLog;
import tdunnick.phinmsx.util.XmlContent;

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
	// common property suffixes shared among the PhinmsX configurations.
	
	// logging...
	public final static String LOGCONTEXT = "logContext";
	public final static String LOGDEBUG = "logDebug";
	public final static String LOGDIR = "logDir";
	public final static String LOGNAME = "logName";
	public final static String LOGLEVEL = "logLevel";
	public final static String LOGSIZE = "maxLogSize";
	public final static String LOGARCHIVE = "logArchive";
	public final static String INCOMINGDIR = "incomingDir";
	// encryption
	public final static String KEYSTORE = "keyStore";
	public final static String KEY = "key";
	public final static String KEYSTOREPASSWD = "keyStorePasswd";
	public final static String PASSWORDFILE = "passwordFile";
	public final static String SEED = "seed";
	// queuing
	public final static String QUEUEMAP = "queueMap";
	public final static String DATABASE = "databasePool.database.";
	public final static String DATABASEID = DATABASE + "databaseId";
	public final static String DBTYPE = DATABASE + "dbType";
	public final static String POOLSIZE = DATABASE + "poolSize";
	public final static String JDBCDRIVER = DATABASE + "jdbcDriver";
	public final static String DATABASEURL = DATABASE + "databaseUrl";
	public final static String DATABASEUSER = DATABASE + "databaseUser";
	public final static String DATABASEPASSWD = DATABASE + "databasePasswd";
	// unique to our configuration
	public final static String PHINMS = "phinms";
	public final static String RECEIVERXML = "receiverXML";
	public final static String SENDERXML = "senderXML";
	public final static String QUEUENAME = "queueName";
	public final static String FILEEXTENSION = "fileExtension";
	public final static String TEMPDIR = "tempDirectory";
	// helper class
	public final static String HELPER = "helper.class";
	// status file
	public final static String STATUS = "status";

	// a few configuration defaults
	public final static String DFLTQUEUE = "workerqueue";
	
	private String propRoot = null;
	private XmlContent props = null;
	private Logger logger = null;
	private String tableName = null;
	Connection conn = null;
		
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
		logger = XLog.console();
		String owner = getParentClassName ();
		String s = null;
		File d = null;
		
		logger.debug ("OWNER " + owner);
		
		// if there is no configuration given, create one
		if (conf == null)
		{		
			props = new XmlContent ();
			props.createDoc();
			propRoot = owner;
		}
		else if (((props = getProps (conf)) == null) ||
				((propRoot = props.getRoot ()) == null))
		{
			logger.error("Unable to load properties from " + conf);
			return false;
		}
		propRoot += ".";
		
		/*
		 * if PHIN-MS hasn't been identified, then try to set it
		 * up by looking at various configuration settings and finally
		 * from our own context.
		 */
		if (Phinms.getPath (null) == null)
		{
			String p = Phinms.setPath(getProperty (Props.PHINMS));
			if (p == null)
				p = Phinms.setPath(getProperty (Props.RECEIVERXML));
			if (p == null)
				p = Phinms.setPath(getProperty (Props.SENDERXML));
			if (p == null)
 			  Phinms.setPath(Phinms.getContextPath());
		}
		
		/*
		 * set up a logging - note done BEFORE merging so that each
		 * configuration will have it's own logging unless discretely
		 * specified.
		 */
		if (getProperty (Props.LOGDIR) == null)
		{
			d = new File (Phinms.getContextPath() + "/../../phinmsx/logs");
			if (!(d.exists() || d.mkdirs ()))
				logger.error ("Can't create " + d.getPath ());
			if (d.isDirectory())
				setProperty (Props.LOGDIR, d.getAbsolutePath());			
		}
		if (getProperty (Props.LOGCONTEXT) == null)
			setProperty (Props.LOGCONTEXT, owner);
		if (getProperty (Props.LOGNAME) == null)
			setProperty (Props.LOGNAME, owner + ".log");
		if (getProperty (Props.LOGLEVEL) == null)
			setProperty (Props.LOGLEVEL, "INFO");
		getLogger (null, true);
		
		/*
		 * if no properties were given, then merge by default 
		 * with receiver.xml
		 */
		if (r == null)
		{
			// get receiver.xml
			s = getProperty(Props.RECEIVERXML);
			if (s == null)
				s = Phinms.getPath("config") + "/receiver/receiver.xml";
			if ((r = getProps(s)) == null)
				logger.error ("Can't load " + s);
		}
		if ((r != null) && !props.merge (r, false))
		{
		  logger.fatal ("Failed merging properties from " + r.getRoot());
			return false;
		}
		
		/*
		 * make sure tomcat has a temp directory
		 */
		if ((s = Phinms.getEnv("CATALINA_TMPDIR")) == null)
		{
			logger.error ("CATALINA_TMPDIR not set for Tomcat");
		}
		else
		{
			d = new File (s);
			if (!(d.exists() || d.mkdirs()))
			{
				logger.error ("can't create CATALINA_TMPDIR");
			}
		}
		
		/*
		 * set up a temp directory
		 */
		if ((s = getProperty (Props.TEMPDIR)) == null)
		{
			s = Phinms.getEnv("CATALINA_TMPDIR");
			if (s == null)
				s = Phinms.getEnv ("TEMP");
			if (s == null)
				s = Phinms.getEnv("TMP");
			if (s == null)
				s = Phinms.getContextPath() + "/../../phinmsx/tmp";

			setProperty (Props.TEMPDIR, s);
		}
		d = new File (s);
		if (!(d.exists() || d.mkdirs()))
		{
			logger.error ("can't create " + d.getPath());
			setProperty (Props.TEMPDIR, null);
		}
		logger.debug("TEMPDIR " + s);
		
		/*
		 * finally the status cache and default queue for the receiver
		 */
		if (owner.equals("Receiver"))
		{
			if (getProperty(Props.STATUS) == null)
				setProperty(Props.STATUS, getProperty(Props.LOGDIR) + "/status.bin");
			if (getProperty(Props.QUEUENAME) == null)
				props.setValue(propRoot + Props.QUEUENAME, Props.DFLTQUEUE);
		}
		return true;
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
		String queue = getProperty (Props.QUEUENAME);
		String mapname = getProperty (Props.QUEUEMAP);
		if ((queue == null) || (mapname == null))
			return null;
		// getLogger().debug("Loading queue map " + mapname);
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
	 * Set a default property.  
	 * @param name of property sans prefix
	 * @param value to set
	 */
	public void setProperty (String name, String value)
	{
		logger.debug("Set " + name + " to " + value);
		if ((props != null) && (propRoot != null))
		{
		  if (!props.setValue (propRoot + name, value))
		  	logger.error("Can't set " + propRoot + name + " to " + value);
		}
		else
			logger.warn ("Can't set " + name + " to " + value);
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
			logger.error("Failed loading " + name + " - " + xml.getError());
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
		if (logger == null)
			return getLogger (null, false);
		return this.logger;
	}

	/**
	 * force the use of this logger
	 * @param logger
	 */
	public void setLogger (Logger logger)
	{
		this.logger = logger;
	}
	
	/**
	 * Set up for logging. 
	 * 
	 * @param logname of rotated log
	 */
	public Logger getLogger (String prefix, boolean reconfigure)
	{
		int FILE_SIZE = 10000;
		int NUM_FILES = 5;
		if (prefix == null)
			prefix = "";
		String l = getProperty (prefix + Props.LOGDEBUG);
		boolean debug = ((l != null) && l.equalsIgnoreCase("TRUE"));
		String s = getProperty (prefix + Props.LOGCONTEXT);
		if (s == null)
		{
			logger = XLog.console ();
			logger.warn ("No LOGCONTEXT set - using console");
			return logger;
		}
		logger = Logger.getLogger(s);
		if (!reconfigure && logger.getAllAppenders().hasMoreElements())
			return logger;
		logger.removeAllAppenders();
    XLog.setLogLevel (logger, getProperty(prefix + Props.LOGLEVEL));
	
		PatternLayout layout = new PatternLayout (debug ? XLog.DFMT : XLog.FMT);
		// create and configure a new logger
	  if (((l = getProperty (prefix + Props.LOGDIR)) == null) || debug)
	  {
		  logger.addAppender(new ConsoleAppender (layout));
		  if (l == null)
		  {
		  	logger.warn("No LOGDIR set - using console");
	      return logger;
		  }
	  }
	  if (!l.matches(".*[\\\\/]"))
	  	l += "/";
		String logName = getProperty(prefix + Props.LOGNAME);
		if ((logName == null) || (logName.length() == 0))
			logName = "phinmsx.log";
		logName = l + logName;
		XLog.console().debug ("Configuring appender for " + logName);
    try
		{
    	RollingFileAppender appender = 
    		new RollingFileAppender (layout, logName, true);
    	appender.setImmediateFlush(true);
    	s = getProperty (Props.LOGSIZE);
    	if ((s != null) && s.matches("[0-9]+"))
    		appender.setMaxFileSize(s);
    	if ((s = getProperty (Props.LOGARCHIVE)) == null)
    		s = "15";
    	if (s.equalsIgnoreCase("false"))
    		appender.setMaxBackupIndex(0);
    	else if (s.matches("[0-9]+")) 
      	appender.setMaxBackupIndex(Integer.parseInt(s));
    	else
      	appender.setMaxBackupIndex(15);
			logger.addAppender(appender);
			return logger;
		}
		catch (IOException e)
		{
		  logger.addAppender(new ConsoleAppender (layout));
			logger.error ("Unable to set log file to " 
					+ logName + ": " + e.getMessage());
		}
		return null;
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
			logger.error ("Unable to complete query " + q + " - " 
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
		
		String jdbcDriver = getProperty(Props.JDBCDRIVER);
		String databaseUrl = getProperty(Props.DATABASEURL);
		String databaseUser = getProperty(Props.DATABASEUSER);
		String databasePasswd = getProperty(Props.DATABASEPASSWD);

		// get the dB password from the password store
		String key = getProperty(Props.KEY);
		String seed = getProperty (Props.SEED);
		String passfile = getProperty(Props.PASSWORDFILE);
		
		Passwords pw = new Passwords ();
		if (!pw.load(passfile, seed, key))
		{
			logger.error ("Can't resolve passwords");
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
			logger.error ("Unable to load " + jdbcDriver);
			return null;
		}
		try
		{
			conn = 
				DriverManager.getConnection(databaseUrl, databaseUser, databasePasswd);
		}
		catch (Exception e)
		{
			logger.error ("Unable to connect to " 
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
			  logger.error ("Failed closing connection - " 
						+ e.getLocalizedMessage());
			}
		  conn = null;
		}
	}
}
