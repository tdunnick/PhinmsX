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

import junit.framework.TestCase
import gov.cdc.nedss.services.transport.message.*
import tdunnick.phinmsx.domain.*
import tdunnick.phinmsx.controller.*

/**
 * @author user
 *
 */
public class Hl7AckServletTest extends TestCase
{
	Receiver srv;
	
	void setUp() throws Exception
	{
		srv = new Receiver ();
		srv.initialize ("config/Hl7AckServlet.xml")
	}
	
	void tearDown() throws Exception
	{
	}
	
	void testGetResponse ()
	{
		RcvEnv env = srv.getEnv (null);
		StringBuffer r;
		//r = srv.getResponse (env)
		//println r.toString() + "\n\n"
		env.fileName = "foobar.txt"
		srv.setResponsePayload(env, "the quick brown fox".bytes)
		r = srv.getResponse (env)
		println r.toString ()
		HttpMultiPartParser hmp = new HttpMultiPartParser();
		InputStream bins = new ByteArrayInputStream (r.toString().getBytes())
		StringBuffer headers = new StringBuffer ()
		hmp.processHttpResponse(bins, headers, 
			"multipart/related; type=\"text/xml\"; boundary=\"1334427098890\"; start=\"textmimepart\"",
			null)
		String payloadcontent = hmp.getPayloadPart();
		String payloadfilename = hmp.getFilename();
		String params = hmp.getTextPart();
		println "Payload:" + payloadcontent + "\n" + 
		  "File:" + payloadfilename + "\n" + 
		  "Parms:" + params
	}

	void testInit()
	{
		// fail("Not yet implemented")
	}	
	
	void testDestroy()
	{
		// fail("Not yet implemented")
	}
	
}
