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

package tdunnick.phinmsx.domain.payload;

import tdunnick.phinmsx.minihl7.*;

/**
 * This is an Hl7 Object with methods for parsing and formatting "bar" delimited
 * Hl7.  Suitable for conversion to FESI JSObject "Payload" in the FESI helper.
 * 
 * @author Thomas Dunnick
 *
 */
public class Hl7Payload extends Hl7Msg implements FesiPayload
{
  private Hl7BarParser parser = new Hl7BarParser ();
  private String name = "HL7"; 

  public Hl7Payload (String name, byte[] data)
  {
  	this.name = name;
  	parse (new String (data));
  }
  
	public Hl7Payload (byte[] data)
  {
  	parse (new String (data));
  }
  
  public byte[] getData()
	{		
   
		return format().getBytes();
	}

	public String getName()
	{
		return name;
	}

	public void setData(byte[] data)
	{
		parse (new String (data));		
	}
  
  public String encode (String s)
  {
  	return parser.encode(s);
  }
  
  public String decode (String s)
  {
  	return parser.decode(s);
  }
  
  public int parse (String msg)
  {
  	try
  	{
  	  return parser.parse (this, msg);
  	}
  	catch (Exception e)
  	{
  		return (0);
  	}
  }
  
  public String format ()
  {
  	try
  	{
  	  return parser.format(this);
  	}
  	catch (Exception e)
  	{
  		return "";
  	}
  }
}
