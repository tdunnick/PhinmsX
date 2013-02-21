package tdunnick.phinmsx.crypt;

import java.io.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import tdunnick.phinmsx.util.XmlContent;

/**
 * Converts encrypted password file to/from XML
 * 
 * @author user
 *
 */
public class Passwords
{
	private XmlContent xml;
	
	/**
	 * This is the salt used by CDC
	 */
	private static final byte salt[] = 
	{
			-57, 115, 33, -116, 126, -56, -18, -103
	};
	
  /**
   * make sure we have bouncy castle loaded before continuing...
   */
  public Passwords()
	{
    Security.insertProviderAt(new BouncyCastleProvider(), 1);
  	xml = new XmlContent ();
	}

  /**
   * Our password cryptographic core, using some other magic from CDC
   * 
   * @param data to process
   * @param password
   * @param encrypt if true, otherwise decrypt
   * @return data after cryptography
   */
  public byte[] pbe (byte[] data, String passwd, boolean encrypt)    
  {
  	if ((passwd == null) || (data == null))
  		return null;
    try
		{
    	// System.out.println ("setting up to decrypt");    	
			PBEKeySpec pbeKeySpec = new PBEKeySpec(passwd.toCharArray());
			SecretKeyFactory fac = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
			SecretKey pbekey = fac.generateSecret(pbeKeySpec);
			PBEParameterSpec ps = new PBEParameterSpec(salt, 20);
			Cipher c = Cipher.getInstance("PBEWithMD5AndDES");
			c.init(encrypt ? 1 : 2, pbekey, ps);
			return c.doFinal(data);
		}
    catch(Exception e)
    {
    	return null;
    }
  }
  
	/**
	 * Load and returns the XML passwords
	 * 
	 * @param file to load
	 * @param seed for password
	 * @param key for password
	 * @return
	 */
	public boolean load (String file, String seed, String key)
  {
		return load (file, getPassword (seed, key));
  }
	
	/**
	 * Load and returns the XML passwords
	 * 
	 * @param inp input stream
	 * @param seed for password
	 * @param key for password
	 * @return
	 */
	public boolean load (InputStream inp, String seed, String key)
  {
		return load (inp, getPassword (seed, key));
  }
	
	/**
	 * Load and returns the XML passwords
	 * 
	 * @param file to load
	 * @param password
	 * @return
	 */
	public boolean load (String file, String passwd)
  {
		if (file == null)
			return false;
  	try
  	{
  		FileInputStream inp = new FileInputStream (file);
  		boolean ok = load (inp, passwd);
  		inp.close ();
  		return ok;
  	}
  	catch (IOException e)
  	{
  		return false;
  	}
 }
  
	/**
	 * Load and returns the XML passwords
	 * 
	 * @param inp input stream
	 * @param password
	 * @return
	 */
	public boolean load (InputStream inp, String passwd)
  {
  	byte[] buf;
  	try
  	{
  		int c;
  		ByteArrayOutputStream b = new ByteArrayOutputStream ();
  		while ((c = inp.read ()) >= 0)
  			b.write(c);
  		buf = b.toByteArray();
  	}
  	catch (IOException e)
  	{
  		return false;
  	}
  	buf = pbe (buf, passwd, false);
  	if (buf == null)
  		return false;
  	if (xml.load(new String (buf)))
  		return true;
  	return false;
  }

  /**
   * Stores passwords to disk
   * 
   * @param file for encrypted data
   * @param seed for password
   * @param key for password
   * @param xml to encrypt
   * @return true if successful
   */
  public boolean save (String file, String seed, String key)
  {
  	return save (file, getPassword (seed, key));
  }
  
  /**
   * Stores passwords to stream
   * 
   * @param out
   * @param seed for password
   * @param key for password
   * @param xml to encrypt
   * @return true if successful
   */
  public boolean save (OutputStream out, String seed, String key)
  {
  	return save (out, getPassword (seed, key));
  }

  /**
   * Stores passwords to disk
   * 
   * @param file for encrypted data
   * @param password
   * @return true if successful
   */
  public boolean save (String file, String passwd)
  {
  	try
  	{
  		FileOutputStream out = new FileOutputStream (file);
  		boolean ok = save (out, passwd);
  		out.close ();
  		return ok;
  	}
  	catch (IOException e)
  	{
  		return false;
  	}
  }
  
  /**
   * Stores passwords to stream
   * 
   * @param out
   * @param password
   * @return true if successful
   */
  public boolean save (OutputStream out, String passwd)
  {

  	byte[] buf = pbe (xml.toString().getBytes(), passwd, true);
  	if (buf == null)
  		return false;
  	try
  	{
  		out.write(buf);
   	}
  	catch (IOException e)
  	{
  		return false;
  	}
  	return true;
  } 
  /**
   * Get the xml for this passwords file
   * 
   * @return
   */
  public XmlContent getXml ()
  {
  	return xml;
  }
  
  /**
   * Set the xml for this passwords file
   * @param xml
   */
  public void setXml (XmlContent xml)
  {
  	if (xml != null) 
  	  this.xml = xml;
  }
  
  /**
   * Get the password value for this item
   * 
   * @param item
   * @return
   */
  public String get (String item)
  {
  	return xml.getValue(xml.getRoot() + "." + item);
  }
  
  /**
   * Add or change the password value for this item
   * 
   * @param item
   * @param value
   * @return true if successful
   */
  public boolean put (String item, String value)
  {
  	String r = xml.getRoot();
  	if ((r == null) || (r.length() == 0))
  		r = "passwordFile";
  	return xml.setValue(r + "." + item, value);
  }
  
  /**
   * Decrypt the PBE password from a seed and key.
   * This is a simple substitution cypher where each 3 digit of
   * the seed are used to obtain a value from which is subtracted the
   * corresponding value of the key.  When the result underflows add
   * back one byte.
   * 
   * @param seed
   * @param key
   * @return the password
   */
  public String getPassword (String seed, String key)
  {
  	if ((seed == null) || (key == null))
  		return null;
  	StringBuffer buf = new StringBuffer ();
  	int v, i, k, 
  	  l = seed.length (), 
  	  kl = key.length();
  	if (l % 3 != 0)
  		return null;
  	try 
  	{
	  	for (i = k = 0; i < l; i += 3)
	  	{
	  		if (k > kl)
	  			k = 0;
	  		v = Integer.parseInt(seed.substring(i, i + 3)) - key.charAt(k++);
	  		if (v < 0) v += 255;
	  		buf.append((char) v);
	  	}
  	}
  	catch (NumberFormatException e)
  	{
  		// seed had garbage!
  		return null;
  	}
  	// System.out.println ("password=" + buf.toString());
  	return buf.toString(); 	
  }
  
  /**
   * Get the encrypted seed for PBE encoding using substitution cypher
   * described for getPassword()
   * 
   * @param passwd
   * @param key
   * @return the seed
   */
  public String getSeed (String passwd, String key)
	{
		if ((passwd == null) || (key == null))
			return null;
		StringBuffer buf = new StringBuffer();
		int v, i, k, l = passwd.length(), kl = key.length();
		for (i = k = 0; i < l; i++)
		{
			if (k > kl)
				k = 0;
			v = passwd.charAt(i) + key.charAt(k++);
			if (v > 255)
				v -= 255;
			if (v < 10)
				buf.append("00");
			else if (v < 100)
				buf.append("0");
			buf.append(Integer.toString(v));
		}
		return buf.toString();
	}
  
  public static void usage ()
  {
  	System.err.println ("usage: Passwords [<options>] [<file>]\n"
  			+ "-e                  encrypt (decrypt)\n"
  			+ "-c                  only run substitution cypher on password\n"
  			+ "-k <key>            set password key\n"
  			+ "-s <seed>           set password seed\n"
  			+ "-p <password        set password\n"
  			+ "-o <file>           write output to file (stdout)\n"
  			+ "-x <file>           get seed,key,file from XML configuration\n"
  			+ "<file>              read input from file (stdin)\n");
  	System.exit(1);
  }
  
  public static void main (String args[])
	{
		Passwords pw = new Passwords();
		String key = null, passwd = null;
		XmlContent xml = null;
		InputStream inp = System.in;
		OutputStream out = System.out;
		boolean doPBE = true;
		boolean encrypt = false;
		boolean isseed = true;

		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equals("-c"))
				doPBE = false;
			else if (args[i].equals("-e"))
				encrypt = true;
			else if (i + 1 >= args.length)
				usage();
			else if (args[i].equals("-k"))
				key = args[++i];
			else if (args[i].equals("-s"))
				passwd = args[++i];
			else if (args[i].equals("-p"))
			{
				passwd = args[++i];
				isseed = false;
			}
			else 
			{
				// if reading a configuration, get seed, key and encrypted file
				if (args[i].equals("-x"))			
				{
				  xml = new XmlContent ();
				  xml.load(args[++i]);
				  isseed = true;
				  String root = xml.getRoot();
				  passwd = xml.getValue(root + ".seed");
				  key = xml.getValue(root + ".key");
			  	args[i] = xml.getValue(root + ".passwordFile");			  	
				  if (encrypt)
				  	args[--i] = "-o";
				}
				// open either input or output
			 	try
				{
					if (args[i].equals("-o"))
						out = new FileOutputStream(args[++i]);
					else if (!args[i].startsWith("-"))
						inp = new FileInputStream(args[i]);
					else
						usage ();
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
		
		if (doPBE)
		{
			if (isseed)
				passwd = pw.getPassword(passwd, key);
			if (encrypt)
			{
			  xml = new XmlContent();
				xml.load(inp);
				pw.setXml(xml);
				pw.save(out, passwd);
			}
			else
			{
				pw.load(inp, passwd);
				xml = pw.getXml();
				xml.beautify(2);
				xml.save(out);
			}
		}
		else		// just substitution cypher
		{
			try
			{
				if (isseed)
					out.write(pw.getPassword(passwd, key).getBytes());
				else
					out.write(pw.getSeed(passwd, key).getBytes());
			}
			catch (IOException e)
			{
				System.err.println("Failed " + e.getLocalizedMessage());
				System.exit(2);
			}
		}
		System.exit(0);
	}
}
