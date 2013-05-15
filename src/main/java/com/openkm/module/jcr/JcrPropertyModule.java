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

package com.openkm.module.jcr;

import javax.jcr.Node;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.cache.UserNodeKeywordsManager;
import com.openkm.core.AccessDeniedException;
import com.openkm.core.Config;
import com.openkm.core.DatabaseException;
import com.openkm.core.LockException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.RepositoryException;
import com.openkm.core.VersionException;
import com.openkm.module.PropertyModule;
import com.openkm.module.jcr.base.BaseNotificationModule;
import com.openkm.module.jcr.base.BasePropertyModule;
import com.openkm.module.jcr.base.BaseScriptingModule;
import com.openkm.module.jcr.stuff.JCRUtils;
import com.openkm.module.jcr.stuff.JcrSessionManager;
import com.openkm.util.UserActivity;

public class JcrPropertyModule implements PropertyModule {
	private static Logger log = LoggerFactory.getLogger(JcrPropertyModule.class);

	@Override
	public void addCategory(String token, String nodePath, String catId) throws VersionException,
			LockException, PathNotFoundException, AccessDeniedException, RepositoryException,
			DatabaseException {
		log.debug("addCategory({}, {}, {})", new Object[] { token, nodePath, catId });
		Node node = null;
		Session session = null;
		
		if (Config.SYSTEM_READONLY) {
			throw new AccessDeniedException("System is in read-only mode");
		}

		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			node = session.getRootNode().getNode(nodePath.substring(1));
			BasePropertyModule.addCategory(session, node, catId);
			
			// Check subscriptions
			BaseNotificationModule.checkSubscriptions(node, session.getUserID(), "ADD_CATEGORY", null);

			// Check scripting
			BaseScriptingModule.checkScripts(session, node, node, "ADD_CATEGORY");

			// Activity log
			UserActivity.log(session.getUserID(), "ADD_CATEGORY", node.getUUID(), nodePath, catId);
		} catch (javax.jcr.PathNotFoundException e) {
			JCRUtils.discardsPendingChanges(node);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.AccessDeniedException e) {
			JCRUtils.discardsPendingChanges(node);
			throw new AccessDeniedException(e.getMessage(), e);
		} catch (javax.jcr.version.VersionException e) {
			JCRUtils.discardsPendingChanges(node);
			throw new VersionException(e.getMessage(), e);
		} catch (javax.jcr.lock.LockException e) {
			JCRUtils.discardsPendingChanges(node);
			throw new LockException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			JCRUtils.discardsPendingChanges(node);
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}

		log.debug("addCategory: void");
	}

	@Override
	public void removeCategory(String token, String nodePath, String catId) throws VersionException,
			LockException, PathNotFoundException, AccessDeniedException, RepositoryException,
			DatabaseException {
		log.debug("removeCategory({}, {}, {})", new Object[] { token, nodePath, catId });
		Node node = null;
		Session session = null;
		
		if (Config.SYSTEM_READONLY) {
			throw new AccessDeniedException("System is in read-only mode");
		}

		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			node = session.getRootNode().getNode(nodePath.substring(1));
			BasePropertyModule.removeCategory(session, node, catId);
			
			// Check subscriptions
			BaseNotificationModule.checkSubscriptions(node, session.getUserID(), "REMOVE_CATEGORY", null);

			// Check scripting
			BaseScriptingModule.checkScripts(session, node, node, "REMOVE_CATEGORY");

			// Activity log
			UserActivity.log(session.getUserID(), "REMOVE_CATEGORY", node.getUUID(), nodePath, catId);
		} catch (javax.jcr.PathNotFoundException e) {
			JCRUtils.discardsPendingChanges(node);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.AccessDeniedException e) {
			JCRUtils.discardsPendingChanges(node);
			throw new AccessDeniedException(e.getMessage(), e);
		} catch (javax.jcr.version.VersionException e) {
			JCRUtils.discardsPendingChanges(node);
			throw new VersionException(e.getMessage(), e);
		} catch (javax.jcr.lock.LockException e) {
			JCRUtils.discardsPendingChanges(node);
			throw new LockException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			JCRUtils.discardsPendingChanges(node);
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}

		log.debug("removeCategory: void");
	}

	@Override
	public String addKeyword(String token, String nodePath, String keyword) throws VersionException,
			LockException, PathNotFoundException, AccessDeniedException, RepositoryException,
			DatabaseException {
		log.debug("addKeyword({}, {}, {})", new Object[] { token, nodePath, keyword });
		Node node = null;
		Session session = null;
		
		if (Config.SYSTEM_READONLY) {
			throw new AccessDeniedException("System is in read-only mode");
		}

		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			node = session.getRootNode().getNode(nodePath.substring(1));
			keyword = BasePropertyModule.addKeyword(session, node, keyword);
			
			// Update cache
			if (Config.USER_KEYWORDS_CACHE) {
				UserNodeKeywordsManager.add(session.getUserID(), node.getUUID(), keyword);
			}
			
			// Check subscriptions
			BaseNotificationModule.checkSubscriptions(node, session.getUserID(), "ADD_KEYWORD", null);
			
			// Check scripting
			BaseScriptingModule.checkScripts(session, node, node, "ADD_KEYWORD");
			
			// Activity log
			UserActivity.log(session.getUserID(), "ADD_KEYWORD", node.getUUID(), nodePath, keyword);
		} catch (javax.jcr.PathNotFoundException e) {
			JCRUtils.discardsPendingChanges(node);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.AccessDeniedException e) {
			JCRUtils.discardsPendingChanges(node);
			throw new AccessDeniedException(e.getMessage(), e);
		} catch (javax.jcr.version.VersionException e) {
			JCRUtils.discardsPendingChanges(node);
			throw new VersionException(e.getMessage(), e);
		} catch (javax.jcr.lock.LockException e) {
			JCRUtils.discardsPendingChanges(node);
			throw new LockException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			JCRUtils.discardsPendingChanges(node);
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}

		log.debug("addKeyword: {}", keyword);
		return keyword;
	}

	@Override
	public void removeKeyword(String token, String nodePath, String keyword) throws VersionException,
			LockException, PathNotFoundException, AccessDeniedException, RepositoryException,
			DatabaseException {
		log.debug("removeKeyword({}, {}, {})", new Object[] { token, nodePath, keyword });
		Node node = null;
		Session session = null;
		
		if (Config.SYSTEM_READONLY) {
			throw new AccessDeniedException("System is in read-only mode");
		}

		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			node = session.getRootNode().getNode(nodePath.substring(1));
			BasePropertyModule.removeKeyword(session, node, keyword);
			
			// Update cache
			if (Config.USER_KEYWORDS_CACHE) {
				UserNodeKeywordsManager.remove(session.getUserID(), node.getUUID(), keyword);
			}
			
			// Check subscriptions
			BaseNotificationModule.checkSubscriptions(node, session.getUserID(), "REMOVE_KEYWORD", null);

			// Check scripting
			BaseScriptingModule.checkScripts(session, node, node, "REMOVE_KEYWORD");

			// Activity log
			UserActivity.log(session.getUserID(), "REMOVE_KEYWORD", node.getUUID(), nodePath, keyword);
		} catch (javax.jcr.PathNotFoundException e) {
			JCRUtils.discardsPendingChanges(node);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.AccessDeniedException e) {
			JCRUtils.discardsPendingChanges(node);
			throw new AccessDeniedException(e.getMessage(), e);
		} catch (javax.jcr.version.VersionException e) {
			JCRUtils.discardsPendingChanges(node);
			throw new VersionException(e.getMessage(), e);
		} catch (javax.jcr.lock.LockException e) {
			JCRUtils.discardsPendingChanges(node);
			throw new LockException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			JCRUtils.discardsPendingChanges(node);
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}

		log.debug("removeKeyword: void");
	}
}
