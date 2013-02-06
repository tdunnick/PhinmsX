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

import org.apache.xerces.impl.dv.util.Base64;

public class RcvResponse
{
	private static int Sequence = 0;
	
  public StringBuffer getResponse (String status, String error, 
  		String msg, byte[] payload, String filename)
  {
  	String boundary = getBoundary();
  	StringBuffer r = new StringBuffer ();
  	r.append ("--" + boundary + "\n");
  	srText (r, status, error, msg);
  	if ((payload != null) && (filename != null))
  	{
	  	r.append("--" + boundary + "\n");
	  	srPayload (r, payload, filename);
  	}
  	r.append("--" + boundary + "--");
    srHeader (r, boundary);
  	return r;	
  }
  
  private synchronized String getBoundary ()
  {
  	return "phinmsx_" + new Date().getTime() + "_"
  	+ RcvResponse.Sequence++ + "_boundary";
  }
  
  private void srHeader (StringBuffer r, String boundary)
  {
  	int len = r.length();
  	r.insert(0, "Content-Type: multipart/related; type=\"text/xml\";" 
		+ " boundary=\"" + boundary + "\";"
		+ " start=\"textmimepart\"\n"
		+ "Content-Length: " + len + "\n\n");
  }
  
  private void srText (StringBuffer r, String status, String error, String msg)
  {
  	r.append("Content-ID: <textmimepart>\nContent-Type: text/plain\n\n"
  		+ "status=" + status + "&error="+ error + "&appdata=" + msg + "\n");
  }
  
  private void srPayload (StringBuffer r, byte[] payload, String filename)
  {
  	r.append ("Content-ID: <payloadmimepart>\n"
  		+ "Content-Type: Application/Octet-Stream\n"
  		+ "Content-Transfer-Encoding: base64\n"
  		+ "Content-Disposition: attachment; name=\"" + filename + "\"\n\n"
  		+ new String (Base64.encode(payload)) + "\n");
  }
}
