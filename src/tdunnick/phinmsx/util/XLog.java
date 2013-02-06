package tdunnick.phinmsx.util;

import java.io.IOException;

import org.apache.log4j.*;

import tdunnick.phinmsx.domain.*;

public class XLog
{
	public static String DFMT = "%d{ISO8601} [%t] %-5p: %m %l%n";
	public static String FMT = "%d{ISO8601} [%t] %-5p: %m %l%n";
	
	public static Logger getRootLogger (boolean debug)
	{
		Logger logger = Logger.getRootLogger();
		if (!logger.getAllAppenders().hasMoreElements())
		{
			Layout p = new PatternLayout (debug ? DFMT : FMT);
		  logger.addAppender(new ConsoleAppender (p));
		}
		return logger;
	}
	
	/**
	 * Set the logging level
	 * @param level one of "all", "debug", "error", "fatal", "info", or "warn"
	 */
	public static void setLogLevel (Logger logger, String level)
	{
		Level l = Level.INFO;
		if (level == null)
			level = "INFO";
		if (level.equalsIgnoreCase("ALL"))
			l = Level.ALL;
		else if (level.equalsIgnoreCase("DEBUG") 
				|| level.equalsIgnoreCase("DETAIL"))
			l = Level.DEBUG;
		else if (level.equalsIgnoreCase("ERROR"))
			l = Level.ERROR;
		else if (level.equalsIgnoreCase("FATAL"))
			l = Level.FATAL;
		else if (level.equalsIgnoreCase("INFO"))
			l = Level.INFO;
		else if (level.equalsIgnoreCase("WARN"))
			l = Level.WARN;
		else
			return;
		logger.setLevel(l);
	}
	
}
