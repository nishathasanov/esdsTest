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

package com.openkm.module.common;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.core.Config;
import com.openkm.principal.PrincipalAdapter;
import com.openkm.principal.PrincipalAdapterException;

public class CommonAuthModule {
	private static Logger log = LoggerFactory.getLogger(CommonAuthModule.class);
	private static PrincipalAdapter principalAdapter = null;
	
	/**
	 * Get users
	 */
	public static List<String> getUsers(String token) throws PrincipalAdapterException {
		log.debug("getUsers()");
		List<String> list = null;

		try {
			PrincipalAdapter principalAdapter = CommonAuthModule.getPrincipalAdapter();
			list = principalAdapter.getUsers();
		} catch (PrincipalAdapterException e) {
			log.error(e.getMessage(), e);
			throw e;
		}

		log.debug("getUsers: {}", list);
		return list;
	}
	
	/**
	 * Get roles
	 */
	public static List<String> getRoles(String token) throws PrincipalAdapterException {
		log.debug("getRoles()");
		List<String> list = null;

		try {
			PrincipalAdapter principalAdapter = CommonAuthModule.getPrincipalAdapter();
			list = principalAdapter.getRoles();
		} catch (PrincipalAdapterException e) {
			log.error(e.getMessage(), e);
			throw e;
		}

		log.debug("getRoles: {}", list);
		return list;
	}
	
	/**
	 * Get users by role
	 */
	public static List<String> getUsersByRole(String token, String role) throws PrincipalAdapterException {
		log.debug("getUsersByRole({})", role);
		List<String> list = null;

		try {
			PrincipalAdapter principalAdapter = CommonAuthModule.getPrincipalAdapter();
			list = principalAdapter.getUsersByRole(role);
		} catch (PrincipalAdapterException e) {
			log.error(e.getMessage(), e);
			throw e;
		}

		log.debug("getUsersByRole: {}", list);
		return list;
	}
	
	/**
	 * Get roles from user
	 */
	public static List<String> getRolesByUser(String token, String user) throws PrincipalAdapterException {
		log.debug("getRolesByUser({})", user);
		List<String> list = null;

		try {
			PrincipalAdapter principalAdapter = CommonAuthModule.getPrincipalAdapter();
			list = principalAdapter.getRolesByUser(user);
		} catch (PrincipalAdapterException e) {
			log.error(e.getMessage(), e);
			throw e;
		}

		log.debug("getRolesByUser: {}", list);
		return list;
	}
	
	/**
	 * Get mail from user
	 */
	public static String getMail(String token, String user) throws PrincipalAdapterException {
		log.debug("getMail({}, {})", token, user);
		String mail = null;

		try {
			PrincipalAdapter principalAdapter = CommonAuthModule.getPrincipalAdapter();
			mail = principalAdapter.getMail(user);
		} catch (PrincipalAdapterException e) {
			log.error(e.getMessage(), e);
			throw e;
		}

		log.debug("getMail: {}", mail);
		return mail;
	}
	
	/**
	 * Get name from user.
	 */
	public static String getName(String token, String user) throws PrincipalAdapterException {
		log.debug("getName({}, {})", token, user);
		String name = null;

		try {
			PrincipalAdapter principalAdapter = CommonAuthModule.getPrincipalAdapter();
			name = principalAdapter.getName(user);
			
			// Prevent NPE when looking for name of deleted users
			if (name == null) {
				name = user;
			}
		} catch (PrincipalAdapterException e) {
			log.error(e.getMessage(), e);
			throw e;
		}

		log.debug("getName: {}", name);
		return name;
	}
	
	/**
	 * Singleton pattern for global Principal Adapter.
	 */
	public static PrincipalAdapter getPrincipalAdapter() throws PrincipalAdapterException {
		if (principalAdapter == null) {
			try {
				log.info("PrincipalAdapter: {}", Config.PRINCIPAL_ADAPTER);
				Object object = Class.forName(Config.PRINCIPAL_ADAPTER).newInstance();
				principalAdapter = (PrincipalAdapter) object;
			} catch (ClassNotFoundException e) {
				log.error(e.getMessage(), e);
				throw new PrincipalAdapterException(e.getMessage(), e);
			} catch (InstantiationException e) {
				log.error(e.getMessage(), e);
				throw new PrincipalAdapterException(e.getMessage(), e);
			} catch (IllegalAccessException e) {
				log.error(e.getMessage(), e);
				throw new PrincipalAdapterException(e.getMessage(), e);
			}
		}

		return principalAdapter;
	}
}
