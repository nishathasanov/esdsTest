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

package com.openkm.module.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import com.openkm.automation.AutomationException;
import com.openkm.bean.Mail;
import com.openkm.bean.Repository;
import com.openkm.core.AccessDeniedException;
import com.openkm.core.Config;
import com.openkm.core.DatabaseException;
import com.openkm.core.ItemExistsException;
import com.openkm.core.LockException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.RepositoryException;
import com.openkm.core.UserQuotaExceededException;
import com.openkm.core.VirusDetectedException;
import com.openkm.dao.NodeBaseDAO;
import com.openkm.dao.NodeFolderDAO;
import com.openkm.dao.NodeMailDAO;
import com.openkm.dao.bean.NodeFolder;
import com.openkm.dao.bean.NodeMail;
import com.openkm.module.MailModule;
import com.openkm.module.db.base.BaseMailModule;
import com.openkm.module.db.base.BaseNotificationModule;
import com.openkm.spring.PrincipalUtils;
import com.openkm.util.PathUtils;
import com.openkm.util.UserActivity;

public class DbMailModule implements MailModule {
	private static Logger log = LoggerFactory.getLogger(DbMailModule.class);

	@Override
	public Mail create(String token, Mail mail) throws PathNotFoundException, ItemExistsException,
			VirusDetectedException, AccessDeniedException, RepositoryException, DatabaseException,
			UserQuotaExceededException {
		log.debug("create({}, {})", token, mail);
		return create(token, mail, null);
	}
	
	/**
	 * Used when importing mail from scheduler
	 */
	public Mail create(String token, Mail mail, String userId) throws AccessDeniedException, RepositoryException,
			PathNotFoundException, ItemExistsException, VirusDetectedException, DatabaseException,
			UserQuotaExceededException {
		log.debug("create({}, {}, {})", new Object[] { token, mail, userId });
		Mail newMail = null;
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
			
			if (userId == null) {
				userId = auth.getName();
			}
			
			String parentPath = PathUtils.getParent(mail.getPath());
			String name = PathUtils.getName(mail.getPath());
			
			// Escape dangerous chars in name
			name = PathUtils.escape(name);
			
			if (!name.isEmpty()) {
				mail.setPath(parentPath + "/" + name);
				
				String parentUuid = NodeBaseDAO.getInstance().getUuidFromPath(parentPath);
				NodeFolder parentFolder = NodeFolderDAO.getInstance().findByPk(parentUuid);
				
				// Create node
				NodeMail mailNode = BaseMailModule.create(userId, parentFolder, name, mail.getSize(), 
						mail.getFrom(), mail.getReply(), mail.getTo(), mail.getCc(), mail.getBcc(),
						mail.getSentDate(), mail.getReceivedDate(), mail.getSubject(), mail.getContent(),
						mail.getMimeType());
				
				// Set returned mail properties
				newMail = BaseMailModule.getProperties(auth.getName(), mailNode);
				
				// Check subscriptions
				BaseNotificationModule.checkSubscriptions(mailNode, userId, "CREATE_MAIL", null);
				
				// Check scripting
				// BaseScriptingModule.checkScripts(session, parentNode, mailNode, "CREATE_MAIL");
				
				// Activity log
				UserActivity.log(userId, "CREATE_MAIL", mailNode.getUuid(), mail.getPath(), null);
			} else {
				throw new RepositoryException("Invalid mail name");
			}
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("create: {}", newMail);
		return newMail;
	}

	@Override
	public Mail getProperties(String token, String mailPath) throws PathNotFoundException, RepositoryException,
			DatabaseException {
		log.debug("getProperties({}, {})", token, mailPath);
		Mail mail = null;
		Authentication auth = null, oldAuth = null;

		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String mailUuid = NodeBaseDAO.getInstance().getUuidFromPath(mailPath);
			NodeMail mailNode = NodeMailDAO.getInstance().findByPk(mailUuid);
			mail = BaseMailModule.getProperties(auth.getName(), mailNode);
			
			// Activity log
			UserActivity.log(auth.getName(), "GET_MAIL_PROPERTIES", mailUuid, mailPath, null);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}

		log.debug("getProperties: {}", mail);
		return mail;
	}

	@Override
	public void delete(String token, String mailPath) throws LockException, PathNotFoundException,
			AccessDeniedException, RepositoryException, DatabaseException {
		log.debug("delete({}, {})", new Object[] { token, mailPath });
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
			
			String mailUuid = NodeBaseDAO.getInstance().getUuidFromPath(mailPath);
			
			if (BaseMailModule.hasLockedNodes(mailUuid)) {
				throw new LockException("Can't delete a mail with child locked attachments");
			}
			
			if (!BaseMailModule.hasWriteAccess(mailUuid)) {
				throw new AccessDeniedException("Can't delete a mail with readonly attachments");
			}
			
			String userTrashPath = "/" + Repository.TRASH + "/" + auth.getName();
			String userTrashUuid = NodeBaseDAO.getInstance().getUuidFromPath(userTrashPath);
			String name = PathUtils.getName(mailPath);
			
			NodeMailDAO.getInstance().delete(name, mailUuid, userTrashUuid);
			
			// Activity log
			UserActivity.log(auth.getName(), "DELETE_MAIL", mailUuid, mailPath, null);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}

		log.debug("delete: void");
	}

	@Override
	public void purge(String token, String mailPath) throws LockException, PathNotFoundException,
			AccessDeniedException, RepositoryException, DatabaseException {
		log.debug("purge({}, {})", token, mailPath);
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
			
			String mailUuid = NodeBaseDAO.getInstance().getUuidFromPath(mailPath);
			
			if (BaseMailModule.hasLockedNodes(mailUuid)) {
				throw new LockException("Can't delete a mail with child locked attachments");
			}
			
			if (!BaseMailModule.hasWriteAccess(mailUuid)) {
				throw new AccessDeniedException("Can't delete a mail with readonly attachments");
			}
			
			NodeMailDAO.getInstance().purge(mailUuid);
			
			// Activity log - Already inside DAO
			// UserActivity.log(auth.getName(), "PURGE_MAIL", mailUuid, mailPath, null);
		} catch (IOException e) {
			throw new RepositoryException(e.getMessage(), e);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}

		log.debug("purge: void");
	}

	@Override
	public Mail rename(String token, String mailPath, String newName) throws PathNotFoundException,
			ItemExistsException, AccessDeniedException, RepositoryException, DatabaseException {
		log.debug("rename({}, {}, {})", new Object[] { token, mailPath, newName });
		Mail renamedMail = null;
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
			
			String name = PathUtils.getName(mailPath);
			String mailUuid = NodeBaseDAO.getInstance().getUuidFromPath(mailPath);
			
			// Escape dangerous chars in name
			newName = PathUtils.escape(newName);
			
			if (newName != null && !newName.isEmpty() && !newName.equals(name)) {
				NodeMail mailNode = NodeMailDAO.getInstance().rename(mailUuid, newName);
				renamedMail = BaseMailModule.getProperties(auth.getName(), mailNode);
			} else {
				// Don't change anything
				NodeMail mailNode = NodeMailDAO.getInstance().findByPk(mailUuid);
				renamedMail = BaseMailModule.getProperties(auth.getName(), mailNode);
			}
			
			// Activity log
			UserActivity.log(auth.getName(), "RENAME_MAIL", mailUuid, mailPath, newName);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}

		log.debug("rename: {}", renamedMail);
		return renamedMail;
	}

	@Override
	public void move(String token, String mailPath, String dstPath) throws PathNotFoundException, ItemExistsException,
			AccessDeniedException, RepositoryException, DatabaseException {
		log.debug("move({}, {}, {})", new Object[] { token, mailPath, dstPath });
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
			
			String mailUuid = NodeBaseDAO.getInstance().getUuidFromPath(mailPath);
			String dstUuid = NodeBaseDAO.getInstance().getUuidFromPath(dstPath);
			NodeMailDAO.getInstance().move(mailUuid, dstUuid);
			
			// Activity log
			UserActivity.log(auth.getName(), "MOVE_MAIL", mailUuid, mailPath, dstPath);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}

		log.debug("move: void");
	}

	@Override
	public void copy(String token, String mailPath, String dstPath) throws PathNotFoundException, ItemExistsException,
			AccessDeniedException, RepositoryException, IOException, AutomationException, DatabaseException,
			UserQuotaExceededException {
		log.debug("copy({}, {}, {}, {})", new Object[] { token, mailPath, dstPath });
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
			
			String mailUuid = NodeBaseDAO.getInstance().getUuidFromPath(mailPath);
			String dstUuid = NodeBaseDAO.getInstance().getUuidFromPath(dstPath);
			NodeMail srcMailNode = NodeMailDAO.getInstance().findByPk(mailUuid);
			NodeFolder dstFldNode = NodeFolderDAO.getInstance().findByPk(dstUuid);
			NodeMail newMailNode = BaseMailModule.copy(auth.getName(), srcMailNode, dstFldNode);
			
			// Check subscriptions
			BaseNotificationModule.checkSubscriptions(dstFldNode, auth.getName(), "COPY_MAIL", null);
			
			// Activity log
			UserActivity.log(auth.getName(), "COPY_MAIL", newMailNode.getUuid(), mailPath, dstPath);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}

		log.debug("copy: void");
	}

	@Override
	@Deprecated
	public List<Mail> getChilds(String token, String fldPath) throws PathNotFoundException, RepositoryException,
			DatabaseException {
		return getChildren(token, fldPath);
	}
	
	@Override
	public List<Mail> getChildren(String token, String fldPath) throws PathNotFoundException, RepositoryException,
			DatabaseException {
		log.debug("getChildren({}, {})", token, fldPath);
		List<Mail> children = new ArrayList<Mail>();
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String fldUuid = NodeBaseDAO.getInstance().getUuidFromPath(fldPath);
			NodeFolder fldNode = NodeFolderDAO.getInstance().findByPk(fldUuid);
			
			for (NodeMail nMail : NodeMailDAO.getInstance().findByParent(fldNode.getUuid())) {
				children.add(BaseMailModule.getProperties(auth.getName(), nMail));
			}
			
			// Activity log
			UserActivity.log(auth.getName(), "GET_CHILDREN_MAILS", fldNode.getUuid(), fldPath, null);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getChildren: {}", children);
		return children;
	}

	@Override
	public boolean isValid(String token, String mailPath) throws PathNotFoundException, AccessDeniedException,
			RepositoryException, DatabaseException {
		log.debug("isValid({}, {})", token, mailPath);
		boolean valid = true;
		@SuppressWarnings("unused")
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String mailUuid = NodeBaseDAO.getInstance().getUuidFromPath(mailPath);
			
			try {
				NodeMailDAO.getInstance().findByPk(mailUuid);
			} catch (PathNotFoundException e) {
				valid = false;
			}
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}

		log.debug("isValid: {}", valid);
		return valid;
	}

	@Override
	public String getPath(String token, String uuid) throws AccessDeniedException, RepositoryException,
			DatabaseException {
		try {
			return NodeBaseDAO.getInstance().getPathFromUuid(uuid);
		} catch (PathNotFoundException e) {
			throw new RepositoryException(e.getMessage(), e);
		}
	}
}
