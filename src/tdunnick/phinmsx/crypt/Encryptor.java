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

import java.util.*;
import java.io.*;
import org.apache.log4j.*;
import javax.naming.*;
import javax.naming.directory.*;
import java.security.*;
import java.security.cert.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.*;

import tdunnick.phinmsx.util.*;

public class Encryptor
{
	public final static String RSA_TRANSFORM = "RSA/ECB/PKCS1PADDING";
	public final static String DES_TRANSFORM = "DESede/CBC/PKCS5Padding";
	Logger logger;
		
	/**
	 * add any needed security providers
	 */
	public Encryptor ()
	{
		init (null);
	}
	
	public Encryptor (Logger logger)
	{
		init (logger);
	}

	private void init (Logger l)
	{
	  if (l == null)
	  	logger = XLog.console ();
	  else
	  	logger = l;
	  if (Security.getProvider ("BC") == null)
	  {
		  if (Security.addProvider(new BouncyCastleProvider()) < 0)
		  	logger.error("Couldn't add BC security provider");
	  }
	}
	
	private String getTransform (Key key)
	{
		logger.debug("Algorithm: " + key.getAlgorithm());
		if (key.getAlgorithm().equals("RSA"))
			return RSA_TRANSFORM;
		return DES_TRANSFORM;
	}
  /** 
   * Generate a secret TripleDES encryption/decryption key 
   * 
   * @return a new secret key
   */
  public SecretKey generateDESKey() 
	{
		try
		{
			KeyGenerator keygen = KeyGenerator.getInstance("DESede");
			return keygen.generateKey();
		}
		catch (NoSuchAlgorithmException e)
		{
			logger.error("Can't generate key for " + DES_TRANSFORM + ": " + e.getMessage());
		}
		return null;
	}


  private KeyStore loadKeyStore (File f, String pw, String type)
  {
  	KeyStore ks = null;
  	FileInputStream is = null;
  	try
  	{
  		is = new FileInputStream (f);
  	}
  	catch (IOException e)
  	{
  		logger.error ("Failed loading " + f.getAbsolutePath() + ": " 
  				+ e.getMessage());
  		return null;
  	}
		try
		{
			ks = KeyStore.getInstance(type);
			ks.load(is, pw.toCharArray());
		}
		catch (Exception e)
		{
			String s = e.getMessage();
			if (!s.equals("Invalid keystore format"))
			  logger.error ("Failed loading " + f.getAbsolutePath() + ": " + s);
			ks = null;
		}
		try
		{
			is.close ();
		}
		catch (IOException e)
		{
  		logger.error ("Failed closeing " + f.getAbsolutePath() + ": " 
  				+ e.getMessage());			
		}
		return ks;
  }
  
	/**
	 * Load a keystore given the path and password.  First try a JKS, then
	 * try a PKCS12.
	 * 
	 * @param path to keystore
	 * @param passwd for keystore
	 * @return the keystore
	 * @throws Exception
	 */
  public KeyStore getKeyStore (String path, String passwd)
	{
  	logger.debug ("getting keystore..");
		if ((path == null) || (passwd == null))
		{
			logger.debug("path or password is null");
			return null;
		}
		File f = new File(path);
		if (!f.canRead())
		{
			logger.error("Can't open keystore " + path + " for read");
			return null;
		}
		KeyStore ks = loadKeyStore(f, passwd, "JKS");
		if (ks == null)
			ks = loadKeyStore(f, passwd, "PKCS12");
		return (ks);
	}
	
  /**
   * Compare two distinguished names in an order/format independent way
   * 
   * @param dn1
   * @param dn2
   * @return true if they match
   */
  public boolean dnequals (String dn1, String dn2)
  {
  	String[] d1 = dn1.split("[ ,]+");
  	String[] d2 = dn2.split("[ ,]+");
  	Arrays.sort(d1);
  	Arrays.sort(d2);
  	for (int i = 0; i < d1.length; i++)
  	{
  		if (i >= d2.length) 
  			return false;
  		if (!d1[i].equalsIgnoreCase(d2[i]))
  			return false;
  	}
  	return true;
  }
  
	/**
	 * Find the alias associate with a DN in a keystore.  If the DN
	 * is empty or null, pick the first entry and fill in a non-null DN.
	 * 
	 * @param ks keystore to search
	 * @param dn DN to match
	 * @return the alias
	 * @throws KeyStoreException
	 */
  public String getAlias (KeyStore ks, StringBuffer dn)
	{
		String alias;
		X509Certificate cert;
		String pdn = null;
		if ((dn != null) && (dn.length() > 0))
			pdn = dn.toString();
		
		if (ks == null)
			return null;
		try
		{
			Enumeration a1 = ks.aliases();
			while (a1.hasMoreElements())
			{
				alias = (String) (a1.nextElement());
				cert = (X509Certificate) ks.getCertificate(alias);
				// if no dn is specified, return the first one found
				if (pdn == null)
				{
					if (dn != null)
						dn.append(cert.getSubjectDN().getName());
					return alias;
				}
				if (dnequals (pdn, cert.getSubjectDN().getName()))
					return alias;
				logger.debug("Skipping " + cert.getSubjectDN().getName());
			}
		}
		catch (KeyStoreException e)
		{
			logger.error("Can't find " + dn + ": " + e.getMessage());
		}
		return null;
	}
	
	/**
	 * Gets the private key from a keystore.  This assumes the password for
	 * the key is the same as for the keystore. If the DN
	 * is empty or null, pick the first entry and fill in a non-null DN.
	 * 
	 * @param path to keystore
	 * @param passwd for keystore and entry
	 * @param dn of the entry
	 * @return private key
	 */
  public Key getPrivateKey (String path, String passwd, StringBuffer dn)
	{
		return getPrivateKey (path, passwd, passwd, dn);
	}
	
	/**
	 * Gets the private key from a keystore.  If the DN
	 * is empty or null, pick the first entry and fill in a non-null DN.
	 * 
	 * @param path to keystore
	 * @param passwd for keystore
	 * @param keypass for entry
	 * @param dn of the entry
	 * @return private key
	 */
  public Key getPrivateKey (String path, String passwd, String keypass, StringBuffer dn)
	{
		KeyStore ks = getKeyStore(path, passwd);
		String alias = getAlias(ks, dn);
		if (alias == null)
			return null;
		try
		{
			return ks.getKey(alias, keypass.toCharArray());
		}
		catch (Exception e)
		{
			logger.error("Can't find " + dn + " in " + path + ": "
					+ e.getMessage());
		}
		return null;
	}
	
  /**
   * Get a public key from an LDAP server
   * 
   * @param host (and port) of LDAP
   * @param baseDN tree to search
   * @param cn comman name to match
   * @param dn returned distinguished name on certificate
   * @return the public key
   */
  public Key getLdapKey (String host, String baseDN, String cn, StringBuffer dn)
	{
		Hashtable env = new Hashtable ();
		String url = "ldap://" + host + "/" + baseDN;
		try
		{
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			env.put(Context.PROVIDER_URL, url);
			DirContext context = new InitialDirContext(env);

			SearchControls ctrl = new SearchControls();
			ctrl.setSearchScope(SearchControls.SUBTREE_SCOPE);
			NamingEnumeration enumeration = context.search("", cn, ctrl);
			if (enumeration.hasMore())
			{
				SearchResult result = (SearchResult) enumeration.next();
				Attributes attribs = result.getAttributes();
				Attribute attr = attribs.get("userCertificate;binary");
				Object cert = null;
				if (attr != null)
					cert = attr.get();
				if ((cert == null) || !cert.getClass().equals(byte[].class))
				{
					logger.error("LDAP " + url + " has no certificate for " + cn);
					return null;					
				}
			  return getPublicKey (new ByteArrayInputStream ((byte[]) cert), dn);	
			}
		}
		catch (Exception e)
		{
			logger.error("Can't get key from " + url + " for " + cn + ": " + e.getMessage());
		}
		return null;
	}
  
  /**
	 * Gets the public key from a java keystore.  If the DN
	 * is empty or null, pick the first entry and fill in a non-null DN.
	 * 
	 * @param path to keystore
	 * @param passwd for keystore and entry
	 * @param dn of the entry
	 * @return public key
	 */
  public Key getKeyStoreKey (String path, String passwd, StringBuffer dn)
	{
 		KeyStore ks = getKeyStore(path, passwd);
		String alias = getAlias(ks, dn);
		if (alias == null)
			return null;
		try
		{
			X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
			return cert.getPublicKey();
		}
		catch (Exception e)
		{
			logger.error("Can't find " + dn + " in " + path + ": "
					+ e.getMessage());
			return null;
		}
	}
	
	/**
	 * Gets the public key from a DER/PEM certificate.  If the DN is not null
	 * and empty fill it in.  Otherwise check to see that it matches.
	 * 
	 * @param path to the certificate
	 * @param dn buffer to retrieve distinguished name
	 * @return the public key
	 */
  public Key getDerKey (String path, StringBuffer dn)
  {
  	try
  	{
			FileInputStream fs = new FileInputStream(path);
			return getPublicKey (fs, dn);
  	}
		catch (Exception e)
		{
			logger.error("Can't get key from " + path + ": " + e.getMessage());
			return null;
		} 	
  }
  
  /**
   * Get the public key from an input stream X509 certificate
   * 
   * @param fs stream of certificate
   * @param dn distinguished name to match or return
   * @return the public key
   */
  public Key getPublicKey(InputStream fs, StringBuffer dn)
	{
		if (fs == null)
			return null;
		try
		{
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			X509Certificate cert = (X509Certificate) cf.generateCertificate(fs);
			fs.close();
			if (dn != null)
			{
				String n = cert.getSubjectDN().getName();
				if (dn.length() == 0)
			    dn.append(n);
				else if (!dn.toString().equals(n))
					return null;
			}
			return cert.getPublicKey();
		}
		catch (Exception e)
		{
			logger.error("Can't get public key: " + e.getMessage());
			return null;
		}
	}
	
  private IvParameterSpec getIv (int mode)
  {
  	byte[] b = new byte[8];
  	if (mode != Cipher.DECRYPT_MODE)
  	{
	  	Random rand = new Random ();
	  	rand.nextBytes(b);
  	}
	  return new IvParameterSpec (b);
  	
  }
  
  private Cipher getCipher (int mode, Key key)
  {
  	try
  	{
  		Cipher cipher = Cipher.getInstance(getTransform (key));
  		logger.debug("cipher algorithm is: " + cipher.getAlgorithm());
			// triple DES CBC block uses 8 byte initial vector
			if (key.getAlgorithm().startsWith("DESede"))
			{
				cipher.init(mode, key, getIv(mode));
			}
			else
			{
			  cipher.init(mode, key);
			}
			return cipher;	
  	}
  	catch (Exception e)
  	{
  		logger.error("Can't get cipher for " + key.getAlgorithm() + ": " + e.getMessage(), e);
  	}
  	return null;
  }
	
	/**
	 * Encrypts some data
	 * @param data to be encrypted
	 * @param key to use for the encryption
	 * @return base64 encoded encrypted data
	 */
  public String encrypt (byte[] data, Key key)
	{
		if ((data == null) || (key == null))
			return null;
		try
		{
			Cipher cipher = getCipher (Cipher.ENCRYPT_MODE, key);
			if (cipher == null)
				return null;
			// triple DES CBC block uses 8 byte pre-pended initial vector
			byte[] iv = cipher.getIV();
			if (iv != null)
				data = ByteArray.append(iv, data);
			byte[] d = cipher.doFinal(data);
			return new String(Base64.encode(d));
		}
		catch (Exception e)
		{
			logger.error("Can't encrypt with " + getTransform(key) + ": " + e.getMessage());
		}
		return null;
	}
	
  /**
   * Encrypts a secret (DES) key, normally using an RSA method
   * 
   * @param skey the secret key
   * @param key to encrypt with
   * @return base64 encoded encryption of key
   */
  public String encryptKey (SecretKey skey, Key key)
  {
  	byte[] d = skey.getEncoded();
  	return encrypt (d, key);
  }
  
	/**
	 * Decrypts some data
	 * @param data base64 encoded to be decrypted
	 * @param key to use for decryption
	 * @return the decrypted data
	 */
  public byte[] decrypt (String data, Key key)
	{
		if ((data == null) || (key == null))
			return null;
		try
		{
			Cipher cipher = getCipher (Cipher.DECRYPT_MODE, key);
			if (cipher == null)
				return null;
			// triple DES CBC block uses 8 byte pre-pended initial vector
			byte[] iv = cipher.getIV();
			byte[] d = Base64.decode(data.getBytes());
			d = cipher.doFinal (d);
			if (iv != null)
				d = ByteArray.copy (d, iv.length);
			return d;
		}
		catch (Exception e)
		{
			logger.error("Can't decrypt with " + getTransform (key),e);
		}
		return null;
	}
  
  /**
   * gets an encrypted key.  Normally decrypts using RSA for a DES key
   * 
   * @param data holding encrypted key
   * @param key to decrypt with
   * @param keymethod of key being decrypted
   * @return
   */
  public SecretKey decryptKey (String data, Key key, String keymethod)
  {
  	byte[] b = decrypt (data, key);
  	if (b == null)
  		return null;
  	return new SecretKeySpec (b, keymethod);
  }
}
