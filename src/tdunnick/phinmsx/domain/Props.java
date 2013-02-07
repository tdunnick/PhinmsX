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
import java.sql.*;
import org.apache.log4j.*;

import tdunnick.phinmsx.crypt.*;
import tdunnick.phinmsx.util.Phinms;
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
	public final static String DFLTCONTEXT = "Hl7Ack";
	public final static String DFLTQUEUE = "workerqueue";
	public final static String DFLTTEMPDIR = "C:/temp/";
	
	private String propRoot = "";
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
	 * with given properties.
	 * 
	 * @param conf name of configuration file.
	 * @param r properties to merge with
	 * @return true if successful
	 */
	public boolean load (String conf, XmlContent r)
	{
		if ((props = getProps (conf)) == null)
		{
			// System.err.println ("Unable to load properties from " + conf);
			return false;
		}
		if ((propRoot = props.getRoot ()) == null)
		{
			// System.err.println ("Unable to find root tag for " + conf);
			return false;
		}
		propRoot += ".";
		/*
		 * if not merging with properties, then merge by default 
		 * with receiver.xml
		 */
		if (r == null)
		{
			// set up the location of our PHIN-MS tomcat server, config, and version
			Phinms.setTomcatPath(getProperty(Props.PHINMS));
			// copy in any missing but needed from receiver's xml
			String s = getProperty(Props.RECEIVERXML);
			if (s == null)
				s = Phinms.getConfigPath() + "/receiver/receiver.xml";
			r = getProps(s);
		}
		if ((r == null) || !props.merge (r, false))
		{
			// System.out.println ("ERROR: failed merging properties from " + s);
			return false;
		}
		// these are must haves
		if (getProperty (Props.LOGCONTEXT) == null)
			props.setValue(propRoot + Props.LOGCONTEXT, Props.DFLTCONTEXT);
		if (getProperty (Props.TEMPDIR) == null)
			props.setValue(propRoot + Props.TEMPDIR, Props.DFLTTEMPDIR);
		if (getProperty (Props.QUEUENAME) == null)
			props.setValue(propRoot + Props.QUEUENAME, Props.DFLTQUEUE);
		return true;
	}

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
	 * Retrieve a default serverlet property.  Note, most properties should
	 * come from the request environment which includes these...
	 * 
	 * @param name of property sans prefix
	 * @return value or null if not found
	 */
	public String getProperty (String name)
	{
		if (props == null)
			return ("");
		return props.getValue (propRoot + name);
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
			prefix = propRoot;
		else
			prefix = propRoot + prefix;
		String l = props.getValue(Props.LOGDEBUG);
		boolean debug = ((l != null) && l.equalsIgnoreCase("TRUE"));
		String s = props.getValue (prefix + Props.LOGCONTEXT);
		if (s == null)
		{
			return (logger = XLog.getRootLogger(debug));
		}
		logger = Logger.getLogger(s);
		if (!reconfigure && logger.getAllAppenders().hasMoreElements())
			return logger;
		logger.removeAllAppenders();
    XLog.setLogLevel (logger, props.getValue(prefix + Props.LOGLEVEL));
	
		PatternLayout layout = new PatternLayout (debug ? XLog.DFMT : XLog.FMT);
		// create and configure a new logger
	  if (((l = props.getValue(prefix + Props.LOGDIR)) == null) || debug)
	  {
		  logger.addAppender(new ConsoleAppender (layout));
		  if (l == null)
	      return logger;
	  }
		String logName = props.getValue(prefix + Props.LOGNAME);
		if ((logName == null) || (logName.length() == 0))
			logName = "phinmsx.log";
		logName = l + logName;
    try
		{
    	RollingFileAppender appender = 
    		new RollingFileAppender (layout, logName, true);
    	appender.setMaxBackupIndex(5);
    	appender.rollOver(); 
    	appender.setImmediateFlush(true);
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
			logger.error ("ERROR: Can't resolve passwords");
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
