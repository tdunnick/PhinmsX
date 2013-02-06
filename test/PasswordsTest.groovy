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
import tdunnick.phinmsx.crypt.Passwords;
import tdunnick.phinmsx.util.XmlContent;


/**
 * @author user
 *
 */
public class PasswordsTest extends GroovyTestCase
{
	Passwords p;
	XmlContent xml;
	String fname = "test/receiverpasswds";
	String tname = "test/receiverpasswds.new"
	String key = "028itfg7";
	String seed = "149148144182192151153106";
		
	
	void setUp() throws Exception
	{
		p = new Passwords ();
		xml = new XmlContent ();
	}
	
	void tearDown() throws Exception
	{
	}
	
	void testLoad ()
	{
		String k = "RECEIVER_databaseUser1";
		String v = "msAccessDbUser";
		
		assert p.load (fname, seed, key) : "Failed loading " + fname;
		xml = p.getXml ();
		assert xml.getRoot().equals ("passwordFile") : "Got root " + xml.getRoot()
		assert p.get(k).equals(v) : "Got " + p.get(k) + " for " + k
		// xml.beautify (2);
		// print xml.toString ();
	}
	
	void testSave ()
	{
	  p.load (fname, seed, key);
	  String s = p.getXml().toString ();
	  assert p.save (tname, seed, key) : "failed saving " + tname;
	  p.load (tname, seed, key);
	  assert s.equals (p.getXml().toString()) : tname + " doesn't match " + fname
	  File f = new File (tname);
	  f.delete ();
	}
}
