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

package com.openkm.dao.bean;

import java.util.Calendar;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.CalendarBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;

@Entity
@Indexed
@Table(name = "OKM_NODE_DOCUMENT")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class NodeDocument extends NodeBase {
	private static final long serialVersionUID = 1L;
	public static final String TEXT_FIELD = "text";
	
	@Column(name = "NDC_LAST_MODIFIED")
	@Field(index = Index.UN_TOKENIZED, store = Store.YES)
	@CalendarBridge(resolution = Resolution.DAY)
	private Calendar lastModified;
	
	@Column(name = "NDC_LANGUAGE", length = 8)
	@Field(index = Index.UN_TOKENIZED, store = Store.YES)
	private String language;
	
	@Column(name = "NDC_TITLE", length = 256)
	@Field(index = Index.TOKENIZED, store = Store.YES)
	private String title;
	
	@Column(name = "NDC_DESCRIPTION", length = 2048)
	@Field(index = Index.TOKENIZED, store = Store.YES)
	private String description;
	
	@Column(name = "NDC_MIME_TYPE", length = 128)
	@Field(index = Index.UN_TOKENIZED, store = Store.YES)
	private String mimeType;
	
	@Column(name = "NDC_TEXT")
	@Lob @Type(type = "org.hibernate.type.TextType")
	@Field(index = Index.TOKENIZED, store = Store.NO)
	private String text;
	
	@Column(name = "NDC_CHECKED_OUT")
	@Type(type = "true_false")
	@Field(index = Index.UN_TOKENIZED, store = Store.YES)
	private boolean checkedOut;
	
	@Column(name = "NDC_TEXT_EXTRACTED")
	@Type(type = "true_false")
	@Field(index = Index.UN_TOKENIZED, store = Store.YES)
	private boolean textExtracted;
	
	@Column(name = "NDC_LOCKED")
	@Type(type = "true_false")
	@Field(index = Index.UN_TOKENIZED, store = Store.YES)
	private boolean locked;
	
	@Embedded
	private NodeLock lock;
	
	public Calendar getLastModified() {
		return lastModified;
	}
	
	public void setLastModified(Calendar lastModified) {
		this.lastModified = lastModified;
	}
	
	public String getLanguage() {
		return language;
	}
	
	public void setLanguage(String language) {
		this.language = language;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getMimeType() {
		return mimeType;
	}
	
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	
	public String getText() {
		return text;
	}
	
	public void setText(String text) {
		this.text = text;
	}
	
	public boolean isCheckedOut() {
		return checkedOut;
	}
	
	public void setCheckedOut(boolean checkedOut) {
		this.checkedOut = checkedOut;
	}
	
	public boolean isTextExtracted() {
		return textExtracted;
	}

	public void setTextExtracted(boolean textExtracted) {
		this.textExtracted = textExtracted;
	}
	
	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	public NodeLock getLock() {
		return lock;
	}

	public void setLock(NodeLock lock) {
		this.lock = lock;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("uuid=").append(uuid);
		sb.append(", context=").append(context);
		sb.append(", parent=").append(parent);
		sb.append(", author=").append(author);
		sb.append(", name=").append(name);
		sb.append(", created=").append(created == null ? null : created.getTime());
		sb.append(", lastModified=").append(lastModified == null ? null : lastModified.getTime());
		sb.append(", language=").append(language);
		sb.append(", title=").append(title);
		sb.append(", description=").append(description);
		sb.append(", mimeType=").append(mimeType);
		sb.append(", checkedOut=").append(checkedOut);
		sb.append(", subscriptors=").append(subscriptors);
		sb.append(", keywords=").append(keywords);
		sb.append(", categories=").append(categories);
		// sb.append(", properties=").append(properties); Prevents Lazy Exception
		sb.append(", userPermissions=").append(userPermissions);
		sb.append(", rolePermissions=").append(rolePermissions);
		sb.append(", textExtracted=").append(textExtracted);
		sb.append(", locked=").append(locked);
		sb.append(", lock=").append(lock);
		sb.append("}");
		return sb.toString();
	}
}
