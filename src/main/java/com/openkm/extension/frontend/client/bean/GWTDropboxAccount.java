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
 * GWTDropboxAccount
 * 
 * @author sochoa
 */
public class GWTDropboxAccount implements IsSerializable {
	
	private String country;
	private String displayName;
	private long quota;
	private long quotaNormal;
	private long quotaShared;
	private String referralLink;
	private long uid;
	
	public String getCountry() {
		return country;
	}
	
	public void setCountry(String country) {
		this.country = country;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	public long getQuota() {
		return quota;
	}
	
	public void setQuota(long quota) {
		this.quota = quota;
	}
	
	public long getQuotaNormal() {
		return quotaNormal;
	}
	
	public void setQuotaNormal(long quotaNormal) {
		this.quotaNormal = quotaNormal;
	}
	
	public long getQuotaShared() {
		return quotaShared;
	}
	
	public void setQuotaShared(long quotaShared) {
		this.quotaShared = quotaShared;
	}
	
	public String getReferralLink() {
		return referralLink;
	}
	
	public void setReferralLink(String referralLink) {
		this.referralLink = referralLink;
	}
	
	public long getUid() {
		return uid;
	}
	
	public void setUid(long uid) {
		this.uid = uid;
	}
	
}
