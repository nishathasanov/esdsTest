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

package com.openkm.module.jcr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.XASession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.bean.Document;
import com.openkm.bean.LockInfo;
import com.openkm.bean.Repository;
import com.openkm.bean.Version;
import com.openkm.cache.UserItemsManager;
import com.openkm.core.AccessDeniedException;
import com.openkm.core.Config;
import com.openkm.core.DatabaseException;
import com.openkm.core.FileSizeExceededException;
import com.openkm.core.ItemExistsException;
import com.openkm.core.LockException;
import com.openkm.core.MimeTypeConfig;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.Ref;
import com.openkm.core.RepositoryException;
import com.openkm.core.UnsupportedMimeTypeException;
import com.openkm.core.UserQuotaExceededException;
import com.openkm.core.VersionException;
import com.openkm.core.VirusDetectedException;
import com.openkm.core.VirusDetection;
import com.openkm.dao.LockTokenDAO;
import com.openkm.dao.MimeTypeDAO;
import com.openkm.extension.core.DocumentExtensionManager;
import com.openkm.extension.core.ExtensionException;
import com.openkm.extractor.RegisteredExtractors;
import com.openkm.jaas.PrincipalUtils;
import com.openkm.module.DocumentModule;
import com.openkm.module.common.CommonGeneralModule;
import com.openkm.module.jcr.base.BaseDocumentModule;
import com.openkm.module.jcr.base.BaseNoteModule;
import com.openkm.module.jcr.base.BaseNotificationModule;
import com.openkm.module.jcr.base.BaseScriptingModule;
import com.openkm.module.jcr.stuff.JCRUtils;
import com.openkm.module.jcr.stuff.JcrSessionManager;
import com.openkm.principal.PrincipalAdapterException;
import com.openkm.util.FileUtils;
import com.openkm.util.FormatUtil;
import com.openkm.util.PathUtils;
import com.openkm.util.Transaction;
import com.openkm.util.UserActivity;

public class JcrDocumentModule implements DocumentModule {
	private static Logger log = LoggerFactory.getLogger(JcrDocumentModule.class);
	
	@Override
	public Document create(String token, Document doc, InputStream is) throws UnsupportedMimeTypeException, 
			FileSizeExceededException, UserQuotaExceededException, VirusDetectedException, 
			ItemExistsException, PathNotFoundException, AccessDeniedException, 
			RepositoryException, IOException, DatabaseException, ExtensionException {
		log.debug("create({}, {}, {})", new Object[] { token, doc, is });
		return create(token, doc, is, null);
	}
	
	/**
	 * Used when importing mail with attachments
	 */
	public Document create(String token, Document doc, InputStream is, String userId) throws 
			UnsupportedMimeTypeException, FileSizeExceededException, UserQuotaExceededException,
			VirusDetectedException, ItemExistsException, PathNotFoundException, AccessDeniedException, 
			RepositoryException, IOException, DatabaseException, ExtensionException {
		log.debug("create({}, {}, {}, {})", new Object[] { token, doc, is, userId });
		Document newDocument = null;
		Node parentNode = null;
		Session session = null;
		int size = is.available();
		
		if (Config.SYSTEM_READONLY) {
			throw new AccessDeniedException("System is in read-only mode");
		}
		
		String parent = PathUtils.getParent(doc.getPath());
		String name = PathUtils.getName(doc.getPath());
		
		// Add to KEA - must have the same extension
		int idx = name.lastIndexOf('.');
		String fileExtension = idx>0 ? name.substring(idx) : ".tmp";
		File tmp = File.createTempFile("okm", fileExtension);
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			if (Config.MAX_FILE_SIZE > 0 && size > Config.MAX_FILE_SIZE) {
				log.error("Uploaded file size: {} ({}), Max file size: {} ({})", new Object[] {
						FormatUtil.formatSize(size), size, FormatUtil.formatSize(Config.MAX_FILE_SIZE),
						Config.MAX_FILE_SIZE });
				String usr = userId == null ? session.getUserID() : userId;
				UserActivity.log(usr, "ERROR_FILE_SIZE_EXCEEDED", null, doc.getPath(), Integer.toString(size));
				throw new FileSizeExceededException(Integer.toString(size));
			}
			
			// Escape dangerous chars in name
			name = PathUtils.escape(name);
			doc.setPath(parent + "/" + name);
			parentNode = session.getRootNode().getNode(parent.substring(1));
			
			// Check file restrictions
			String mimeType = MimeTypeConfig.mimeTypes.getContentType(name.toLowerCase());
			doc.setMimeType(mimeType);
			
			if (Config.RESTRICT_FILE_MIME && MimeTypeDAO.findByName(mimeType) == null) {
				String usr = userId == null ? session.getUserID() : userId;
				UserActivity.log(usr, "ERROR_UNSUPPORTED_MIME_TYPE", null, doc.getPath(), mimeType);
				throw new UnsupportedMimeTypeException(mimeType);
			}
			
			// Manage temporary files
			byte[] buff = new byte[4*1024];
			FileOutputStream fos = new FileOutputStream(tmp);
			int read;
			
			while ((read = is.read(buff)) != -1) {
				fos.write(buff, 0, read);
			}
			
			fos.flush();
			fos.close();
			is.close();
			is = new FileInputStream(tmp);
			
			if (!Config.SYSTEM_ANTIVIR.equals("")) {
				String info = VirusDetection.detect(tmp);
				
				if (info != null) {
					String usr = userId == null ? session.getUserID() : userId;
					UserActivity.log(usr, "ERROR_VIRUS_DETECTED", null, doc.getPath(), info);
					throw new VirusDetectedException(info);
				}
			}
			
			Collection<String> keywords = doc.getKeywords() != null ? doc.getKeywords() : new ArrayList<String>();
	        
	        // EP - PRE
			Ref<Node> refParentNode = new Ref<Node>(parentNode);
			Ref<File> refTmp = new Ref<File>(tmp);
			Ref<Document> refDoc = new Ref<Document>(doc);
			DocumentExtensionManager.getInstance().preCreate(session, refParentNode, refTmp, refDoc);
			parentNode = refParentNode.get();
			name = PathUtils.escape(PathUtils.getName(refDoc.get().getPath()));
			mimeType = refDoc.get().getMimeType();
			keywords = refDoc.get().getKeywords();
			
			Node documentNode = BaseDocumentModule.create(session, parentNode, name, doc.getTitle(),
					mimeType, keywords.toArray(new String[keywords.size()]), is);
			
			// EP - POST
			Ref<Node> refDocumentNode = new Ref<Node>(documentNode);
			DocumentExtensionManager.getInstance().postCreate(session, refParentNode, refDocumentNode);
			
			// Check document filters
			// DocumentUtils.checkFilters(session, documentNode, mimeType);
			
			// Set returned document properties
			newDocument = BaseDocumentModule.getProperties(session, documentNode);
			
			if (userId == null) {
				// Check subscriptions
				BaseNotificationModule.checkSubscriptions(documentNode, session.getUserID(), "CREATE_DOCUMENT", null);
				
				// Check scripting
				BaseScriptingModule.checkScripts(session, parentNode, documentNode, "CREATE_DOCUMENT");
				
				// Activity log
				UserActivity.log(session.getUserID(), "CREATE_DOCUMENT", documentNode.getUUID(), doc.getPath(), mimeType + ", " + size);
			} else {
				// Check subscriptions
				BaseNotificationModule.checkSubscriptions(documentNode, userId, "CREATE_MAIL_ATTACHMENT", null);
				
				// Check scripting
				BaseScriptingModule.checkScripts(session, parentNode, documentNode, "CREATE_MAIL_ATTACHMENT");
				
				// Activity log
				UserActivity.log(userId, "CREATE_MAIL_ATTACHMENT", documentNode.getUUID(), doc.getPath(), mimeType + ", " + size);
			}
		} catch (javax.jcr.ItemExistsException e) {
			log.warn(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(parentNode);
			throw new ItemExistsException(e.getMessage(), e);
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(parentNode);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.AccessDeniedException e) {
			log.warn(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(parentNode);
			throw new AccessDeniedException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(parentNode);
			throw new RepositoryException(e.getMessage(), e);
		} catch (java.io.IOException e) {
			log.error(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(parentNode);
			throw e;
		} catch (VirusDetectedException e) {
			JCRUtils.discardsPendingChanges(parentNode);
			throw e;
		} catch (DatabaseException e) {
			JCRUtils.discardsPendingChanges(parentNode);
			throw e;
		} catch (ExtensionException e) {
			JCRUtils.discardsPendingChanges(parentNode);
			throw e;
		} finally {
			IOUtils.closeQuietly(is);
			org.apache.commons.io.FileUtils.deleteQuietly(tmp);
			if (token == null) JCRUtils.logout(session);
		}

		log.debug("create: {}", newDocument);
		return newDocument;
	}

	@Override
	public void delete(String token, String docPath) throws AccessDeniedException, RepositoryException,
			PathNotFoundException, LockException, DatabaseException, ExtensionException {
		log.debug("delete({}, {})", token, docPath);
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
			
			String name = PathUtils.getName(docPath);
			Node documentNode = session.getRootNode().getNode(docPath.substring(1));
			Node parentNode = documentNode.getParent();
			Node userTrash = session.getRootNode().getNode(Repository.TRASH+"/"+session.getUserID());
			
			// PRE
			Ref<Node> refDocumentNode = new Ref<Node>(documentNode);
			DocumentExtensionManager.getInstance().preDelete(session, refDocumentNode);

			if (documentNode.isLocked()) {
				throw new LockException("Can't delete a locked node");
			}

			// Test if already exists a document whith the same name in the trash
			String destPath = userTrash.getPath() + "/";
			String testName = name;
			String fileName = FileUtils.getFileName(name);
			String fileExtension = FileUtils.getFileExtension(name);

			for (int i=1; session.itemExists(destPath + testName); i++) {
				testName = fileName + " (" + i + ")." + fileExtension;
			}

			session.move(documentNode.getPath(), destPath + testName);
			session.getRootNode().save();
			
			// POST
			DocumentExtensionManager.getInstance().postDelete(session, docPath);

			// Check scripting
			BaseScriptingModule.checkScripts(session, parentNode, documentNode, "DELETE_DOCUMENT");

			// Activity log
			UserActivity.log(session.getUserID(), "DELETE_DOCUMENT", documentNode.getUUID(), docPath, null);
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(session);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.AccessDeniedException e) {
			log.warn(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(session);
			throw new AccessDeniedException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(session);
			throw new RepositoryException(e.getMessage(), e);
		} catch (ExtensionException e) {
			log.error(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(session);
			throw new ExtensionException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}

		log.debug("delete: void");
	}

	@Override
	public Document getProperties(String token, String docPath) throws RepositoryException, 
			PathNotFoundException, DatabaseException {
		log.debug("getProperties({}, {})", token, docPath);
		Document doc = null;
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			Node documentNode = session.getRootNode().getNode(docPath.substring(1));
			doc = BaseDocumentModule.getProperties(session, documentNode);

			// Activity log
			UserActivity.log(session.getUserID(), "GET_DOCUMENT_PROPERTIES", documentNode.getUUID(), docPath, doc.getKeywords().toString());
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}

		log.debug("getProperties: {}", doc);
		return doc;
	}

	@Override
	public InputStream getContent(String token, String docPath, boolean checkout) throws 
			PathNotFoundException, RepositoryException, IOException, DatabaseException {
		log.debug("getContent({}, {}, {})", new Object[] { token, docPath, checkout });
		InputStream is = null;
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			is = BaseDocumentModule.getContent(session, docPath, checkout);
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			throw new RepositoryException(e.getMessage(), e);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			throw e;
		} finally {
			if (token == null) JCRUtils.logout(session);
		}

		log.debug("getContent: {}", is);
		return is;
	}

	@Override
	public InputStream getContentByVersion(String token, String docPath, String versionId) throws 
			RepositoryException, PathNotFoundException, IOException, DatabaseException {
		log.debug("getContentByVersion({}, {}, {})", new Object[] { token, docPath, versionId });
		InputStream is;
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			Node documentNode = session.getRootNode().getNode(docPath.substring(1));
			Node contentNode = documentNode.getNode(Document.CONTENT);
			VersionHistory vh = contentNode.getVersionHistory();
			javax.jcr.version.Version ver = vh.getVersion(versionId);
			Node frozenNode = ver.getNode(JcrConstants.JCR_FROZENNODE);
			is = frozenNode.getProperty(JcrConstants.JCR_DATA).getStream();

			// Activity log
			UserActivity.log(session.getUserID(), "GET_DOCUMENT_CONTENT_BY_VERSION", documentNode.getUUID(), docPath, versionId+", "+is.available());
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}

		log.debug("getContentByVersion: {}", is);
		return is;
	}
	
	@Override
	@Deprecated
	public List<Document> getChilds(String token, String fldPath) throws PathNotFoundException,
			RepositoryException, DatabaseException {
		return getChildren(token, fldPath);
	}
	
	@Override
	public List<Document> getChildren(String token, String fldPath) throws PathNotFoundException, RepositoryException,
			DatabaseException {
		log.debug("getChildren({}, {})", token, fldPath);
		List<Document> children = new ArrayList<Document>();
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			Node folderNode = session.getRootNode().getNode(fldPath.substring(1));

			for (NodeIterator ni = folderNode.getNodes(); ni.hasNext(); ) {
				Node child = ni.nextNode();
				log.debug("Child: " + child.getPath() + ", " + child.getPrimaryNodeType().getName());

				if (child.isNodeType(Document.TYPE)) {
					children.add(BaseDocumentModule.getProperties(session, child));
				}
			}

			// Activity log
			UserActivity.log(session.getUserID(), "GET_CHILDREN_DOCUMENTS", folderNode.getUUID(), fldPath, null);
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}

		log.debug("getChildren: {}", children);
		return children;
	}

	@Override
	public Document rename(String token, String docPath, String newName) throws AccessDeniedException,
			RepositoryException, PathNotFoundException, ItemExistsException, DatabaseException, ExtensionException {
		log.debug("rename:({}, {}, {})", new Object[] { token, docPath, newName });
		Document renamedDocument = null;
		Session session = null;
		Node documentNode = null;
		
		if (Config.SYSTEM_READONLY) {
			throw new AccessDeniedException("System is in read-only mode");
		}

		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			String parent = PathUtils.getParent(docPath);
			String name = PathUtils.getName(docPath);
			
			// Escape dangerous chars in name
			newName = PathUtils.escape(newName);

			if (newName != null && !newName.equals("") && !newName.equals(name)) {
				String newPath = parent + "/" + newName;
				
				// PRE
				Ref<Node> refDocumentNode = new Ref<Node>(documentNode);
				DocumentExtensionManager.getInstance().preRename(session, docPath, newPath, refDocumentNode);
				
				session.move(docPath, newPath);

				// Set new name
				documentNode = session.getRootNode().getNode(newPath.substring(1));
				documentNode.setProperty(Document.NAME, newName);

				// Publish changes
				session.save();

				// Set returned document properties
				renamedDocument = BaseDocumentModule.getProperties(session, documentNode);
				
				// POST
				DocumentExtensionManager.getInstance().postRename(session, docPath, newPath, refDocumentNode);
			} else {
				// Don't change anything
				documentNode = session.getRootNode().getNode(docPath.substring(1));
				renamedDocument = BaseDocumentModule.getProperties(session, documentNode);				
			}
			
			// Activity log
			UserActivity.log(session.getUserID(), "RENAME_DOCUMENT", documentNode.getUUID(), docPath, newName);
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(session);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.ItemExistsException e) {
			log.warn(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(session);
			throw new ItemExistsException(e.getMessage(), e);
		} catch (javax.jcr.AccessDeniedException e) {
			log.warn(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(session);
			throw new AccessDeniedException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(session);
			throw new RepositoryException(e.getMessage(), e);
		} catch (ExtensionException e) {
			log.error(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(session);
			throw new ExtensionException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}

		log.debug("rename: {}", renamedDocument);
		return renamedDocument;
	}

	@Override
	public void setProperties(String token, Document doc) throws VersionException, LockException,
			PathNotFoundException, AccessDeniedException, RepositoryException, DatabaseException {
		log.debug("setProperties({}, {})", token, doc);
		Node documentNode = null;
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
			
			documentNode = session.getRootNode().getNode(doc.getPath().substring(1));
			
			// Update document keyword cache
			//UserKeywordsManager.put(session.getUserID(), documentNode.getUUID(), doc.getKeywords());
			
			// Update document title
			// documentNode.setProperty(Document.TITLE, doc.getTitle() == null ? "" : doc.getTitle());

			// Check subscriptions
			BaseNotificationModule.checkSubscriptions(documentNode, session.getUserID(), "SET_DOCUMENT_PROPERTIES", null);

			// Check scripting
			BaseScriptingModule.checkScripts(session, documentNode, documentNode, "SET_DOCUMENT_PROPERTIES");

			// Activity log
			UserActivity.log(session.getUserID(), "SET_DOCUMENT_PROPERTIES", documentNode.getUUID(), doc.getPath(), null);
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(documentNode);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.AccessDeniedException e) {
			log.warn(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(documentNode);
			throw new AccessDeniedException(e.getMessage(), e);
		} catch (javax.jcr.version.VersionException e) {
			log.error(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(documentNode);
			throw new VersionException(e.getMessage(), e);
		} catch (javax.jcr.lock.LockException e) {
			log.error(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(documentNode);
			throw new LockException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(documentNode);
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}

		log.debug("setProperties: void");
	}

	@Override
	public void checkout(String token, String docPath) throws AccessDeniedException, RepositoryException, 
			PathNotFoundException, LockException, DatabaseException {
		checkout(token, docPath, null);
	}
	
	/**
	 * Used in Zoho extension
	 */
	public void checkout(String token, String docPath, String userId) throws AccessDeniedException, RepositoryException, 
			PathNotFoundException, LockException, DatabaseException {
		log.debug("checkout({}, {}, {})", new Object[] { token, docPath, userId });
		Transaction t = null;
		XASession session = null;
		
		if (Config.SYSTEM_READONLY) {
			throw new AccessDeniedException("System is in read-only mode");
		}

		try {
			if (token == null) {
				session = (XASession) JCRUtils.getSession();
			} else {
				session = (XASession) JcrSessionManager.getInstance().get(token);
			}
			
			javax.jcr.lock.Lock lck = null;

			t = new Transaction(session);
			t.start();

			Node documentNode = session.getRootNode().getNode(docPath.substring(1));
			Node contentNode = documentNode.getNode(Document.CONTENT);
			lck = documentNode.lock(true, false);
			JCRUtils.addLockToken(session, documentNode);
			contentNode.checkout();

			t.end();
			t.commit();

			// Check scripting
			BaseScriptingModule.checkScripts(session, documentNode, documentNode, "CHECKOUT_DOCUMENT");

			// Activity log
			UserActivity.log(session.getUserID(), "CHECKOUT_DOCUMENT", documentNode.getUUID(), docPath, lck.getLockToken());
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			t.rollback();
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.lock.LockException e) {
			log.error(e.getMessage(), e);
			t.rollback();
			throw new LockException(e.getMessage(), e);
		} catch (javax.jcr.AccessDeniedException e) {
			log.warn(e.getMessage(), e);
			t.rollback();
			throw new AccessDeniedException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			t.rollback();
			throw new RepositoryException(e.getMessage(), e);
		} catch (DatabaseException e) {
			log.error(e.getMessage(), e);
			t.rollback();
			throw e;
		} finally {
			if (token == null) JCRUtils.logout(session);
		}

		log.debug("checkout: void");
	}

	@Override
	public void cancelCheckout(String token, String docPath) throws AccessDeniedException,
			RepositoryException, PathNotFoundException, LockException, DatabaseException {
		log.debug("cancelCheckout({}, {})", token, docPath);
		Transaction t = null;
		XASession session = null;
		
		if (Config.SYSTEM_READONLY) {
			throw new AccessDeniedException("System is in read-only mode");
		}

		try {
			if (token == null) {
				session = (XASession) JCRUtils.getSession();
			} else {
				session = (XASession) JcrSessionManager.getInstance().get(token);
			}
			
			t = new Transaction(session);
			t.start();
			
			JCRUtils.loadLockTokens(session);
			Node documentNode = session.getRootNode().getNode(docPath.substring(1));
			Node contentNode = documentNode.getNode(Document.CONTENT);
			contentNode.restore(contentNode.getBaseVersion(), true);
			documentNode.unlock();
			JCRUtils.removeLockToken(session, documentNode);
			
			t.end();
			t.commit();

			// Check subscriptions
			BaseNotificationModule.checkSubscriptions(documentNode, session.getUserID(), "CANCEL_DOCUMENT_CHECKOUT", null);

			// Check scripting
			BaseScriptingModule.checkScripts(session, documentNode, documentNode, "CANCEL_DOCUMENT_CHECKOUT");

			// Activity log
			UserActivity.log(session.getUserID(), "CANCEL_DOCUMENT_CHECKOUT", documentNode.getUUID(), docPath, null);
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			t.rollback();
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.lock.LockException e) {
			log.error(e.getMessage(), e);
			t.rollback();
			throw new LockException(e.getMessage(), e);
		} catch (javax.jcr.AccessDeniedException e) {
			log.warn(e.getMessage(), e);
			t.rollback();
			throw new AccessDeniedException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			t.rollback();
			throw new RepositoryException(e.getMessage(), e);
		} catch (DatabaseException e) {
			log.error(e.getMessage(), e);
			t.rollback();
			throw e;
		} finally {
			if (token == null) JCRUtils.logout(session);
		}

		log.debug("cancelCheckout: void");
	}
	
	@Override
	public void forceCancelCheckout(String token, String docPath) throws AccessDeniedException,
			RepositoryException, PathNotFoundException, LockException, DatabaseException,
			PrincipalAdapterException {
		log.debug("forceCancelCheckout({}, {})", token, docPath);
		Transaction t = null;
		XASession session = null;
		
		if (Config.SYSTEM_READONLY) {
			throw new AccessDeniedException("System is in read-only mode");
		}

		try {
			if (token == null) {
				session = (XASession) JCRUtils.getSession();
			} else {
				session = (XASession) JcrSessionManager.getInstance().get(token);
			}
			
			if (!PrincipalUtils.hasRole(Config.DEFAULT_ADMIN_ROLE)) {
				throw new AccessDeniedException("Only administrator use allowed");
			}
			
			t = new Transaction(session);
			t.start();
			
			JCRUtils.loadLockTokens(session);
			Node documentNode = session.getRootNode().getNode(docPath.substring(1));
			Node contentNode = documentNode.getNode(Document.CONTENT);
			javax.jcr.lock.Lock lock = documentNode.getLock();
			
			if (lock.getLockOwner().equals(session.getUserID())) {
				contentNode.restore(contentNode.getBaseVersion(), true);
				documentNode.unlock();
				JCRUtils.removeLockToken(session, documentNode);
			} else {
				String lt = JCRUtils.getLockToken(documentNode.getUUID());
				session.addLockToken(lt);
				contentNode.restore(contentNode.getBaseVersion(), true);
				documentNode.unlock();
				LockTokenDAO.delete(lock.getLockOwner(), lt);
			}
			
			t.end();
			t.commit();

			// Check subscriptions
			BaseNotificationModule.checkSubscriptions(documentNode, session.getUserID(), "FORCE_CANCEL_DOCUMENT_CHECKOUT", null);

			// Check scripting
			BaseScriptingModule.checkScripts(session, documentNode, documentNode, "FORCE_CANCEL_DOCUMENT_CHECKOUT");

			// Activity log
			UserActivity.log(session.getUserID(), "FORCE_CANCEL_DOCUMENT_CHECKOUT", documentNode.getUUID(), docPath, null);
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			t.rollback();
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.lock.LockException e) {
			log.error(e.getMessage(), e);
			t.rollback();
			throw new LockException(e.getMessage(), e);
		} catch (javax.jcr.AccessDeniedException e) {
			log.warn(e.getMessage(), e);
			t.rollback();
			throw new AccessDeniedException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			t.rollback();
			throw new RepositoryException(e.getMessage(), e);
		} catch (DatabaseException e) {
			log.error(e.getMessage(), e);
			t.rollback();
			throw e;
		} finally {
			if (token == null) JCRUtils.logout(session);
		}

		log.debug("forceCancelCheckout: void");
	}

	@Override
	public boolean isCheckedOut(String token, String docPath) throws RepositoryException,
			PathNotFoundException, DatabaseException {
		log.debug("isCheckedOut({}, {})", token, docPath);
		boolean checkedOut = false;
		Session session = null;

		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			Node documentNode = session.getRootNode().getNode(docPath.substring(1));
			Node contentNode = documentNode.getNode(Document.CONTENT);
			checkedOut = contentNode.isCheckedOut();
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}

		log.debug("isCheckedOut: {}", checkedOut);
		return checkedOut;
	}
	
	@Override
	public Version checkin(String token, String docPath, InputStream is, String comment) throws
			FileSizeExceededException, UserQuotaExceededException, VirusDetectedException,
			AccessDeniedException, RepositoryException, PathNotFoundException, LockException,
			VersionException, IOException, DatabaseException, ExtensionException {
		return checkin(token, docPath, is, comment, null);
	}
	
	/**
	 * Used in Zoho extension
	 */
	public Version checkin(String token, String docPath, InputStream is, String comment, String userId) throws 
			FileSizeExceededException, UserQuotaExceededException, VirusDetectedException, AccessDeniedException,
			RepositoryException, PathNotFoundException, LockException, VersionException, IOException,
			DatabaseException, ExtensionException {
		log.debug("checkin({}, {}, {}, {}, {})", new Object[] { token, docPath, is, comment, userId });		
		Version version = new Version();
		Transaction t = null;
		XASession session = null;
		int size = is.available();
		
		if (Config.SYSTEM_READONLY) {
			throw new AccessDeniedException("System is in read-only mode");
		}
		
		String name = PathUtils.getName(docPath);
		int idx = name.lastIndexOf('.');
		String fileExtension = idx>0 ? name.substring(idx) : ".tmp";
		File tmp = File.createTempFile("okm", fileExtension);

		try {
			if (token == null) {
				session = (XASession) JCRUtils.getSession();
			} else {
				session = (XASession) JcrSessionManager.getInstance().get(token);
			}
			
			if (userId == null) {
				userId = session.getUserID();
			}
						
			if (Config.MAX_FILE_SIZE > 0 && size > Config.MAX_FILE_SIZE) {
				log.error("Uploaded file size: {} ({}), Max file size: {} ({})", new Object[] {
						FormatUtil.formatSize(size), size, FormatUtil.formatSize(Config.MAX_FILE_SIZE),
						Config.MAX_FILE_SIZE });
				UserActivity.log(userId, "ERROR_FILE_SIZE_EXCEEDED", null, docPath, Integer.toString(size));
				throw new FileSizeExceededException(Integer.toString(size));
			}
			
			// Manage temporary files
			byte[] buff = new byte[4*1024];
			FileOutputStream fos = new FileOutputStream(tmp);
			int read;
			
			while ((read = is.read(buff)) != -1) {
				fos.write(buff, 0, read);
			}
			
			fos.flush();
			fos.close();
			is.close();
			is = new FileInputStream(tmp);
			
			if (!Config.SYSTEM_ANTIVIR.equals("")) {
				String info = VirusDetection.detect(tmp);
				
				if (info != null) {
					UserActivity.log(userId, "ERROR_VIRUS_DETECTED", null, docPath, info);
					throw new VirusDetectedException(info);
				}
			}
			
			t = new Transaction(session);
			t.start();
			
			Node documentNode = session.getRootNode().getNode(docPath.substring(1));
			Node contentNode = null;
			
			synchronized (documentNode) {
				// EP - PRE
				Ref<Node> refDocumentNode = new Ref<Node>(documentNode);
				DocumentExtensionManager.getInstance().preCheckin(session, refDocumentNode);
				
				JCRUtils.loadLockTokens(session);
				contentNode = documentNode.getNode(Document.CONTENT);
				
				// Set data
				contentNode.setProperty(Document.SIZE, size);
				contentNode.setProperty(JcrConstants.JCR_DATA, is);
				
				if (Config.MANAGED_TEXT_EXTRACTION) {
					String mimeType = contentNode.getProperty(JcrConstants.JCR_MIMETYPE).getString();
					RegisteredExtractors.index(documentNode, contentNode, mimeType);
				}
				
				// Set version author
				contentNode.setProperty(Document.AUTHOR, userId);
				contentNode.setProperty(Document.VERSION_COMMENT, comment);
				contentNode.setProperty(JcrConstants.JCR_LASTMODIFIED, Calendar.getInstance());
				contentNode.save();
				
				// Performs checkin & unlock
				javax.jcr.version.Version ver = contentNode.checkin();
				version.setAuthor(contentNode.getProperty(Document.AUTHOR).getString());
				version.setSize(contentNode.getProperty(Document.SIZE).getLong());
				version.setComment(contentNode.getProperty(Document.VERSION_COMMENT).getString());
				version.setName(ver.getName());
				version.setCreated(ver.getCreated());
				version.setActual(true);
				documentNode.unlock();
				JCRUtils.removeLockToken(session, documentNode);
				
				// EP - POST
				Ref<Version> refVersion = new Ref<Version>(version);
				DocumentExtensionManager.getInstance().postCheckin(session, refDocumentNode, refVersion);
				version = refVersion.get();
				
				// Add comment (as system user)
				String text = "New version " + version.getName() + " by " + userId + ": " + comment;
				Session sysSession = JcrSessionManager.getInstance().getSystemSession();
				BaseNoteModule.add(sysSession, documentNode, text);
			}
			
			t.end();
			t.commit();
			
			// Update user items
			if (Config.USER_ITEM_CACHE) {
				UserItemsManager.incSize(session.getUserID(), size);
			}
			
			// Remove pdf & preview from cache
			CommonGeneralModule.cleanPreviewCache(documentNode.getUUID());

			// Check subscriptions
			BaseNotificationModule.checkSubscriptions(documentNode, userId, "CHECKIN_DOCUMENT", comment);
			
			// Check scripting
			BaseScriptingModule.checkScripts(session, documentNode, documentNode, "CHECKIN_DOCUMENT");

			// Activity log
			UserActivity.log(userId, "CHECKIN_DOCUMENT", documentNode.getUUID(), docPath, size + ", " + comment);
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			t.rollback();
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.lock.LockException e) {
			log.error(e.getMessage(), e);
			t.rollback();
			throw new LockException(e.getMessage(), e);
		} catch (javax.jcr.AccessDeniedException e) {
			log.warn(e.getMessage(), e);
			t.rollback();
			throw new AccessDeniedException(e.getMessage(), e);
		} catch (javax.jcr.version.VersionException e) {
			log.error(e.getMessage(), e);
			t.rollback();
			throw new VersionException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			t.rollback();
			throw new RepositoryException(e.getMessage(), e);
		} catch (DatabaseException e) {
			log.error(e.getMessage(), e);
			t.rollback();
			throw e;
		} catch (ExtensionException e) {
			log.error(e.getMessage(), e);
			t.rollback();
			throw e;
		} finally {
			IOUtils.closeQuietly(is);
			org.apache.commons.io.FileUtils.deleteQuietly(tmp);
			if (token == null) JCRUtils.logout(session);
		}

		log.debug("checkin: {}", version);
		return version;
	}

	@Override
	public List<Version> getVersionHistory(String token, String docPath) throws PathNotFoundException,
			RepositoryException, DatabaseException {
		log.debug("getVersionHistory({}, {})", token, docPath);
		List<Version> history = new ArrayList<Version>();
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			Node documentNode = session.getRootNode().getNode(docPath.substring(1));
			Node contentNode = documentNode.getNode(Document.CONTENT);
			VersionHistory vh = contentNode.getVersionHistory();
			String baseVersion = contentNode.getBaseVersion().getName();

			for (VersionIterator vi = vh.getAllVersions(); vi.hasNext(); ) {
				javax.jcr.version.Version ver = vi.nextVersion();
				String versionName = ver.getName();

				// The rootVersion is not a "real" version node.
				if (!versionName.equals(JcrConstants.JCR_ROOTVERSION)) {
					Version version = new Version();
					Node frozenNode = ver.getNode(JcrConstants.JCR_FROZENNODE);
					version.setAuthor(frozenNode.getProperty(Document.AUTHOR).getString());
					version.setSize(frozenNode.getProperty(Document.SIZE).getLong());
					version.setComment(frozenNode.getProperty(Document.VERSION_COMMENT).getString());
					version.setName(ver.getName());
					version.setCreated(ver.getCreated());

					if (versionName.equals(baseVersion)) {
						version.setActual(true);
					} else {
						version.setActual(false);
					}

					history.add(version);
				}
			}
			
			// Reverse history
			Collections.reverse(history);

			// Activity log
			UserActivity.log(session.getUserID(), "GET_DOCUMENT_VERSION_HISTORY", documentNode.getUUID(), docPath, null);
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}

		log.debug("getVersionHistory: {}", history);
		return history;
	}

	@Override
	public LockInfo lock(String token, String docPath) throws AccessDeniedException, RepositoryException, 
			PathNotFoundException, LockException, DatabaseException {
		log.debug("lock({})", docPath);
		Session session = null;
		LockInfo lock = new LockInfo();
		
		if (Config.SYSTEM_READONLY) {
			throw new AccessDeniedException("System is in read-only mode");
		}

		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			Node documentNode = session.getRootNode().getNode(docPath.substring(1));
			javax.jcr.lock.Lock lck = documentNode.lock(true, false);
			lock.setOwner(lck.getLockOwner());
			lock.setNodePath(lck.getNode().getPath());
			lock.setToken(lck.getLockToken());
			JCRUtils.addLockToken(session, documentNode);
			
			// Check subscriptions
			BaseNotificationModule.checkSubscriptions(documentNode, session.getUserID(), "LOCK_DOCUMENT", null);

			// Check scripting
			BaseScriptingModule.checkScripts(session, documentNode, documentNode, "LOCK_DOCUMENT");

			// Activity log
			UserActivity.log(session.getUserID(), "LOCK_DOCUMENT", documentNode.getUUID(), docPath, lck.getLockToken());
		} catch (javax.jcr.lock.LockException e) {
			log.error(e.getMessage(), e);
			throw new LockException(e.getMessage(), e);
		} catch (javax.jcr.AccessDeniedException e) {
			log.warn(e.getMessage(), e);
			throw new AccessDeniedException(e.getMessage(), e);
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			throw new RepositoryException(e.getMessage(), e);
		} catch (DatabaseException e) {
			log.error(e.getMessage(), e);
			throw e;
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("lock: {}", lock);
		return lock;
	}

	@Override
	public void unlock(String token, String docPath) throws AccessDeniedException, RepositoryException, 
			PathNotFoundException, LockException, DatabaseException {
		log.debug("unlock({}, {})", token, docPath);
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
			
			JCRUtils.loadLockTokens(session);
			Node documentNode = session.getRootNode().getNode(docPath.substring(1));
			documentNode.unlock();
			JCRUtils.removeLockToken(session, documentNode);

			// Check subscriptions
			BaseNotificationModule.checkSubscriptions(documentNode, session.getUserID(), "UNLOCK_DOCUMENT", null);
			
			// Check scripting
			BaseScriptingModule.checkScripts(session, documentNode, documentNode, "UNLOCK_DOCUMENT");

			// Activity log
			UserActivity.log(session.getUserID(), "UNLOCK_DOCUMENT", documentNode.getUUID(), docPath, null);
		} catch (javax.jcr.lock.LockException e) {
			log.error(e.getMessage(), e);
			throw new LockException(e.getMessage(), e);
		} catch (javax.jcr.AccessDeniedException e) {
			log.warn(e.getMessage(), e);
			throw new AccessDeniedException(e.getMessage(), e);
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			throw new RepositoryException(e.getMessage(), e);
		} catch (DatabaseException e) {
			log.error(e.getMessage(), e);
			throw e;
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("unlock: void");
	}
	
	@Override
	public void forceUnlock(String token, String docPath) throws LockException, PathNotFoundException,
			AccessDeniedException, RepositoryException, DatabaseException, PrincipalAdapterException {
		log.debug("forceUnlock({}, {})", token, docPath);
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
			
			if (!PrincipalUtils.hasRole(Config.DEFAULT_ADMIN_ROLE)) {
				throw new AccessDeniedException("Only administrator use allowed");
			}
			
			Node documentNode = session.getRootNode().getNode(docPath.substring(1));
			javax.jcr.lock.Lock lock = documentNode.getLock();
			
			if (lock.getLockOwner().equals(Config.SYSTEM_USER)) {
				Session sysSession = JcrSessionManager.getInstance().getSystemSession();
				Node sysDocumentNode = sysSession.getRootNode().getNode(docPath.substring(1));
				sysDocumentNode.unlock();
				LockTokenDAO.delete(lock.getLockOwner(), lock.getLockToken());
			} else if (lock.getLockOwner().equals(session.getUserID())) {
				JCRUtils.loadLockTokens(session);
				documentNode.unlock();
				JCRUtils.removeLockToken(session, documentNode);
			} else {
				String lt = JCRUtils.getLockToken(documentNode.getUUID());
				session.addLockToken(lt);
				documentNode.unlock();
				LockTokenDAO.delete(lock.getLockOwner(), lt);
			}

			// Check subscriptions
			BaseNotificationModule.checkSubscriptions(documentNode, session.getUserID(), "FORCE_UNLOCK", null);
			
			// Check scripting
			BaseScriptingModule.checkScripts(session, documentNode, documentNode, "FORCE_UNLOCK_DOCUMENT");

			// Activity log
			UserActivity.log(session.getUserID(), "FORCE_UNLOCK_DOCUMENT", documentNode.getUUID(), docPath, null);
		} catch (javax.jcr.lock.LockException e) {
			log.error(e.getMessage(), e);
			throw new LockException(e.getMessage(), e);
		} catch (javax.jcr.AccessDeniedException e) {
			log.warn(e.getMessage(), e);
			throw new AccessDeniedException(e.getMessage(), e);
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			throw new RepositoryException(e.getMessage(), e);
		} catch (DatabaseException e) {
			log.error(e.getMessage(), e);
			throw e;
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("forceUnlock: void");
	}

	@Override
	public boolean isLocked(String token, String docPath) throws RepositoryException, PathNotFoundException, 
			DatabaseException {
		log.debug("isLocked({}, {})", token, docPath);
		boolean locked;
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			Node documentNode = session.getRootNode().getNode(docPath.substring(1));
			locked = documentNode.isLocked();
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("isLocked: {}", locked);
		return locked;
	}

	@Override
	public LockInfo getLockInfo(String token, String docPath) throws RepositoryException, PathNotFoundException,
			LockException, DatabaseException {
		log.debug("getLockInfo({}, {})", token, docPath);
		LockInfo lock = new LockInfo();
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			lock = BaseDocumentModule.getLockInfo(session, docPath);
		} catch (javax.jcr.lock.LockException e) {
			log.error(e.getMessage(), e);
			throw new LockException(e.getMessage(), e);
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("getLock: {}", lock);
		return lock;
	}

	@Override
	public void purge(String token, String docPath) throws AccessDeniedException, RepositoryException, 
			PathNotFoundException, DatabaseException, ExtensionException {
		log.debug("purge({}, {})", token, docPath);
		Node parentNode = null;
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
			
			Node documentNode = session.getRootNode().getNode(docPath.substring(1));
			String docUuid = documentNode.getUUID();
			
			// PRE
			Ref<Node> refDocumentNode = new Ref<Node>(documentNode);
			DocumentExtensionManager.getInstance().prePurge(session, refDocumentNode);
			
			synchronized (documentNode) {
				parentNode = documentNode.getParent();
				BaseDocumentModule.purge(session, parentNode, documentNode);
			}
			
			// POST
			DocumentExtensionManager.getInstance().postPurge(session, docPath);

			// Check scripting
			BaseScriptingModule.checkScripts(session, parentNode, documentNode, "PURGE_DOCUMENT");
			
			// Activity log
			UserActivity.log(session.getUserID(), "PURGE_DOCUMENT", docUuid, docPath, null);
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(parentNode);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.AccessDeniedException e) {
			log.warn(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(parentNode);
			throw new AccessDeniedException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(parentNode);
			throw new RepositoryException(e.getMessage(), e);
		} catch (ExtensionException e) {
			log.error(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(parentNode);
			throw new ExtensionException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("purge: void");
	}

	@Override
	public void move(String token, String docPath, String dstPath) throws PathNotFoundException,
			ItemExistsException, AccessDeniedException, RepositoryException, DatabaseException,
			ExtensionException {
		log.debug("move({}, {}, {})", new Object[] { token, docPath, dstPath });
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
			
			String name = PathUtils.getName(docPath);
			String dstNodePath = dstPath + "/" + name;
			
			// EP - PRE
			Node rootNode = session.getRootNode();
			Node srcDocNode = rootNode.getNode(docPath.substring(1));
			Node dstFldPath = rootNode.getNode(dstPath.substring(1));
			Ref<Node> refSrcDocumentNode = new Ref<Node>(srcDocNode);
			Ref<Node> refDstFolderNode = new Ref<Node>(dstFldPath);
			DocumentExtensionManager.getInstance().preMove(session, refSrcDocumentNode, refDstFolderNode);
			docPath = refSrcDocumentNode.get().getPath();
			dstNodePath = refDstFolderNode.get().getPath() + "/" + name;
			
			session.move(docPath, dstNodePath);
			session.save();
			
			// EP - POST
			String srcDocParent = PathUtils.getParent(docPath);
			Node srcFldNode = rootNode.getNode(srcDocParent.substring(1));
			Node dstDocNode = rootNode.getNode(dstNodePath.substring(1));
			Ref<Node> refSrcFolderNode = new Ref<Node>(srcFldNode);
			Ref<Node> refDstDocumentNode = new Ref<Node>(dstDocNode);
			DocumentExtensionManager.getInstance().postMove(session, docPath, refSrcFolderNode, refDstDocumentNode);
			
			// Check scripting
			BaseScriptingModule.checkScripts(session, dstDocNode.getParent(), dstDocNode, "MOVE_DOCUMENT");

			// Activity log
			UserActivity.log(session.getUserID(), "MOVE_DOCUMENT", dstDocNode.getUUID(), docPath, dstPath);
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(session);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.ItemExistsException e) {
			log.warn(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(session);
			throw new ItemExistsException(e.getMessage(), e);
		} catch (javax.jcr.AccessDeniedException e) {
			log.warn(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(session);
			throw new AccessDeniedException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(session);
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}

		log.debug("move: void");
	}

	@Override
	public void copy(String token, String docPath, String dstPath) throws ItemExistsException,
			PathNotFoundException, AccessDeniedException, RepositoryException, IOException, DatabaseException, 
			UserQuotaExceededException, ExtensionException {
		log.debug("copy({}, {}, {})", new Object[]  { token, docPath, dstPath });
		Node dstFolderNode = null;
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
			
			Node rootNode = session.getRootNode();
			Node srcDocumentNode = rootNode.getNode(docPath.substring(1));
			dstFolderNode = rootNode.getNode(dstPath.substring(1));
			
			// PRE
			Ref<Node> refSrcNode = new Ref<Node>(srcDocumentNode);
			Ref<Node> refDstFolderNode = new Ref<Node>(dstFolderNode);
			DocumentExtensionManager.getInstance().preCopy(session, refSrcNode, refDstFolderNode);
			
			Node newDocument = BaseDocumentModule.copy(session, srcDocumentNode, dstFolderNode);
			
			// POST
			Ref<Node> refNewDocument = new Ref<Node>(newDocument);
			DocumentExtensionManager.getInstance().postCopy(session, refSrcNode, refNewDocument, refDstFolderNode);
			
			// Check subscriptions
			BaseNotificationModule.checkSubscriptions(dstFolderNode, session.getUserID(), "COPY_DOCUMENT", null);

			// Activity log
			UserActivity.log(session.getUserID(), "COPY_DOCUMENT", newDocument.getUUID(), docPath, dstPath);
		} catch (javax.jcr.ItemExistsException e) {
			log.warn(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(dstFolderNode);
			throw new ItemExistsException(e.getMessage(), e);
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(dstFolderNode);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.AccessDeniedException e) {
			log.warn(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(dstFolderNode);
			throw new AccessDeniedException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(dstFolderNode);
			throw new RepositoryException(e.getMessage(), e);
		} catch (java.io.IOException e) {
			log.error(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(dstFolderNode);
			throw e;
		} catch (ExtensionException e) {
			log.error(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(dstFolderNode);
			throw e;
		} finally {
			if (token == null) JCRUtils.logout(session);
		}

		log.debug("copy: void");
	}

	@Override
	public void restoreVersion(String token, String docPath, String versionId) throws AccessDeniedException,
			RepositoryException, PathNotFoundException, DatabaseException, ExtensionException {
		log.debug("restoreVersion({}, {}, {})", new Object[] { token, docPath, versionId });
		Node contentNode = null;
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
			
			Node documentNode = session.getRootNode().getNode(docPath.substring(1));
			
			// PRE
			Ref<Node> refDocumentNode = new Ref<Node>(documentNode);
			DocumentExtensionManager.getInstance().preRestoreVersion(session, refDocumentNode);

			synchronized (documentNode) {
				contentNode = documentNode.getNode(Document.CONTENT);
				contentNode.restore(versionId, true);
				contentNode.save();
			}
			
			// Remove pdf & preview from cache
			CommonGeneralModule.cleanPreviewCache(documentNode.getUUID());
			
			// POST
			DocumentExtensionManager.getInstance().postRestoreVersion(session, refDocumentNode);
			
			// Activity log
			UserActivity.log(session.getUserID(), "RESTORE_DOCUMENT_VERSION", documentNode.getUUID(), docPath, versionId);
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(contentNode);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.AccessDeniedException e) {
			log.warn(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(contentNode);
			throw new AccessDeniedException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(contentNode);
			throw new RepositoryException(e.getMessage(), e);
		} catch (ExtensionException e) {
			log.error(e.getMessage(), e);
			JCRUtils.discardsPendingChanges(contentNode);
			throw new ExtensionException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("restoreVersion: void");
	}

	@Override
	public void purgeVersionHistory(String token, String docPath) throws AccessDeniedException,
			RepositoryException, PathNotFoundException, DatabaseException {
		log.debug("purgeVersionHistory({}, {})", token, docPath);
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
			
			Node documentNode = session.getRootNode().getNode(docPath.substring(1));
			
			synchronized (documentNode) {
				Node contentNode = documentNode.getNode(Document.CONTENT);
				VersionHistory vh = contentNode.getVersionHistory();
				String baseVersion = contentNode.getBaseVersion().getName();

				for (VersionIterator vi = vh.getAllVersions(); vi.hasNext(); ) {
					javax.jcr.version.Version ver = vi.nextVersion();
					String versionName = ver.getName();

					// The rootVersion is not a "real" version node.
					if (!versionName.equals(JcrConstants.JCR_ROOTVERSION) && !versionName.equals(baseVersion)) {
						vh.removeVersion(versionName);
					}
				}				
			}

			// Activity log
			UserActivity.log(session.getUserID(), "PURGE_DOCUMENT_VERSION_HISTORY", documentNode.getUUID(), docPath, null);
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}
		
		log.debug("purgeVersionHistory: void");
	}

	@Override
	public long getVersionHistorySize(String token, String docPath) throws RepositoryException,
			PathNotFoundException, DatabaseException {
		log.debug("getVersionHistorySize({}, {})", token, docPath);
		long ret = 0;
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			Node documentNode = session.getRootNode().getNode(docPath.substring(1));
			Node contentNode = documentNode.getNode(Document.CONTENT);
			VersionHistory vh = contentNode.getVersionHistory();

			for (VersionIterator vi = vh.getAllVersions(); vi.hasNext(); ) {
				javax.jcr.version.Version ver = vi.nextVersion();
				String versionName = ver.getName();

				// The rootVersion is not a "real" version node.
				if (!versionName.equals(JcrConstants.JCR_ROOTVERSION)) {
					Node frozenNode = ver.getNode(JcrConstants.JCR_FROZENNODE);
					ret += frozenNode.getProperty(Document.SIZE).getLong();
				}
			}
		} catch (javax.jcr.PathNotFoundException e) {
			log.warn(e.getMessage(), e);
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}

		log.debug("getVersionHistorySize: {}", ret);
		return ret;
	}

	@Override
	public boolean isValid(String token, String docPath) throws PathNotFoundException, RepositoryException, DatabaseException {
		log.debug("isValid({}, {})", token, docPath);
		boolean valid = false;
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			Node node = session.getRootNode().getNode(docPath.substring(1));

			if (node.isNodeType(Document.TYPE)) {
				valid = true;
			}
		} catch (javax.jcr.PathNotFoundException e) {
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.AccessDeniedException e) {
			throw new PathNotFoundException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}

		log.debug("isValid: {}", valid);
		return valid;
	}

	@Override
	public String getPath(String token, String uuid) throws AccessDeniedException, RepositoryException,
			DatabaseException {
		log.debug("getPath({}, {})", token, uuid);
		String path = null;
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			Node node = session.getNodeByUUID(uuid);

			if (node.isNodeType(Document.TYPE)) {
				path = node.getPath();
			}
		} catch (javax.jcr.AccessDeniedException e) {
			log.warn(e.getMessage(), e);
			throw new AccessDeniedException(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null) JCRUtils.logout(session);
		}

		log.debug("getPath: {}", path);
		return path;
	}
}
