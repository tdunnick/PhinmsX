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
 *  Foobar is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package tdunnick.phinmsx.helper;

import java.text.SimpleDateFormat;
import java.util.*;

import tdunnick.phinmsx.domain.RcvEnv;
import gov.cdc.nedss.services.logging.*;

public class Hl7AckHelper implements RcvHelper
{
  String code = "AA";
  String comment = "MSG OK";
    
	public boolean setResponse(RcvEnv env, byte[] data)
	{
		String s[] = new String(data).replace('\n', '\r').split("\r+");
		env.applicationResponse = getAck (s, 1).replace('&', '_');
		if (env.applicationResponse.length() == 0)
		{
			env.applicationError = "failed";
			env.applicationStatus = "application processing error";
			env.applicationResponse = "invalid HL7 message";
			return false;
		}
		env.payload = getAck (s, 0).getBytes();
		env.payloadName = env.fileName + ".ack";
		return true;
	}

	/**
	 * get a date in HL7 format
	 * 
	 * @return formatted date
	 */
	public String now ()
	{
		SimpleDateFormat fmt = new SimpleDateFormat ("yyyyMMddHHmmss");
		return fmt.format(new Date ());		
	}
	
	public String genAck (String seg, String msa1, String msa2)
	{
		if (!seg.startsWith("MSH"))
			return "";
		// do a very simply parse
		String delim = seg.substring(3, 4);
		String[] field = seg.split("\\" + delim);
		if (field.length < 12)
			return ("");
		return ("MSH" + 
				delim + field[1] + // HL7 delimiters
				delim + field[4] + // swap sending and receiving application and facility
				delim + field[5] +
				delim + field[2] + 
				delim + field[3] +
				delim + now () + // time stamp it
				delim + delim + "ACK" + delim +
				delim + field[10] +  // process ID
				delim + field[11] + // HL7 version ID
				"\rMSA" + delim + msa1 + // code
				delim + field[9] + // message ID
				delim + msa2 + "\r");		// comment
	}
	
	public String getAck (String[] seg, int n)
	{
		String ack = "";
		if (seg.length == 0)
			return "";
		for (int i = 0; i < seg.length; i++)
		{
			if (seg[i].startsWith("MSH"))
			{
				if (n == 0)
				  ack += genAck (seg[i], code, comment);
				else if (n-- == 1)
					return genAck (seg[i], code, comment);
			}
		}
		return ack;
	}
}
