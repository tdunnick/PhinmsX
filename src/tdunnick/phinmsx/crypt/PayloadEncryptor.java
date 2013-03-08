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
package tdunnick.phinmsx.crypt;

import java.io.*;
import java.security.*;
import java.util.logging.*;

import tdunnick.phinmsx.util.XmlContent;

/**
 * A PHINMS payload container consists of an XML wrapper holding information about
 * how the encryption was done and a payload.  This assumes triple DES encryption
 * of the payload using an RSA encrypted key designated by a distinguished name
 * from the wrapper.
 * 
 * @author tld
 *
 */
public class PayloadEncryptor
{	
	// manifest payload tags
	static final String ENCRYPT_ROOT = "EncryptedData";
	static final String PAYLOAD_DN = "EncryptedData.KeyInfo.EncryptedKey.KeyInfo.KeyName";
	static final String PAYLOAD_KEY = "EncryptedData.KeyInfo.EncryptedKey.CipherData.CipherValue";
	static final String PAYLOAD_DATA = "EncryptedData.CipherData.CipherValue";
	
	Logger logger;

	static final String TEMPLATE =
	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
	"<EncryptedData Id=\"ed1\" Type=\"http://www.w3.org/2001/04/xmlenc#Element\" xmlns=\"http://www.w3.org/2001/04/xmlenc#\">" +
	  "<EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#tripledes-cbc\"/>" +
	  "<KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\">" +
	    "<EncryptedKey xmlns=\"http://www.w3.org/2001/04/xmlenc#\">" +
	      "<EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-1_5\"/>" +
	      "<KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\">" +
	        "<KeyName>key</KeyName>" +
	      "</KeyInfo>" +
	      "<CipherData>" +
	        "<CipherValue/>" +
	      "</CipherData>" +
	    "</EncryptedKey>" +
	  "</KeyInfo>" +
	  "<CipherData>" +
	    "<CipherValue/>" +
	  "</CipherData>" +
	"</EncryptedData>";
	
	public PayloadEncryptor()
	{
		setLogger (null);
	}

	public PayloadEncryptor (Logger logger)
	{
		setLogger (logger);
	}
	
	private void setLogger (Logger l)
	{
		if (l == null)
			logger = Logger.getLogger("");
		else
		  logger = l;
	}
	
	/**
	 * Encrypt and return a payload as a string.  
	 * @param path to the RSA encryption keystore or certficate
	 * @param passwd for the keystore
	 * @param dn distinguished name for the key to use
	 * @param template path to XML wrapper
	 * @param payload data to encrypt
	 * @return XML wrapped payload
	 */
	public String encryptPayload (String path, String passwd, 
			StringBuffer dn, byte[] payload)
	{
		if ((path == null) || (dn == null) || (payload == null))
			return null;
		Encryptor crypt = new Encryptor (logger);
		XmlContent xml = new XmlContent ();
		try
		{
			FileInputStream in = new FileInputStream (TEMPLATE);
			xml.load(in);
			in.close();
		}
		catch (Exception e)
		{
			logger.severe("Can't parse payload XML: " + e.getMessage());
			return null;
		}
		Key key = crypt.generateDESKey();
		xml.setValue(PAYLOAD_DATA, crypt.encrypt(payload, key));
		Key pkey;
		if ((passwd == null) || (passwd.length() == 0))
		{
			dn.setLength(0);
			pkey = crypt.getDerKey(path, dn);
		}
		else
		{
		  pkey = crypt.getKeyStoreKey(path, passwd, dn);
		}
		xml.setValue(PAYLOAD_KEY, crypt.encrypt(key.getEncoded(), pkey));
		if (dn == null)
		{
			logger.severe(PAYLOAD_DN + " not found");
			return null;
		}
		xml.setValue(PAYLOAD_DN, dn.toString());
		ByteArrayOutputStream out = new ByteArrayOutputStream ();
		try
		{
		  xml.save(out);
		}
		catch (Exception e)
		{
			logger.severe("Can't store payload: " + e.getMessage());
			return null;
		}
		return out.toString();
	}

	/**
	 * Determine if this payload is encrypted XML format
	 * 
	 * @param payload to check
	 * @return true if it appears encrypted
	 */
	public boolean isEncrypted (String payload)
	{
		return payload.indexOf ("<" + ENCRYPT_ROOT) >= 0;
	}
	
	/**
	 * Decrypt a payload wrapped in XML
	 * @param path to decryption keystore
	 * @param storepass to keystore
	 * @param keypass and entry
	 * @param payload wrapped in XML
	 * @return binary payload data
	 */
	public byte[] decryptPayload (String path, String storepass, 
			String keypass, String payload)
	{
		if (!isEncrypted (payload))
			return null;
		ByteArrayInputStream in = new ByteArrayInputStream (payload.getBytes());
		XmlContent xml = new XmlContent();
		try
		{
			xml.load(in);
		}
		catch (Exception e)
		{
			logger.severe("Can't parse payload XML: " + e.getMessage());
			return null;
		}
		String dn = xml.getValue(PAYLOAD_DN);
		if (dn == null)
		{
			logger.severe(PAYLOAD_DN + " not found");
			return null;
		}
		String ckey = xml.getValue(PAYLOAD_KEY);
		if (ckey == null)
		{
			logger.severe(PAYLOAD_KEY + " not found");
			return null;
		}
		String data = xml.getValue(PAYLOAD_DATA);
		if (data == null)
		{
			logger.severe(PAYLOAD_DATA + " not found");
			return null;
		}
		return decryptData (path, storepass, keypass, new StringBuffer(dn), ckey, data);
	}
	
	/**
	 * Decrypt data using a DESede key encrypted by an RSA key from a keystore.
	 * TODO get key algorithms/transforms from the XML rather than assume.
	 * For now this is what PHIN-MS always uses.
	 * 
	 * @param path to the keystore
	 * @param storepass for the keystore 
	 * @param keypass and private key
	 * @param dn of the keystore entry
	 * @param ckey encrypted DES key
	 * @param data to decrypt
	 * @return the decrypted data
	 */
	private byte[] decryptData (String path, String storepass, 
			String keypass, StringBuffer dn, String ckey, String data)
	{
		Encryptor crypt = new Encryptor (logger);
	  logger.finest("getting RSA key");
		Key key = crypt.getPrivateKey(path, storepass, keypass, dn);
		if (key == null)
		{
			logger.severe ("Couldn't get RSA private key\n");
			return null;
		}
		logger.finest("getting DES key");
    key = crypt.decryptKey(ckey, key, Encryptor.DES_TRANSFORM);
    if (key == null)
    {
    	logger.severe ("Couldn't decrypt DES key\n");
    	return null;
    }
    logger.finest("decrypting payload");
    return crypt.decrypt(data, key);
	}
	
  public static void usage (String m)
  {
  	System.err.println (m);
  	System.err.println ("usage: PayloadEncryptor [<options>] [<file>]\n"
  			+ "-e                  encrypt (decrypt)\n"
  			+ "-c <certificate>    set <certificate> file\n"
  			+ "-p <password>       set certificate <password>\n"
  			+ "-o <file>           write output to <file> (stdout)\n"
  			// + "-x <file>           get certificate/password from XML configuration <file>\n"
  			+ "<file>              read input from <file> (stdin)\n");
  	System.exit(1);
  }

	public static final void main (String[] args)
	{
		PayloadEncryptor crypt = new PayloadEncryptor ();
		boolean encrypt = false;
		String cert = null;
		String passwd = null;
		XmlContent xml = null;
		InputStream inp = System.in;
		OutputStream out = System.out;
	
		for (int i = 0; i < args.length; i++)
		{	
			if (args[i].equals("-e"))
				encrypt = true;
			else if ((args[i].charAt(0) == '-') && (i + 1 >= args.length))
				usage("Argument expected after " + args[i]);
			else if (args[i].equals("-c"))
				cert = args[++i];
			else if (args[i].equals("-p"))
			{
				passwd = args[++i];
			}
			else 
			{
				/*
				 *  if reading a configuration, get seed, key and encrypted file
				if (args[i].equals("-x"))			
				{
				  xml = new XmlContent ();
				  xml.load(new File (args[++i]));
				  Passwords pw = new Passwords ();
				  if (!pw.load(xml))
				  	usage ("Can't load passwords using " + args[i]);
				}
				 */
				// open either input or output
			 	try
				{
					if (args[i].equals("-o"))
						out = new FileOutputStream(args[++i]);
					else if (!args[i].startsWith("-"))
						inp = new FileInputStream(args[i]);
					else
						usage ("==> " + args[i]);
				}
				catch (IOException e)
				{
					System.err.println ("Can't open " + args[i] 
					   + " - " + e.getLocalizedMessage());
					System.exit (3);
				}
			}
		}
		
		/* 
		 * finally have all the args parsed, so either do the file
		 * or password encryption/decryption
		 */
		
			try
			{
				byte[] payload;
				
				payload = new byte[inp.available()];
				inp.read (payload);
			  if (encrypt)
				{
					String s = crypt.encryptPayload(cert, passwd, new StringBuffer(), payload);
					out.write (s.getBytes());
				}
				else
				{
					payload = crypt.decryptPayload(cert, passwd, passwd, new String (payload));
					out.write(payload);
				}
			}
			catch (IOException e)
			{
				System.err.println("Failed " + e.getLocalizedMessage());
				System.exit(2);
			}
	}
}
