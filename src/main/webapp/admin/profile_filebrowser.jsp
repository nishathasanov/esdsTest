<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.openkm.com/tags/utils" prefix="u" %>
<fieldset>
  <legend>File browser</legend>
  <table>
    <tr>
      <td>Status</td>
      <td>
        <c:choose>
          <c:when test="${prf.prfFileBrowser.statusVisible}">
            <input name="prf_filebrowser_status_visible" type="checkbox" checked="checked"/>
          </c:when>
          <c:otherwise>
            <input name="prf_filebrowser_status_visible" type="checkbox"/>
          </c:otherwise>
        </c:choose>
      </td>
    </tr>
    <tr>
      <td>Massive</td>
      <td>
        <c:choose>
          <c:when test="${prf.prfFileBrowser.massiveVisible}">
            <input name="prf_filebrowser_massive_visible" type="checkbox" checked="checked"/>
          </c:when>
          <c:otherwise>
            <input name="prf_filebrowser_massive_visible" type="checkbox"/>
          </c:otherwise>
        </c:choose>
      </td>
    </tr>
    <tr>
      <td>Icon</td>
      <td>
        <c:choose>
          <c:when test="${prf.prfFileBrowser.iconVisible}">
            <input name="prf_filebrowser_icon_visible" type="checkbox" checked="checked"/>
          </c:when>
          <c:otherwise>
            <input name="prf_filebrowser_icon_visible" type="checkbox"/>
          </c:otherwise>
        </c:choose>
      </td>
    </tr>
    <tr>
      <td>Name</td>
      <td>
        <c:choose>
          <c:when test="${prf.prfFileBrowser.nameVisible}">
            <input name="prf_filebrowser_name_visible" type="checkbox" checked="checked"/>
          </c:when>
          <c:otherwise>
            <input name="prf_filebrowser_name_visible" type="checkbox"/>
          </c:otherwise>
        </c:choose>
      </td>
    </tr>
    <tr>
      <td>Size</td>
      <td>
        <c:choose>
          <c:when test="${prf.prfFileBrowser.sizeVisible}">
            <input name="prf_filebrowser_size_visible" type="checkbox" checked="checked"/>
          </c:when>
          <c:otherwise>
            <input name="prf_filebrowser_size_visible" type="checkbox"/>
          </c:otherwise>
        </c:choose>
      </td>
    </tr>
    <tr>
      <td>Last modified</td>
      <td>
        <c:choose>
          <c:when test="${prf.prfFileBrowser.lastModifiedVisible}">
            <input name="prf_filebrowser_lastmod_visible" type="checkbox" checked="checked"/>
          </c:when>
          <c:otherwise>
            <input name="prf_filebrowser_lastmod_visible" type="checkbox"/>
          </c:otherwise>
        </c:choose>
      </td>
    </tr>
    <tr>
      <td>Author</td>
      <td>
        <c:choose>
          <c:when test="${prf.prfFileBrowser.authorVisible}">
            <input name="prf_filebrowser_author_visible" type="checkbox" checked="checked"/>
          </c:when>
          <c:otherwise>
            <input name="prf_filebrowser_author_visible" type="checkbox"/>
          </c:otherwise>
        </c:choose>
      </td>
    </tr>
    <tr>
      <td>Version</td>
      <td>
        <c:choose>
          <c:when test="${prf.prfFileBrowser.versionVisible}">
            <input name="prf_filebrowser_version_visible" type="checkbox" checked="checked"/>
          </c:when>
          <c:otherwise>
            <input name="prf_filebrowser_version_visible" type="checkbox"/>
          </c:otherwise>
        </c:choose>
      </td>
    </tr>
  </table>
</fieldset>