<!--  
  Copyright (c) 2012-2013 Thomas Dunnick (https://mywebspace.wisc.edu/tdunnick/web)
  
  This file is part of PhinmsX.

  PhinmsX is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  PhinmsX is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with PhinmsX.  If not, see <http://www.gnu.org/licenses/>.
-->
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:set var="e" value="${requestScope.error}" />
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>PhinmsX Error</title>
</head>
<body>
<content tag="pagetitle">PhinmsX Error</content>
An error has occurred that prevents access to this page.  Please correct
and restart <b>PhinmsX</b>.
<div style="border: solid grey 1px; margin: 10px 0 10px 0; padding: 5px 5px 5px 5px;">
  <c:forEach items="${e.message}" var="m" >
    <p></p><c:out value="${m}" /></p>
  </c:forEach>
</div>
</body>
</html>