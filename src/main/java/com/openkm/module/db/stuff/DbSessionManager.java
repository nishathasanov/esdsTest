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

package com.openkm.module.db.stuff;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.openkm.bean.DbSessionInfo;
import com.openkm.core.Config;

/**
 * @author pavila
 */
public class DbSessionManager {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(DbSessionManager.class);
	private static DbSessionManager instance = new DbSessionManager();
	private Map<String, DbSessionInfo> sessions = new HashMap<String, DbSessionInfo>();
	private static String systemToken;
	
	/**
	 * Prevents class instantiation
	 */
	private DbSessionManager() {
	}
	
	/**
	 * Instantiate a SessionManager.
	 */
	public static DbSessionManager getInstance() {
		return instance;
	}
	
	/**
	 * Get system token
	 */
	public String getSystemToken() {
		return systemToken;
	}
	
	/**
	 * Set system session
	 */
	public void putSystemSession() {
		systemToken = UUID.randomUUID().toString();
		Authentication auth = new UsernamePasswordAuthenticationToken(Config.SYSTEM_USER, null, null);
		add(systemToken, auth);
	}
	
	/**
	 * Add a new session
	 */
	public synchronized void add(String token, Authentication auth) {
		DbSessionInfo si = new DbSessionInfo();
		si.setAuth(auth);
		si.setCreation(Calendar.getInstance());
		si.setLastAccess(Calendar.getInstance());
		sessions.put(token, si);
	}
	
	/**
	 * Return a session
	 */
	public Authentication getAuthentication(String token) {
		DbSessionInfo si = (DbSessionInfo) sessions.get(token);
		
		if (si != null) {
			si.setLastAccess(Calendar.getInstance());
			return si.getAuth();
		}
		
		return null;
	}
	
	/**
	 * Return a token which pertains to a authentication session
	 */
	public String getToken(Authentication auth) {
		for (Entry<String, DbSessionInfo> entry : sessions.entrySet()) {
			if (entry.getValue().getAuth().equals(auth)) {
				return entry.getKey();
			}
		}
		
		return null;
	}
	
	/**
	 * Return a session info
	 */
	public DbSessionInfo getInfo(String token) {
		return sessions.get(token);
	}
	
	/**
	 * Remove a session
	 */
	public synchronized void remove(String token) {
		sessions.remove(token);
	}
		
	/**
	 * Return all active tokens
	 */
	public List<String> getTokens() {
		List<String> list = new ArrayList<String>();
		
		for (String token : sessions.keySet()) {
			list.add(token);
		}
		
		return list;
	}
	
	/**
	 * Get active sessions
	 */
	public Map<String, DbSessionInfo> getSessions() {
		return sessions;
	}
}
