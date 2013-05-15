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

package com.openkm.principal;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.core.Config;

public class DummyPrincipalAdapter implements PrincipalAdapter {
	private static Logger log = LoggerFactory.getLogger(DummyPrincipalAdapter.class);
	private static final String TEST_USER = "monkiki";
	
	@Override
	public List<String> getUsers() throws PrincipalAdapterException {
		log.debug("getUsers()");
		List<String> list = new ArrayList<String>();
		list.add(Config.ADMIN_USER);
		list.add(TEST_USER);
		log.debug("getUsers: {}", list);
		return list;
	}

	@Override
	public List<String> getRoles() throws PrincipalAdapterException {
		log.debug("getRoles()");
		List<String> list = new ArrayList<String>();
		list.add(Config.DEFAULT_ADMIN_ROLE);
		list.add(Config.DEFAULT_USER_ROLE);
		log.debug("getRoles: {}", list);
		return list;
	}
	
	@Override
	public List<String> getUsersByRole(String role) throws PrincipalAdapterException {
		List<String> list = new ArrayList<String>();
		
		if (role.equals(Config.DEFAULT_ADMIN_ROLE)) {
			list.add(Config.ADMIN_USER);
		} else if (role.equals(Config.DEFAULT_USER_ROLE)) {
			list.add(Config.ADMIN_USER);
			list.add(TEST_USER);
		}
		
		return list;
	}
	
	@Override
	public List<String> getRolesByUser(String user) throws PrincipalAdapterException {
		List<String> list = new ArrayList<String>();
		
		if (user.equals(Config.ADMIN_USER)) {
			list.add(Config.DEFAULT_ADMIN_ROLE);
			list.add(Config.DEFAULT_USER_ROLE);
		} else if (user.equals(TEST_USER)) {
			list.add(Config.DEFAULT_USER_ROLE);
		}
		
		return list;
	}

	@Override
	public String getMail(String user) throws PrincipalAdapterException {
		String mail = null;
		
		if (user.equals(Config.ADMIN_USER)) {
			mail = "admin@openkm.com";
		} else if (user.equals(TEST_USER)) {
			mail = "monkiki@openkm.com";
		}
		
		return mail;
	}

	@Override
	public String getName(String user) throws PrincipalAdapterException {
		String name = null;
		
		if (user.equals(Config.ADMIN_USER)) {
			name = "Administrator";
		} else if (user.equals(TEST_USER)) {
			name = "Monkiki";
		}
		
		return name;
	}

	@Override
	public String getPassword(String user) throws PrincipalAdapterException {
		String password = null;
		
		if (user.equals(Config.ADMIN_USER)) {
			password = "admin";
		} else if (user.equals(TEST_USER)) {
			password = "monkiki";
		}
		
		return password;
	}
}
