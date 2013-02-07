package tdunnick.phinmsx.util;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

public class Phinms
{
	static private String tomcatPath = null;
	static private String configPath = null;
	static private String version = null;
	
  /**
   * get the PHIN-MS version we are running against. 
   * @return
   */
  static public String getVersion ()
  {
		if (version == null)
			setVersion (null);
 	return version;
  }
  
  /**
   * Set the PHIN-MS version we are running against. This uses the
   * tomcat path to reflectively load the Defines class from PHIN-MS
   * if given version is null.
   * 
   * @param v version to force
   */
  static public void setVersion (String v)
  {
  	if ((v == null) && (getTomcatPath() != null))
		{
			try
			{
			  URL[] defurl = 
			  { 
			  		new URL ("jar:file:" + tomcatPath 
			  				+ "/webapps/receiver/WEB-INF/lib/ebxml.jar!/") 
			  };
			  ClassLoader loader = URLClassLoader.newInstance(defurl);
			  Class defines = loader.loadClass("gov.cdc.nedss.common.Defines");
			  v = (String) defines.getField("VERSION").get(null);
			}
			catch (Exception e)
			{
				// System.err.println ("ERROR: can't get version - " + e.getMessage());
			}
		}
  	if (v == null)
  		version = "Unknown PHIN-MS Version";
  	else
  		version = v;
  }
  
	/**
	 * @return path to the configurations or null if not found
	 */
	static public String getConfigPath ()
	{
		if (configPath == null)
		  setConfigPath (null);
		return configPath;
	}
	
	/**
	 * Set the path to the PHIN-MS configuration folder.  If path is
	 * null assume its on the tomcatPath and look from there.
	 * 
	 * @param path
	 */
	static public void setConfigPath (String path)
	{
		if ((path == null) && (getTomcatPath() != null))
		{
			File d = new File(tomcatPath + "/phinms/config/");
			if (!d.isDirectory())
				d = new File(tomcatPath + "/../config/");
			if (d.isDirectory())
				path = d.getPath();
		}
		configPath = path;
	}
	
	/**
	 * Find the PHIN-MS install path
	 * @return
	 */
	static public String getTomcatPath  ()
	{
		if (tomcatPath == null)
			setTomcatPath (null);
		return tomcatPath;
	}
	
	/**
	 * Set the PHIN-MS install path.  If the path is null, then use
	 * the classloader to get our own path and use that instead.
	 * Either way set the config path and version too.
	 * 
	 * @param p path to set
	 */
	static public void setTomcatPath (String p)
	{
		if (p == null)
		{
			String me = Phinms.class.getName().replace ('.', '/') + ".class";
			// System.out.println (me);
			ClassLoader l = Phinms.class.getClassLoader();
			URL url = l.getResource(me);
			p = url.getPath().replace("/" + me, "");
		}
		tomcatPath = (p.replace('\\', '/').replaceFirst("/webapps/.*$", ""));
		setConfigPath (null);
		setVersion (null);
	}
}
