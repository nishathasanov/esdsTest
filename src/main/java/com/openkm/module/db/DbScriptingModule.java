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

package com.openkm.module.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import com.openkm.core.AccessDeniedException;
import com.openkm.core.Config;
import com.openkm.core.DatabaseException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.RepositoryException;
import com.openkm.dao.NodeBaseDAO;
import com.openkm.module.ScriptingModule;
import com.openkm.spring.PrincipalUtils;
import com.openkm.util.UserActivity;

public class DbScriptingModule implements ScriptingModule {
	private static Logger log = LoggerFactory.getLogger(DbScriptingModule.class);
	
	@Override
	public void setScript(String token, String nodePath, String code) throws PathNotFoundException,
			AccessDeniedException, RepositoryException, DatabaseException {
		log.debug("setScript({}, {}, {})", new Object[] { token, nodePath, code });
		Authentication auth = null, oldAuth = null;
		
		if (Config.SYSTEM_READONLY) {
			throw new AccessDeniedException("System is in read-only mode");
		}
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			if (PrincipalUtils.getRoles().contains(Config.DEFAULT_ADMIN_ROLE)) {
				String uuid = NodeBaseDAO.getInstance().getUuidFromPath(nodePath);
				NodeBaseDAO.getInstance().setScript(uuid, code);
				
				// Activity log
				UserActivity.log(auth.getName(), "SET_SCRIPT", uuid, nodePath, code);
			} else {
				throw new AccessDeniedException("Sorry, only for admin user");
			}
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
	}
	
	@Override
	public void removeScript(String token, String nodePath) throws PathNotFoundException, AccessDeniedException,
			RepositoryException, DatabaseException {
		log.debug("removeScript({}, {})", token, nodePath);
		Authentication auth = null, oldAuth = null;
		
		if (Config.SYSTEM_READONLY) {
			throw new AccessDeniedException("System is in read-only mode");
		}
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			if (PrincipalUtils.getRoles().contains(Config.DEFAULT_ADMIN_ROLE)) {
				String uuid = NodeBaseDAO.getInstance().getUuidFromPath(nodePath);
				NodeBaseDAO.getInstance().removeScript(uuid);
				
				// Activity log
				UserActivity.log(auth.getName(), "REMOVE_SCRIPT", uuid, nodePath, null);
			} else {
				throw new AccessDeniedException("Sorry, only for admin user");
			}
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
	}
	
	@Override
	public String getScript(String token, String nodePath) throws PathNotFoundException, AccessDeniedException,
			RepositoryException, DatabaseException {
		log.debug("getScript({}, {})", token, nodePath);
		String code = null;
		@SuppressWarnings("unused")
		Authentication auth = null, oldAuth = null;
		
		if (Config.SYSTEM_READONLY) {
			throw new AccessDeniedException("System is in read-only mode");
		}
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			if (PrincipalUtils.getRoles().contains(Config.DEFAULT_ADMIN_ROLE)) {
				String uuid = NodeBaseDAO.getInstance().getUuidFromPath(nodePath);
				code = NodeBaseDAO.getInstance().getScript(uuid);
			} else {
				throw new AccessDeniedException("Sorry, only for admin user");
			}
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getScript: {}", code);
		return code;
	}
}
