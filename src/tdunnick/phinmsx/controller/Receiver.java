/*
 *  Copyright (c) 2012-2013 Thomas Dunnick (https://mywebspace.wisc.edu/tdunnick/web)
 *  
 *  This file is part of PhinmsX.
 *
 *  PhinmsX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  PhinmsX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with PhinmsX.  If not, see <http://www.gnu.org/licenses/>.
 */

package tdunnick.phinmsx.controller;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import org.apache.log4j.*;

import javax.servlet.*;
import javax.servlet.http.*;

import tdunnick.phinmsx.domain.*;
import tdunnick.phinmsx.domain.receiver.*;
import tdunnick.phinmsx.crypt.*;
import tdunnick.phinmsx.helper.*;
import tdunnick.phinmsx.util.*;

/**
 * PHINMS custom receiver servlet.  Features cascading configuration and
 * dynamic loading of message "helper" classes.  By itself this can
 * route and name files based on configuration.  The base configuration
 * normally pulls in Receiver.xml and uses those associated resources
 * (e.g. queue maps, password files, etc).  Additionally, either sender
 * or receiver can include a conf=... parameter to configure for specific
 * folders, message helpers, etc.
 * 
 * @author Thomas Dunnick
 * 
 */
public class Receiver extends HttpServlet
{

	public final static String Version = "Receiver RO 0.10 03/31/2012";
	/*
	 * identifiers found in our XML configuration file - note this follows
	 * receiver.xml conventions, with our own additions.
	 */
	// servlets properties
	private static Props props = null;
	private Logger logger = null;
	
	// our dB queue manager and current map...
	private static String queueMap = "";
	
	// our stats
	String[] heading = 
	  { "Date/Time", "File Name", "Status", "Error", "Response" };
	private static ArrayList stats = new ArrayList ();
	private static RcvStatus status = null;
	
	/**
	 * set up an environment for this request. This holds information unique to
	 * this request.
	 * 
	 * @param propname of XML properties file unique to this request
	 * @return a new environment
	 */
	private RcvEnv getEnv (String propname)
	{
		RcvEnv env = new RcvEnv();
		if (propname == null)
		{
			if (props == null)
				return null;
			env.props = props;
		}
		else
		{
			env.props = new Props();
			if (!env.props.load(propname, props.getProps()))
			{
				logger.error("Failed reading " + propname);
				if (props == null)
					return null;
				env.props = props;
			}
		}
		// create temp dir if needed
		String tempdir = env.getProperty (Props.TEMPDIR);
		if (tempdir != null)
		{
			File t = new File(tempdir);
			if (!t.exists() && !t.mkdir())
				return (null);
		}
		setDecryptor(env);
		setResponse(env, "success", "none", "InsertSucceeded", null, null);
		return env;
	}

	/**
	 * Retrieve a default serverlet property.  Note, most properties should
	 * come from the request environment which includes these...
	 * 
	 * @param name of property sans prefix
	 * @return value or null if not found
	 */
	private String getProperty (String name)
	{
		if (props == null)
			return null;
		return props.getProperty (name);
	}


	/**
	 * add any needed escapes for SQL functions.
	 * 
	 * @param s data to escape
	 * @return escaped data
	 */
	private String sqlEscape(String s)
	{
		if (s == null)
			return null;
		StringBuffer b = new StringBuffer(s);
		for (int i = 0; i < b.length(); i++)
		{
			if (b.charAt(i) == '\'')
				b.deleteCharAt(i--);
		}
		return b.toString();
	}
	
	/**
	 * Fill in defaults for a queue entry and add it
	 * 
	 * @param env  environment for this response
	 * @param rec to add
	 * @return true if successful
	 */
	private boolean updateQueue(RcvEnv env, RcvRecord rec)
	{
		rec.setErrorCode(env.applicationStatus);
		rec.setErrorMessage(sqlEscape(env.applicationError));
		// today's date formatted for access
		// need a formatter for numeric values...
		NumberFormat twoDigits = new DecimalFormat("00");
		Calendar c = Calendar.getInstance();
		String now = c.get(Calendar.YEAR) + "-"
				+ twoDigits.format(c.get(Calendar.MONTH) + 1) + "-"
				+ twoDigits.format(c.get(Calendar.DAY_OF_MONTH)) + "T"
				+ twoDigits.format(c.get(Calendar.HOUR_OF_DAY)) + ":"
				+ twoDigits.format(c.get(Calendar.MINUTE)) + ":"
				+ twoDigits.format(c.get(Calendar.SECOND));
		rec.setLastUpdateTime(now);
		// the name we gave the payload when we stored it
		rec.setLocalFileName(getFilePath(env));
		// null in testworkerqueue, but we'll use our hostname...
		String host = "localhost";
		try
		{
			host = InetAddress.getLocalHost().getHostName();
		}
		catch (UnknownHostException e)
		{
			// put with localhost
		}
		rec.setMessageRecipient(host);
		// the name the payload arrived with
		rec.setPayloadName(getFileName(env));
		// null in testworkerqueue, we use the payload's file name extension...
		rec.setProcessId(getProcessId(env));
		// "queued" in testworkerqueue
		rec.setReceivedTime(now);
		// autonumbered...
		// rec.setRecId(arg0);
		rec.setStatus(sqlEscape(env.applicationResponse));
		// add this record...
		if (!rec.insert (env.props))
		{
    	logger.error("Can't push queue " 
					+ env.getProperty (Props.QUEUENAME));
			// + " trying " + dfltIncomingQueue);
			// queuedb.pushToQueue(dfltIncomingQueue, rec);
		}
		return true;
	}

	private synchronized void updateStats (RcvEnv env)
	{
		String[] s = new String[5];
		s[0] = new Date().toString();
		s[1] = env.fileName;
		s[2] = env.applicationStatus;
		s[3] = env.applicationError;
		s[4] = env.applicationResponse;
		stats.add (0, s);
		while (stats.size() > 24)
			stats.remove(24);
	}
	
	/**
	 * Load status from disk cache.  The cache name is in the configuration.
	 * @return true if successful
	 */
	private boolean loadStats ()
	{
		String fname = getProperty (Props.STATUS);
		if (fname == null)
		{
			logger.info("WARNING: No status cache provided");
			return false;
		}
		try
		{
			ObjectInputStream input = 
				new ObjectInputStream (new FileInputStream (fname));
			stats = (ArrayList) input.readObject();
			input.close();
		}
		catch (Exception e)
		{
			logger.info("WARNING: Couldn't read status from " + fname);
			return false;
		}
		logger.debug("Loaded " + stats.size() + " statistics");
		return true;
	}
	
	/**
	 * Save status to disk cache.  The cache name is in the configuration.
	 * @return true if successful
	 */
	private boolean saveStats ()
	{
		String fname = getProperty (Props.STATUS);
		if (fname == null)
			return false;
		try
		{
			ObjectOutputStream out = 
				new ObjectOutputStream (new FileOutputStream (fname));
			out.writeObject(stats);
			out.close();
			return true;
		}
		catch (Exception e)
		{
			logger.error("Failed to save statistics - " 
					+ e.getLocalizedMessage());
			return false;
		}		
	}
	
	/**
	 * Construct a mime encoded response to return to the SOAP receiver process.
	 * Eventually this gets passed back to the sender. The payload parts of the
	 * response are used to send data back to the receiver, which must be
	 * configured to do something with it.
	 * 
	 * @return reponse MIME encoded string
	 */
	private StringBuffer getResponse(RcvEnv env)
	{
		/* 
		 * compose mime multi-part response with...
		 * The payload is normally null, but should be base 64 encoded.
		 * However, the MessageProcessor fails to make the needed updates
		 * to the SOAP envelope needed to process the payload (at least
		 * from servlets), so it just gets ignored at the sender anyway.
		 * 
		 * Also, beware '&' in the first three items.  PHINMS fails to URLDecode
		 * so while URLEncode gets things through, they look like hell on the
		 * sender side (sigh).
		 */
		RcvResponse m = new RcvResponse ();
		StringBuffer response = m.getResponse (
				env.applicationStatus,
				env.applicationError,
				env.applicationResponse,
				env.payload, 
				env.payloadName); // payload MUST be named
		logger.debug("Response: " + response.toString());
		return (response);
	}

	/**
	 * Parses a value from an XML style manifest for the given field tag
	 * 
	 * @param manifest provided
	 * @param field  that we want
	 * @return value of that field
	 */
	private String parseManifest(String manifest, String field)
	{
		// this should work!!!! -- friggin broke AFAIKT
		// return (manifest.replaceFirst("^.*<" + field + ">(.*)</" + field +
		// ">.*$", "\\1"));
		int f, t;

		if ((f = manifest.indexOf("<" + field + ">")) < 0)
			return (null);
		if ((t = manifest.indexOf("</" + field + ">")) < 0)
			return (null);
		return (manifest.substring(f + field.length() + 2, t));
	}

	/**
	 * Set the values for the response message to the sender. Note that the
	 * errorCode doesn't get returned to the sender, but rather is used for the
	 * receiver's queue entry.
	 * 
	 * @param status for the response
	 * @param error for the response
	 * @param response for the response
	 * @param payload to return to sender
	 * @param payloadNameto identify the payload
	 */
	private void setResponse(RcvEnv env, String status, String error,
			String response, byte[] payload, String payloadName)
	{
		env.applicationStatus = status;
		env.applicationError = error;
		env.applicationResponse = response;
		env.payload = payload;
		env.payloadName = payloadName;
	}


	/**
	 * get the filename used for this message
	 * 
	 * @param env of the response
	 * @return filename
	 */
	private String getFileName(RcvEnv env)
	{
		if (env.fileName == null)
			return ("");
		return env.fileName;
	}

	/**
	 * sets the filename to use for this message as well the the suffix found.
	 * 
	 * @param env of this response
	 * @param fileNameto set
	 */
	private void setFileName(RcvEnv env, String fileName)
	{
		if ((env.fileName = fileName) != null)
		{
			String[] parts = fileName.split("[.]");
			if (parts.length > 1)
				env.fileSuffix = parts[parts.length - 1];
			else
				env.fileSuffix = "";
		}
		else
			env.fileSuffix = fileName = null;
	}

	/**
	 * Get a temporary file name to use for this message
	 * 
	 * @param env of response
	 * @return temporary file name
	 * @throws IOException
	 */
	private File getTmpFile(RcvEnv env) throws IOException
	{
		String p = getFileName(env);
		if (p.length() < 3)
			p += "_tmp";
		return File.createTempFile(p, null, new File(env.getProperty(Props.TEMPDIR)));
	}

	/**
	 * returns a process ID based on the suffix of the incoming message name
	 * 
	 * @param env of this response
	 * @return the ID or empty string if none found
	 */
	private String getProcessId(RcvEnv env)
	{
		if (env.fileSuffix == null)
			return ("");
		return env.fileSuffix;
	}

	/**
	 * returns the folder to use to store incoming messages
	 * 
	 * @param env of this response
	 * @return the folder path
	 */
	private String getIncomingDir(RcvEnv env)
	{
		String dir = env.getProperty(Props.INCOMINGDIR);
		if (dir == null)
			dir = "";
		return dir;
	}

	/**
	 * Create a path name for the incoming file based on directory and extensions
	 * specified in the environment. In particular, strip off the process ID and
	 * replace the incoming extension if the file name will remain unique.
	 * Otherwise simply tag on the extension.
	 * 
	 * @param env environment with naming information
	 * @return true if path successfully set, false if file exists
	 */
	private boolean setFilePath(RcvEnv env)
	{
		env.filePath = null;
		if (env.fileName == null)
			return false;
		// strip suffix and extension, add our extension
		env.filePath = getIncomingDir(env)
			+ env.fileName.replaceFirst("([.][^.]*){0,1}[.]" + env.fileSuffix,"") 
			+ env.getProperty (Props.FILEEXTENSION);
		File f = new File (env.filePath);
		if (!f.exists())
			return true;
		logger.debug("File " + env.filePath + " exists, trying full name");
		// if the above wasn't unique, or the preferred extension didn't match
		// simply add it
		env.filePath = getIncomingDir(env) + env.fileName 
		  + env.getProperty (Props.FILEEXTENSION);
		f = new File (env.filePath);
		if (!f.exists())
			return true;
		env.filePath = null;
		return false;
	}

	/**
	 * Gets the full path to incoming payload file
	 * 
	 * @param env of this request
	 * @return the path
	 */
	private String getFilePath(RcvEnv env)
	{
		if (env.filePath == null)
			return ("");
		return env.filePath;
	}

	/**
	 * Sets up a decryption object based on data found in properties. Note this
	 * requires a four way keyed process, starting the the key and seed used in a
	 * substitution cypher to get the descryption key for the passwords file while
	 * then can be read for the decryptions key for the keystore which finally has
	 * the decryption key for the payload (sigh!)
	 * 
	 * @param context of the servlet
	 * @param env of the request
	 * @param props properties for decryption
	 */
	private void setDecryptor(RcvEnv env)
	{
	}

	/**
	 * Parses the MIME multipart request message and returns a response. This is
	 * where all the REAL work gets done.
	 * 
	 * @param request HttpServletRequest object
	 * @return Response string buffer
	 */
	private StringBuffer processRequest(HttpServletRequest request)
	{
		// BufferedReader in = null;
		String token = null;

		// note if payload was encoded
		String encrypted = "no";

		RcvEnv env = getEnv (null);

		// use the multi part parser
		RcvRequest mpp = new RcvRequest (env.props.getLogger());
		try
		{
			// Splits mime message fields into text and payload
			if (!mpp.parse (request.getHeader("Content-Type"), 
					request.getInputStream()))
			{
				logger.error("Parsing multipart fields in request");
				setResponse(env, "aborted", "bad mime format", "failure", null,	null);
				return getResponse(env);
			}
		}
		catch (Exception e)
		{
			logger.error("Parsing message in messagehandler");
			setResponse(env, "aborted", "message format exception", "failure",
					null, null);
			return getResponse(env);
		}

		/*
		 * Note that the PHINMS API apparently does not check strings set in a
		 * WorkerQueueRecord for SQL special chars, so be sure to avoid single
		 * quotes, or other possibly magic stuff. Or if you know the backend then
		 * create your own escape method.
		 */
		// prepare a new queue record...
		RcvRecord rec = new RcvRecord();
		// 06/19/10 - fill in defaults required for non-null fields
		rec.setService("unknown");
		rec.setAction("unknown");
		rec.setLocalFileName("unknown");
		rec.setEncryption("no");

		// parse the text parameters
		// the Message Receiver can send multiple text parameters
		logger.debug("Checking arguments...");
		rec.setFromPartyId(mpp.getArgument("from"));
		rec.setService(mpp.getArgument("service"));
		rec.setAction(mpp.getArgument("action"));

		String m = mpp.getArgument("manifest");
		if (m != null)
		{

			rec.setArguments(m);
			logger.debug("manifest= " + m);
			rec.setMessageId(parseManifest(m, "MessageId"));
			logger.debug("MessageID: " + rec.getMessageId());
		}
		// load this environment
		env = getEnv(mpp.getArgument("conf"));
		// Do necessary processing of data here
		// e.g., Copy file to disk as below
		logger.debug("Getting payload...");
		try
		{
			String payload;
			byte[] data;

			if ((data = mpp.getPayLoad()) != null)
			{
				setFileName(env, mpp.getFileName());
				// create a path to write the payload to
				if (!setFilePath(env))
				{
					logger.error("Can't set path for " + env.fileName);
					rec.setApplicationStatus("aborted");
					rec.setProcessingStatus("rejected");
					setResponse(env, "aborted", "duplicate or unusable file name",
							"failure", null, null);
					updateQueue(env, rec);
					return (getResponse(env));
				}
				// include userDir subdirectory in payload path
				File outFile = new File(getFilePath(env));
				File tmpFile = getTmpFile(env);
				FileOutputStream fos = new FileOutputStream(tmpFile);
		
				PayloadEncryptor crypt = new PayloadEncryptor ();

				// is this encrypted?
				if (crypt.isEncrypted (payload = new String(data)))
				{
					logger.debug("Payload is encrypted");
					encrypted = "yes";
					// are we prepared to decrypt it?
					data = crypt.decryptPayload (
							env.getProperty (Props.KEYSTORE),
							env.getProperty (Props.KEYSTOREPASSWD),
							env.getProperty(Props.KEYSTOREPASSWD),
							payload);
					// successfully decrypted?
					if ((data == null) || (data.length == 0))
					{
						setResponse(env, "abnormal", "can not decrypt payload",
								"warning", null, null);
						logger.error("Decryption failed " + (data == null ? 
								"returned buffer null" : "returned buffer empty"));
						logger.error("Unable to decrypt payload"
								+ (crypt == null ? " null decryptor" : " failed keystore "
										+ env.getProperty (Props.KEYSTORE)));
					}
				}
				else
				// wasn't encrypted
				{
					logger.debug ("Payload is not encrypted");
				}
				fos.write(data);
				fos.flush();
				fos.close();
				if (!tmpFile.renameTo(outFile))
				{
					logger.error("Unable to rename " + tmpFile.getPath() + " to "
							+ outFile.getPath());
				}
				// run the helper if one exists, in which case it sets the response
				String helper = env.getProperty(Props.HELPER);
				if (helper != null)
				{
					logger.debug ("running helper " + helper);
					try
					{
						Class c = Class.forName(helper);
						RcvHelper h = (RcvHelper) c.newInstance();
						h.setResponse(env, data);
					}
					catch (Exception e)
					{
					  logger.error(helper + " exception " + e.getMessage());
						setResponse(env, "aborted", "application processing error", 
								e.getMessage(), null, null);
					}
				}
			}
		}
		catch (Exception e)
		{
			logger.error("Internal error processing payload " + e.getMessage());
			rec.setApplicationStatus("aborted");
			rec.setProcessingStatus("rejected");
			setResponse(env, "aborted", "internal failure decoding payload",
					e.getMessage(), null, null);
			updateQueue(env, rec);
			return getResponse(env);
		}

		// stuff the queue record
		// whether encrypted as discovered above...
		rec.setEncryption(encrypted);
		// rec.setApplicationStatus("processed");
		rec.setApplicationStatus(env.applicationResponse);
		rec.setProcessingStatus("received");
		// stuff we'll ignore...
		// doesn't matter, we don't store the payload
		// rec.setIsTextPayload(false);
		// empty, we write it out...
		// rec.setPayloadBinaryContent(null);
		// empty, we write it out...
		// rec.setPayloadTextContent(null);

		updateQueue(env, rec);
		updateStats (env);

		// note this in the log
		logger.info("File:" + getFilePath(env) 
				+ " Response:"	+ env.applicationResponse 
				+ " Error:" + env.applicationError);

		return getResponse(env);
	}

	/**
	 * do basic initialization needed for this servlet
	 * 
	 * @param conf name of XML properties files
	 * @return true if successful
	 */
	private boolean initialize(String conf)
	{
		props = new Props();
		if (!props.load(conf))
		{
			logger = XLog.console();
			logger.error("Failed initializing " + conf);
			props = null;
			return false;
		}
		
		logger = props.getLogger(null, true);
		// initialize and set up statistics and status bean
		loadStats ();
		status = RcvStatus.getStatus ();
		status.setVersion(Receiver.Version);
		// status.setPhinmsVersion(Defines.VERSION);
		status.setFields(heading);
		status.setRecords(stats);
		logger.info("Started ebxml.receivefile servlet");
		return (true);
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		if (props == null)
		{
			RequestDispatcher view = req.getRequestDispatcher("conferror.html");
			view.forward(req, resp);
			return;
		}
		req.setAttribute("status", status);
		RequestDispatcher view = req.getRequestDispatcher("receiver.jsp");
		view.forward(req, resp);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		logger.info("********************** Begin message "
				+ " processing **********************");
		// process the request

		try
		{
			// call processRequest to do the dirty work
			StringBuffer response = processRequest(req);
			if (response == null)
			{
				logger.error("Failed to process POST request - no response");
			}
			else
			{
				PrintWriter out = resp.getWriter();
				out.println(response.toString());
				out.close();
			}
		}
		catch (Exception e)
		{
			logger.error("Post exception " + e.getMessage());
			e.printStackTrace();
		}
		logger.info("******************* Completed message "
				+ "processing ********************");
	}

	public void destroy()
	{
		saveStats ();
		super.destroy();
	}

	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		System.out.println ("Initializing PhinmsX Receiver");
		Phinms.setContextPath(config.getServletContext().getRealPath("/"));
		String configFile = config.getInitParameter("Config");
		// System.out.println ("Configuration file=" + configFile);
		if (!initialize(configFile))
		{
			System.err.println ("Fatal error: error initializing PhinmsX Receiver");
			return;
		}
		else
		{
			return;
		}
	}
}
