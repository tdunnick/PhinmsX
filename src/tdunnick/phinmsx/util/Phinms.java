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
package tdunnick.phinmsx.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
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
	static private String xversion = "1.0b 02/20/2013";
	static private String contextPath = null;
	static private String phinmsPath = null;
	static private String version = null;
	static final private String UNKNOWNVERSION = "Unknown PHIN-MS Version";
	
	/**
	 * Our PhinmsX version string
	 * 
	 * @return
	 */
	static public String getXVersion ()
	{
		return xversion;
	}
	
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
				XLog.console().error ("Can't get version - " + e.getMessage());
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
	 * Set the path to our context - only happens once.
	 * 
	 * @param p the path
	 * @return the path
	 */
	static public synchronized String setContextPath (String p)
	{
		if ((contextPath == null) && (p != null))
			contextPath = p;
		return contextPath;
	}
	
	/**
	 * @return the path to our context
	 */
	static public String getContextPath ()
	{
		return contextPath;
	}
	
	/**
	 * Look in the system enviroment for a value.  This only tries
	 * to differentiate between *NIX and MS based on the file separator
	 * used by the system, and assumes MS has cmd.exe.
	 * 
	 * @param name of the value we seek
	 * @return the value or null if not found
	 */
	public static String getEnv (String name)
	{
		try
		{
			Runtime r = Runtime.getRuntime();
			String s = "cmd /c echo %" + name + "%";
			if (System.getProperty("file.separator").equals("/"))
				s = "echo $" + name;
			Process p = r.exec(s);
			BufferedReader rdr = 
				new BufferedReader (new InputStreamReader(p.getInputStream()));
			String v = rdr.readLine();
			p.destroy();
			if (v.indexOf (name) > 0)
				return null;
			return v;
		}
		catch (Exception e)
		{
			return null;
		}
	}

}
