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

<c:set var="s" value="${requestScope.status}" />

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>PhinmsX Receiver</title>
</head>
<body>
<content tag="pagetitle">Receiver Status</content>
<content tag="pageinfo"><c:out value="${s.version}"/></content>
<c:if test="${empty s.records}">
  No messages received
</c:if>
<c:if test="${not empty s.records}">
  <div class='records'>
    <table>
        <tr>
          <c:forEach items="${s.fields}" var="f">
            <th><c:out value="${f}"/></th>
          </c:forEach>
        </tr>
      <c:forEach items="${s.records}" var="r">
        <tr>
          <c:forEach items="${r}" var="f">
            <td><c:out value="${f}"/></td>
          </c:forEach>
        </tr>
      </c:forEach>
    </table>
  </div>
</c:if>

</body>
</html>