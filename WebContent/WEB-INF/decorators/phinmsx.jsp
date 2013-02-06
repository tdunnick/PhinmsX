<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ taglib uri="http://www.opensymphony.com/sitemesh/decorator" prefix="dec" %>
<%@ page import="tdunnick.phinmsx.util.Phinms" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<!--  
  Copyright (c) 2012 Thomas Dunnick (https://mywebspace.wisc.edu/tdunnick/web)
  
  This file is part of PhinmsX.

  PhinmsX is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  Foobar is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
-->
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
  <title><dec:title default="PhinmsX" /></title>
  <link rel="stylesheet" type="text/css" href="css/phinmsx.css" />
  <dec:head />
</head>
<body>
  <div id="logo">
    <img src="images/logosmall.gif">
  </div>
  <div class="heading"><dec:getProperty property="page.pagetitle" /></div>
  <dec:getProperty property="page.pageinfo" />
  <br>
  <div class="button">
    <a href="index.html">Home</a>
    <a href="dashboard.html">Dash Board</a>
    <a href="queues.html">Queues</a>
    <a href="status.html">Status</a>
  </div>
  <hr style="clear:both"/>
  <!--  body code -->
  <dec:body />
  <div id="footer">
     <hr style="clear:both"/>
     Copyright &copy; 2012 Thomas Dunnick - All Rights Reserved<p>
     Currently running under <%= Phinms.getVersion() %> library. <br>
     For additional information please see
     <a href="https://mywebspace.wisc.edu/tdunnick/web/index.html" target="_blank">
      https://mywebspace.wisc.edu/tdunnick/web/index.html</a>.
  </div> 
</body>
</html>