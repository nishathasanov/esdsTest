/**
 *  ESDS, Open Document Management System (http://www.openkm.com)
 *  Copyright (c) 2006-2013  Paco Avila & Josep Llort
 *
 *  No bytes were intentionally harmed during the development of this application.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.openkm.frontend.client.bean;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * GWTProfileFileBrowser
 * 
 * @author jllort
 *
 */
public class GWTProfileFileBrowser implements IsSerializable {
	private boolean statusVisible;
	private boolean massiveVisible;
	private boolean iconVisible;
	private boolean nameVisible;
	private boolean sizeVisible;
	private boolean lastModifiedVisible;
	private boolean authorVisible;
	private boolean versionVisible;

	public boolean isStatusVisible() {
		return statusVisible;
	}

	public void setStatusVisible(boolean statusVisible) {
		this.statusVisible = statusVisible;
	}

	public boolean isMassiveVisible() {
		return massiveVisible;
	}

	public void setMassiveVisible(boolean massiveVisible) {
		this.massiveVisible = massiveVisible;
	}

	public boolean isIconVisible() {
		return iconVisible;
	}

	public void setIconVisible(boolean iconVisible) {
		this.iconVisible = iconVisible;
	}

	public boolean isNameVisible() {
		return nameVisible;
	}

	public void setNameVisible(boolean nameVisible) {
		this.nameVisible = nameVisible;
	}

	public boolean isSizeVisible() {
		return sizeVisible;
	}

	public void setSizeVisible(boolean sizeVisible) {
		this.sizeVisible = sizeVisible;
	}

	public boolean isLastModifiedVisible() {
		return lastModifiedVisible;
	}

	public void setLastModifiedVisible(boolean lastModifiedVisible) {
		this.lastModifiedVisible = lastModifiedVisible;
	}

	public boolean isAuthorVisible() {
		return authorVisible;
	}

	public void setAuthorVisible(boolean authorVisible) {
		this.authorVisible = authorVisible;
	}

	public boolean isVersionVisible() {
		return versionVisible;
	}

	public void setVersionVisible(boolean versionVisible) {
		this.versionVisible = versionVisible;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("statusVisible="); sb.append(statusVisible);
		sb.append(", massiveVisible="); sb.append(massiveVisible);
		sb.append(", iconVisible="); sb.append(iconVisible);
		sb.append(", nameVisible="); sb.append(nameVisible);
		sb.append(", sizeVisible="); sb.append(sizeVisible);
		sb.append(", lastModifiedVisible="); sb.append(lastModifiedVisible);
		sb.append(", authorVisible="); sb.append(authorVisible);
		sb.append(", versionVisible="); sb.append(versionVisible);
		sb.append("}");
		return sb.toString();
	}
}
