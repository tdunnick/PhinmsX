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

public class PhinmsX
{
	// common property suffixes shared among the PhinmsX configurations.
	public final static String PHINMS = "phinms";
	public final static String PHINMSX = "phinmsx";
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
	
	public final static String xversion = "1.0d 03/17/2013";
	private static String contextPath = null;
	public final static String PHINMSXPATH = "/../../../phinmsx";
	
	private static String ENVBAT =
		"SET IPATH=__ipath__\n" +
		"SET JAVA_HOME=%IPATH%\\JDK\n" +
		"SET JARS=\n" +
		"FOR %%f IN (\"%IPATH%appserver\\webapps\\phinmsx\\WEB-INF\\lib\\*.jar\")" +
		"DO CALL :addjar %%f\n" +
		"SET RUN=\"%JAVA_HOME%\\bin\\java.exe\" -cp \"%JARS%\"\n" +
		"GOTO :EOF\n" +
		":addjar\n" +
		"IF \"%CLASSPATH%.\" == \".\" GOTO :firstjar\n" +
		"SET CLASSPATH=%CLASSPATH%;%*\n" +
		"GOTO :EOF\n" +
		":firstjar\n" +
		"SET CLASSPATH=%*\n" +
		"GOTO :EOF\n";

				
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
	 * Generate a bin folder with scripts to run various things
	 * 
	 * @return true if successful
	 */
	static public synchronized boolean makebin ()
	{
		if (contextPath == null)
			return false;
		// attempt to set up a bin
		File f = new File (contextPath + PHINMSXPATH + "/bin");
    if (!(f.isDirectory() || f.mkdirs()))
    {
    	System.err.println ("ERROR: unable to create " + f.getAbsolutePath());
    	return false;
    }
    try
    {
    	f = new File (f.getAbsoluteFile() + "/env.bat");
    	if (!f.exists())
    	{
    	  FileOutputStream out = new FileOutputStream (f);
    	  out.write (ENVBAT.replaceFirst("__ipath__", contextPath + PHINMSXPATH).getBytes());
    	  out.close ();
    	}

    }
    catch (IOException e)
    {
    	System.err.println ("Unable to create " + f.getAbsolutePath()
    			+ " - " + e.getMessage());
    	return false;
    }
    return true;		
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
		{
			File d = new File (p);
			if (d.isDirectory())
			  contextPath = p;
		}
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
