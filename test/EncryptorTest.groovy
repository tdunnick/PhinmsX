import groovy.util.GroovyTestCase;
import tdunnick.phinmsx.crypt.*;

class EncryptorTest extends GroovyTestCase
{
	Encryptor crypt;
	String ksname = "test/test.pfx"
	String kspass = "changeit"
	String certname = "test/test.der"
	String dn = "CN=test.slh.wisc.edu, OU=WSLH, O=UW, L=Madison, ST=Wisconsin, C=US"
    String dnB = "C=US,ST=Wisconsin,L=Madison,O=UW,OU=WSLH,CN=test.slh.wisc.edu"
	String testdata = "The quick grey fox jumped over the lazy dogs"
	
	protected void setUp() throws Exception
	{
		crypt = new Encryptor ();
	}

	public final void testGetKeyStore ()
	{
		def k = crypt.getKeyStore (ksname, kspass)
		assert k != null : "Failed to load " + ksname
	}
	
	public final void testGetAlias ()
	{
		def ks = crypt.getKeyStore (ksname, kspass)
		String alias = crypt.getAlias (ks, null)
		assert alias != null : "Failed to get alias for null DN"
		alias = crypt.getAlias (ks, new StringBuffer (dn))
		assert alias != null : "Failed to get alias for " + dn
	    alias = crypt.getAlias (ks, new StringBuffer (dnB))
	    assert alias != null : "Failed to get alias for " + dnB
	}
	
	public final void testGetPrivateKey ()
	{
		def key = crypt.getPrivateKey (ksname, kspass, null)
		assert key != null : "Failed to get private key for null DN"
	  key = crypt.getPrivateKey (ksname, kspass, new StringBuffer (dn))
		assert key != null : "Failed to get private key for " + dn
	}
	
	public final void testGetPublicKey ()
	{
		def pdn = new StringBuffer()
		def key = crypt.getKeyStoreKey (ksname, kspass, null)
		assert key != null : "Failed to get public key for null DN"
	  key = crypt.getKeyStoreKey (ksname, kspass, new StringBuffer (dn))
		assert key != null : "Failed to get public key for " + dn
	  key = crypt.getDerKey (certname, pdn)
		assert key != null : "Failed to get public key from " + certname
		assert pdn.toString().equals(dn) : "DN didn't match " + pdn.toString()
	}
	
	public final void testGetLdapKey ()
	{
		def pdn = new StringBuffer ()
		def key = crypt.getLdapKey ("directory.verisign.com:389", 
  			"O=Centers for Disease Control and Prevention", "cn=cdc phinms", pdn)
  	assert key != null : "Failed to get LDAP public key for cdc phinms"
  	// println pdn.toString()
	}
	
	public final void testEncrypt ()
	{
		def key = crypt.getKeyStoreKey (ksname, kspass, null)
		def data = crypt.encrypt (testdata.getBytes(), key)
		assert data != null : "Failed encryption"		
	}
	
	public final void testDecrypt ()
	{
		def key = crypt.getDerKey (certname, null)
		String data = crypt.encrypt (testdata.getBytes(), key)
		key = crypt.getPrivateKey (ksname, kspass, null)
		byte[] ds = crypt.decrypt (data, key)
		assert ds != null : "Failed RSA decryption"
		assert new String(ds).equals(testdata) : "RSA Decryption " + new String(ds) + " didn't match"
		key = crypt.generateDESKey();
		data = crypt.encrypt (testdata.getBytes(), key)
		ds = crypt.decrypt (data, key)
		assert ds != null : "Failed DES decryption"
		assert new String(ds).equals(testdata) : "DES Decryption " + new String(ds) + " didn't match"
	}
}
