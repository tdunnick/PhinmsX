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
 *  Foobar is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * general purpose
 */

// get element location in browser
function getOffset(el)
{
  var _x = 0;
  var _y = 0;
  while (el && !isNaN(el.offsetLeft) && !isNaN(el.offsetTop))
  {
    _x += el.offsetLeft - el.scrollLeft;
    _y += el.offsetTop - el.scrollTop;
    el = el.offsetParent;
  }
  return { top: _x, left: _y }
}
/*
 * pop-up calendars
 */

function pickDate(theDate, id)
{
  var d = new Date(theDate.value);
  var theDiv = document.getElementById(id);
  var offset = getOffset(theDate);
  // alert ("top:" + offset.top + " left:" + offset.left);
  theDiv.style.top = offset.top;
  theDiv.style.left = offset.left;

  this.hideCal = function()
  {
    theDiv.style.display = "none";
  };

  this.pick = function(day)
  {
    if (d == null)
      return;
    d.setDate(day);
    theDate.value = (d.getMonth() + 1) + "/" + d.getDate() + "/"
        + d.getFullYear();
    theDiv.style.display = "none";
  };

  this.prevMonth = function()
  {
    d.setMonth(d.getMonth() - 1);
    this.showCal();
  };

  this.nextMonth = function()
  {
    d.setMonth(d.getMonth() + 1);
    this.showCal();
  };

  this.showCal = function()
  {
    var weekday;
    var dt = new Date(d);
    var month = dt.getMonth();
    var today = 1;
    dt.setDate(today);
    var s = dt.toDateString().split(" ");
    var cal = "<table class='calendar'><caption>"
        + "<button class=\"exit\" onClick='hideCal()'>X</button>"
        + "<button onClick='prevMonth()'>" + "&lt;&lt;</button>&nbsp;&nbsp;"
        + s[1] + " " + s[3] + "&nbsp;&nbsp;"
        + "<button onClick='nextMonth()'>&gt;&gt;</button>"
        + "</caption><tr><th>Sun</th><th>Mon</th><th>Tue</th><th>Wed</th>"
        + "<th>Thu</th><th>Fri</th><th>Sat</th></tr><tr>";
    for (weekday = 0; weekday < dt.getDay(); weekday++)
      cal += "<td></td>";
    while (month == dt.getMonth())
    {
      if (weekday == 0)
        cal += "<tr>";
      cal += "<td><button onClick='pick(" + today + ")'>" + today
          + "</button></td>";
      dt.setDate(++today);
      if (weekday++ == 6)
      {
        cal += "</tr>";
        weekday = 0;
      }
    }
    if (weekday)
      cal += "</tr>";
    theDiv.innerHTML = cal + "</table>";
    theDiv.style.display = "block";
  };
  this.showCal();
}
