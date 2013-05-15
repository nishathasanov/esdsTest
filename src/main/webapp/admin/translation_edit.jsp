<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.openkm.servlet.admin.BaseServlet" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri='http://java.sun.com/jsp/jstl/functions' prefix='fn' %>
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
  <link rel="Shortcut icon" href="favicon.ico" />
  <link rel="stylesheet" type="text/css" href="css/style.css" />
  <title>Translation List</title>
</head>
<body>
  <c:set var="isAdmin"><%=BaseServlet.isAdmin(request)%></c:set>
  <c:choose>
    <c:when test="${isAdmin}">
      <ul id="breadcrumb">
	   <li class="path">
	     <a href="Language">Language list</a>
	   </li>
	   <li class="path">Translation list</li>
	  </ul>
      <br/>
      <form action="Language" method="post">
        <input type="hidden" name="action" value="${action}"/>
        <input type="hidden" name="persist" value="${persist}"/>
        <input type="hidden" name="lg_id" value="${lg_id}"/>
        <table class="results" width="95%">
          <tr>
            <th>#</th><th>Module</th><th>Key property</th><th>${langBase.name}</th><th>${langToTranslateName}</th>
          </tr>
          <c:forEach var="translation" items="${langBase.translations}" varStatus="row">
            <c:choose>
              <c:when test="${empty translations[translation.translationId.key]}">
                <c:set var="rowClass">warn</c:set>
              </c:when>
              <c:otherwise>
                <c:set var="rowClass">${row.index % 2 == 0 ? 'even' : 'odd'}</c:set>
              </c:otherwise>
            </c:choose>
            <tr class="${rowClass}">
              <td align="right">${row.index+1}&nbsp;&nbsp;</td>
              <td width="10%">${translation.translationId.module}</td>
              <td width="30%">${translation.translationId.key}</td>
              <td width="30%">${translation.text}</td>
              <td width="30%">
                <input size="60" name="${translation.translationId.key}" value="${translations[translation.translationId.key]}"/>
              </td>
            </tr>
          </c:forEach>
          <tr class="fuzzy">
            <td colspan="5" align="right">
              <input type="button" onclick="javascript:window.history.back()" value="Cancel"/>
              <input type="submit" value="Send"/>
            </td>
          </tr>
        </table>
      </form>
    </c:when>
    <c:otherwise>
      <div class="error"><h3>Only admin users allowed</h3></div>
    </c:otherwise>
  </c:choose>
</body>
</html>