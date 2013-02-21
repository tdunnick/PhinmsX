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

import org.apache.log4j.Logger
import groovy.util.GroovyTestCase;
import tdunnick.phinmsx.util.Phinms;

public class PhinmsTest extends GroovyTestCase
{	
  String p270 = "C:\\Program Files\\PhinMS\\2.7.0\\tomcat-5.0.19\\webapps\\receiver";
  String expected = null;
  String v = null;
  
  void testUnknown ()
  {
    expected = "Unknown PHIN-MS Version";
    assert Phinms.setPath (".") == null : "Set bogus PHIN-MS path";
    v = Phinms.getVersion ();
    assert v != null : "Couldn't find PHINMS version";
    assert v.equals (expected) : "Expected " + expected + " got " + v;
  }
  
  void testGetVersion ()
  {
    Phinms.setPath(p270);
    expected = "CDC PHIN-MS Version 2.7.00 SP1 Build 20070327";
    v = Phinms.getVersion ();
    assert v != null : "Couldn't find PHINMS version";
    assert v.equals (expected) : "Expected " + expected + " got " + v;

  }
  
  void testGetPath ()
  {
    expected = p270.replaceFirst("\\\\webapps.*\$", "");
    v = Phinms.getPath(null);
    assert v != null : "Couldn't find tomcat install path";
    assert v.equals (expected) : "Expected " + expected + " got " + v;
    expected += "\\phinms\\logs"
    v = Phinms.getPath ("logs");
    assert v != null : "Couldn't find phinms logs";
    assert v.equals (expected) : "Expected " + expected + " got " + v;
  }
  
  void testGetenv ()
  {
  	v = Phinms.getenv ("PATH");
  	assert v != null : "Couldn't read PATH from environment"
  	println v
  	v = Phinms.getenv ("foobar");
  	assert v == null : "Got foobar=" + v
  }
}
