package tdunnick.phinmsx.util;

import org.apache.log4j.*;

public class XLog
{
	public static String DFMT = "%d{ISO8601} [%t] %-5p: %m %l%n";
	public static String FMT = "%d{ISO8601} [%t] %-5p: %m %l%n";
	public static String DFTCONTEXT = "PhinmsxConsole";
	public static Logger console = null;
	
	public static synchronized Logger console ()
	{
		if (console == null)
		{
			console = Logger.getLogger(DFTCONTEXT);
			if (!console.getAllAppenders().hasMoreElements())
			{
				console.setLevel(Level.ALL);
				Layout p = new PatternLayout(FMT);
				console.addAppender(new ConsoleAppender(p));
			}
			/*
			 * console = new ConsoleLogger (DFTCONTEXT);
			 */
		}
		return console;
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
