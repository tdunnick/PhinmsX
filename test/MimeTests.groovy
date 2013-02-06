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

import groovy.util.GroovyTestCase;
import tdunnick.phinmsx.domain.receiver.*;


/**
 * @author user
 *
 */
public class MimeTests extends GroovyTestCase
{
	RcvRequest rq;
	String status = "the status",
	error = "the error",
	appdata = "the message",
	payload = "the payload",
	filename = "the filename";
	
	void setUp() throws Exception
	{
		rq = new RcvRequest ();
		ByteArrayInputStream inp = new ByteArrayInputStream (getResponse().getBytes())
		assert rq.parse (inp) : "failed to parse response"
	}
	
	void tearDown() throws Exception
	{
	}
	
	String getResponse ()
	{
		RcvResponse rs = new RcvResponse ();
		StringBuffer s = rs.getResponse (status, error, appdata,
				payload.getBytes(), filename);
		// println s.toString();
		return s.toString();
	}
	
	void testGetArgument()
	{  
		String s = rq.getArgument("status")
		assert s.equals(status) : "status doesn't match '"  + s + "'";
		s = rq.getArgument("error")
		assert s.equals(error) : "error doesn't match '" + s + "'";
		s = rq.getArgument("appdata")
		assert s.equals(appdata) : "appdata doesn't match '" + s + "'";
	}
	
	void testGetPayLoad()
	{
		String s = new String(rq.getPayLoad())
		assert s.equals(payload) : "payload doesn't match '" + s + "'"
	}
	
	void testGetFileName ()
	{
		String s = rq.getFileName()
		assert s.equals(filename) : "file name doesn't match '" + s + "'"
	}	
}
