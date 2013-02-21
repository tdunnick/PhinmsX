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
import org.apache.log4j.*;

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
			logger = Logger.getRootLogger();
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
			StringBuffer dn, String template, byte[] payload)
	{
		if ((path == null) || (dn == null) || (template == null) || (payload == null))
			return null;
		Encryptor crypt = new Encryptor (logger);
		XmlContent xml = new XmlContent ();
		try
		{
			FileInputStream in = new FileInputStream (template);
			xml.load(in);
			in.close();
		}
		catch (Exception e)
		{
			logger.error("Can't parse payload XML: " + e.getMessage());
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
			logger.error(PAYLOAD_DN + " not found");
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
			logger.error("Can't store payload: " + e.getMessage());
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
			logger.error("Can't parse payload XML: " + e.getMessage());
			return null;
		}
		String dn = xml.getValue(PAYLOAD_DN);
		if (dn == null)
		{
			logger.error(PAYLOAD_DN + " not found");
			return null;
		}
		String ckey = xml.getValue(PAYLOAD_KEY);
		if (ckey == null)
		{
			logger.error(PAYLOAD_KEY + " not found");
			return null;
		}
		String data = xml.getValue(PAYLOAD_DATA);
		if (data == null)
		{
			logger.error(PAYLOAD_DATA + " not found");
			return null;
		}
		return decryptData (path, storepass, keypass, new StringBuffer(dn), ckey, data);
	}
	
	/**
	 * Decrypt data using a DES key encrypted by an RSA key from a keystore.
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
	  logger.debug("getting RSA key");
		Key key = crypt.getPrivateKey(path, storepass, keypass, dn);
		logger.debug("getting DES key");
    key = crypt.decryptKey(ckey, key, Encryptor.DES_TRANSFORM);
    logger.debug("decrypting payload");
    return crypt.decrypt(data, key);
	}
}
