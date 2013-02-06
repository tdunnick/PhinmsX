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

package tdunnick.phinmsx.util;

import java.io.*;
import java.util.*;

import org.w3c.dom.*;
import org.apache.xml.serialize.*;
import javax.xml.parsers.*;

/** 
 * A simplified narrow view of XML documents implemented using the DOM model.
 * Access is accomplished using (Properties like) name strings, where individual
 * XML tag names are separated by dots, and repeated names are accessed as arrays.  
 * For example consider...
 * <pre>
 * 
 * <foo>
 *   <bar>
 *     <junk>stuff</junk>
 *   </bar>
 *   <bar>
 *     <junk>crap</junk>
 *   </bar>
 *   <bar>
 *     <junk>nothing</junk>
 *   </bar>
 * </foo>
 * 
 * </pre>
 * In this case getValue("foo.bar[1].junk") would return "crap".  Note the
 * first name can never be indexed since it is the root.
 * 
 * "Values" are set and retrieved as text nodes, with only one logical "Value"
 * per names element path.
 * 
 * @author tld
 * 
 */
public class XmlContent
{
  private Document doc = null;
  private boolean beautify = false;
  private String lastError = "none";

  public String getError ()
  {
  	return lastError;
  }
  
  /**
   * Gets the currently loaded document (for external use)
   * @return document
   */
  public Document getDoc()
	{
		return doc;
	}

	/**
	 * Sets the currently loaded document (for external use)
	 * @param doc to set
	 */
	public void setDoc(Document doc)
	{
		this.doc = doc;
		beautify = false;
	}
	
	/**
	 * Create a document from scratch.
	 * @return true if successful
	 */
	public boolean createDoc ()
	{
		doc = null;
		beautify = false;
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			doc = builder.newDocument();
			return true;
		}
		catch (Exception e)
		{
			lastError = "Failed loading XML: " + e.getMessage();
		}
		return false;
	}
	
	/**
   * Load an XML document from a stream. 
   * @param is stream to read from
   * @return true if successful
   */
  public boolean load (InputStream is)
  {
  	doc = null;
  	beautify = false;
  	if (is == null)
  		return false;
  	try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			doc = builder.parse(is);
			return true;
		}
  	catch (Exception e)
  	{
  		lastError = "Failed loading XML: " + e.getMessage();
  	}
  	return false;
  }
  
	/**
   * Load an XML document from a file.
   * @param f file to read from
   * @return true if successful
   */
  public boolean load (File f)
  {
  	if (f == null)
  		return false;
  	boolean ok = false;
  	try
  	{
  		FileInputStream is = new FileInputStream (f);
  	  ok = load (is);
  		is.close();
  	}
  	catch (IOException e)
  	{
  	  lastError = "Failed opening " + f.getPath() + ": " + e.getMessage();
  	}
  	return ok;
  }
  
  /**
   * Load an XML document from it's string representation
   * @param s with the document
   * @return the document
   */
  public boolean load (String s)
  {
  	if (s == null)
  		return false;
  	ByteArrayInputStream is = new ByteArrayInputStream (s.getBytes());
  	return load (is);
  }
  
  /**
   * Save an XML document to a stream
   * @param n node in the document
   * @param os stream to save to
   * @param fragment true if this is a fragment (no XML declaration)
   * @return true if successful
   */
  public boolean save (Node n, OutputStream os, boolean fragment)
	{
  	if ((doc == null) || (os == null))
  		return false;
  	try
		{
      OutputFormat format = new OutputFormat(doc);
      /*
      if (beautify)
      {
        format.setLineWidth(65);
        format.setIndenting(true);
        format.setIndent(2);
      }
      */
      Writer out = new OutputStreamWriter (os);
      XMLSerializer serializer = new XMLSerializer(out, format);
      serializer.serialize(doc);
      return true;
		}
		catch (Exception e)
		{
  		lastError = "Failed saving XML: " + e.getMessage();
		}
		return false;
	}
  /**
   * Save an XML document to a stream
   * @param os stream to save to
   * @return true if successful
   */
   
  public boolean save (OutputStream os)
  {
  	return save (doc, os, false);
  }
  
  /**
   * Save an XML document to a file
   * @param f file to save to
   * @return true if successful
   */
  
  public boolean save (File f)
  {
  	boolean ok = false;
  	try
  	{
  		FileOutputStream os = new FileOutputStream (f);
  	  ok = save (os);
  		os.close();
  	}
  	catch (IOException e)
  	{
  		lastError = "Failed opening " + f.getPath() + ": " + e.getMessage();
  	}
  	return ok;
  }
  
  /**
   * Finds a child in this element matching the name. 
   * 
   * @param e element to search
   * @param name of child to find
   * @return element matching or null if not found
   */
  private Element findElement (Element e, String name)
  {
  	if ((e == null) || (name == null))
  		return null;
  	NodeList l = e.getChildNodes();
  	int count = 0;
  	int i = name.lastIndexOf('[');
  	if (i > 0)
  	{
  		count = parseInt(name.substring(i+1));
  		name = name.substring(0, i);
  	}
  	for (i = 0; i < l.getLength(); i++)
  	{
  		Node n = l.item(i);
  		if (n.getNodeType() != Node.ELEMENT_NODE)
  			continue;
  		if (n.getNodeName().equals(name) && (count-- < 1))
  			return (Element) n;
  	}
  	return null;
  }
  
  /**
   * Finds an element in the element subtree matching the names.  Ignore redundent
   * "dots" in the name (e.g. "foo.bar.stuff" == "foo.bar..stuff..."
   * @param e
   * @param names
   * @return
   */
  public Element getElement (Element e, String names)
  {
		String[] name = names.split("[.]");
		
		for (int i = 0; i < name.length; i++)
		{
			if (name[i].length() == 0)
				continue;
			e = findElement (e, name[i]);
			if (e == null)
				return null;
		}
		return e;  	
	
  }
  
  /**
   * get the root name for this document.
   * @return null if no root
   */
  public String getRoot ()
  {
  	if (doc == null)
  		return null;
  	Element e = doc.getDocumentElement();
  	if (e == null)
  		return null;
  	return e.getNodeName();
  }
  
  /**
   * Finds an element in the document matching the names.  Ignore redundent
   * "dots" in the name (e.g. "foo.bar.stuff" == "foo.bar..stuff..."
   * 
   * @param names to find
   * @return element for that tag
   */
  public Element getElement (String names)
  {
		if ((doc == null) || (names == null) || (names.length() == 0))
			return null;
		String root = names;
		int dot = names.indexOf(".");
		if (dot > 0)
		{
			root = names.substring(0, dot);
			names = names.substring (dot + 1);
	  }
	  else
	  	names = "";
		Element e = doc.getDocumentElement();
		if (!root.equals(e.getNodeName()))
		{
			lastError = "Root does not match " + names;
			return null;
		}
		return getElement (e, names);
  }
  
  /**
   * Adds an element name (and any needed siblings) to a given element.
   * and return it.
   * 
   * @param e element to add tag to 
   * @param name to add
   * @return element added
   */
  private Element addENode (Element e, String name)
	{
  	if (e == null)
  		return null;
		NodeList l = e.getChildNodes();
		Element el = null;
		int count = 0;
		int i = name.lastIndexOf('[');
		if (i > 0)
		{
			count = parseInt(name.substring(i + 1));
			name = name.substring(0, i);
		}
		for (i = 0; i < l.getLength(); i++)
		{
			Node n = l.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE)
				continue;
			if (n.getNodeName().equals(name))
				count--;
		}
		while (count-- >= 0)
		{
			el = doc.createElement(name);
			e.appendChild(el);
		}
		return el;
	}
  
  /**
   * Adds all the needed elements for names for an element subtree.  Ignore redundant
   * "dots" in the name (e.g. "foo.bar.stuff" == "foo.bar..stuff..."
   * @param e place in subtree
   * @param names to add
   * @return element for the added (or found) tag
   */
 
  public Element addElement (Element e, String names)
  {
  	if ((e == null) || (names == null))
  		return null;
		String[] name = names.split("[.]");
		int i;
		for (i = 0; i < name.length; i++)
		{
			if (name[i].length() == 0)
				continue;
			Element e2 = findElement (e, name[i]);
			if (e2 == null)
				break;
			e = e2;
		}
		while (i < name.length)
		{
			if (name[i].length() > 0)
				e = addENode (e, name[i]);
			i++;
		}
		return e;  		
  }
  
  /**
   * Adds all the needed elements for names.  Ignore redundant
   * "dots" in the name (e.g. "foo.bar.stuff" == "foo.bar..stuff..."
   * @param names to add
   * @return element for the added (or found) tag
   */
  public Element addElement (String names)
  {
		if ((doc == null) || (names == null) || (names.length() == 0))
			return null;
		String root = names;
		int dot = names.indexOf(".");
		if (dot > 0)
		{
			root = names.substring(0, dot);
			names = names.substring (dot + 1);
	  }
	  else
	  	names = "";
		Element e = doc.getDocumentElement();
		if (e == null)
		{
		  e = doc.createElement(root);
			doc.appendChild(e);
		}
		else if (!root.equals(e.getNodeName()))
		{
			lastError = "Root does not match " + names;
			return null;
		}
		return addElement (e, names);
 }
    
  /**
   * Gets the text values for children of this node.
   * All text nodes for the named element are trimmed, concatenated by a 
   * space and returned as the value.
   * @param n node to use
   * @return text value or null if the node doesn't exist
   */
  private String getNodeValue (Node n)
  {
  	if (n == null)
  		return null;
    NodeList l = n.getChildNodes();
    String s = "";
    for (int i = 0; i < l.getLength(); i++)
    {
    	n = l.item(i);
    	if (n.getNodeType() == Node.TEXT_NODE)
     		s = s.trim () + " " + n.getNodeValue();
    }
    return s.trim ();  	
  }
  
  /**
   * Gets the "value" for names.  All text nodes for the named element
   * are trimmed, concatenated by a space and returned as the value.
   * @param names of value desired
   * @return value of name or null if not found
   */
  public String getValue (String names)
	{
    return getNodeValue (getElement (names));
	}
  
  /**
   * Gets the "value" for names from an element subtree.  
   * All text nodes for the named element
   * are trimmed, concatenated by a space and returned as the value.
   * @param e element to search
   * @param names to match
   * @return value of name or null if not found
   */
  public String getValue (Element e, String names)
  {
  	return getNodeValue (getElement (e, names));
  }
  
  /**
   * Get node value as an integer.
   * @param names to match
   * @return value or 0 if not found
   */
  public int getInt (String names)
  {
  	return getInt (names, 0);
  }
  
  /**
   * Get node value as an integer.
   * @param names to match
   * @param dflt value if non-numeric or not found
   * @return value or dflt if not found
   */
  public int getInt (String names, int dflt)
  {
  	String s = getValue (names);
  	if ((s == null) || !s.matches("[0-9]+"))
  	  return dflt;
  	return parseInt(s);
  }
  
  private boolean setValue (Node e, String value)
  {
  	if (e == null)
  		return false;
    NodeList l = e.getChildNodes();
    for (int i = 0; i < l.getLength(); i++)
    {
    	Node n = l.item(i);
    	if (n.getNodeType() == Node.TEXT_NODE)
    	{
    		e.removeChild(n);
    		i--;
    	}
    }
  	Node n = doc.createTextNode(value);
 		e.appendChild(n);
  	return true;
  }
  
  /**
   * Sets the "value" for names at element subtree.  The corresponding element has all of its
   * text nodes removed and replace by this single node value.  A null
   * value is set as an empty string.
   * 
   * @param e subtree
   * @param names desired to set
   * @param value to set
   * @return true if successful
   */
  public boolean setValue (Element e, String names, String value)
  {
  	if (value == null)
  		value = "";
  	return setValue (addENode (e, names), value); 	
  }
  
 
  /**
   * Sets the "value" for names.  The corresponding element has all of its
   * text nodes removed and replace by this single node value.  A null
   * value is set as an empty string.
   * 
   * @param names desired to set
   * @param value to set
   * @return true if successful
   */
  public boolean setValue (String names, String value)
  {
  	if (value == null)
  		value = "";
  	return setValue (addElement (names), value);
  }
  
  /**
   * Delete all the children of a element at names.
   * 
   * @param names element to leave childless
   * @return true if successful
   */
  public boolean delete (String names)
  {
  	Element e = getElement (names);
  	if (e == null)
  		return false;
  	while (true)
  	{
  		Node n = e.getFirstChild();
  		if (n == null)
  			break;
  		e.removeChild(n);
  	}
  	return true;
  }
  /**
   * Get an attribute for names
   * @param names list of interest
   * @param attrib of attribute
   * @return attribute value of null if tag not found
   */
  public String getAttribute (String names, String attrib)
  {
  	if (attrib == null)
  		return null;
  	Element e = getElement (names);
  	if (e == null)
  		return null;
  	return e.getAttribute(attrib);
  }
  
  /**
   * Set an attribute for names.  The element must exist.  A null value
   * is set as an empty string.
   * @param names list of interest
   * @param attrib of attribute to set
   * @param value of attribute to set
   * @return true if successful
   */
  public boolean setAttribute (String names, String attrib, String value)
  {
  	if (attrib == null)
  	  return false;
  	if (value == null)
  		value = "";
  	Element e = getElement (names);
  	if (e == null)
  		return false;
  	e.setAttribute(attrib, value);
  	return true;
  }
  
  /**
   * Get a count of elements matching names
   * 
   * @param names to count
   * @return number of matching elements
   */
  public int getTagCount (String names)
  {
  	int cnt = 0;
  	while (getElement (names + "[" + cnt + "]") != null)
  		cnt++;
  	return cnt;
  }
  
  /**
   * Recursively Merge with another document which may have a different root.
   * This copies elements from the source into this document.
   * 
   * @param src to merge with
   * @param clobber existing nodes if true
   * @param to node being copied
   * @return true if successful
   */
  private boolean merge (XmlContent src, boolean clobber, String path)
  {
  	if (path == null)
  		return false;
  	// first take care of this node
  	Node e = src.getElement(path);
  	if (e == null)
  		return (false);
  	String v = src.getNodeValue (e);
  	if (v != null)
  	{
  		String mypath = getRoot ();
  		int i = path.indexOf('.');
  		if (i > 0)
  			mypath += path.substring(i);
  		if ((getValue (mypath) == null) || clobber)
  			setValue (mypath, v);
  	}
  	// then do all the children, noting the correct index for each
  	e = e.getFirstChild();
  	HashMap nindex = new HashMap ();
  	while (e != null)
  	{
  		if (e.getNodeType() == Node.ELEMENT_NODE)
  		{
  			String name = e.getNodeName();
  			int i = 0;
  			if (nindex.containsKey(name))
  				i = ((Integer)nindex.get(name)).intValue();  			
  			merge (src, clobber, path + "." + name + "[" + i);
  			nindex.put(name, new Integer(i+1));
  		}
  		e = e.getNextSibling();
  	}
  	return true;
  }
  
  /**
   * Merge with another document which may have a different root.
   * This copies elements from the source into this document.
   * 
   * @param src to merge with
   * @param clobber existing nodes if true
   * @return true if successful
   */
  public boolean merge (XmlContent src, boolean clobber)
  {
  	return merge (src, clobber, src.getRoot ());
  }
  
  /**
   * Create an indent string for formatting
   * @param sz of then indentation in spaces
   * @return the indent text
   */
  private String getIndent (int sz)
  {
  	if (sz < 0)
  		return "";
  	StringBuffer buf = new StringBuffer("\n");
  	while (sz-- > 0)
  		buf.append(" ");
  	return buf.toString();
  }
  
  /**
   * Adjust the indent for child elements and format each to it's
   * own line. If the tabsz is < 0, run everything together.  Run
   * recursively over all children.
   * 
   * @param parent to beautify
   * @param tabsz to indent
   * @param level to indent
   */
  private void beautify (Node parent, int tabsz, int level)
  {
  	if (parent == null)
  		return;
  	// boolean needindent = true;
  	String indent = getIndent (tabsz * level);
  	NodeList l = parent.getChildNodes();
  	// check for parent with single child
  	if ((l.getLength() == 1) && (l.item(0).getNodeType() == Node.TEXT_NODE))
  	{
  		return;
  	}
		for (int i = 0; i < l.getLength(); i++)
		{			
			Node n = l.item(i);
			// if we are at a text node, trim it
			if (n.getNodeType() == Node.TEXT_NODE)
			{
				String s = n.getNodeValue().trim();
				// remove empty nodes
				if (s.length() == 0)
				{
					parent.removeChild(n);
					i--;
					continue;
				}
				n.setNodeValue(s);
			}
			// add an indent node...
			Node tn = doc.createTextNode(indent);
			parent.insertBefore(tn, n);
			// get next node
			n = l.item(++i);
			// recurse for non-text nodes, beautify all the children
			if (n.getNodeType() != Node.TEXT_NODE)
				beautify(n, tabsz, level + 1);
		}
		indent = getIndent (tabsz * --level);
		Node n = doc.createTextNode(indent);
		parent.appendChild(n);
  }
  
  /**
   * Adjust text nodes between tags that don't have "values" to help beautify
   * the resulting XML.  This assumes that only text nodes that are single
   * children actually have interesting data.  If the tabsz < 0 run everything
   * together (packed XML).
   * 
   * @param tabsz size of indent for each tag set
   */
  public void beautify (int tabsz)
  {
  	if (doc == null)
  		return;
		beautify (doc.getDocumentElement(), tabsz, 1);
		doc.normalize();
		// note if transform indent needed in save above
		beautify = (tabsz >= 0);
  }
  
  private int parseInt (String s)
  {
  	int v = 0;
  	for (int i = 0; i < s.length(); i++)
  	{
  		if (Character.isDigit(s.charAt(i)))
  			v = v * 10 + (s.charAt(i) - '0');
  		else
  			break;
  	}
  	return v;
  }
  
  /**
   * get the String representation of the Element
   * @param n node in document
   * @param fragment true if this is a fragment (no XML declaration)
   * @return the formatted XML
   */
  public String toString (Node n, boolean fragment)
  {
  	ByteArrayOutputStream os = new ByteArrayOutputStream ();
  	if (save (n, os, fragment))
  		return os.toString();
  	return null;
  }
 
  /**
   * get the String representation of the named node
   * @param names node in document
   * @param fragment true if this is a fragment (no XML declaration)
   * @return the formatted XML
   */  
  public String toString (String names, boolean fragment)
  {
  	return toString (getElement (names), fragment);
  }
  
  /**
   * give the String representation of this XML
   * @return the formatted XML
   */
  public String toString ()
  {
  	return toString (doc, false);
  }

}
