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

package tdunnick.phinmsx.minihl7;

public class Hl7Ack extends Hl7Msg
{
	
	public Hl7Ack()
	{
		super();
	}

	public Hl7Ack (Hl7Msg msg)
	{
		super ();
		genAck (msg);
	}
	
	public boolean genAck (Hl7Msg msg)
	{
		for (int i = 1; msg.segIndex("MSH:" + i) >= 0; i++)
			addAck (msg, "MSH:" + i, "AA", "MSG OK");
		return true;
	}
	
	private void copyField (String dst, Hl7Msg msg, String src)
	{
		for (int r = 1; r <= msg.size (src); r++)
		{
			String s1 = ":" + r;
			for (int c = 1; c <= msg.size (src + s1); c++)
			{
				String s2 = s1 + "-" + c;
				for (int s = 1; s <= msg.size (src + s2); s++)
				{
					String s3 = s2 + "-" + s;
					set (dst + s2, msg.get(src + s2));
				}
			}
		}
	
		//set (dst, msg.get (src));
	}
	
	public boolean addAck (Hl7Msg msg, String path, String code, String cmt)
	{
		String p = addSegment("MSH");
		copyField (p + "-3", msg, path + "-5"); // set sending to receiving app
		copyField(p + "-4", msg, path + "-6"); // set sending to receiving facility
		copyField(p + "-5", msg, path + "-3"); // and vice versa
		copyField(p + "-6", msg, path + "-4");
		set(p + "-7", getDate(null)); // message date and time
		set(p + "-9", "ACK"); // message type
		copyField(p + "-11", msg, path + "-11"); // processing ID
		copyField(p + "-12", msg, path + "-12"); // version ID
		p = addSegment("MSA");
		set(p + "-1", code);
		copyField(p + "-2", msg, path + "-10");
		set(p + "-3", cmt);
		return true;
	}
}
