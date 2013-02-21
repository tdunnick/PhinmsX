import junit.framework.Test;
import junit.framework.TestSuite;
public class AllTests
{
	public static Test suite()
	{
		TestSuite suite = new TestSuite("PhinmsX tests");
		suite.addTestSuite(EncryptorTest.class);
		suite.addTestSuite(XmlContentTest.class);
		suite.addTestSuite(Hl7AckTest.class);
		suite.addTestSuite(Hl7BarParserTest.class);
		suite.addTestSuite(Hl7MsgTest.class);
		suite.addTestSuite(MimeTests.class);
		suite.addTestSuite(PasswordsTest.class);
		suite.addTestSuite(ReceiverTest.class);
		suite.addTestSuite(StrUtilTest.class);
		return suite;
	}
}
