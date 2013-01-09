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
 * A minimal HL7 message implementation.  This uses a very simply minded
 * 5 dimensional array with segments at the top and sub-components at the
 * bottom dimension.  While this is not "sparse", it does greatly simplify
 * the code.
 * 
 * It also provides a "path" method of access to the message data that follows
 * HL7 conventions.  A path starts with the segment name, followed by an optional
 * occurrence, field, repetition, component, and sub-component indexes.
 * 
 * The occurrence indicates which segment of that name to select and the 
 * repetition selects the field repetition.  These are both delimited by
 * the repetition delimiter, defaulting to a colon.  The other indexes are
 * delimited by the path delimiter, defaulting to a dash.  All indexes are
 * "one" based, in that '1' indicates the first item.
 * 
 * Index values may be followed by optional text, as long as it does not
 * contain a path or repetition delimiter.  All parts are optional.  Some
 * example paths for the exact same data are...
 * 
 * MSH-6:2
 * MSH:1-6:2-1
 * MSH:1-6th field:2
 * MSH:1-6th field:2nd repeat-1st component-1st subcomponent
 * 
 * If for example you set the path delimiter to dot and repeat to left 
 * square bracket you could express the same path as...
 * 
 * MSH.6[2
 * MSH[1.6[2nd repetition]
 * MSH[1].6[2].1.1
 * 
 * @author Thomas Dunnick
 *
 */
public class Hl7Msg
{
	// a pattern that segment names must match
	protected final static String SEGEXP = "[A-Z][A-Z][A-Z0-9]";
	// the message segment list
	protected ArrayList segments;
	// HL7 "path" delimiters
	protected String pDelim = "-";
	protected String rDelim = ":";

	/**
	 * creates an empty HL7 message with default delimiters
	 */
	public Hl7Msg()
	{
		segments = null;
	}

	/*********************** methods for indexed use ********************/

	/**
	 * Set up a list for a specified index.
	 * 
	 * @param l to index
	 * @param i index used
	 * @return list at that index or null if fails
	 */
	private ArrayList setList (ArrayList l, int i)
	{
		if ((l == null) || (i < 0))
			return (null);
		while (l.size () <= i)
			l.add (null);
		if (l.get(i) == null)
			l.set (i, new ArrayList ());
		return (ArrayList) l.get(i);
	}
	
	/**
	 * Get a list for a specified index.
	 * 
	 * @param l to index
	 * @param i index used
	 * @return list at that index or null if fails
	 */
	private ArrayList getList (ArrayList l, int i)
	{
		if ((l == null) || (i < 0) || (l.size() <= i))
			return null;
		return (ArrayList) l.get(i);
	}
	
	/**
	 * Get the number of elements at the given index.  Count from where the
	 * index is negative.  E.G if seg < 0, return a count of segments.
	 * 
	 * @param index array to elements
	 * @return count of elements
	 */
	public int size (int[] index)
	{
		if (segments == null)
			return (0);
		ArrayList l = segments;
		for (int i = 0; i < 5; i++)
		{
		  if (index[i] < 0)
			 return l.size ();
		  if ((l = getList (l, index[i])) == null)
			  return (0);
		}
		return (l.size ());
	}
	
	/**
	 * @return number of segments in this message
	 */
	public int size ()
	{
		int[] p = { -1, -1, -1, -1, -1 };
		return (size(p));
	}
	

	/**
	 * Set the value for an HL7 element.  This will force the creation of
	 * all needed parts of a message, but beware un-named segments!
	 * 
	 * @param index to message
	 * @param value to set
	 * @return true if successful
	 */
	public boolean set (int[] index, String value)
	{
		ArrayList l = segments;
		
		if ((l == null) || (index[0] < 0) || (index[0] >= segments.size()))
			return (false);
		for (int i = 0; i < 4; i++)
		{
			l = setList (l, index[i]);
		}
		while (l.size() <= index[4])
			l.add (null);
	  l.set (index[4], value);
	  return true;
	}
	
	/**
	 * Get the value of an HL7 element.
	 * 
	 * @param indexes to message
	 * @return the element value or null if not found
	 */
	public String get (int[] index)
	{
		ArrayList l = segments;
		for (int i = 0; i < 4; i++)
		  l = getList (l, index[i]);
		if (l != null)
			return (String) l.get(index[4]);
		return null;
	}

	/**
	 * clears data from the given element(s).  If an index is negative,
	 * clears all the elements below it.  A negative seg clears the whole
	 * message, while a negative field removes a segment.
	 * 
	 * @para indexes to message
	 */
	public void clear (int[] index)
	{
		ArrayList p, l = segments;
		if (index[0] < 0)
		{
			segments = null;
			return;
		}
		for (int i = 0; i < 4; i++)
		{
		if ((l = getList (p = l, index[i])) == null)
			return;
		if (index[i+1] < 0)
		{
			if (i == 0)
				segments.remove(index[0]);
			else
				p.set(index[i], null);
			return;				
		}
		}
		if ((l = getList (l, index[4])) != null)
			l.set(index[4], null);
	}
	
	/**
	 * Create a new segment
	 * 
	 * @param index in the message to add the segment
	 * @param name of the segment to add
	 * @return path to the new segment
	 */
	protected boolean newSegment (int index, String name)
	{
		int[] p = { index, 0, 0, 0, 0 };
		if (!name.matches(SEGEXP))
			return (false);
		if (segments == null)
			segments = new ArrayList ();
		if ((index < 0) || (index > segments.size ()))
			return (false);
		if (index < segments.size())
			segments.add (index, new ArrayList());
		else
			segments.add (new ArrayList());
		set(p, name);
		return (true);
	}

	
	/******************* methods for path based use **********************/
	/**
	 * Set the delimiters used for HL7 paths
	 * 
	 * @param rd repeat delimiter
	 * @param pd path delimiter
	 */
	public void setPathDelims (String rd, String pd)
	{
		rDelim = rd;
		pDelim = pd;
	}
	
	/**
	 * Gets the segment index for a path
	 * @param path to parse
	 * @return the segment index or -1 for failure
	 */
	protected int segIndex (String path)
	{
		int[] p = { 0, 0, 0, 0, 0 };
		if (path == null)
			return (-1);
		int rep = 1;
		if (path.length() > 3)
		{
			if (!path.substring(3, 4).equals(rDelim) ||
			  ((rep = getInt (path.substring(4))) < 1))
				return (-1);
		}
		for (int i = 0; i < segments.size(); i++)
		{
			p[0] = i;
			String s = get (p);
			if ((s != null) && path.startsWith(s) && (rep-- == 1))
				return (i);
		}
		return (-1);
	}
	
	/**
	 * build a path to a segment
	 * @param index of segment needing path
	 * @return path or null if none found
	 */
	protected String segPath (int index)
	{
		int[] p = { 0, 0, 0, 0, 0 };
		if ((segments == null) || (index < 0) || (index >= segments.size()))
		  return null;
		HashMap seg = new HashMap ();
		String s = null;
		int n = 1;
		
		for (int i = 0; i <= index; i++)
		{
			p[0] = i;
			s = get (p);
			if (s != null)
			{
				n = 1;
				if (seg.containsKey(s))
					n = ((Integer) seg.get(s)).intValue() + 1;
				seg.put(s, new Integer (n));
			}
		}
		if (s == null)
			return null;
		return (s + rDelim + n);
	}
	
	/**
	 * Parse a path into indexes for accessing elements.
	 * 
	 * @param path to parse
	 * @param pi default index to element
	 * @return array of indexes to elements.
	 */
	protected int[] getPath (String path, int[] pi)
	{				
		if (path == null)
			return pi;
		String[] p = path.split(pDelim);
		pi[0] = segIndex (p[0]);
		// parse out field and repeat
		if (p.length > 1)
		{
			String[] p2 = p[1].split(rDelim);
			pi[1] = getInt(p2[0]);
			if (p2.length > 1)
				pi[2] = getInt(p2[1]) - 1;
		}
		// finally the comp and sub
		if (p.length > 2)
		{
			// default repeat to 0 if something follows...
			if (pi[2] < 0)
				pi[2] = 0;
			pi[3] = getInt (p[2]) - 1;
			if (p.length > 3)
				pi[4] = getInt (p[3]) - 1;
		}
		return pi;
	}
	
	/**
	 * Parse a path into indexes for accessing elements.  The default
	 * path is to the first index of each dimension in the path
	 * 
	 * @param path to parse
	 * @return array of indexes to elements.
	 */

	protected int[] getPath (String path)
	{
		int[] pi = { 0, 0, 0, 0, 0 };
    return getPath (path, pi);
	}
	
	public int size (String path)
	{
	  int[] pi = {-1, -1, -1, -1, -1};
	  return size (getPath (path, pi));
	}
	
	/**
	 * put a new segment into the message return it's path
	 * 
	 * @param index of new segement
	 * @param name of the segment
	 * @return path the new segment or null if fails
	 */
	public String putSegment (int index, String name)
	{
		if (newSegment (index, name))
		  return segPath (index);
		return null;
	}
	
	/**
	 * Insert a new segment just before the identified segment.
	 * 
	 * @param path to identified segment
	 * @param name of the segment to insert
	 * @return path to the new segment
	 */
	public String insertSegment (String path, String name)
	{
		return (putSegment (segIndex (path), name));
	}
	
	/**
	 * Add a new segment to the message.
	 * 
	 * @param name of the segment to add
	 * @return path to the new segment
	 */
	public String addSegment (String name)
	{
		if (segments == null)
			return putSegment (0, name);
		return (putSegment (segments.size(), name));
	}
	
	/**
	 * Append a segment after an identified segment
	 * @param path to the identified segment
	 * @param name of the segment to append
	 * @return path to the new segment
	 */
	public String appendSegment (String path, String name)
	{
		int i = segIndex (path);
		if (i++ < 0)
			return null;
		return (putSegment (i, name));
	}

	/**
	 * Set the value for an HL7 element.
	 * 
	 * @param path to that element
	 * @param value to set
	 * @return true if successful
	 */
	public boolean set (String path, String value)
	{
		int[] p = getPath (path);
		String segname = path.substring(0, 3);
		
		return set (p, value);		
	}
	
	/**
	 * Get the value of an HL7 element.
	 * 
	 * @param path to the element
	 * @return the element value or null if not found
	 */
	public String get (String path)
	{
		int[] p = getPath (path);
		return get (p);
	}

	/**
	 * clear the element and all sub-elements defined by a path.  If the
	 * path is null the entire message gets cleared.  Field repeats default
	 * to the first if anything follows.  Otherwise the entire field gets cleared.
	 * Likewise, segment occurrences default to the first.
	 * 
	 * @param path to element(s) to clear
	 */
	public void clear (String path)
	{
		int[] pi = { -1, -1, -1, -1, -1 };
		if (path != null)
		{
			pi = getPath(path, pi);
			// ignore silly or invalid paths
			if (pi[0] < 0)
				return;
		}
		clear(pi);
	}
	
	public void clear ()
	{
		clear ("");
	}
	
	/**************************** utility *******************************/
	
	/**
	 * get a date in HL7 format
	 * 
	 * @param d date to format - if null then NOW
	 * @return formatted date
	 */
	public String getDate (Date d)
	{
		SimpleDateFormat fmt = new SimpleDateFormat ("yyyyMMddHHmmss");
		if (d == null)
			d = new Date ();
		return fmt.format(d);		
	}
	
	/**
	 * determine if this segment identifies delimiters.
	 * 
	 * @param s segment to check
	 * @return true if followed by delimiter definitions
	 */
	public boolean hasDelims (String s)
	{
		return s.startsWith("MSH") ||
		  s.startsWith("FHS") ||
		  s.startsWith("BHS");
	}
	
	
	/**
	 * Safe integer parse
	 * @param s integer to parse
	 * @return value or -1 if fails
	 */
	protected int getInt (String s)
	{
		if (s == null)
			return (-1);
		try
		{
			return Integer.parseInt(s);
		}
		catch (Exception e)
		{
			// ignore
		}
		return (-1);
	}
	
	protected void debug (String msg)
	{
		System.out.println (msg);
	}
}
