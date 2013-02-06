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

import org.apache.log4j.Logger
import groovy.util.GroovyTestCase;
import tdunnick.phinmsx.domain.*;
import tdunnick.phinmsx.util.*;

/**
 * @author user
 *
 */
public class PropsTest extends GroovyTestCase
{
	Props props;
	
	void setUp() throws Exception
	{
		props = new Props ();
		assert props.load ("config/receiver.xml") : "Failed loading properties"
		props.setLogger (XLog.getRootLogger(true));
	}
	
	void tearDown() throws Exception
	{
	}
	
	void testGetTableName()
	{
		assert props.getTableName().equals("testworkerqueue") : "failed getting table name"
	}
	
	void testGetProperty()
	{
		assert props.getProperty (Props.QUEUENAME).equals("workerqueue") : "failed getting queue name"
 	}
	
	void testGetLogger()
	{
		assert props.getLogger() != null : "no logger!"
	}	
}
