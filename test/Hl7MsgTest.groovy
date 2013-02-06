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
import tdunnick.phinmsx.minihl7.*;

class Hl7MsgTest extends GroovyTestCase
{
	Hl7Msg hl7 = null;
	
	
	public void setUp() throws Exception
	{
		hl7 = new Hl7Msg();
	}

	public void tearDown() throws Exception
	{
	}
	
	public void testNewSegment ()
	{
		String[] seg = [ "PID", "NK1", "ORC", "OBR", "OBX", "OBX" ];
		for (int i = 0; i < seg.length; i++)
		  assert hl7.newSegment (i, seg[i]) : "didn't add " + seg[i] + " segment"
		assert !hl7.newSegment (1, "999") : "didn't reject bad segment name"
		assert !hl7.newSegment (20, "MSH") : "didn't reject bad index"
		assert hl7.newSegment (0, "MSH") : "didn't insert MSH"
	}
	
	public void testSetGet()
	{
		testNewSegment ();
		String s = "this is a test!";
		int[] p = [ 4, 3, 2, 1, 0 ]
		assert hl7.set (p, s) : "Unable to set " + s;
		String g = hl7.get (p);
		assert g.equals (s) : "Get " + s + " returned " + g;
	}
	
	public void testPath()
	{
		testSetGet ()
		int[] p = hl7.getPath("OBR:1-3:3-2")
		assert p[0] == 4 : "OBR segment wrong"
		assert p[1] == 3 : "OBR field wrong"
		assert p[2] == 2 : "OBR repeat wrong"
		assert p[3] == 1 : "OBR comp wrong"
		assert p[4] == 0 : "OBR sub wrong"
		p = hl7.getPath("OBX:2-3")
		assert p[0] == 6 : "OBX segment wrong"
		assert p[1] == 3 : "OBX field wrong"
		assert p[2] == 0 : "OBX repeat wrong"
		assert p[3] == 0 : "OBX comp wrong"
		assert p[4] == 0 : "OBX sub wrong"
		String s = hl7.get ("OBR:1-3:3-2");
		assert s != null : "get OBR:1-3:3-2 returned null";
		assert s.equals("this is a test!") : "expect this is a test! but got " + s;
	}
	
	public void testSegPath ()
	{
		testNewSegment ()
		String s = hl7.segPath (6);
		assert s.equals ("OBX:2") : "expect OBX:2 but got " + s	
	}
	
	public void testClear ()
	{
		testSetGet ();
		hl7.set ("OBR:1-3-2", "Some other text!");
		String s = hl7.get ("OBR:1-3-2")
		assert s != null : "get OBR:1-3-2 returned null";
		assert s.equals("Some other text!") : "expect Some other text! but got " + s;
		hl7.clear ("OBR:1-3-2");
		assert hl7.get ("OBR:1-3-2") == null : "failed to clear OBR:1-3-2"
		s = hl7.get ("OBR:1-3:3-2");
		assert s != null : "get OBR:1-3:3-2 returned null";
		assert s.equals("this is a test!") : "expect this is a test! but got " + s;
	}
}
