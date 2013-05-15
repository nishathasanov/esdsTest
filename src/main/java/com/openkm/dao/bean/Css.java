/**
 * ESDS, Open Document Management System (http://www.openkm.com)
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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Type;

@Entity
@Table(name = "OKM_CSS", uniqueConstraints = {
		// ALTER TABLE OKM_CSS ADD CONSTRAINT IDX_CSS_NAME_CTX UNIQUE (CSS_NAME, CSS_CONTEXT)
		@UniqueConstraint(name = "IDX_CSS_NAME_CTX", columnNames = { "CSS_NAME", "CSS_CONTEXT" })
	})
public class Css implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final String CONTEXT_FRONTEND = "frontend";
	public static final String CONTEXT_ADMINISTRATION = "administration";
	public static final String CONTEXT_EXTENSION = "extension";
	
	@Id
	@Column(name = "CSS_ID")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;
	
	@Column(name = "CSS_NAME", length = 64)
	private String name;
	
	@Column(name = "CSS_CONTEXT", length = 64)
	private String context;
	
	@Column(name = "CSS_CONTENT")
	@Lob
	@Type(type = "org.hibernate.type.StringClobType")
	private String content;
	
	@Column(name = "CSS_ACTIVE", nullable = false)
	@Type(type = "true_false")
	private boolean active;
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getContent() {
		return content;
	}
	
	public void setContent(String content) {
		this.content = content;
	}
	
	public boolean isActive() {
		return active;
	}
	
	public void setActive(boolean active) {
		this.active = active;
	}
	
	public String getContext() {
		return context;
	}
	
	public void setContext(String context) {
		this.context = context;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("id=").append(id);
		sb.append(", name=").append(name);
		sb.append(", type=").append(context);
		sb.append(", active=").append(active);
		sb.append(", content=").append(content);
		sb.append("}");
		return sb.toString();
	}
}