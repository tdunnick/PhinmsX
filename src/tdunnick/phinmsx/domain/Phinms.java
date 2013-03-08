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

import java.util.logging.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Common access to our PHIN-MS environment.  Note that we only want
 * ONE PHIN-MS server per PhinmsX context, so we only set most things
 * ONCE!
 * 
 * @author t. dunnick
 *
 */

public class Phinms
{
	// Common tags found in PHIN-MS configurations sans root prefix
	
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
	public final static String INSTALLDIR = "installDir";
	public final static String KEYSTORE = "keyStore";
	public final static String KEY = "key";
	public final static String SERVICEKEY = "serviceKey";
	public final static String KEYSTOREPASSWD = "keyStorePasswd";
	public final static String PASSWORDFILE = "passwordFile";
	public final static String SEED = "seed";
	public final static String SERVICESEED = "serviceSeed";
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

	// supporting values
	static private String phinmsPath = null;
	static private String version = null;
	static private SimpleDateFormat dfmt = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss");
	static final String UNKNOWNVERSION = "Unknown PHIN-MS Version";
	
  /**
   * get the PHIN-MS version we are running against. 
   * @return the version
   */
	static public String getVersion ()
	{
		if (version == null)
			return UNKNOWNVERSION;
		return version;
	}
  
  /**
   * Set the PHIN-MS version we are running against. This uses the
   * tomcat path to reflectively load the Defines class from PHIN-MS
   * if given version is null.
   * 
   * @param v version to force
   */
	static private boolean setVersion (String path)
  {
		if ((version == null) && (path != null))
		{
			try
			{
			  URL[] defurl = 
			  { 
			  		new URL ("jar:file:" + path 
			  				+ "/webapps/receiver/WEB-INF/lib/ebxml.jar!/") 
			  };
			  ClassLoader loader = URLClassLoader.newInstance(defurl);
			  //URL url = loader.getResource("gov/cdc/nedss/common/Defines.class");
			  //System.out.println ("URL:" + url.getPath());
			  Class defines = loader.loadClass("gov.cdc.nedss.common.Defines");
			  version = (String) defines.getField("VERSION").get(null);
			  return true;
			}
			catch (Exception e)
			{
				Logger.getLogger("").severe ("Can't get version - " + e.getMessage());
			}
		}
		return false;
 }
  
	/**
	 * Set the PHIN-MS install path.  If the path is null, then use
	 * the classloader to get our own path and use that instead.
	 * Either way set the config path and version too.
	 * 
	 * @param p path to set
	 */
	static public synchronized String setPath (String p)
	{
		if (phinmsPath != null)
			return phinmsPath;
		if (p == null)
			return null;
		// XLog.console().debug("Trying path '" + p + "'");
		File d = new File (p);
		p = d.getAbsolutePath();
		// System.out.println (p);
		int i = p.indexOf("tomcat-5.0.19");
		if (i < 0)
			i = p.indexOf("appserver");
		if (i < 0)
			return null;
		if ((i = p.indexOf('\\', i)) < 0)
		  i = p.indexOf('/', i);
		if (i > 0)
			p = p.substring(0, i);
		// XLog.console().debug("Modified path '" + p + "'");
		if (setVersion (p))
			phinmsPath = p;
		return phinmsPath;
	}
	
	/**
	 * Get the path to a PHIN-MS folder.  
	 * @return folder path
	 */
	static public String getPath (String folder)
	{
		if (phinmsPath == null)
			return null;
		if (folder == null)
			return phinmsPath;
		File d = new File(phinmsPath + "/phinms/" + folder + "/");
		if (!d.isDirectory())
			d = new File(phinmsPath + "/../" + folder + "/");
		if (d.isDirectory())
			return d.getPath();
		return null;
	}
	
	/**
	 * Format a date PHIN-MS style
	 * @param t time to format
	 * @return
	 */
	static public String fmt_date (long t)
	{
		return fmt_date (new Date(t));
	}
	
	/**
	 * Format a date PHIN-MS style
	 * @param d to format or null for NOW
	 * @return the formatted date
	 */
	static public String fmt_date (Date d)
	{
		if (d == null)
			d = new Date();
		String s = dfmt.format(d);
	  if (version.matches (".*2[.]8[.].*"))
	  	s = s.replace('T', ' ');
	  return s;

	}
	
	/**
	 * Parse a PHIN-MS style date
	 * @param d
	 * @return the time
	 */
	static public long get_time (String d)
	{
		if (d != null) try
		{
			return dfmt.parse(d.replace(' ', 'T')).getTime();
		}
		catch (Exception e)
		{
		}
		return new Date().getTime();
	}
}
