package tdunnick.phinmsx.util;

import java.util.*;
import java.text.*;
import java.util.logging.*;

public class PhinmsXFormatter extends Formatter
{
  SimpleDateFormat dfmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ");
  boolean debug = false;
  
  public PhinmsXFormatter ()
  {  	
  }
  
  public PhinmsXFormatter (boolean debug)
  {
  	this.debug = debug;
  }
  
	public String format(LogRecord rec)
	{
		StringBuffer buf = new StringBuffer ();
		
		buf.append(dfmt.format(new Date (rec.getMillis())));
		buf.append(rec.getLoggerName() + " ");
		buf.append ("[" + rec.getThreadID() + "] ");
		buf.append (rec.getLevel().getName() + " ");
		buf.append(rec.getMessage() + " @");
		buf.append (rec.getSourceClassName() + ".");
		buf.append (rec.getSourceMethodName() + "()");
		buf.append("\n");
		return buf.toString();
	}
}
