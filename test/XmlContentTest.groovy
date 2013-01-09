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
import java.io.*;
import tdunnick.phinmsx.util.XmlContent;

class XmlContentTest extends TestCase
{
  def xml = """<?xml version="1.0" encoding="UTF-8"?>  	
<EncryptedData Id="ed1" Type="http://www.w3.org/2001/04/xmlenc#Element" xmlns="http://www.w3.org/2001/04/xmlenc#">
  <EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#tripledes-cbc"/>
  <KeyInfo xmlns="http://www.w3.org/2000/09/xmldsig#">
    <EncryptedKey xmlns="http://www.w3.org/2001/04/xmlenc#">
      <EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#rsa-1_5"/>
      <KeyInfo xmlns="http://www.w3.org/2000/09/xmldsig#">
        <KeyName>key</KeyName>
      </KeyInfo>
      <CipherData>
        <CipherValue/>
      </CipherData>
    </EncryptedKey>
  </KeyInfo>
  <CipherData>
    <CipherValue/>
  </CipherData>
</EncryptedData>"""
  XmlContent xmlc = new XmlContent();
  String tag = "EncryptedData.KeyInfo.EncryptedKey.KeyInfo.KeyName"

  
	protected void setUp() throws Exception
	{
		ByteArrayInputStream inp = new ByteArrayInputStream (xml.toString().getBytes())
		xmlc.load (inp);
	}

	protected void tearDown() throws Exception
	{
		super.tearDown();
	}

	public final void testLoad()
	{
		assert xmlc.getDoc() != null : "Failed to load xml"
		assert xmlc.createDoc() : "document creation failed"
	}

	public final void testSave()
	{
		ByteArrayOutputStream outp = new ByteArrayOutputStream ();
		assert xmlc.save(outp) : "Save failed"
	}
	
	public final void testGetValue()
	{
		// println "'" + xmlc.getValue(tag) + "'"
		assert xmlc.getValue (tag).equals("key") : "tag value not retrieved"
	    assert xmlc.getValue (tag + "[0").equals("key") : "tag[0 value not retrieved"
	}

	public final void testSetValue ()
	{
		xmlc.setValue (tag, "foobar");
		assert xmlc.getValue(tag).equals("foobar") : "tag value not set"
		xmlc.setValue (tag + "[3]", "the third")
		assert xmlc.getValue(tag + "[3]").equals("the third") : "index value not set"
		assert xmlc.getValue(tag + "[0]").equals("foobar") : "zero index corrupted"
		// println "'" + xmlc.getValue(tag + "[1]") + "'"
		assert xmlc.getValue(tag + "[1]").length() == 0 : "wrong length for empty tag"
		assert xmlc.getTagCount(tag) == 4 : "incorrect tag count"
		// xdump (xmlc)
	}
	
	public final void testAttribute ()
	{
	  assert xmlc.setAttribute (tag, "id", "foobar") : "couldn't set attribute"
	  assert xmlc.getAttribute (tag, "id").equals("foobar") : "attribute didn't match"
		assert xmlc.getAttribute (tag, "id2").length() == 0 : "returned invalid attribute"
	}
	
	public final void testNS ()
	{
		String xname = "test/soap_defaults.xml"
		String xtag = "soap-env:Envelope.soap-env:Header.eb:MessageHeader.eb:From.eb:PartyId"
		FileInputStream inp = new FileInputStream (xname);
		assert xmlc.load (inp) : "Couldn't load " + xname
		assert xmlc.getValue(xtag).equals("FROMPARTYID") : "didn't match value"
		assert xmlc.getAttribute(xtag, "eb:type").equals("zz") : "didn't match attribute"
		xdump (xmlc)
	}
	
	public final void testToString ()
	{
		xdump (xmlc)
		def s = xmlc.toString()
		assert s != null : "failed to save to string"
		// println s
		xmlc.beautify (0);
		s = xmlc.toString(xmlc.getElement ("EncryptedData.KeyInfo"), true)
		assert s != null : "failed to element save to string"
		//println s
	}
	void xdump (XmlContent x)
	{
		ByteArrayOutputStream outp = new ByteArrayOutputStream ();
		x.beautify (-1)
		assert x.save(outp) : "Save failed"
		// println outp.toString()
	}
}
