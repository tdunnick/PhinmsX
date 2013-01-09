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

/**
 * A simple class to encapsulate the payload data.  Useful for FESI
 * 
 * @author Thomas Dunnick
 *
 */
public class Payload implements FesiPayload
{
	private String name = "Payload";
	private byte[] data = null;
	
	public Payload (byte[] data)
	{
		this.data = data;
	}
	
	public Payload (String name, byte[] data)
	{
		this.name = name;
		this.data = data;
	}
	
	public byte[] getData()
	{
		return data;
	}

	public void setData(byte[] data)
	{
		this.data = data;
	}

	public String getName()
	{
		return name;
	}

	public int length ()
	{
		return data.length;
	}
	
	public void parse (String data)
	{
		this.data = data.getBytes();
	}
	
	public String toString ()
	{
		return new String (data);
	}
	
	public byte get (int i)
	{
		return data[i];
	}
	
	public void set (int i, int v)
	{
		data[i] = (byte) v;
	}
}
