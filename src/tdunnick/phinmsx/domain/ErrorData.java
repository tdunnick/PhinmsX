package tdunnick.phinmsx.domain;

import java.util.*;
import java.io.*;
import org.apache.log4j.*;
import tdunnick.phinmsx.util.*;

public class ErrorData
{
	private ArrayList message = new ArrayList ();
	Logger console = XLog.console ();
	
  public ErrorData (Props props)
  {
  	initialize (props);
  }
  
  private void initialize (Props props)
  {
  	if (props == null)
  	{
  		message.add ("Unknown Error - null properties");
  		return;
  	}
  	Logger logger = props.getLogger();
  	if (logger == console)
  	{
 		  message.add ("Unknown Error - Only console log available");
  		return;
  	}
  	Enumeration e = logger.getAllAppenders();
  	while (e.hasMoreElements())
  	{
  		Object o = e.nextElement();
  		if (o.getClass().getName().indexOf("FileAppender") >= 0)
  		{
  			loadErrors (((FileAppender) o).getFile());
  			break;
   		}
  	}
  }
  
  private boolean loadErrors (String fn)
  {
  	console.debug("Reading " + fn);
  	try
  	{
  		BufferedReader inp = new BufferedReader (new FileReader (fn));
  		String s;
  		while ((s = inp.readLine()) != null)
  		{
  			if (s.indexOf ("ERROR") >= 0)
  			{
  				s = StrUtil.replace (s, "<","&lt;");
  				message.add(0, StrUtil.replace (s, ">", "&gt;"));
  			}
  		}
  		inp.close();
  	}
  	catch (IOException e)
  	{
  		console.error ("Can't read error from " + fn + " - " + e.getMessage());
  		return false;
  	}
  	return true;
  }
  
  public ArrayList getMessage ()
  {
  	if (message.size() == 0)
  		message.add ("Unknown Error");
  	return message;
  }
  
  public void setMessage (ArrayList m)
  {
  	message = m;
  }
}
