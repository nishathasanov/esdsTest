/**
 * OpenKM, Open Document Management System (http://www.openkm.com)
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

package com.openkm.module.db.base;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.automation.AutomationException;
import com.openkm.bean.Document;
import com.openkm.bean.Folder;
import com.openkm.bean.Mail;
import com.openkm.bean.Note;
import com.openkm.bean.Permission;
import com.openkm.core.AccessDeniedException;
import com.openkm.core.Config;
import com.openkm.core.DatabaseException;
import com.openkm.core.ItemExistsException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.RepositoryException;
import com.openkm.core.UserQuotaExceededException;
import com.openkm.dao.NodeBaseDAO;
import com.openkm.dao.NodeDocumentDAO;
import com.openkm.dao.NodeFolderDAO;
import com.openkm.dao.NodeMailDAO;
import com.openkm.dao.NodeNoteDAO;
import com.openkm.dao.bean.NodeDocument;
import com.openkm.dao.bean.NodeFolder;
import com.openkm.dao.bean.NodeMail;
import com.openkm.dao.bean.NodeNote;
import com.openkm.module.db.stuff.DbAccessManager;
import com.openkm.module.db.stuff.SecurityHelper;
import com.openkm.util.CloneUtils;

public class BaseMailModule {
	private static Logger log = LoggerFactory.getLogger(BaseMailModule.class);
	
	/**
	 * Create a new mail
	 */
	public static NodeMail create(String user, NodeFolder parentFolder, String name, long size, String from,
			String[] reply, String[] to, String[] cc, String[] bcc, Calendar sentDate, Calendar receivedDate,
			String subject, String content, String mimeType) throws PathNotFoundException, AccessDeniedException,
			ItemExistsException, DatabaseException {
		// Create and add a new folder node
		NodeMail mailNode = new NodeMail();
		mailNode.setUuid(UUID.randomUUID().toString());
		mailNode.setContext(parentFolder.getContext());
		mailNode.setParent(parentFolder.getUuid());
		mailNode.setAuthor(user);
		mailNode.setName(name);
		mailNode.setSize(size);
		mailNode.setFrom(from);
		mailNode.setReply(new HashSet<String>(Arrays.asList(reply)));
		mailNode.setTo(new HashSet<String>(Arrays.asList(to)));
		mailNode.setCc(new HashSet<String>(Arrays.asList(cc)));
		mailNode.setBcc(new HashSet<String>(Arrays.asList(bcc)));
		mailNode.setSentDate(sentDate);
		mailNode.setReceivedDate(receivedDate);
		mailNode.setSubject(subject);
		mailNode.setContent(content);
		mailNode.setMimeType(mimeType);
		mailNode.setCreated(Calendar.getInstance());
		
		// Get parent node auth info
		Map<String, Integer> userPerms = parentFolder.getUserPermissions();
		Map<String, Integer> rolePerms = parentFolder.getRolePermissions();
		
		// Always assign all grants to creator
		if (Config.USER_ASSIGN_DOCUMENT_CREATION) {
			userPerms.put(user, Permission.ALL_GRANTS);
		}
		
		// Set auth info
		// NOTICE: Pay attention to the need of cloning
		mailNode.setUserPermissions(CloneUtils.clone(userPerms));
		mailNode.setRolePermissions(CloneUtils.clone(rolePerms));
		
		NodeMailDAO.getInstance().create(mailNode);
		
		// if (Config.USER_ITEM_CACHE) {
			// Update user items size
			// UserItemsManager.incFolders(user, 1);
		// }
		
		return mailNode;
	}
	
	/**
	 * Get folder properties
	 */
	public static Mail getProperties(String user, NodeMail nMail) throws PathNotFoundException, DatabaseException {
		log.debug("getProperties({}, {})", user, nMail);
		Mail mail = new Mail();
		
		// Properties
		String mailPath = NodeBaseDAO.getInstance().getPathFromUuid(nMail.getUuid());
		mail.setPath(mailPath);
		mail.setCreated(nMail.getCreated());
		mail.setAuthor(nMail.getAuthor());
		mail.setUuid(nMail.getUuid());
		mail.setSize(nMail.getSize());
		mail.setFrom(nMail.getFrom());
		mail.setReply(nMail.getReply().toArray(new String[nMail.getReply().size()]));
		mail.setTo(nMail.getTo().toArray(new String[nMail.getTo().size()]));
		mail.setCc(nMail.getCc().toArray(new String[nMail.getCc().size()]));
		mail.setBcc(nMail.getBcc().toArray(new String[nMail.getBcc().size()]));
		mail.setSentDate(nMail.getSentDate());
		mail.setReceivedDate(nMail.getReceivedDate());
		mail.setSubject(nMail.getSubject());
		mail.setContent(nMail.getContent());
		mail.setMimeType(nMail.getMimeType());
		
		// Get attachments
		ArrayList<Document> attachments = new ArrayList<Document>();
		
		for (NodeDocument nDocument : NodeDocumentDAO.getInstance().findByParent(nMail.getUuid())) {
			attachments.add(BaseDocumentModule.getProperties(user, nDocument));
		}
		
		mail.setAttachments(attachments);
		
		// Get permissions
		if (Config.SYSTEM_READONLY) {
			mail.setPermissions(Permission.NONE);
		} else {
			DbAccessManager am = SecurityHelper.getAccessManager();
			
			if (am.isGranted(nMail, Permission.READ)) {
				mail.setPermissions(Permission.READ);
			}
			
			if (am.isGranted(nMail, Permission.WRITE)) {
				mail.setPermissions(mail.getPermissions() | Permission.WRITE);
			}
			
			if (am.isGranted(nMail, Permission.DELETE)) {
				mail.setPermissions(mail.getPermissions() | Permission.DELETE);
			}
			
			if (am.isGranted(nMail, Permission.SECURITY)) {
				mail.setPermissions(mail.getPermissions() | Permission.SECURITY);
			}
		}
		
		// Get user subscription & keywords
		// mail.setSubscriptors(nMail.getSubscriptors());
		// mail.setSubscribed(nMail.getSubscriptors().contains(user));
		mail.setKeywords(nMail.getKeywords());
		
		// Get categories
		Set<Folder> categories = new HashSet<Folder>();
		NodeFolderDAO nFldDao = NodeFolderDAO.getInstance();
		Set<NodeFolder> resolvedCategories = nFldDao.resolveCategories(nMail.getCategories());
		
		for (NodeFolder nfldCat : resolvedCategories) {
			categories.add(BaseFolderModule.getProperties(user, nfldCat));
		}
		
		mail.setCategories(categories);
		
		// Get notes
		List<Note> notes = new ArrayList<Note>();
		List<NodeNote> nNoteList = NodeNoteDAO.getInstance().findByParent(nMail.getUuid());
		
		for (NodeNote nNote : nNoteList) {
			notes.add(BaseNoteModule.getProperties(nNote, mailPath + "/" + nNote.getUuid()));
		}
		
		mail.setNotes(notes);
		
		log.debug("getProperties: {}", mail);
		return mail;
	}
	
	/**
	 * Is invoked from DbDocumentNode and DbFolderNode.
	 */
	public static NodeMail copy(String user, NodeMail srcMailNode, NodeFolder dstFldNode) throws
			ItemExistsException, UserQuotaExceededException, PathNotFoundException, AccessDeniedException,
			AutomationException, DatabaseException, IOException {
		log.debug("copy({}, {}, {}, {})", new Object[] { user, srcMailNode, dstFldNode });
		NodeMail newMail = null;
		
		try {
			String[] reply = (String[]) srcMailNode.getReply().toArray(new String[srcMailNode.getReply().size()]);
			String[] to = (String[]) srcMailNode.getTo().toArray(new String[srcMailNode.getTo().size()]);
			String[] cc = (String[]) srcMailNode.getCc().toArray(new String[srcMailNode.getCc().size()]);
			String[] bcc = (String[]) srcMailNode.getBcc().toArray(new String[srcMailNode.getBcc().size()]);
			
			newMail = create(user, dstFldNode, srcMailNode.getName(), srcMailNode.getSize(), srcMailNode.getFrom(),
					reply, to, cc, bcc, srcMailNode.getSentDate(), srcMailNode.getReceivedDate(),
					srcMailNode.getSubject(), srcMailNode.getContent(), srcMailNode.getMimeType());
			
			// Add attachments
			for (NodeDocument nDocument : NodeDocumentDAO.getInstance().findByParent(srcMailNode.getUuid())) {
				String newPath = NodeBaseDAO.getInstance().getPathFromUuid(newMail.getUuid());
				BaseDocumentModule.copy(user, nDocument, newPath, newMail, nDocument.getName());
			}
		} finally {
		}
		
		log.debug("copy: {}", newMail);
		return newMail;
	}
	
	/**
	 * Check recursively if the mail contains locked nodes
	 */
	public static boolean hasLockedNodes(String mailUuid) throws PathNotFoundException, DatabaseException,
			RepositoryException {
		boolean hasLock = false;
		
		for (NodeDocument nDoc : NodeDocumentDAO.getInstance().findByParent(mailUuid)) {
			hasLock |= nDoc.isLocked();
		}
		
		return hasLock;
	}
	
	/**
	 * Check if a node has removable childs TODO: Is this necessary? The access manager should prevent this and make the
	 * core thrown an exception.
	 */
	public static boolean hasWriteAccess(String mailUuid) throws PathNotFoundException, DatabaseException,
			RepositoryException {
		log.debug("hasWriteAccess({})", mailUuid);
		DbAccessManager am = SecurityHelper.getAccessManager();
		boolean canWrite = true;
		
		for (NodeDocument nDoc : NodeDocumentDAO.getInstance().findByParent(mailUuid)) {
			canWrite &= am.isGranted(nDoc, Permission.WRITE);
		}
		
		log.debug("hasWriteAccess: {}", canWrite);
		return canWrite;
	}
}
