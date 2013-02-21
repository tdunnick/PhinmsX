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

package tdunnick.phinmsx.domain.receiver;
import java.sql.*;

import tdunnick.phinmsx.domain.Props;
import tdunnick.phinmsx.util.StrUtil;

import org.apache.log4j.*;

public class RcvRecord
{
	private String 
  service,
	action,
	localFileName,
	encryption,
	fromPartyId,
	arguments,
	messageId,
	applicationStatus,
	processingStatus,
	errorCode,
	errorMessage,
	lastUpdateTime,
	messageRecipient,
	payloadName,
	processId,
	receivedTime,
	status;
	
	
	private int getNextRecordId (Props props, String tablename)
	throws SQLException
	{
		int id = 1;
		
		Statement stmt = props.query("select max(recordId) from " + tablename);
		ResultSet res = stmt.getResultSet ();
		if (res == null)
			return (0);
    if (res.next())
			id = res.getInt(1) + 1;
    res.close();
    stmt.close();
		return id;
	}
	
	private boolean insertRecord (Props props, String tablename)
    throws SQLException
	{
		int recordId = getNextRecordId (props, tablename);
		String upd =  "insert into " + tablename
	  + " (recordId, messageId, payloadName, localFileName, service, action, "
	  + "arguments, fromPartyId, messageRecipient, errorCode, "
	  + "errorMessage, processingStatus, encryption, receivedTime, "
	  + "lastUpdateTime, processId) values (" + recordId + ","
	  + quote (messageId) + "," + quote (payloadName) + "," 
	  + quote (localFileName) + ","  + quote (service) + "," 
	  + quote (action) + "," + quote (arguments)  + "," 
	  + quote (fromPartyId) + "," + quote (messageRecipient) + "," 
	  + quote (errorCode) + "," + quote (errorMessage) + ","
	  + quote (processingStatus) + "," + quote (encryption) + "," 
	  + quote (receivedTime) + "," + quote (lastUpdateTime) + "," 
	  + quote (processId) + ")";
		Statement stmt = props.query(upd);
		boolean ok = stmt.getUpdateCount() == 1;
		stmt.close();
		return ok;
	}
	/**
	 * "Push" to the reciever's queue.  Normally I like the SQL to
	 * live in the xml configuration to easily accommodate differences
	 * in syntax, but this is as vanilla as it gets.
	 * 
	 * @param props
	 * @return
	 */
	public boolean insert (Props props)
	{
		boolean ok = false;
		try
		{
		  ok = insertRecord (props, props.getTableName ());
			props.closeConnection();
		}
		catch (SQLException e)
		{
			props.getLogger ().error("Insert failed " + e.getMessage());
		}
		return ok;
	}
	
	private String quote (String s)
	{
		return "'" + StrUtil.replace(s, "'", "''") + "'";
	}
	
  public String getService()
	{
		return service;
	}

	public void setService(String service)
	{
		this.service = service;
	}

	public String getAction()
	{
		return action;
	}

	public void setAction(String action)
	{
		this.action = action;
	}

	public String getLocalFileName()
	{
		return localFileName;
	}

	public void setLocalFileName(String localFileName)
	{
		this.localFileName = localFileName;
	}

	public String getEncryption()
	{
		return encryption;
	}

	public void setEncryption(String encryption)
	{
		this.encryption = encryption;
	}

	public String getFromPartyId()
	{
		return fromPartyId;
	}

	public void setFromPartyId(String fromPartyId)
	{
		this.fromPartyId = fromPartyId;
	}

	public String getArguments()
	{
		return arguments;
	}

	public void setArguments(String arguments)
	{
		this.arguments = arguments;
	}

	public String getMessageId()
	{
		return messageId;
	}

	public void setMessageId(String messageId)
	{
		this.messageId = messageId;
	}

	public String getApplicationStatus()
	{
		return applicationStatus;
	}

	public void setApplicationStatus(String applicationStatus)
	{
		this.applicationStatus = applicationStatus;
	}

	public String getProcessingStatus()
	{
		return processingStatus;
	}

	public void setProcessingStatus(String processingStatus)
	{
		this.processingStatus = processingStatus;
	}

	public String getErrorCode()
	{
		return errorCode;
	}

	public void setErrorCode(String errorCode)
	{
		this.errorCode = errorCode;
	}

	public String getErrorMessage()
	{
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage)
	{
		this.errorMessage = errorMessage;
	}

	public String getLastUpdateTime()
	{
		return lastUpdateTime;
	}

	public void setLastUpdateTime(String lastUpdateTime)
	{
		this.lastUpdateTime = lastUpdateTime;
	}

	public String getMessageRecipient()
	{
		return messageRecipient;
	}

	public void setMessageRecipient(String messageRecipient)
	{
		this.messageRecipient = messageRecipient;
	}

	public String getPayloadName()
	{
		return payloadName;
	}

	public void setPayloadName(String payloadFileName)
	{
		this.payloadName = payloadFileName;
	}

	public String getProcessId()
	{
		return processId;
	}

	public void setProcessId(String processingId)
	{
		this.processId = processingId;
	}

	public String getReceivedTime()
	{
		return receivedTime;
	}

	public void setReceivedTime(String receivedTime)
	{
		this.receivedTime = receivedTime;
	}

	public String getStatus()
	{
		return status;
	}

	public void setStatus(String status)
	{
		this.status = status;
	}

  	
}
