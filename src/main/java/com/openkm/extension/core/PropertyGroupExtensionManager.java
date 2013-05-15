/**
 *  OpenKM, Open Document Management System (http://www.openkm.com)
 *  Copyright (c) 2006-2011  Paco Avila & Josep Llort
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

package com.openkm.extension.core;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.ServiceConfigurationError;

import javax.jcr.Node;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.bean.form.FormElement;
import com.openkm.core.AccessDeniedException;
import com.openkm.core.DatabaseException;
import com.openkm.core.LockException;
import com.openkm.core.NoSuchGroupException;
import com.openkm.core.NoSuchPropertyException;
import com.openkm.core.ParseException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.Ref;
import com.openkm.core.RepositoryException;

public class PropertyGroupExtensionManager {
	private static Logger log = LoggerFactory.getLogger(PropertyGroupExtensionManager.class);
	private static PropertyGroupExtensionManager service = null;
	
	private PropertyGroupExtensionManager() {}
	
	public static synchronized PropertyGroupExtensionManager getInstance() {
		if (service == null) {
			service = new PropertyGroupExtensionManager();
		}
		
		return service;
	}
	
	/**
	 * Handle PRE addGroup
	 */
	public void preAddGroup(Session session, Ref<Node> node, String grpName) throws NoSuchGroupException,
			LockException, PathNotFoundException, AccessDeniedException, RepositoryException,
			DatabaseException, ExtensionException {
		log.debug("preAddGroup({}, {}, {})", new Object[] { session, node, grpName });
		
		try {
			ExtensionManager em = ExtensionManager.getInstance();
			List<PropertyGroupExtension> col = em.getPlugins(PropertyGroupExtension.class);
			Collections.sort(col, new OrderComparator<PropertyGroupExtension>());
			
			for (PropertyGroupExtension ext : col) {
				log.debug("Extension class: {}", ext.getClass().getCanonicalName());
				ext.preAddGroup(session, node, grpName);
			}
		} catch (ServiceConfigurationError e) {
			log.error(e.getMessage(), e);
		}
	}
	
	/**
	 * Handle POST addGroup
	 */
	public void postAddGroup(Session session, Ref<Node> node, String grpName) throws NoSuchGroupException,
			LockException, PathNotFoundException, AccessDeniedException, RepositoryException,
			DatabaseException, ExtensionException {
		log.debug("postAddGroup({}, {}, {})", new Object[] { session, node, grpName });
		
		try {
			ExtensionManager em = ExtensionManager.getInstance();
			List<PropertyGroupExtension> col = em.getPlugins(PropertyGroupExtension.class);
			Collections.sort(col, new OrderComparator<PropertyGroupExtension>());
			
			for (PropertyGroupExtension ext : col) {
				log.debug("Extension class: {}", ext.getClass().getCanonicalName());
				ext.postAddGroup(session, node, grpName);
			}
		} catch (ServiceConfigurationError e) {
			log.error(e.getMessage(), e);
		}
	}
	
	/**
	 * Handle PRE removeGroup
	 */
	public void preRemoveGroup(Session session, Ref<Node> node, String grpName) throws NoSuchGroupException,
			LockException, PathNotFoundException, AccessDeniedException, RepositoryException,
			DatabaseException, ExtensionException {
		log.debug("preRemoveGroup({}, {}, {})", new Object[] { session, node, grpName });
		
		try {
			ExtensionManager em = ExtensionManager.getInstance();
			List<PropertyGroupExtension> col = em.getPlugins(PropertyGroupExtension.class);
			Collections.sort(col, new OrderComparator<PropertyGroupExtension>());
			
			for (PropertyGroupExtension ext : col) {
				log.debug("Extension class: {}", ext.getClass().getCanonicalName());
				ext.preRemoveGroup(session, node, grpName);
			}
		} catch (ServiceConfigurationError e) {
			log.error(e.getMessage(), e);
		}
	}
	
	/**
	 * Handle POST removeGroup
	 */
	public void postRemoveGroup(Session session, Ref<Node> node, String grpName) throws NoSuchGroupException,
			LockException, PathNotFoundException, AccessDeniedException, RepositoryException,
			DatabaseException, ExtensionException {
		log.debug("postRemoveGroup({}, {}, {})", new Object[] { session, node, grpName });
		
		try {
			ExtensionManager em = ExtensionManager.getInstance();
			List<PropertyGroupExtension> col = em.getPlugins(PropertyGroupExtension.class);
			Collections.sort(col, new OrderComparator<PropertyGroupExtension>());
			
			for (PropertyGroupExtension ext : col) {
				log.debug("Extension class: {}", ext.getClass().getCanonicalName());
				ext.postRemoveGroup(session, node, grpName);
			}
		} catch (ServiceConfigurationError e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Handle PRE setProperties
	 */
	public void preSetProperties(Session session, Ref<Node> node, String grpName, List<FormElement> properties)
			throws IOException, ParseException, NoSuchPropertyException, NoSuchGroupException, LockException,
			PathNotFoundException, AccessDeniedException, RepositoryException, DatabaseException, ExtensionException {
		log.debug("preSetProperties({}, {}, {}, {})", new Object[] { session, node, grpName, properties });
		
		try {
			ExtensionManager em = ExtensionManager.getInstance();
			List<PropertyGroupExtension> col = em.getPlugins(PropertyGroupExtension.class);
			Collections.sort(col, new OrderComparator<PropertyGroupExtension>());
			
			for (PropertyGroupExtension ext : col) {
				log.debug("Extension class: {}", ext.getClass().getCanonicalName());
				ext.preSetProperties(session, node, grpName, properties);
			}
		} catch (ServiceConfigurationError e) {
			log.error(e.getMessage(), e);
		}
	}
	
	/**
	 * Handle POST setProperties
	 */
	public void postSetProperties(Session session, Ref<Node> node, String grpName, List<FormElement> properties)
			throws IOException, ParseException, NoSuchPropertyException, NoSuchGroupException, LockException,
			PathNotFoundException, AccessDeniedException, RepositoryException, DatabaseException, ExtensionException {
		log.debug("postSetProperties({}, {}, {}, {})", new Object[] { session, node, grpName, properties });
		
		try {
			ExtensionManager em = ExtensionManager.getInstance();
			List<PropertyGroupExtension> col = em.getPlugins(PropertyGroupExtension.class);
			Collections.sort(col, new OrderComparator<PropertyGroupExtension>());
			
			for (PropertyGroupExtension ext : col) {
				log.debug("Extension class: {}", ext.getClass().getCanonicalName());
				ext.postSetProperties(session, node, grpName, properties);
			}
		} catch (ServiceConfigurationError e) {
			log.error(e.getMessage(), e);
		}
	}
}
