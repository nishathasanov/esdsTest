<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<fieldset>
  <legend>Tab</legend>
  <table>
    <tr>
      <td>Desktop</td>
      <td>
        <c:choose>
          <c:when test="${prf.prfTab.desktopVisible}">
            <input name="prf_tab_desktop_visible" type="checkbox" checked="checked"/>
          </c:when>
          <c:otherwise>
            <input name="prf_tab_desktop_visible" type="checkbox"/>
          </c:otherwise>
        </c:choose>
      </td>
    </tr>
    <tr>
      <td>Search</td>
      <td>
        <c:choose>
          <c:when test="${prf.prfTab.searchVisible}">
            <input name="prf_tab_search_visible" type="checkbox" checked="checked"/>
          </c:when>
          <c:otherwise>
            <input name="prf_tab_search_visible" type="checkbox"/>
          </c:otherwise>
        </c:choose>
      </td>
    </tr>
    <tr>
      <td>Dashboard</td>
      <td>
        <c:choose>
          <c:when test="${prf.prfTab.dashboardVisible}">
            <input name="prf_tab_dashboard_visible" type="checkbox" checked="checked"/>
          </c:when>
          <c:otherwise>
            <input name="prf_tab_dashboard_visible" type="checkbox"/>
          </c:otherwise>
        </c:choose>
      </td>
    </tr>
    <tr>
      <td>Administration</td>
      <td>
        <c:choose>
          <c:when test="${prf.prfTab.administrationVisible}">
            <input name="prf_tab_administration_visible" type="checkbox" checked="checked"/>
          </c:when>
          <c:otherwise>
            <input name="prf_tab_administration_visible" type="checkbox"/>
          </c:otherwise>
        </c:choose>
      </td>
    </tr>
  </table>
</fieldset>