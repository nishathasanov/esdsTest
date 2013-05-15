<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.openkm.core.Config" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.openkm.com/tags/utils" prefix="u" %>
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
  <link rel="Shortcut icon" href="favicon.ico" />
  <link rel="stylesheet" type="text/css" href="css/style.css" />
  <link rel="stylesheet" type="text/css" href="css/selectlist/style.css" />
  <link rel="stylesheet" type="text/css" href="css/selectlist/selectlist.css" />
  <script src="../js/jquery-1.7.1.min.js" type="text/javascript"></script>
  <script src="../js/vanadium-min.js" type="text/javascript"></script>
  <script type="text/javascript" src="js/jquery.selectlist.min.js"></script>
  <title>OMR Template</title>
  <script type="text/javascript">
    $(document).ready(function() {
    	$('#alert').hide();
    	$('select#om_properties').selectList();
    	
    	$('#form').submit(function() {
    		$('#alert').show();
    		$('input:submit').attr('disabled', true);
    		return true;
    	});
	});
  </script>
</head>
<body> 
<c:set var="isAdmin"><%=request.isUserInRole(Config.DEFAULT_ADMIN_ROLE)%></c:set>
  <c:choose>
    <c:when test="${isAdmin}">
      <ul id="breadcrumb">
        <li class="path">
          <a href="Omr">OMR template</a>
        </li>
        <li class="path">
          <c:choose>
            <c:when test="${action == 'create'}">Create OMR template</c:when>
            <c:when test="${action == 'edit'}">Edit OMR template</c:when>
            <c:when test="${action == 'delete'}">Delete OMR template</c:when>
          </c:choose>
        </li>
      </ul>
      <br/>
      <c:url value="Omr" var="urlDownload">
        <c:param name="action" value="downloadFile"/>
        <c:param name="om_id" value="${om.id}"/>
      </c:url>
  	<form action="Omr" method="post" enctype="multipart/form-data" id="form">
  	<input type="hidden" name="action" value="${action}"/>
        <input type="hidden" name="om_id" value="${om.id}"/>
        <table class="form" width="425px">
          <tr>
            <td colspan="2">
            	<div id="alert" class="ok" style="text-align: center;">The operation may take several minutes, please be patient...</div>
            </td>
          </tr>
          <tr>
            <td nowrap="nowrap">Template name</td>
            <td><input class=":required :only_on_blur" name="om_name" value="${om.name}"/></td>
          </tr>
          <tr>
            <td valign="top">Template</td>
            <td valign="top">
              <c:if test="${om.templateFileName != null && om.templateFileName ne ''}">
            	<a href="${urlDownload}&type=1">${om.templateFileName}</a><br/>
              </c:if>
              <c:choose>
                <c:when test="${action == 'create'}">
                  <input class=":required :only_on_blur" type="file" name="file"/>
                </c:when>
                <c:otherwise>
                  <input type="file" name="file"/>
                </c:otherwise>
              </c:choose>
            </td>
          </tr>
          <tr>
          	<td valign="top">Properties</td>
          	<td>
          		<select id="om_properties" name="om_properties" title="Select property" multiple="multiple">
	           	  <c:forEach var="property" items="${properties}" varStatus="row">
	           	    <c:choose>
	           	    	<c:when test="${u:contains(om.properties, property)}">
	           	    		<option value="${property}" selected="selected">${property}</option>
	           	    	</c:when>
	           	    	<c:otherwise>
	           	    		<option value="${property}">${property}</option>
	           	    	</c:otherwise>
	           	    </c:choose>
	           	  </c:forEach>
           	  	</select>
          	</td>
          </tr>
          <c:if test="${om.ascFileName != null && om.ascFileName ne ''}">
              <c:url value="Omr" var="urlEditAsc">
	            <c:param name="action" value="editAsc"/>
	            <c:param name="om_id" value="${om.id}"/>
	          </c:url>
			  <tr>
			  	<td valign="top">Asc</td>
			  	<td valign="top">
			  		<a href="${urlDownload}&type=2">${om.ascFileName}</a>
			  		<c:if test="${action ne 'delete'}">
			  			<a href="${urlEditAsc}"><img src="img/action/edit.png" alt="Edit" title="Edit"/></a>
			  		</c:if>
			  	</td>
			  </tr>
          </c:if>
          <c:if test="${om.configFileName != null && om.configFileName ne ''}">
			  <tr>
			  	<td valign="top">Config</td>
			  	<td valign="top">
			  		<a href="${urlDownload}&type=3">${om.configFileName}</a>
			  	</td>
			  </tr>
          </c:if>
          <c:if test="${om.ascFileName != null && om.ascFileName ne ''}">
              <c:url value="Omr" var="urlEditFields">
	            <c:param name="action" value="editFields"/>
	            <c:param name="om_id" value="${om.id}"/>
	          </c:url>
			  <tr>
			  	<td valign="top">Fields</td>
			  	<td valign="top">
			  		<c:choose>
					    <c:when test="${om.fieldsFileName != null && om.fieldsFileName ne ''}">
					        <a href="${urlDownload}&type=4">${om.fieldsFileName}</a>
					    </c:when>
					    <c:otherwise>
					    	<c:if test="${action ne 'delete' }">
					        	Upload new file
							</c:if>        
					    </c:otherwise>
					</c:choose>
					<c:if test="${action ne 'delete' }">
			  			<a href="${urlEditFields}"><img src="img/action/edit.png" alt="Edit" title="Edit"/></a>
			  		</c:if>
			  	</td>
			  </tr>
          </c:if>
          <tr>
            <td>Active</td>
            <td>
              <c:choose>
                <c:when test="${om.active}">
                  <input name="om_active" id="ot_active" type="checkbox" checked="checked"/>
                </c:when>
                <c:otherwise>
                  <input name="om_active" id="ot_active" type="checkbox"/>
                </c:otherwise>
              </c:choose>
            </td>
          </tr>
          <tr>
            <td colspan="2" align="right">
              <div id="buttons">
              	<input type="button" onclick="javascript:window.history.back()" value="Cancel"/>
              	<input type="submit" value="Send"/>
              </div>
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