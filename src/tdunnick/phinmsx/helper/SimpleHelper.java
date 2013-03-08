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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.*;
import tdunnick.phinmsx.model.*;
import tdunnick.phinmsx.domain.*;
import tdunnick.phinmsx.domain.receiver.RcvEnv;
import tdunnick.phinmsx.util.*;

/**
 * An implementation of the Receiver's helper.  Note that the receiver
 * calls the setResponse() method AFTER decryption and saving the result
 * to disk.  So the helper get's an environment with the filename already
 * filled in and ready to use.
 * 
 * In this version the helper saves the post processed file to an alternate
 * directory found in the helper's config if given using the same naming
 * Conventions.
 * 
 * @author Thomas Dunnick
 *
 */
public class SimpleHelper implements RcvHelper
{
  /**
   * Get a property specific to the helper, or if not found return the
   * one provided by the receiver.
   * 
   * @param name of property
   * @return empty string or property found
   */
  protected String getProperty (RcvEnv env, String name)
  {
  	String s = env.getProperty("helper." + name);
  	if (s.length() == 0)
  	  s = env.getProperty(name);
  	return (s);
  }

  protected Logger getLogger(RcvEnv env)
	{
		return env.props.getLogger();
	}

	/**
	 * Get a temporary file name to use for this message
	 * 
	 * @param env of response
	 * @return temporary file name
	 * @throws IOException
	 */
  protected File getTmpFile(RcvEnv env) throws IOException
	{
		String p = env.fileName;
		if (p.length() < 3)
			p += "_tmp";
		return File.createTempFile(p, null, new File(getProperty(env, PhinmsX.TEMPDIR)));
	}

	/**
	 * Create a path name for the incoming file based on directory and extensions
	 * specified in the environment. In particular, strip off the process ID and
	 * replace the incoming extension if the file name will remain unique.
	 * Otherwise simply tag on the extension.  If the helper has "overwrite" set
	 * to true just return the receiver's file path.
	 * 
	 * @param env environment with naming information
	 * @return true if path successfully set, false if file exists
	 */
  protected File getFilePath (RcvEnv env)
	{
  	if (env.getProperty ("helper.overwrite").equalsIgnoreCase("true"))
  		return new File (env.filePath);
		String folder = env.getProperty("helper." + Phinms.INCOMINGDIR);
		String ext = getProperty (env, PhinmsX.FILEEXTENSION);
		// strip suffix and extension, add our extension
		String filePath = folder +
			env.fileName.replaceFirst("([.][^.]*){0,1}[.]" + env.fileSuffix,"") + ext;
		File f = new File (env.filePath);
		if (!f.exists())
			return f;
		getLogger(env).finest ("File " + env.filePath + " exists, trying full name");
		// if the above wasn't unique, or the preferred extension didn't match
		// simply add it
		env.filePath = folder + env.fileName + ext;
		f = new File (env.filePath);
		if (!f.exists())
			return f;
		return null;
	}
	
	/**
	 * Write out this payload to disk if we have a helper folder set.
	 * 
	 * Note that if we get this far, the environment should have a valid
	 * filename!  We write to a temp file, and then rename it. The helper 
	 * folder should be on the same device as the temp folder
	 * in order to rename the file.
	 * 
	 * @param data to write
	 * @return true if successful
	 */
	protected boolean writeData (RcvEnv env, byte[] data)
	{
		File outFile = getFilePath(env);
		if (outFile == null)
			return true;
		try
		{
			// include userDir subdirectory in payload path
			File tmpFile = getTmpFile(env);
			FileOutputStream fos = new FileOutputStream(tmpFile);
			fos.write(data);
			fos.flush();
			fos.close();
			if (!tmpFile.renameTo(outFile))
			{
				getLogger(env).severe ("ERROR: Unable to rename " + tmpFile.getPath() + 
						" to "	+ outFile.getPath());
			}
		}
		catch (IOException e)
		{
			getLogger(env).severe ("Failed writing data to " + outFile.getPath());
			return false;
		}
		return true;
	}
		
	/**
	 * extend this method in super-classes for processing logic
	 * @param env
	 * @param data
	 * @return
	 */
	protected byte[] process (RcvEnv env, byte[] data)
	{
		return data;
	}
	
	public boolean setResponse(RcvEnv env, byte[] data)
	{
		writeData (env, process (env, data));
		return true;
	}
}
