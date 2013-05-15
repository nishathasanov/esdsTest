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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.bean.Notification;
import com.openkm.core.AccessDeniedException;
import com.openkm.core.Config;
import com.openkm.core.DatabaseException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.RepositoryException;
import com.openkm.module.NotificationModule;
import com.openkm.module.common.CommonNotificationModule;
import com.openkm.module.jcr.stuff.JCRUtils;
import com.openkm.module.jcr.stuff.JcrSessionManager;
import com.openkm.util.UserActivity;

public class JcrNotificationModule implements NotificationModule {
	private static Logger log = LoggerFactory.getLogger(JcrNotificationModule.class);
	
	@Override
	public synchronized void subscribe(String token, String nodePath) throws PathNotFoundException,
			AccessDeniedException, RepositoryException, DatabaseException {
		log.debug("subscribe({}, {})", token, nodePath);
		Node node = null;
		Node sNode = null;
		Session session = null;
		Session systemSession = null;
		String lt = null;
		
		if (Config.SYSTEM_READONLY) {
			throw new AccessDeniedException("System is in read-only mode");
		}
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			systemSession = JcrRepositoryModule.getSystemSession();
			node = session.getRootNode().getNode(nodePath.substring(1));
			sNode = systemSession.getNodeByUUID(node.getUUID());
			lt = JCRUtils.getLockToken(node.getUUID());
			systemSession.addLockToken(lt);
			
			// Perform subscription
			if (node.isNodeType(Notification.TYPE)) {
				Value[] actualUsers = node.getProperty(Notification.SUBSCRIPTORS).getValues();
				String[] newUsers = new String[actualUsers.length + 1];
				boolean alreadyAdded = false;
				
				for (int i = 0; i < actualUsers.length; i++) {
					newUsers[i] = actualUsers[i].getString();
					
					// Don't add a user twice
					if (actualUsers[i].getString().equals(session.getUserID())) {
						alreadyAdded = true;
					}
				}
				
				if (!alreadyAdded) {
					newUsers[newUsers.length - 1] = session.getUserID();
					sNode.setProperty(Notification.SUBSCRIPTORS, newUsers);
				}
			} else {
				sNode.addMixin(Notification.TYPE);
				sNode.setProperty(Notification.SUBSCRIPTORS, new String[] { session.getUserID() });
			}
			
			sNode.save();
			
			// Activity log
			UserActivity.log(session.getUserID(), "SUBSCRIBE_USER", node.getUUID(), nodePath, null);
		} catch (javax.jcr.AccessDeniedException e) {
			log.warn(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(sNode);
			throw new AccessDeniedException(e.getMessage(), e);
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(sNode);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(sNode);
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (lt != null)
				systemSession.removeLockToken(lt);
			if (token == null)
				JCRUtils.logout(session);
		}
		
		log.debug("subscribe: void");
	}
	
	@Override
	public synchronized void unsubscribe(String token, String nodePath) throws PathNotFoundException,
			AccessDeniedException, RepositoryException, DatabaseException {
		log.debug("unsubscribe({}, {})", token, nodePath);
		Node node = null;
		Node sNode = null;
		Session session = null;
		Session systemSession = null;
		String lt = null;
		
		if (Config.SYSTEM_READONLY) {
			throw new AccessDeniedException("System is in read-only mode");
		}
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			systemSession = JcrRepositoryModule.getSystemSession();
			node = session.getRootNode().getNode(nodePath.substring(1));
			sNode = systemSession.getNodeByUUID(node.getUUID());
			lt = JCRUtils.getLockToken(node.getUUID());
			systemSession.addLockToken(lt);
			
			// Perform unsubscription
			if (node.isNodeType(Notification.TYPE)) {
				Value[] actualUsers = node.getProperty(Notification.SUBSCRIPTORS).getValues();
				ArrayList<String> newUsers = new ArrayList<String>();
				
				for (int i = 0; i < actualUsers.length; i++) {
					if (!actualUsers[i].getString().equals(session.getUserID())) {
						newUsers.add(actualUsers[i].getString());
					}
				}
				
				if (newUsers.isEmpty()) {
					sNode.removeMixin(Notification.TYPE);
				} else {
					sNode.setProperty(Notification.SUBSCRIPTORS,
							(String[]) newUsers.toArray(new String[newUsers.size()]));
				}
			}
			
			sNode.save();
			
			// Activity log
			UserActivity.log(session.getUserID(), "UNSUBSCRIBE_USER", node.getUUID(), nodePath, null);
		} catch (javax.jcr.AccessDeniedException e) {
			log.warn(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(sNode);
			throw new AccessDeniedException(e.getMessage(), e);
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(sNode);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(sNode);
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (lt != null)
				systemSession.removeLockToken(lt);
			if (token == null)
				JCRUtils.logout(session);
		}
		
		log.debug("unsubscribe: void");
	}
	
	@Override
	public Set<String> getSubscriptors(String token, String nodePath) throws PathNotFoundException,
			AccessDeniedException, RepositoryException, DatabaseException {
		log.debug("getSusbcriptions({}, {})", token, nodePath);
		Set<String> users = new HashSet<String>();
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			Node node = session.getRootNode().getNode(nodePath.substring(1));
			
			if (node.isNodeType(Notification.TYPE)) {
				Value[] notifyUsers = node.getProperty(Notification.SUBSCRIPTORS).getValues();
				
				for (int i = 0; i < notifyUsers.length; i++) {
					users.add(notifyUsers[i].getString());
				}
			}
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null)
				JCRUtils.logout(session);
		}
		
		log.debug("getSusbcriptions: {}", users);
		return users;
	}
	
	@Override
	public void notify(String token, String nodePath, List<String> users, String message, boolean attachment)
			throws PathNotFoundException, AccessDeniedException, RepositoryException {
		log.debug("notify({}, {}, {}, {})", new Object[] { token, nodePath, users, message });
		List<String> to = new ArrayList<String>();
		Session session = null;
		
		if (!users.isEmpty()) {
			try {
				log.debug("Nodo: {}, Message: {}", nodePath, message);
				
				// TODO This JCR Session could be removed
				if (token == null) {
					session = JCRUtils.getSession();
				} else {
					session = JcrSessionManager.getInstance().get(token);
				}
				
				for (String usr : users) {
					String mail = new JcrAuthModule().getMail(token, usr);
					
					if (mail != null) {
						to.add(mail);
					}
				}
				
				// Get session user email address && send notification
				String from = new JcrAuthModule().getMail(token, session.getUserID());
				
				if (!to.isEmpty() && from != null && !from.isEmpty()) {
					CommonNotificationModule.sendNotification(session.getUserID(), nodePath, from, to, message, attachment);
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (MessagingException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (token == null)
					JCRUtils.logout(session);
			}
		}
		
		log.debug("notify: void");
	}
}
