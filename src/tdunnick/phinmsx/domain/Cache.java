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

/**
 * A simple data cache with a time-to-live.  The cache gets cleaned on 
 * every "put".  A more elegant solution would be a cleaner thread...
 * 
 * @author Thomas Dunnick
 *
 */
public class Cache
{
	private HashMap map = new HashMap();
	
	private void clean ()
	{
		long now = new Date().getTime();
		ArrayList expired = new ArrayList();
		Iterator it = map.entrySet().iterator();
		// collect all the expired keys
		while (it.hasNext())
		{
			Map.Entry e = (Map.Entry) it.next();
			Object[] entry = (Object[]) e.getValue();
			long t = ((Long) entry[0]).longValue();
			if (t < now)
				expired.add(e.getKey());
		}
		// remove them from the map
		for (int i = 0; i < expired.size(); i++)
			map.remove (expired.get(i));
	}
	
	/**
	 * Add or replace data in the cache.  Clean out expired data.  Use null
	 * key if you simply want to run the cleaner.
	 * 
	 * @param key of data to put
	 * @param data saved
	 * @param lifetime (minimum) in seconds for data to live in cache
	 */
	public void put (String key, Object data, int lifetime)
	{
		if (key != null)
		{
			long now = new Date().getTime() + lifetime * 1000;
			Object[] entry = { new Long(now), data };
			map.put(key, entry);
		}
		clean();
	}
	
	public Object get (String key)
	{
		Object[] entry;
		if ((entry = (Object[]) map.get(key)) == null)
			return null;
		return entry[1];
	}
}
