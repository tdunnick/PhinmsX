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

package tdunnick.phinmsx.domain.receiver;

import java.util.*;
import java.io.*;
import org.apache.log4j.*;
import org.apache.xerces.impl.dv.util.Base64;

import tdunnick.phinmsx.util.XLog;

public class RcvRequest
{
	private String filename = null;
	private byte[] payload = null;
	private HashMap arguments = new HashMap ();
	private Logger logger = null;
	
	
	
  public RcvRequest()
	{ 
  	this.logger = XLog.getRootLogger(true);
	}
  
  public RcvRequest (Logger logger)
  {
  	this.logger = logger;
  }

  /**
   * Parse an entire stream
   * @param in
   * @return
   */
  public boolean parse (InputStream in)
  {
  	String rq = readIn (in);
  	return parse (getBoundary (rq), rq);
  }
  
	/**
	 * Parse a stream given the Content-type header
	 * 
	 * @param boundary
	 * @param in
	 * @return
	 */
	public boolean parse (String header, InputStream in)
  {
		return parse (getBoundary (header), readIn (in));
  }
	
	/**
	 * Parse the request using this boundary
	 * 
	 * @param boundary
	 * @param rq
	 * @return
	 */
	public boolean parse (String boundary, String rq)
	{
  	int p, e;
  	
   // parse the request - first get the multipart boundary
    e = rq.indexOf("--" + boundary, 0);
    while ((p = e + boundary.length() + 3) < rq.length() - 2)
    {
    	// System.out.println ("Next part at " + p);
    	if ((e = rq.indexOf ("--" + boundary, p)) < 0)
    	{
    		logger.error ("Request mulitpart missing closing boundary");
    		return false;
    	}
    	String part = rq.substring(p, e);
    	if (part.indexOf("Content-Type: text/plain\n") > 0)
    	{
    		String[] args = part.substring(part.indexOf ("\n\n") + 2).split("[&]");
    		for (int i = 0; i < args.length; i++)
    		{
    			String[] hdr = args[i].split("=");
  				// System.out.println (hdr[0] + ":" + hdr[1]);
    			if (hdr.length == 2)
    			  arguments.put(hdr[0].trim(), hdr[1].trim());
    		}
    	}
    	else if ((part.indexOf("Content-Type: Application/Octet-Stream\n") > 0)
    			|| (part.indexOf("Content-Type: text/xml\n") > 0))
    	{
    		String pl = part.substring(part.indexOf ("\n\n") + 2).trim();
    		if (part.indexOf("Content-Transfer-Encoding: base64\n") > 0)
    			payload = Base64.decode(pl);
    		else
    			payload = pl.getBytes(); 
    		int f = part.indexOf("name=\"");
    		if (f < 0)
    		{
      		logger.error ("Request mulitpart payload missing name");
    			return false;
    		}
        f += 6;
    		filename = part.substring (f, part.indexOf ('"', f));
    	}
    }
    return true;
  }
  
  public String getArgument (String name)
  {
  	return (String) arguments.get(name);
  }
  
  public byte[] getPayLoad ()
  {
  	return payload;
  }
  
  public String getFileName ()
  {
  	return filename;
  }
  
  private String readIn (InputStream in)
  {
    try
    {
    	StringBuffer b = new StringBuffer ();
    	int c;
    	while ((c = in.read()) > 0)
    		b.append ((char) c);
      logger.debug("read:\n" + b.toString());
    	return b.toString().replaceAll("\\r\\n", "\n");
    }
    catch (IOException e)
    {
    	logger.error ("Failed reading parse - " + e.getLocalizedMessage());
    	return null;
    }
  }
  
  private String getBoundary (String m)
	{
		final String BOUNDARY = "boundary=";
		int p, e;
		if ((p = m.indexOf(BOUNDARY)) < 0)
		{
			logger.error("Request mulitpart boundary not specified");
			return null;
		}
		if (((p = m.indexOf('"', p)) < 0) || ((e = m.indexOf('"', ++p)) < 0))
		{
			logger.error("Request mulitpart boundary not quoted");
			return null;
		}
		return m.substring(p, e);
	}
}
