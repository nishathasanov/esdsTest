<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="com.openkm.util.WarUtils" %>
<%@ page import="com.openkm.api.OKMRepository" %>
<%@ page import="com.openkm.servlet.admin.BaseServlet" %>
<%@ page import="com.openkm.extractor.TextExtractorWork" %>
<%@ page import="com.openkm.extractor.TextExtractorWorker" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
  <link rel="Shortcut icon" href="favicon.ico" />
  <link rel="stylesheet" href="css/style.css" type="text/css" />
  <link rel="stylesheet" type="text/css" href="css/fixedTableHeader.css" />
  <script type="text/javascript" src="../js/jquery-1.7.1.min.js"></script>
  <script type="text/javascript" src="js/fixedTableHeader.js"></script>
  <script type="text/javascript">
    $(document).ready(function() {
    	TABLE.fixHeader('#trick');
	});
  </script>
  <title>Text Extraction Queue</title>
</head>
<body>
  <c:set var="isAdmin"><%=BaseServlet.isAdmin(request)%></c:set>
  <c:choose>
    <c:when test="${isAdmin}">
      <ul id="breadcrumb">
        <li class="path">
          <a href="text_extraction_queue.jsp">Text extraction queue</a>
        </li>
        <li class="action">
          <a href="stats.jsp">Statistics</a>
        </li>
        <li class="action">
          <a href="text_extraction_queue.jsp">
            <img src="img/action/refresh.png" alt="Refresh" title="Refresh" style="vertical-align: middle;"/>
            Refresh
          </a>
        </li>
      </ul>
      <br/>
      <table id="treat" class="results" width="90%">
        <thead>
          <tr class="fuzzy">
            <td colspan="4" style="text-align: center; font-weight: bold; font-size: 14px">Extractions In Progress</td>
          </tr>
          <tr><th>#</th><th>UUID</th><th>Path</th><th>Date</th></tr>
        </thead>
        <tbody>
        <%
        int row = 0;
        for (TextExtractorWork work : TextExtractorWorker.getInProgressWorks()) {
        %>
          <tr class="<%=(row++) % 2 == 0 ? "even" : "odd"%>">
            <td><%=row%></td>
            <td nowrap="nowrap"><%=work.getDocUuid()%></td>
            <td><%=work.getDocPath()%></td>
            <td nowrap="nowrap"><%=work.getDate().getTime()%></td>
          </tr>
        <%
        }
        %>  
        </tbody>
      </table>
      <br/>
      <table id="trick" class="results" width="90%">
        <thead>
          <tr class="fuzzy">
            <td colspan="4" style="text-align: center; font-weight: bold; font-size: 14px">Pending Extractions</td>
          </tr>
          <tr><th>#</th><th>UUID</th><th>Path</th><th>Date</th></tr>
        </thead>
        <tbody>
        <%
        row = 0;
        final int MAX_RESULTS = 20;
        List<TextExtractorWork> pending = TextExtractorWorker.getPendingWorks(MAX_RESULTS + 1);
        Iterator<TextExtractorWork> it = pending.iterator();
        
        while (row < MAX_RESULTS && it.hasNext()) {
        	TextExtractorWork work = it.next();
        %>
          <tr class="<%=(row++) % 2 == 0 ? "even" : "odd"%>">
            <td><%=row%></td>
            <td nowrap="nowrap"><%=work.getDocUuid()%></td>
            <td><%=work.getDocPath()%></td>
            <td nowrap="nowrap"><%=work.getDate().getTime()%></td>
          </tr>
        <%
        }
        
        if (it.hasNext()) {
        %>
          <tr class="fuzzy">
            <td colspan="4" style="text-align: center; font-weight: bold;">Total pending extractions: <%=TextExtractorWorker.getPendingSize() %></td>
          </tr>
        <%
        }
        %>
        </tbody>
      </table>
    </c:when>
    <c:otherwise>
      <div class="error"><h3>Only admin users allowed</h3></div>
    </c:otherwise>
  </c:choose>
</body>
</html>