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

<c:set var="m" value="${requestScope.queues}" />

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>PhinmsX Queue Monitor</title>
</head>
<body>
<content tag="pagetitle">Queue Monitor</content>
<content tag="pageinfo"><c:out value="${m.version}"/></content>
    <!--  this side gets the list of queues/routes/etc -->

<h2><c:out value="${m.table}"/>
<c:if test="${not empty m.constraint}">
  <c:out value="${m.constraint}"/>
</c:if>
</h2>
 
<div class='mtab'>
 <c:if test="${not empty m.rows}">
    <div class='records'>
      <table>
        <tr>
    	  <c:forEach items="${m.rowfields}" var="n">
    	    <th><c:out value="${n}"/></th>
    	  </c:forEach>
        </tr>
        <c:forEach items="${m.rows}" var="r" varStatus="rs">
          <tr class="${m.rowClass[rs.index]}"
            <c:if test="${m.recordId == r[0]}">
              style="font-weight:bold;"
            </c:if>
          >
          <c:forEach items="${r}" var="n" varStatus="s">
            <td>
               <c:if test="${empty n}">
                 &nbsp;
               </c:if>
               <c:if test="${not empty n}">
                 <c:if test="${s.first}">
                   <a href='?recordId=<c:out value="${n}"/>'>
                 </c:if>
                 <c:out value="${n}"/>
                 <c:if test="${s.first}">
                   </a>
                 </c:if>
               </c:if>
            </td>
          </c:forEach>
        </tr>
        </c:forEach>
      </table>
    </div>
    <div class="button">
      <c:if test="${not empty m.next}">
        <a href="?top=<c:out value="${m.next}"/>">Next</a>
      </c:if>
      <c:if test="${not empty m.prev}">
        <a href="?top=<c:out value="${m.prev}"/>">Previous</a>
      </c:if>
    </div>
  </c:if>
  <c:if test="${empty m.rows}">
    No records found!
  </c:if>
</div>

<div class='mlist'>
  <c:forEach items="${m.tables}" var="t">
     <a href='?table=<c:out value="${t[0]}"/>'><c:out value="${t[0]}"/></a><br/>
     <c:if test="${m.table == t[0]}">
      <c:forEach items="${t[1]}" var="c">
         &nbsp;&nbsp;
          <a href='?constraint=<c:out value="${c}"/>'>
            <c:out value="${c}"/></a><br/>
     </c:forEach>
     </c:if>
  </c:forEach>
</div>


<!-- finally the details of the selected row -->
<c:if test="${not empty m.record}">
  <hr style="clear:both;" />
  <div class='details'>
    <table cellspacing=0 >
	  <c:forEach items="${m.record}" var="f" varStatus="s">
	    <tr>
	      <td><b><c:out value="${m.fields[s.index]}"/>:</b>&nbsp;</td>
	      <td><c:out value="${f}"/></td>
	    </tr>   
  	  </c:forEach>
    </table>
  </div>
</c:if>
</body>
</html>