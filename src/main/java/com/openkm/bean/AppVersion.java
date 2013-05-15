/**
 *  OpenKM, Open Document Management System (http://www.openkm.com)
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

package com.openkm.bean;

public class AppVersion {
	public static final String EXTENSION_COM = "Community";
	
	private String major = "0";
	private String minor = "0";
	private String maintenance = "0";
	private String build = "0";
	private String extension = EXTENSION_COM;
	
	public String getMajor() {
		return major;
	}
	
	public void setMajor(String major) {
		this.major = major;
	}
	
	public String getMinor() {
		return minor;
	}
	
	public void setMinor(String minor) {
		this.minor = minor;
	}
	
	public String getMaintenance() {
		return maintenance;
	}
	
	public void setMaintenance(String maintenance) {
		this.maintenance = maintenance;
	}
	
	public String getBuild() {
		return build;
	}
	
	public void setBuild(String build) {
		this.build = build;
	}

	public String getExtension() {
		return extension;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}
	
	public String getVersion() {
		return major + "." + minor + "." + maintenance;
	}
	
	public String toString() {
		return major + "." + minor + "." + maintenance + " (build: " + build + ")";
	}
}
