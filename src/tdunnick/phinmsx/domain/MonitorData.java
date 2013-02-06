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

import java.util.*;
import java.io.*;

/**
 * @author user
 *
 */
public class MonitorData
{
	String version = null;
	String phinmsVersion = null;
	ArrayList tables = null;					// phinms table names
	String table = null;					// current table
	String constraint = null;					// table route or partyid constraint
	ArrayList fields = null;					// list of dB field names
	ArrayList rowfields = null;					// list shown in rows table
	ArrayList rows = null;					// list of dB records
	ArrayList rowClass = null;			// class names for each row
	ArrayList record = null;				// selected record	
	int recordId = 0;						// current record
	int top, prev = 0;
  
  public MonitorData (String version, String phinmsVersion)
	{
		super();
		this.version = version;
		this.phinmsVersion = phinmsVersion;
	}

	public String getVersion ()
  {
  	return version;
  }
  
  public String getPhinmsVersion ()
  {
  	return phinmsVersion;
  }
  
  public ArrayList getTables()
	{
		return tables;
	}

	public void setTables(ArrayList tables)
	{
		this.tables = tables;
	}

	public ArrayList getFields()
	{
		return fields;
	}

	public void setFields(ArrayList fields)
	{
		this.fields = fields;
	}

	public ArrayList getRows()
	{
		return rows;
	}

	public void setRows(ArrayList rows)
	{
		this.rows = rows;
	}

	public ArrayList getRowClass()
	{
		return rowClass;
	}

	public void setRowClass (ArrayList rowClass)
	{
		this.rowClass = rowClass;
	}
	
	public ArrayList getRecord()
	{
		return record;
	}

	public void setRecord(ArrayList record)
	{
		this.record = record;
	}

	public String getTable()
	{
		return table;
	}

	public void setTable(String table)
	{
		this.table = table;
	}

	public String getConstraint()
	{
		return constraint;
	}

	public void setConstraint(String constraint)
	{
		this.constraint = constraint;
	}

	public int getRecordId()
	{
		return recordId;
	}

	public void setRecordId(int recordId)
	{
		this.recordId = recordId;
	}

	public void setVersion(String version)
	{
		this.version = version;
	}

	public void setPhinmsVersion(String phinmsVersion)
	{
		this.phinmsVersion = phinmsVersion;
	}

	public int getTop()
	{
		return top;
	}

	public void setTop(int top)
	{
		this.top = top;
	}

	public void setPrev(int prev)
	{
		this.prev = prev;
	}
	
	public String getPrev ()
	{
		if (prev > 0)
			return (Integer.toString(prev));
		return null;
	}

	public ArrayList getRowfields()
	{
		return rowfields;
	}

	public void setRowfields(ArrayList rowfields)
	{
		this.rowfields = rowfields;
	}
  
	private int getRecordId (int row)
	{
		try
		{
		ArrayList r = (ArrayList) rows.get(row);
		return (Integer.parseInt((String) r.get(0)));
		}
		catch (Exception e)
		{
			return (0);
		}
	}
		
	public String getNext ()
	{
		if (rows.size() >= 10)
		{
			return (Integer.toString(getRecordId (9)));
		}
		return null;
	}
}
