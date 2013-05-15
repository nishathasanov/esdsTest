<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<fieldset>
  <legend>Explorer</legend>
  <table>
    <tr>
      <td>Type filter</td>
      <td>
        <c:choose>
          <c:when test="${prf.prfExplorer.typeFilterEnabled}">
            <input name="prf_explorer_type_filter_enabled" type="checkbox" checked="checked"/>
          </c:when>
          <c:otherwise>
            <input name="prf_explorer_type_filter_enabled" type="checkbox"/>
          </c:otherwise>
        </c:choose>
      </td>
    </tr>
  </table>
</fieldset>