package tdunnick.phinmsx.domain;

import java.util.*;
import java.io.*;
import java.util.logging.*;
import tdunnick.phinmsx.util.*;

public class ErrorData
{
	private ArrayList message = new ArrayList ();
	Logger console = Logger.getLogger("");
	
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
 	  if (props.getLogger() == console)
  	{
 		  message.add ("Unknown Error - Only console log available");
  		return;
  	}
  }
  
  private boolean loadErrors (String fn)
  {
  	console.finest("Reading " + fn);
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
  		console.severe ("Can't read error from " + fn + " - " + e.getMessage());
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
