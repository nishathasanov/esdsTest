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

package com.openkm.principal;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ReferralException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.core.Config;

/**
 * @author pavila
 */
public class LdapPrincipalAdapter implements PrincipalAdapter {
	private static Logger log = LoggerFactory.getLogger(LdapPrincipalAdapter.class);
		
	@Override
	public List<String> getUsers() throws PrincipalAdapterException {
		log.debug("getUsers()");
		List<String> list = new ArrayList<String>();
		
		// @formatter:off
		List<String> ldap = ldapSearch(
				Config.PRINCIPAL_LDAP_USER_SEARCH_BASE,
				Config.PRINCIPAL_LDAP_USER_SEARCH_FILTER,
				Config.PRINCIPAL_LDAP_USER_ATTRIBUTE);
		// @formatter:on
		
		for (Iterator<String> it = ldap.iterator(); it.hasNext();) {
			String user = it.next();
			
			if (!Config.SYSTEM_USER.equals(user)) {
				if (Config.SYSTEM_LOGIN_LOWERCASE) {
					user = user.toLowerCase();
				}
				
				list.add(user);
			}
		}
		
		if (Config.PRINCIPAL_LDAP_USERS_FROM_ROLES) {
			// Get Roles
			// @formatter:off
			List<String> roles = ldapSearch(
					Config.PRINCIPAL_LDAP_ROLE_SEARCH_BASE,
					Config.PRINCIPAL_LDAP_ROLE_SEARCH_FILTER,
					Config.PRINCIPAL_LDAP_ROLE_ATTRIBUTE);
			// @formatter:on
			
			// Get Users by Role
			for (String role : roles) {
				// @formatter:off
				List<String> users = ldapSearch(
						MessageFormat.format(Config.PRINCIPAL_LDAP_USERS_BY_ROLE_SEARCH_BASE, role),
						MessageFormat.format(Config.PRINCIPAL_LDAP_USERS_BY_ROLE_SEARCH_FILTER, role),
						Config.PRINCIPAL_LDAP_USERS_BY_ROLE_ATTRIBUTE);
				// @formatter:on
				
				for (String user : users) {
					if (!Config.SYSTEM_USER.equals(user)) {
						if (Config.SYSTEM_LOGIN_LOWERCASE) {
							user = user.toLowerCase();
						}
						
						if (!list.contains(user)) {
							list.add(user);
						}
					}
				}
			}
		}
		
		// limit users
        if (list.size() > 150) {
            throw new PrincipalAdapterException("User limit exceeded");
        }
		
		log.debug("getUsers: {}", list);
		return list;
	}
	
	@Override
	public List<String> getRoles() throws PrincipalAdapterException {
		log.debug("getRoles()");
		List<String> list = new ArrayList<String>();
		
		// @formatter:off
		List<String> ldap = ldapSearch(
				Config.PRINCIPAL_LDAP_ROLE_SEARCH_BASE,
				Config.PRINCIPAL_LDAP_ROLE_SEARCH_FILTER,
				Config.PRINCIPAL_LDAP_ROLE_ATTRIBUTE);
		// @formatter:on
		
		for (Iterator<String> it = ldap.iterator(); it.hasNext();) {
			String role = it.next();
			list.add(role);
		}
		
		log.debug("getRoles: {}", list);
		return list;
	}
	
	@Override
	public String getMail(String user) throws PrincipalAdapterException {
		log.debug("getMail({})", user);
		String mail = null;
		
		// @formatter:off
		List<String> ldap = ldapSearch(
				MessageFormat.format(Config.PRINCIPAL_LDAP_MAIL_SEARCH_BASE, user),
				MessageFormat.format(Config.PRINCIPAL_LDAP_MAIL_SEARCH_FILTER, user),
				Config.PRINCIPAL_LDAP_MAIL_ATTRIBUTE);
		// @formatter:on
		
		if (!ldap.isEmpty()) {
			mail = ldap.get(0);
		}
		
		log.debug("getMail: {}", mail);
		return mail;
	}
	
	@Override
	public String getName(String user) throws PrincipalAdapterException {
		log.debug("getName({})", user);
		String name = null;
		
		// @formatter:off
		List<String> ldap = ldapSearch(
				MessageFormat.format(Config.PRINCIPAL_LDAP_USERNAME_SEARCH_BASE, user),
				MessageFormat.format(Config.PRINCIPAL_LDAP_USERNAME_SEARCH_FILTER, user),
				Config.PRINCIPAL_LDAP_USERNAME_ATTRIBUTE);
		// @formatter:on
		
		if (!ldap.isEmpty()) {
			name = ldap.get(0);
		}
		
		log.debug("getName: {}", name);
		return name;
	}
	
	@Override
	public String getPassword(String user) throws PrincipalAdapterException {
		throw new UnsupportedOperationException("Not implemented");
	}
	
	@Override
	public List<String> getUsersByRole(String role) throws PrincipalAdapterException {
		log.debug("getUsersByRole({})", role);
		List<String> list = new ArrayList<String>();
		
		// @formatter:off
		List<String> ldap = ldapSearch(
				MessageFormat.format(Config.PRINCIPAL_LDAP_USERS_BY_ROLE_SEARCH_BASE, role),
				MessageFormat.format(Config.PRINCIPAL_LDAP_USERS_BY_ROLE_SEARCH_FILTER, role),
				Config.PRINCIPAL_LDAP_USERS_BY_ROLE_ATTRIBUTE);
		// @formatter:on
		
		for (Iterator<String> it = ldap.iterator(); it.hasNext();) {
			String user = it.next();
			
			if (!Config.SYSTEM_USER.equals(user)) {
				if (Config.SYSTEM_LOGIN_LOWERCASE) {
					user = user.toLowerCase();
				}
				
				list.add(user);
			}
		}
		
		log.debug("getUsersByRole: {}", list);
		return list;
	}
	
	@Override
	public List<String> getRolesByUser(String user) throws PrincipalAdapterException {
		log.debug("getRolesByUser({})", user);
		List<String> list = new ArrayList<String>();
		
		// @formatter:off
		List<String> ldap = ldapSearch(
				MessageFormat.format(Config.PRINCIPAL_LDAP_ROLES_BY_USER_SEARCH_BASE, user),
				MessageFormat.format(Config.PRINCIPAL_LDAP_ROLES_BY_USER_SEARCH_FILTER, user),
				Config.PRINCIPAL_LDAP_ROLES_BY_USER_ATTRIBUTE);
		// @formatter:on
		
		for (Iterator<String> it = ldap.iterator(); it.hasNext();) {
			String role = it.next();
			list.add(role);
		}
		
		log.debug("getRolesByUser: {}", list);
		return list;
	}
	
	/**
	 * LDAP Search
	 */
	private List<String> ldapSearch(String searchBase, String searchFilter, String attribute) {
		List<String> searchBases = new ArrayList<String>();
		searchBases.add(searchBase);
		return ldapSearch(searchBases, searchFilter, attribute);
	}
	
	private List<String> ldapSearch(List<String> searchBases, String searchFilter, String attribute) {
		log.debug("ldapSearch({}, {}, {})", new Object[] { searchBases, searchFilter, attribute });
		List<String> al = new ArrayList<String>();
		DirContext ctx = null;
		
		Hashtable<String, String> env = getEnvironment();
		
		try {
			ctx = new InitialDirContext(env);
			SearchControls searchCtls = new SearchControls();
			searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			
			for (String searchBase : searchBases) {
				NamingEnumeration<SearchResult> results = ctx.search(searchBase, searchFilter, searchCtls);
				
				while (results.hasMore()) {
					SearchResult searchResult = (SearchResult) results.next();
					Attributes attributes = searchResult.getAttributes();
					
					if (attribute.equals("")) {
						StringBuilder sb = new StringBuilder();
						
						for (NamingEnumeration<?> ne = attributes.getAll(); ne.hasMore();) {
							Attribute attr = (Attribute) ne.nextElement();
							sb.append(attr.toString());
							sb.append("\n");
						}
						
						al.add(sb.toString());
					} else {
						Attribute attrib = attributes.get(attribute);
						
						if (attrib != null) {
							// Handle multi-value attributes
							for (NamingEnumeration<?> ne = attrib.getAll(); ne.hasMore();) {
								String value = (String) ne.nextElement();
								
								// If FQDN get only main part
								if (value.startsWith("CN=") || value.startsWith("cn=")) {
									String cn = value.substring(3, value.indexOf(','));
									log.debug("FQDN: {}, CN: {}", value, cn);
									al.add(cn);
								} else {
									al.add(value);
								}
							}
						}
					}
				}
			}
		} catch (ReferralException e) {
			log.error("ReferralException: {}", e.getMessage());
			log.error("ReferralInfo: {}", e.getReferralInfo());
			log.error("ResolvedObj: {}", e.getResolvedObj());
			
			try {
				log.error("ReferralContext: {}", e.getReferralContext());
			} catch (NamingException e1) {
				log.error("NamingException logging context: {}", e1.getMessage());
			}
		} catch (NamingException e) {
			log.error("NamingException: {} (Base: {} - Filter: {} - Attribute: {})",
					new Object[] { e.getMessage(), searchBases, searchFilter, attribute });
		} finally {
			try {
				if (ctx != null) {
					ctx.close();
				}
			} catch (NamingException e) {
				log.error("NamingException closing context: {}", e.getMessage());
			}
		}
		
		log.debug("ldapSearch: {}", al);
		return al;
	}
	
	/**
	 * Create static LDAP configuration environment.
	 */
	private static Hashtable<String, String> getEnvironment() {
		Hashtable<String, String> env = new Hashtable<String, String>();
		
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.PROVIDER_URL, Config.PRINCIPAL_LDAP_SERVER);
		
		// Enable connection pooling
		// @see http://docs.oracle.com/javase/jndi/tutorial/ldap/connect/pool.html
		env.put("com.sun.jndi.ldap.connect.pool", "true");
		
		/**
		 * Referral values: ignore, follow or throw.
		 * 
		 * @see http://docs.oracle.com/javase/jndi/tutorial/ldap/referral/jndi.html
		 * @see http://java.sun.com/products/jndi/jndi-ldap-gl.html
		 */
		if (!"".equals(Config.PRINCIPAL_LDAP_REFERRAL)) {
			env.put(Context.REFERRAL, Config.PRINCIPAL_LDAP_REFERRAL);
		}
		
		// Optional is some cases (Max OS/X)
		if (!Config.PRINCIPAL_LDAP_SECURITY_PRINCIPAL.equals("")) {
			env.put(Context.SECURITY_PRINCIPAL, Config.PRINCIPAL_LDAP_SECURITY_PRINCIPAL);
		}
		
		if (!Config.PRINCIPAL_LDAP_SECURITY_CREDENTIALS.equals("")) {
			env.put(Context.SECURITY_CREDENTIALS, Config.PRINCIPAL_LDAP_SECURITY_CREDENTIALS);
		}
		
		return env;
	}
}
