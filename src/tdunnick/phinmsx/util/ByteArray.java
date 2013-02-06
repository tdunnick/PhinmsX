package tdunnick.phinmsx.util;

import java.util.*;

public class ByteArray
{
	public final static byte[] insert (byte[] b1, byte[] b2, int start)
	{
		byte[] b = new byte [b1.length + b2.length];
		int i;
		for (i = 0; i < start; i++)
			b[i] = b1[i];
		for (int j = 0; j < b2.length; j++)
			b[i++] = b2[j];
		for (int j = start; j < b1.length; j++)
			b[i++] = b1[j];
		return b;
	}
	
	public final static byte[] append (byte[] b1, byte[] b2)
	{
		return insert (b1, b2, b1.length);
	}
	
	public final static byte[] copy (byte[] b1, int start, int length)
	{
		byte[] b = new byte[length];
		for (int i = 0; i < length; i++)
			b[i] = b1[start + i];
		return b;
	}
	
	public final static byte[] copy (byte[] b1, int start)
	{
		return copy (b1, start, b1.length - start);
	}
}
