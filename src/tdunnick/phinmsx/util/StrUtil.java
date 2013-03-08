package tdunnick.phinmsx.util;

public class StrUtil
{
	/**
	 * 1.4 method for String.replace()
	 * @param s string to replace
	 * @param old string to match
	 * @param rep replacement string
	 * @return new string
	 */
	public static String replace (String s, String old, String rep)
	{
		if (s == null)
			return null;
		if ((old == null) || (rep == null))
		  return s;
		StringBuffer buf = new StringBuffer (s);
		int i, n, l;
		
		i = 0;
		l = old.length();
		while ((n = buf.indexOf(old, i)) >= 0)
		{
			buf.replace(n, n + l, rep);
			i = n;
		}
		return buf.toString();
	}

}
