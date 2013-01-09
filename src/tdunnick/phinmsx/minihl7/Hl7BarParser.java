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

package tdunnick.phinmsx.minihl7;

import java.util.*;
import java.text.*;

/**
 * Minimal implementation of an HL7 message class for "bar" formated
 * messages.
 * 
 * @author Thomas Dunnick
 *
 */
public class Hl7BarParser implements Hl7Parser
{
	// standard HL7 delimiters
	private final static String DELIMS = "|^~\\&";
	private final static int FIELD = 0;
	private final static int COMP = 1;
	private final static int REP = 2;
	private final static int ESC = 3;
	private final static int SUB = 4;
	
	/**
	 * Trim naked delimiters from this buffer
	 * @param buf to trim
	 * @param c delimiter to trim
	 */
	private void buftrim (StringBuffer buf, char c)
	{
		int i = buf.length();
		while ((i > 0) && (buf.charAt(i-1) == c))
		  i--;
		buf.setLength(i);
	}
	
	public String encode (String data)
	{
		return encode (data, DELIMS.toCharArray());
	}
	
	/**
	 * encodes HL7 escapes
	 * 
	 * @param data to encode
	 * @param dl delimiters to use
	 * @return encoded string
	 */
	public String encode (String data, char[] dl)
	{
		char[] esc = { 'F', 'S', 'R', 'E', 'T' };
		if (data == null)
			return (null);
		StringBuffer b = new StringBuffer (data);
		if (dl == null)
			dl = DELIMS.toCharArray();
		int i = 0;
		while (i < b.length())
		{
			char c = b.charAt(i);
			for (int j = 0; j < dl.length; j++)
			{
			  if (c == dl[j])
			  {
			  	b.setCharAt(i++, dl[ESC]);
			  	b.insert(i++, esc[j]);
			  	b.insert(i, dl[ESC]);
			  	break;
			  }
			}
			i++;
		}
		return b.toString();
	}
	
	public String decode (String data)
	{
		return decode (data, DELIMS.toCharArray());
	}

	/**
	 * decodes HL7 escapes
	 * 
	 * @param data to decode
	 * @param dl delimiters used
	 * @return decoded string
	 */
	public String decode (String data, char[] dl)
	{
		char[] esc = { 'F', 'S', 'R', 'E', 'T' };
		if (data == null)
			return (null);
		StringBuffer b = new StringBuffer (data);
		if (dl == null)
			dl = DELIMS.toCharArray();
		int i = 0;
		while (i < b.length())
		{
			if (b.charAt(i) == dl[ESC])
			{
				b.deleteCharAt(i);
				char c = b.charAt(i);
				for (int j = 0; j < dl.length; j++)
				{
					if (c == esc[j])
					{
						b.setCharAt(i, dl[j]);
						break;
					}
				}
				b.deleteCharAt(i + 1);
			}
			i++;
		}
		return b.toString();
	}
	

	/**
	 * Parse HL7 data into a message. 
	 * @param data to parse
	 * @return number of segments parsed
	 * @throws Exception if data parse fails
	 */
	public int parse (Hl7Msg msg, String data) throws Exception
	{
		int[] p = { 0, 0, 0, 0, 0 };
		char[] dl = DELIMS.toCharArray();
		String[] s = data.replace('\n','\r').split("\r+");
		p[0] = msg.size();
		for (int i = 0; i < s.length; i++)
		{
			// be nice and ignore blank lines
			if (s[i].trim().length() == 0)
				continue;
			// gather delimiter for MSH, etc segments
			if (msg.hasDelims (s[i]))
			{
				String delims = s[i].substring (3,8);
				s[i] = s[i].replace (delims.substring(1), delims.substring(0,1));
				dl = delims.toCharArray();
			}
			else if (!s[i].matches(msg.SEGEXP + dl[0] + ".*"))
			{
				// garbage in the data cause big time failure
				msg.clear ();
				throw new Exception ("Invalid segment at line " + (i+1) + ": " + s[i]);
			}
			// parse fields, repeats, components, and subcomponents of this segment
			String[] f = s[i].split ("\\" + dl[FIELD]);
			if (!msg.newSegment (p[0], f[0]))
			{
				throw new Exception ("Failed to add segment " + f[0]);
			}
			for (p[1] = 1; p[1] < f.length; p[1]++)
			{
				String[] r = f[p[1]].split("\\" + dl[REP]);
				for (p[2] = 0; p[2] < r.length; p[2]++)
				{
					String[] c = r[p[2]].split("\\" + dl[COMP]);
					for (p[3] = 0; p[3] < c.length; p[3]++)
					{
						String[] sc = c[p[3]].split("\\" + dl[SUB]);
						for (p[4] = 0; p[4] < sc.length; p[4]++)
						{
							// decode the value and stuff it
							msg.set (p, decode (sc[p[4]], dl));
						}
					}
				}
			}
			p[0]++;
		}
		// set up to rehash our segment map
		return p[0];
	}
	
	public String format (Hl7Msg msg) throws Exception
	{
		return format (msg, DELIMS);
	}
	
	/**
	 * Format a message using a set of delimiters
	 * @param delims to use
	 * @return formated HL7 message with specified delimiters
	 * @throws Exception for malformed messages
	 */
	public String format (Hl7Msg msg, String delims) throws Exception
	{
		int[] p = { -1, -1, -1, -1, -1 };
		StringBuffer buf = new StringBuffer();
		if (delims == null) 
			delims = DELIMS;
		else if (delims.length() < 5)
			return ("");
		char[] dl = delims.toCharArray();
		/*
		 *  walk through our segment formatting fields, repeats, component, 
		 *  and sub components.  In each loop we append a delimiter.  Then
		 *  at the end of the loop remove any extraneous delimiter.  Note
		 *  semantically required, but tidy!
		 */
		int numseg = msg.size ();
		for (p[0] = 0; p[0] < numseg; p[0]++)
		{
			int numfields = msg.size (p);
			for (p[1] = 0; p[1] < numfields; p[1]++)
			{
				int numreps = msg.size (p);
				for (p[2] = 0; p[2] < numreps; p[2]++)
				{
					int numcomps = msg.size (p);
					for (p[3] = 0; p[3] < numcomps; p[3]++)
					{
						String s = "";
						int numsubs = msg.size (p);
						for (p[4] = 0; p[4] < numsubs; p[4]++)
						{
							s = msg.get(p);
							if (s != null)
							{
								// encode and append the data
								buf.append(encode(s, dl));
							}
							buf.append(dl[SUB]);
						}
						buftrim(buf, dl[SUB]);

						/*
						 * if this is a MSH or other segment where delimiters are defined,
						 * append the current set and skip the next pair of fields
						 * (semantically, the p[1] separator and other delimiters)
						 */
						if (p[1] == 0)
						{
							if (!s.matches(msg.SEGEXP))
							{
								throw new Exception("Invalid segment name " + s);
							}
							if (msg.hasDelims(s))
							{
								buf.append(delims);
								p[1] += 2;
							}
						}
						buf.append(dl[COMP]);
						p[4] = -1;
					}
					buftrim(buf, dl[COMP]);
					buf.append(dl[REP]);
					p[3] = -1;
				}
				buftrim(buf, dl[REP]);
				buf.append(dl[FIELD]);
				p[2] = -1;
			}
			buftrim(buf, dl[FIELD]);
			buf.append('\r');
			p[1] = -1;
		}
		return buf.toString();
	}	
}