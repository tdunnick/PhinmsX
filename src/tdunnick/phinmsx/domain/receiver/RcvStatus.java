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

/**
 * This singleton is used to maintain the receiver status and serves as the 
 * bean for rcvservlet.jsp
 * 
 * @author user
 *
 */
public class RcvStatus
{
	static RcvStatus status = new RcvStatus();
	String version = null;
	String phinmsVersion = null;
	String[] fields = null;
  ArrayList records = null;
  
  private RcvStatus ()
  {
  }

	public static RcvStatus getStatus()
	{
		return status;
	}

	public String getVersion()
	{
		return version;
	}

	public void setVersion(String version)
	{
		this.version = version;
	}

	public String getPhinmsVersion()
	{
		return phinmsVersion;
	}

	public void setPhinmsVersion(String phinmsVersion)
	{
		this.phinmsVersion = phinmsVersion;
	}

	public String[] getFields()
	{
		return fields;
	}

	public void setFields(String[] fields)
	{
		this.fields = fields;
	}

	public ArrayList getRecords()
	{
		return records;
	}

	public void setRecords(ArrayList records)
	{
		this.records = records;
	}
}
