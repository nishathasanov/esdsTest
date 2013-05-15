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

package com.openkm.module.jcr.stuff;

import java.security.Principal;
import java.security.acl.Group;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import org.apache.jackrabbit.core.security.authentication.AbstractLoginModule;
import org.apache.jackrabbit.core.security.authentication.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author pavila
 *
 * JBoss security framework (several login modules):
 * http://wiki.jboss.org/wiki/Wiki.jsp?page=JBossSX
 * 
 * JBoss UsersRolesLoginModule.java source code:
 * http://wiki.jboss.org/wiki/Wiki.jsp?page=JBossSX
 */
public class LoginModule extends AbstractLoginModule {
	private static Logger log = LoggerFactory.getLogger(LoginModule.class);

	@Override
	@SuppressWarnings("rawtypes")
	protected void doInit(CallbackHandler callbackHandler, Session session, Map options) throws LoginException {
		log.info("CallbackHandler: {}", callbackHandler);
		log.info("Session: {}", session);
		log.info("Options: {}", options);
		log.info("init: LoginModule. Done.");
	}

	@Override
	protected boolean impersonate(Principal principal, Credentials credentials)
			throws RepositoryException, LoginException {
		if (principal instanceof Group) {
            return false;
        }
		
        Subject impersSubject = getImpersonatorSubject(credentials);
        return impersSubject != null;
	}

	@Override
	protected Authentication getAuthentication(Principal principal,
			Credentials creds) throws RepositoryException {
		if (principal instanceof Group) {
            return null;
        }
		
        return new OKMAuthentication();
	}

	@Override
	protected Principal getPrincipal(Credentials credentials) {
		String userId = getUserID(credentials);
		Principal principal = principalProvider.getPrincipal(userId);
		
		if (principal == null || principal instanceof Group) {
			// no matching user principal
			return null;
		} else {
			return principal;
		}
	}
	
	/**
	 * Define methods to validate Credentials upon authentication.
	 */
	class OKMAuthentication implements Authentication {
		@Override
		public boolean canHandle(Credentials credentials) {
			log.info("Credentials: {}", credentials.getClass().getCanonicalName());
			return true;
		}

		@Override
		public boolean authenticate(Credentials credentials) throws RepositoryException {
			if (credentials instanceof SimpleCredentials) {
				SimpleCredentials sc = (SimpleCredentials) credentials;
				log.info("User: {}", sc.getUserID());
				log.info("Password: {}", sc.getPassword());
				return true;
			} else {
				throw new RepositoryException("Unexpected credentials: " + credentials.getClass().getCanonicalName());
			}
		}		
	}
}
