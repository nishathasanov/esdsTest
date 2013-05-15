/**
 * OpenKM, Open Document Management System (http://www.openkm.com)
 * Copyright (c) 2006-2013 Paco Avila & Josep Llort
 * 
 * No bytes were intentionally harmed during the development of this application.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.openkm.extension.frontend.client.bean;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * GWTDropboxEntry
 * 
 * @author sochoa
 */
public class GWTDropboxEntry implements IsSerializable {
	private long bytes;
	private String clientMTime;
	private String hash;
	private String icon;
	private boolean deleted;
	private boolean dir;
	private String mimeType;
	private String modified;
	private String path;
	private String rev;
	private String root;
	private String size;
	private boolean thumbExists;
	private String fileName;
	private String parentPath;
	private boolean children;
	
	/**
	 * GWTDropboxEntry
	 */
	public GWTDropboxEntry() {
		
	}
	
	public long getBytes() {
		return bytes;
	}
	
	public void setBytes(long bytes) {
		this.bytes = bytes;
	}
	
	public String getClientMTime() {
		return clientMTime;
	}
	
	public void setClientMTime(String clientMTime) {
		this.clientMTime = clientMTime;
	}
	
	public String getHash() {
		return hash;
	}
	
	public void setHash(String hash) {
		this.hash = hash;
	}
	
	public String getIcon() {
		return icon;
	}
	
	public void setIcon(String icon) {
		this.icon = icon;
	}
	
	public boolean isDeleted() {
		return deleted;
	}
	
	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
	
	public boolean isDir() {
		return dir;
	}
	
	public void setDir(boolean dir) {
		this.dir = dir;
	}
	
	public String getMimeType() {
		return mimeType;
	}
	
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	
	public String getModified() {
		return modified;
	}
	
	public void setModified(String modified) {
		this.modified = modified;
	}
	
	public String getPath() {
		return path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	public String getRev() {
		return rev;
	}
	
	public void setRev(String rev) {
		this.rev = rev;
	}
	
	public String getRoot() {
		return root;
	}
	
	public void setRoot(String root) {
		this.root = root;
	}
	
	public String getSize() {
		return size;
	}
	
	public void setSize(String size) {
		this.size = size;
	}
	
	public boolean isThumbExists() {
		return thumbExists;
	}
	
	public void setThumbExists(boolean thumbExists) {
		this.thumbExists = thumbExists;
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public String getParentPath() {
		return parentPath;
	}
	
	public void setParentPath(String parentPath) {
		this.parentPath = parentPath;
	}
	
	public boolean isChildren() {
		return children;
	}
	
	public void setChildren(boolean children) {
		this.children = children;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("fileName=").append(fileName);
		sb.append(", mimeType=").append(mimeType);
		sb.append(", size=").append(size);
		sb.append(", path=").append(path);
		sb.append("}");
		return sb.toString();
	}
}
