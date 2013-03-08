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
import tdunnick.phinmsx.domain.*;
import tdunnick.phinmsx.util.*;

/**
 * Data passed around within the message handler to process a particular
 * request.
 * 
 * @author tld
 *
 */
public class RcvEnv
{
	// properties for this environment
	public Props props = null;
	
	// values set from the incoming message METADATA
	// incoming file name
	public String fileName = null;
	// incoming file suffix (should be the process ID)
	public String fileSuffix = null;
	// path to destination file
	public String filePath = null;
	// return payload and name
	public String payloadName = null;
	public byte[] payload = null;

	// items returned to our sender
	// the initial values are typical stock returns for PHINMS
	public String applicationStatus = "success";
	public String applicationError = "none";
	public String applicationResponse = "success";
  
  /**
   * returns one of the environments properties prepending the root
   * 
   * @param name of property desired
   * @return the property
   */
  public String getProperty (String name)
  {
  	return props.getProperty (name);
  }
  
  public String getPassword (String name)
  {
  	return props.getPassword (name);
  }
}
