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

package tdunnick.phinmsx.helper;

import java.io.*;
import tdunnick.phinmsx.domain.*;
import tdunnick.phinmsx.domain.payload.FesiPayload;
import tdunnick.phinmsx.domain.payload.Payload;
import tdunnick.phinmsx.domain.receiver.RcvEnv;
import FESI.jslib.*;

public class FesiHelper extends SimpleHelper
{	
  public final static String SCRIPT = "helper.script";
  private JSGlobalObject global = null;
  
	public FesiHelper()
	{
		try
		{
			String[] extensions = new String[] 
			{
					"FESI.Extensions.BasicIO", 
					"FESI.Extensions.FileIO",
					"FESI.Extensions.JavaRegExp"
			};
			global = JSUtil.makeEvaluator(extensions);
		}
		catch (Exception e)
		{
			System.err.println ("Failed initializing: " + e.getMessage());
		}
	}

 /**
   * Over ride this method to change objects used by FESI
   * 
   * @param env for helper
   * @param data to process
   * @return the object to use
   */
  protected FesiPayload getPayload (RcvEnv env, byte[] data)
  {
  	return new Payload ("Payload", data);
  }
  
	/**
	 * read a file returning it as a byte array
	 * @param fname
	 * @return null if fails
	 */
	public byte[] readFile (String fname)
	{
		File f = new File (fname);
		if (!f.canRead())
		{
			return null;
		}
		try
		{
			FileInputStream inp = new FileInputStream(f);
			byte data[] = new byte[inp.available()];
			inp.read(data);
			inp.close();
			return data;
		}
		catch (IOException e)
		{
			return null;
		}
	}
	
	/**
	 * read a FESI script as specified in the helper's properties
	 * 
	 * @param props
	 * @return null if it fails
	 */
	public String getScript (Props props)
	{
		String progname = props.getProperty(SCRIPT);
		if (progname == null)
		{
			props.getLogger().severe ("Missing script name in configuration");
			return null;
		}
		byte[] data = readFile (progname);
		if (data == null)
		{
			props.getLogger().severe ("Cannot read script " + progname);
			return null;
		}
		return new String (data);
	}
	
	/**
	 * Run the fesi interpreter for this script and data with extensions
	 * for regular expressions, basic and file IO
	 * 
	 * @param script to run
	 * @param var name for object added
	 * @param object to add to FESI environment
	 * @return oject returned from FESI environment
	 */
	protected Object interpret (String script, FesiPayload payload)
	{
		try
		{
			if (payload != null)
				global.setMember(payload.getName(), global.makeObjectWrapper(payload));
			return global.eval(script);
		}
		catch (Exception e)
		{
			System.err.println ("Failed: " + e.getMessage());
			return null;
		}
	}
	
	/*
	 * 
	 * @param data
	 * @param env
	 * @return
	 */
	protected byte[] process (RcvEnv env, byte[] data)
	{
		String script = getScript (env.props);
		if (script == null)
		{
			env.applicationError = "aborted";
			env.applicationStatus = "failed";
			env.applicationResponse = "Unable to read configuration script";
			return null;
		}
		Object result = null;
		if (data != null)
		{
			FesiPayload payload = getPayload (env, data);
		  result = interpret (script, payload);
		  data = payload.getData ();
		}
		else
		{
			result = interpret (script, null);
		}
		if (result == null)
			result = "Processed " + data.length + " bytes";
		env.applicationResponse = result.toString();
		return data;
	}
	
	
	public static void usage (String prog, String msg)
	{
		System.err.println ("ERROR:" + msg + 
				"\nUsage:" + prog + " where options are...\n" + 
				"\t-d file    read data from file\n" +
				"\t-p props   read properties from props" +
				"\t-c command execute FESI command\n" +
				"\t script    execute script (file)\n");
		System.exit(1);
	}
	
	/**
	 * For stand-alone use and script testing.  
	 * 
	 * @param args
	 */	
	public static void main (String args[])
	{
		byte[] data = null,
		    script = null;
		String response;
		FesiHelper f = new FesiHelper ();
		
		for (int i = 1; i < args.length; i++)
		{
			if (args[i].equals("-d"))
			{
				if (++i < args.length)
				{
					if ((data = f.readFile (args[i])) == null)
						usage (args[0], "can't read data from " + args[i]);
				}
				else
					usage (args[0], "missing data");
			}
			else if (args[i].equals("-c"))
			{
				if (++i < args.length)
				{
					response = f.interpret(args[i], f.getPayload(null, data)).toString();
					if (response != null)
						System.out.println (response);
				}
				else
					usage (args[0], "command expected");
			}
			else if (args[i].equals("-p"))
			{
				if (++i < args.length)
				{
					RcvEnv e = new RcvEnv ();
					e.props = new Props ();
					if (!e.props.load(args[i]))
						usage (args[0], "failed to load properties from " + args[i]);
					System.out.println (e.applicationResponse);
				}
				else
					usage (args[0], "missing configuration properties");
				
			}
			else
			{
				if ((script = f.readFile (args[i])) != null)
				{
					response = f.interpret(new String(script), 
							f.getPayload(null, data)).toString();
					if (response != null)
						System.out.println (response);
				}
				else
					usage (args[0], "can't read script from " + args[0]);
			}
		}
		System.exit (0);
	}
}
