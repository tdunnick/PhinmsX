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
import java.util.logging.*;
import gov.cdc.nedss.services.security.encryption.PasswordResolver;

import tdunnick.phinmsx.util.XmlContent;

/**
 * Definitions and shared methods for managing PHIN-MS properties.
 * This normally uses PHIN-MS configuration files, but may stand-alone.
 * However, it expects the configuration to mirror those found in
 * PHIN-MS (other than the root, which may be anything).
 * 
 * @author user
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
	
	/**
	 * Load a configuration and initial the properties.  Will merge
	 * with PHIN-MS receiver properties.
	 * 
	 * @param conf name of configuration file.
	 * @return
	 */
	public boolean load (String conf)
	{
		if ((props = getProps (conf)) == null)
		{
			System.err.println ("Unable to load properties from " + conf);
			return false;
		}
		if ((propRoot = props.getRoot ()) == null)
		{
			System.err.println ("Unable to find root tag for " + conf);
			return false;
		}
		propRoot += ".";
		// copy in any missing but needed from receiver's xml
		String s = getProperty (Props.RECEIVERXML);
		if (s != null)
		{
			XmlContent r = getProps (s);
			if ((r == null) || !props.merge (r, false))
			{
				System.out.println ("ERROR: failed merging properties from " + s);
				return false;
			}
			System.out.println ("Merged configuration with " + s);
		}
		else
		{
			System.err.println ("WARNING: missing " + propRoot + Props.RECEIVERXML);
		}
		// these are must haves
		if (getProperty (Props.LOGCONTEXT) == null)
			props.setValue(propRoot + Props.LOGCONTEXT, Props.DFLTCONTEXT);
		if (getProperty (Props.TEMPDIR) == null)
			props.setValue(propRoot + Props.TEMPDIR, Props.DFLTTEMPDIR);
		if (getProperty (Props.QUEUENAME) == null)
			props.setValue(propRoot + Props.QUEUENAME, Props.DFLTQUEUE);
		return (true);
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
			logger.severe("Failed loading " + name + " - " + xml.getError());
			return null;
		}
		return xml;
	}
	
	/**************************** logging ********************************/

	/**
	 * Set the logging level
	 * @param level one of "all", "debug", "error", "fatal", "info", or "warn"
	 */
	public void setLogLevel (Logger logger, String level)
	{
		Level l = Level.INFO;
		if (level == null)
			level = "INFO";
		if (level.equalsIgnoreCase("ALL"))
			l = Level.ALL;
		else if (level.equalsIgnoreCase("DETAIL"))
			l = Level.FINEST;
		else if (level.equalsIgnoreCase("ERROR"))
			l = Level.SEVERE;
		else if (level.equalsIgnoreCase("FATAL"))
			l = Level.SEVERE;
		else if (level.equalsIgnoreCase("INFO"))
			l = Level.INFO;
		else if (level.equalsIgnoreCase("WARN"))
			l = Level.WARNING;
		else
			l = Level.parse(level);
		logger.setLevel(l);
		Handler[] h = logger.getHandlers();
		for (int i = 0; i < h.length; i++)
			h[i].setLevel(l);
	}
	
	/**
	 * return our default logger
	 * @return
	 */
	public Logger getLogger ()
	{
		if (logger == null)
			return getLogger (null, true);
		return this.logger;
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
		String s = props.getValue (prefix + Props.LOGCONTEXT);
		// check to see if this context is set and use it if it is
		Logger newlog = LogManager.getLogManager().getLogger(s);
		if (newlog == null)
		{
			newlog = Logger.getLogger(s);
			if (logger == null)
				logger = newlog;
		}
		else if (!reconfigure)			
			return (newlog);
		Handler[] h = newlog.getHandlers();
		for (int i = 0; i < h.length; i++)
		  newlog.removeHandler(h[i]);
		
		// create and configure a new logger
		String logName = props.getValue(prefix + Props.LOGDIR);
		if (logName == null)
			logName = "%t/";
		if ((s = props.getValue(prefix + Props.LOGNAME)) == null)
			s = props.getValue (prefix + Props.LOGCONTEXT);
		logName += s + "%g.log";
		try
		{
		  Handler handler = new FileHandler(logName, FILE_SIZE, NUM_FILES,	true);
			handler.setFormatter(new SimpleFormatter());
			newlog.addHandler(handler);
			if (((s = props.getValue(prefix + Props.LOGDEBUG)) != null) && 
			  s.equalsIgnoreCase("true"))
			{
				System.out.println ("add console handler");
				handler = new ConsoleHandler ();
				newlog.addHandler(handler);
			}
			newlog.setUseParentHandlers(false);
		}
		catch (IOException e)
		{
			newlog.severe("Unable to set log file to " + logName + ": " + e.getMessage());
		}
    setLogLevel (newlog, props.getValue(prefix + Props.LOGLEVEL));
		return newlog;
	}
	
	
	/*************************** db management *************************/
	
	/**
	 * Issues an SQL query.  Caller is responsible for closing the returned
	 * object to prevent memory leaks.
	 * 
	 * @param conn dB connection
	 * @param q query to issue
	 * @return resulting statement
	 */
	public Statement query (Connection conn, String q, int rowlimit)
	{
		if (q == null)
			return (null);
		try
		{
		  Statement s = conn.createStatement();
		  if (rowlimit > 0)
		  	s.setMaxRows(rowlimit);
		  if (!s.execute(q))
		  {
		    logger.severe ("failed execution - " + q);
		  	s.close ();
		  	return null;
		  }
		  return (s);
		}
		catch (SQLException e)
		{
			logger.severe ("Unable to complete query " + q + " - " 
				+ e.getLocalizedMessage());
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
		String jdbcDriver = getProperty(Props.JDBCDRIVER);
		String databaseUrl = getProperty(Props.DATABASEURL);
		String databaseUser = getProperty(Props.DATABASEUSER);
		String databasePasswd = getProperty(Props.DATABASEPASSWD);

		// get the dB password from the password store
		String key = getProperty(Props.KEY);
		String seed = getProperty (Props.SEED);
		String passfile = getProperty(Props.PASSWORDFILE);

		PasswordResolver res;
		try
		{
			res = new PasswordResolver(passfile, seed, key);
		}
		catch (Exception e)
		{
			logger.severe ("ERROR: Can't resolve passwords");
			return null;
		}
		databaseUser = res.resolvePassword(databaseUser);
		databasePasswd = res.resolvePassword(databasePasswd);

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
		Connection conn = null;
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
	public void closeConnection (Connection conn)
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
