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
import org.apache.xerces.impl.dv.util.Base64;
import groovy.util.GroovyTestCase;
import tdunnick.phinmsx.domain.receiver.*
import tdunnick.phinmsx.controller.*

/**
 * @author user
 *
 */
public class ReceiverTest extends GroovyTestCase
{
	Receiver srv;
	
	void setUp() throws Exception
	{
		srv = new Receiver ();
		srv.initialize ("config/Receiver.xml")
	}
	
	void tearDown() throws Exception
	{
	}
	
	void testGetResponse ()
	{
		String fname = "foobar.txt"
		String payload = "the quick brown fox"
		println ("Loading env")
		RcvEnv env = srv.getEnv (null);
		env.payloadName = fname
		env.payload = payload.getBytes();
		println ("getting response")
		StringBuffer r = srv.getResponse (env)
		println r.toString ()
		RcvRequest rq = new RcvRequest ();
        ByteArrayInputStream inp = new ByteArrayInputStream (r.toString().getBytes())
        assert rq.parse (inp) : "failed to parse response"
        assert rq.getFileName().equals(fname) : "file name"
        assert new String(rq.getPayLoad()).equals(payload)
	}	
}
