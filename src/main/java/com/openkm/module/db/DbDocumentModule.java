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

package com.openkm.module.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import com.openkm.automation.AutomationException;
import com.openkm.automation.AutomationManager;
import com.openkm.automation.AutomationUtils;
import com.openkm.bean.Document;
import com.openkm.bean.FileUploadResponse;
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
import com.openkm.core.WorkflowException;
import com.openkm.dao.MimeTypeDAO;
import com.openkm.dao.NodeBaseDAO;
import com.openkm.dao.NodeDocumentDAO;
import com.openkm.dao.NodeDocumentVersionDAO;
import com.openkm.dao.NodeFolderDAO;
import com.openkm.dao.bean.AutomationRule;
import com.openkm.dao.bean.NodeBase;
import com.openkm.dao.bean.NodeDocument;
import com.openkm.dao.bean.NodeDocumentVersion;
import com.openkm.dao.bean.NodeFolder;
import com.openkm.dao.bean.NodeLock;
import com.openkm.extension.core.ExtensionException;
import com.openkm.module.DocumentModule;
import com.openkm.module.common.CommonGeneralModule;
import com.openkm.module.db.base.BaseDocumentModule;
import com.openkm.module.db.base.BaseModule;
import com.openkm.module.db.base.BaseNoteModule;
import com.openkm.module.db.base.BaseNotificationModule;
import com.openkm.principal.PrincipalAdapterException;
import com.openkm.spring.PrincipalUtils;
import com.openkm.util.ConfigUtils;
import com.openkm.util.FormatUtil;
import com.openkm.util.PathUtils;
import com.openkm.util.UserActivity;

public class DbDocumentModule implements DocumentModule {
	private static Logger log = LoggerFactory.getLogger(DbDocumentModule.class);
	
	@Override
	public Document create(String token, Document doc, InputStream is) throws UnsupportedMimeTypeException,
			FileSizeExceededException, UserQuotaExceededException, VirusDetectedException, ItemExistsException,
			PathNotFoundException, AccessDeniedException, RepositoryException, IOException, DatabaseException,
			ExtensionException, AutomationException {
		log.debug("create({}, {}, {})", new Object[] { token, doc, is });
		return create(token, doc, is, is.available(), null);
	}
	
	/**
	 * Used when big files and WebDAV and GoogleDocs
	 */
	public Document create(String token, Document doc, InputStream is, long size, String userId)
			throws UnsupportedMimeTypeException, FileSizeExceededException, UserQuotaExceededException,
			VirusDetectedException, ItemExistsException, PathNotFoundException, AccessDeniedException,
			RepositoryException, IOException, DatabaseException, ExtensionException, AutomationException {
		log.debug("create({}, {}, {}, {}, {})", new Object[] { token, doc, is, size, userId });
		return create(token, doc, is, size, userId, new Ref<FileUploadResponse>(null));
	}
	
	/**
	 * Used when big files and FileUpload
	 */
	public Document create(String token, Document doc, InputStream is, long size, String userId,
			Ref<FileUploadResponse> fuResponse) throws UnsupportedMimeTypeException, FileSizeExceededException,
			UserQuotaExceededException, VirusDetectedException, ItemExistsException, PathNotFoundException,
			AccessDeniedException, RepositoryException, IOException, DatabaseException, ExtensionException,
			AutomationException {
		log.debug("create({}, {}, {}, {}, {}, {})", new Object[] { token, doc, is, size, userId, fuResponse });
		Document newDocument = null;
		Authentication auth = null, oldAuth = null;
		
		if (Config.SYSTEM_READONLY) {
			throw new AccessDeniedException("System is in read-only mode");
		}
		
		String parentPath = PathUtils.getParent(doc.getPath());
		String name = PathUtils.getName(doc.getPath());
		
		// Add to KEA - must have the same extension
		int idx = name.lastIndexOf('.');
		String fileExtension = idx > 0 ? name.substring(idx) : ".tmp";
		File tmp = File.createTempFile("okm", fileExtension);
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			if (Config.MAX_FILE_SIZE > 0 && size > Config.MAX_FILE_SIZE) {
				log.error("Uploaded file size: {} ({}), Max file size: {} ({})",
						new Object[] { FormatUtil.formatSize(size), size, FormatUtil.formatSize(Config.MAX_FILE_SIZE),
								Config.MAX_FILE_SIZE });
				String usr = userId == null ? auth.getName() : userId;
				UserActivity.log(usr, "ERROR_FILE_SIZE_EXCEEDED", null, doc.getPath(), Long.toString(size));
				throw new FileSizeExceededException(Long.toString(size));
			}
			
			// Escape dangerous chars in name
			name = PathUtils.escape(name);
			
			if (!name.isEmpty()) {
				doc.setPath(parentPath + "/" + name);
				
				// Check file restrictions
				String mimeType = MimeTypeConfig.mimeTypes.getContentType(name.toLowerCase());
				doc.setMimeType(mimeType);
				
				if (Config.RESTRICT_FILE_MIME && MimeTypeDAO.findByName(mimeType) == null) {
					String usr = userId == null ? auth.getName() : userId;
					UserActivity.log(usr, "ERROR_UNSUPPORTED_MIME_TYPE", null, doc.getPath(), mimeType);
					throw new UnsupportedMimeTypeException(mimeType);
				}
				
				// Restrict for extension
				if (!Config.RESTRICT_FILE_NAME.isEmpty()) {
					StringTokenizer st = new StringTokenizer(Config.RESTRICT_FILE_NAME, Config.LIST_SEPARATOR);
					
					while (st.hasMoreTokens()) {
						String wc = st.nextToken().trim();
						String re = ConfigUtils.wildcard2regexp(wc);
						
						if (Pattern.matches(re, name)) {
							String usr = userId == null ? auth.getName() : userId;
							UserActivity.log(usr, "ERROR_UNSUPPORTED_MIME_TYPE", null, doc.getPath(), mimeType);
							throw new UnsupportedMimeTypeException(mimeType);
						}
					}
				}
				
				// Manage temporary files
				byte[] buff = new byte[4 * 1024];
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
						String usr = userId == null ? auth.getName() : userId;
						UserActivity.log(usr, "ERROR_VIRUS_DETECTED", null, doc.getPath(), info);
						throw new VirusDetectedException(info);
					}
				}
				
				String parentUuid = NodeBaseDAO.getInstance().getUuidFromPath(parentPath);
				NodeBase parentNode = NodeBaseDAO.getInstance().findByPk(parentUuid);
				
				// AUTOMATION - PRE
				// INSIDE BaseDocumentModule.create
				
				// Create node
				Set<String> keywords = doc.getKeywords() != null ? doc.getKeywords() : new HashSet<String>();
				NodeDocument docNode = BaseDocumentModule.create(auth.getName(), parentPath, parentNode, name, 
						doc.getTitle(), doc.getCreated(), mimeType, is, size, keywords, new HashSet<String>(), fuResponse);
				
				// AUTOMATION - POST
				// INSIDE BaseDocumentModule.create
				
				// Set returned folder properties
				newDocument = BaseDocumentModule.getProperties(auth.getName(), docNode);
				
				// Setting wizard properties
				// INSIDE BaseDocumentModule.create
				
				if (fuResponse.get() == null) {
					fuResponse.set(new FileUploadResponse());
				}
				
				fuResponse.get().setHasAutomation(AutomationManager.getInstance().hasAutomation());
				
				if (userId == null) {
					// Check subscriptions
					BaseNotificationModule.checkSubscriptions(docNode, auth.getName(), "CREATE_DOCUMENT", null);
					
					// Check scripting
					// BaseScriptingModule.checkScripts(session, parentNode, documentNode, "CREATE_DOCUMENT");
					
					// Activity log
					UserActivity.log(auth.getName(), "CREATE_DOCUMENT", docNode.getUuid(), doc.getPath(), mimeType + ", " + size);
				} else {
					// Check subscriptions
					BaseNotificationModule.checkSubscriptions(docNode, userId, "CREATE_MAIL_ATTACHMENT", null);
					
					// Check scripting
					// BaseScriptingModule.checkScripts(session, parentNode, documentNode, "CREATE_MAIL_ATTACHMENT");
					
					// Activity log
					UserActivity.log(userId, "CREATE_MAIL_ATTACHMENT", docNode.getUuid(), doc.getPath(), mimeType + ", "
							+ size);
				}
			} else {
				throw new RepositoryException("Invalid document name");
			}
		} finally {
			IOUtils.closeQuietly(is);
			org.apache.commons.io.FileUtils.deleteQuietly(tmp);
			
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("create: {}", newDocument);
		return newDocument;
	}
	
	@Override
	public void delete(String token, String docPath) throws LockException, PathNotFoundException,
			AccessDeniedException, RepositoryException, DatabaseException {
		log.debug("delete({}, {})", new Object[] { token, docPath });
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
			
			String docUuid = NodeBaseDAO.getInstance().getUuidFromPath(docPath);
			
			if (BaseDocumentModule.hasWorkflowNodes(docUuid)) {
				throw new LockException("Can't delete a document used in a workflow");
			}
			
			String userTrashPath = "/" + Repository.TRASH + "/" + auth.getName();
			String userTrashUuid = NodeBaseDAO.getInstance().getUuidFromPath(userTrashPath);
			String name = PathUtils.getName(docPath);
			
			NodeDocumentDAO.getInstance().delete(name, docUuid, userTrashUuid);
			
			// Check subscriptions
			NodeDocument documentNode = NodeDocumentDAO.getInstance().findByPk(docUuid);
			BaseNotificationModule.checkSubscriptions(documentNode, PrincipalUtils.getUser(), "DELETE_DOCUMENT", null);
			
			// Activity log
			UserActivity.log(auth.getName(), "DELETE_DOCUMENT", docUuid, docPath, null);
		} catch (WorkflowException e) {
			throw new RepositoryException(e.getMessage());
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
	public Document rename(String token, String docPath, String newName) throws PathNotFoundException,
			ItemExistsException, AccessDeniedException, LockException, RepositoryException, DatabaseException {
		log.debug("rename({}, {}, {})", new Object[] { token, docPath, newName });
		Document renamedDocument = null;
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
			
			String name = PathUtils.getName(docPath);
			String docUuid = NodeBaseDAO.getInstance().getUuidFromPath(docPath);
			
			// Escape dangerous chars in name
			newName = PathUtils.escape(newName);
			
			if (newName != null && !newName.isEmpty() && !newName.equals(name)) {
				NodeDocument documentNode = NodeDocumentDAO.getInstance().rename(docUuid, newName);
				renamedDocument = BaseDocumentModule.getProperties(auth.getName(), documentNode);
				
				// Check subscriptions
				BaseNotificationModule.checkSubscriptions(documentNode, PrincipalUtils.getUser(), "RENAME_DOCUMENT", null);
			} else {
				// Don't change anything
				NodeDocument documentNode = NodeDocumentDAO.getInstance().findByPk(docUuid);
				renamedDocument = BaseDocumentModule.getProperties(auth.getName(), documentNode);
			}
			
			// Activity log
			UserActivity.log(auth.getName(), "RENAME_DOCUMENT", docUuid, docPath, newName);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("rename: {}", renamedDocument);
		return renamedDocument;
	}
	
	@Override
	public Document getProperties(String token, String docPath) throws PathNotFoundException, RepositoryException,
			DatabaseException {
		log.debug("getProperties({}, {})", token, docPath);
		Document doc = null;
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String docUuid = NodeBaseDAO.getInstance().getUuidFromPath(docPath);
			NodeDocument docNode = NodeDocumentDAO.getInstance().findByPk(docUuid);
			doc = BaseDocumentModule.getProperties(auth.getName(), docNode);
			
			// Activity log
			UserActivity.log(auth.getName(), "GET_DOCUMENT_PROPERTIES", docUuid, docPath, null);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getProperties: {}", doc);
		return doc;
	}
	
	@Override
	public void setProperties(String token, Document doc) throws VersionException, LockException,
			PathNotFoundException, AccessDeniedException, RepositoryException, DatabaseException {
		log.debug("setProperties({}, {})", token, doc);
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String docUuid = NodeBaseDAO.getInstance().getUuidFromPath(doc.getPath());
			NodeDocument docNode = NodeDocumentDAO.getInstance().findByPk(docUuid);
			
			// Check subscriptions
			BaseNotificationModule.checkSubscriptions(docNode, auth.getName(), "SET_DOCUMENT_PROPERTIES", null);
			
			// Check scripting
			// BaseScriptingModule.checkScripts(session, documentNode, documentNode, "SET_DOCUMENT_PROPERTIES");
			
			// Activity log
			UserActivity.log(auth.getName(), "SET_DOCUMENT_PROPERTIES", docUuid, doc.getPath(), null);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("setProperties: void");
	}
	
	@Override
	public InputStream getContent(String token, String docPath, boolean checkout) throws PathNotFoundException,
			AccessDeniedException, RepositoryException, IOException, DatabaseException {
		log.debug("getContent({}, {}, {})", new Object[] { token, docPath, checkout });
		return getContent(token, docPath, checkout, true);
	}
	
	/**
	 * Retrieve the content input stream from a document
	 * 
	 * @param token Authorization token.
	 * @param docPath Path of the document to get the content.
	 * @param checkout If the content is retrieved due to a checkout or not.
	 * @param extendedSecurity If the extended security DOWNLOAD permission should be evaluated.
	 * This is used to enable the document preview.
	 */
	public InputStream getContent(String token, String docPath, boolean checkout, boolean extendedSecurity) 
			throws PathNotFoundException, AccessDeniedException, RepositoryException, IOException,
			DatabaseException {
		log.debug("getContent({}, {}, {}, {})", new Object[] { token, docPath, checkout, extendedSecurity });
		InputStream is = null;
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			is = BaseDocumentModule.getContent(auth.getName(), docPath, checkout, extendedSecurity);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getContent: {}", is);
		return is;
	}
	
	@Override
	public InputStream getContentByVersion(String token, String docPath, String verName) throws RepositoryException,
			PathNotFoundException, IOException, DatabaseException {
		log.debug("getContentByVersion({}, {}, {})", new Object[] { token, docPath, verName });
		InputStream is = null;
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String docUuid = NodeBaseDAO.getInstance().getUuidFromPath(docPath);
			is = NodeDocumentVersionDAO.getInstance().getVersionContentByParent(docUuid, verName);
			
			// Activity log
			UserActivity.log(auth.getName(), "GET_DOCUMENT_CONTENT_BY_VERSION", docUuid, docPath, verName + ", " + is.available());
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getContentByVersion: {}", is);
		return is;
	}
	
	@Override
	@Deprecated
	public List<Document> getChilds(String token, String fldPath) throws PathNotFoundException, RepositoryException,
			DatabaseException {
		return getChildren(token, fldPath);
	}
	
	@Override
	public List<Document> getChildren(String token, String fldPath) throws PathNotFoundException, RepositoryException,
			DatabaseException {
		log.debug("getChildren({}, {})", token, fldPath);
		List<Document> children = new ArrayList<Document>();
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String fldUuid = NodeBaseDAO.getInstance().getUuidFromPath(fldPath);
			
			for (NodeDocument nDocument : NodeDocumentDAO.getInstance().findByParent(fldUuid)) {
				children.add(BaseDocumentModule.getProperties(auth.getName(), nDocument));
			}
			
			// Activity log
			UserActivity.log(auth.getName(), "GET_CHILDREN_DOCUMENTS", fldUuid, fldPath, null);
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
	public void checkout(String token, String docPath) throws LockException, PathNotFoundException,
			AccessDeniedException, RepositoryException, DatabaseException {
		checkout(token, docPath, null);
	}
	
	/**
	 * Used in Zoho extension
	 */
	public void checkout(String token, String docPath, String userId) throws LockException, PathNotFoundException,
			AccessDeniedException, RepositoryException, DatabaseException {
		log.debug("checkout({}, {}, {})", new Object[] { token, docPath, userId });
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
			
			String docUuid = NodeBaseDAO.getInstance().getUuidFromPath(docPath);
			NodeDocumentDAO.getInstance().checkout(userId, docUuid);
			
			// Activity log
			UserActivity.log(auth.getName(), "CHECKOUT_DOCUMENT", docUuid, docPath, null);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("checkout: void");
	}
	
	@Override
	public void cancelCheckout(String token, String docPath) throws LockException, PathNotFoundException,
			AccessDeniedException, RepositoryException, DatabaseException {
		log.debug("cancelCheckout({}, {})", token, docPath);
		cancelCheckoutHelper(token, docPath, false);
		log.debug("cancelCheckout: void");
	}
	
	@Override
	public void forceCancelCheckout(String token, String docPath) throws LockException, PathNotFoundException,
			AccessDeniedException, RepositoryException, DatabaseException, PrincipalAdapterException {
		log.debug("forceCancelCheckout({}, {})", token, docPath);
		
		if (PrincipalUtils.getRoles().contains(Config.DEFAULT_ADMIN_ROLE)) {
			cancelCheckoutHelper(token, docPath, true);
		} else {
			throw new AccessDeniedException("Only administrator use allowed");
		}
		
		log.debug("forceCancelCheckout: void");
	}
	
	/**
	 * Implement cancelCheckout and forceCancelCheckout features
	 */
	private void cancelCheckoutHelper(String token, String docPath, boolean force) throws LockException,
			PathNotFoundException, AccessDeniedException, RepositoryException, DatabaseException {
		log.debug("cancelCheckoutHelper({}, {}, {})", new Object[] { token, docPath, force });
		Authentication auth = null, oldAuth = null;
		String action = force ? "FORCE_CANCEL_DOCUMENT_CHECKOUT" : "CANCEL_DOCUMENT_CHECKOUT";
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String docUuid = NodeBaseDAO.getInstance().getUuidFromPath(docPath);
			NodeDocument docNode = NodeDocumentDAO.getInstance().findByPk(docUuid);
			NodeDocumentDAO.getInstance().cancelCheckout(auth.getName(), docUuid, force);
			
			// Check subscriptions
			BaseNotificationModule.checkSubscriptions(docNode, auth.getName(), action, null);
			
			// Check scripting
			// BaseScriptingModule.checkScripts(session, documentNode, documentNode, action);
			
			// Activity log
			UserActivity.log(auth.getName(), action, docUuid, docPath, null);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("cancelCheckoutHelper: void");
	}
	
	@Override
	public boolean isCheckedOut(String token, String docPath) throws PathNotFoundException, RepositoryException,
			DatabaseException {
		log.debug("isCheckedOut({}, {})", token, docPath);
		boolean checkedOut = false;
		@SuppressWarnings("unused")
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String docUuid = NodeBaseDAO.getInstance().getUuidFromPath(docPath);
			checkedOut = NodeDocumentDAO.getInstance().isCheckedOut(docUuid);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("isCheckedOut: {}", checkedOut);
		return checkedOut;
	}
	
	@Override
	public Version checkin(String token, String docPath, InputStream is, String comment)
			throws FileSizeExceededException, UserQuotaExceededException, VirusDetectedException,
			AccessDeniedException, RepositoryException, PathNotFoundException, LockException, VersionException,
			IOException, DatabaseException {
		return checkin(token, docPath, is, comment, null);
	}
	
	/**
	 * Used in Zoho extension
	 */
	public Version checkin(String token, String docPath, InputStream is, String comment, String userId)
			throws FileSizeExceededException, UserQuotaExceededException, VirusDetectedException,
			AccessDeniedException, RepositoryException, PathNotFoundException, LockException, VersionException,
			IOException, DatabaseException {
		return checkin(token, docPath, is, is.available(), comment, userId);
	}
	
	/**
	 * Used when big files and WebDAV
	 */
	public Version checkin(String token, String docPath, InputStream is, long size, String comment, String userId)
			throws FileSizeExceededException, UserQuotaExceededException, VirusDetectedException,
			AccessDeniedException, RepositoryException, PathNotFoundException, LockException, VersionException,
			IOException, DatabaseException {
		log.debug("checkin({}, {}, {}, {}, {}, {})", new Object[] { token, docPath, is, size, comment, userId });
		Version version = new Version();
		Authentication auth = null, oldAuth = null;
		
		if (Config.SYSTEM_READONLY) {
			throw new AccessDeniedException("System is in read-only mode");
		}
		
		String name = PathUtils.getName(docPath);
		int idx = name.lastIndexOf('.');
		String fileExtension = idx > 0 ? name.substring(idx) : ".tmp";
		File tmp = File.createTempFile("okm", fileExtension);
		
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
			
			if (Config.MAX_FILE_SIZE > 0 && size > Config.MAX_FILE_SIZE) {
				log.error("Uploaded file size: {} ({}), Max file size: {} ({})",
						new Object[] { FormatUtil.formatSize(size), size, FormatUtil.formatSize(Config.MAX_FILE_SIZE),
								Config.MAX_FILE_SIZE });
				UserActivity.log(userId, "ERROR_FILE_SIZE_EXCEEDED", null, docPath, Long.toString(size));
				throw new FileSizeExceededException(Long.toString(size));
			}
			
			// Manage temporary files
			byte[] buff = new byte[4 * 1024];
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
			
			String docUuid = NodeBaseDAO.getInstance().getUuidFromPath(docPath);
			NodeDocument docNode = NodeDocumentDAO.getInstance().findByPk(docUuid);
			NodeDocumentVersion newDocVersion = NodeDocumentVersionDAO.getInstance().checkin(userId, comment, docUuid,
					is, size);
			version = BaseModule.getProperties(newDocVersion);
			
			// Add comment (as system user)
			String text = "New version " + version.getName() + " by " + userId + ": " + comment;
			BaseNoteModule.create(docUuid, Config.SYSTEM_USER, text);
			
			// Update user items size
			if (Config.USER_ITEM_CACHE) {
				UserItemsManager.incSize(auth.getName(), size);
			}
			
			// Remove pdf & preview from cache
			CommonGeneralModule.cleanPreviewCache(docUuid);
			
			// Check subscriptions
			BaseNotificationModule.checkSubscriptions(docNode, userId, "CHECKIN_DOCUMENT", comment);
			
			// Check scripting
			// BaseScriptingModule.checkScripts(session, documentNode, documentNode, "CHECKIN_DOCUMENT");
			
			// Activity log
			UserActivity.log(auth.getName(), "CHECKIN_DOCUMENT", docUuid, docPath, size + ", " + comment);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			IOUtils.closeQuietly(is);
			org.apache.commons.io.FileUtils.deleteQuietly(tmp);
			
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("checkin: {}", version);
		return version;
	}	
	
	@Override
	public LockInfo lock(String token, String docPath) throws LockException, PathNotFoundException,
			AccessDeniedException, RepositoryException, DatabaseException {
		log.debug("lock({}, {})", token, docPath);
		LockInfo lck = null;
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String docUuid = NodeBaseDAO.getInstance().getUuidFromPath(docPath);
			NodeDocument docNode = NodeDocumentDAO.getInstance().findByPk(docUuid);
			NodeLock nLock = NodeDocumentDAO.getInstance().lock(auth.getName(), docUuid);
			lck = BaseModule.getProperties(nLock, docPath);
			
			// Check subscriptions
			BaseNotificationModule.checkSubscriptions(docNode, auth.getName(), "LOCK_DOCUMENT", null);
			
			// Check scripting
			// BaseScriptingModule.checkScripts(session, documentNode, documentNode, "LOCK_DOCUMENT");
			
			// Activity log
			UserActivity.log(auth.getName(), "LOCK_DOCUMENT", docUuid, docPath, lck.getToken());
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("lock: {}", lck);
		return lck;
	}
	
	@Override
	public void unlock(String token, String docPath) throws LockException, PathNotFoundException,
			AccessDeniedException, RepositoryException, DatabaseException {
		log.debug("unlock({}, {})", token, docPath);
		unlockHelper(token, docPath, false);
		log.debug("unlock: void");
	}
	
	@Override
	public void forceUnlock(String token, String docPath) throws LockException, PathNotFoundException,
			AccessDeniedException, RepositoryException, DatabaseException, PrincipalAdapterException {
		log.debug("forceUnlock({}, {})", token, docPath);
		
		if (PrincipalUtils.getRoles().contains(Config.DEFAULT_ADMIN_ROLE)) {
			unlockHelper(token, docPath, true);
		} else {
			throw new AccessDeniedException("Only administrator use allowed");
		}
		
		log.debug("forceUnlock: void");
	}
	
	/**
	 * Implement unlock and forceUnlock features
	 */
	private void unlockHelper(String token, String docPath, boolean force) throws LockException, PathNotFoundException,
			AccessDeniedException, RepositoryException, DatabaseException {
		log.debug("unlock({}, {}, {})", new Object[] { token, docPath, force });
		Authentication auth = null, oldAuth = null;
		String action = force ? "FORCE_UNLOCK_DOCUMENT" : "UNLOCK_DOCUMENT";
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String docUuid = NodeBaseDAO.getInstance().getUuidFromPath(docPath);
			NodeDocument docNode = NodeDocumentDAO.getInstance().findByPk(docUuid);
			NodeDocumentDAO.getInstance().unlock(auth.getName(), docUuid, force);
			
			// Check subscriptions
			BaseNotificationModule.checkSubscriptions(docNode, auth.getName(), action, null);
			
			// Check scripting
			// BaseScriptingModule.checkScripts(session, documentNode, documentNode, action);
			
			// Activity log
			UserActivity.log(auth.getName(), action, docUuid, docPath, null);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("unlock: void");
	}
	
	@Override
	public boolean isLocked(String token, String docPath) throws RepositoryException, PathNotFoundException,
			DatabaseException {
		log.debug("isLocked({}, {})", token, docPath);
		boolean locked = false;
		@SuppressWarnings("unused")
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String docUuid = NodeBaseDAO.getInstance().getUuidFromPath(docPath);
			locked = NodeDocumentDAO.getInstance().isLocked(docUuid);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("isLocked: {}", locked);
		return locked;
	}
	
	@Override
	public LockInfo getLockInfo(String token, String docPath) throws RepositoryException, PathNotFoundException,
			LockException, DatabaseException {
		log.debug("getLock({}, {})", token, docPath);
		LockInfo lock = null;
		@SuppressWarnings("unused")
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String docUuid = NodeBaseDAO.getInstance().getUuidFromPath(docPath);
			NodeLock nLock = NodeDocumentDAO.getInstance().getLock(docUuid);
			lock = BaseModule.getProperties(nLock, docPath);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getLock: {}", lock);
		return lock;
	}
	
	@Override
	public void purge(String token, String docPath) throws LockException, AccessDeniedException, RepositoryException,
			PathNotFoundException, DatabaseException {
		log.debug("purge({}, {})", token, docPath);
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
			
			String docUuid = NodeBaseDAO.getInstance().getUuidFromPath(docPath);
			NodeDocumentDAO.getInstance().purge(docUuid);
			
			// Activity log - Already inside DAO
			// UserActivity.log(auth.getName(), "PURGE_DOCUMENT", docUuid, docPath, null);
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
	public void move(String token, String docPath, String dstPath) throws PathNotFoundException, ItemExistsException,
			AccessDeniedException, LockException, RepositoryException, DatabaseException, ExtensionException,
			AutomationException {
		log.debug("move({}, {}, {})", new Object[] { token, docPath, dstPath });
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
			
			String docUuid = NodeBaseDAO.getInstance().getUuidFromPath(docPath);
			String dstUuid = NodeBaseDAO.getInstance().getUuidFromPath(dstPath);
			
			// AUTOMATION - PRE
			Map<String, Object> env = new HashMap<String, Object>();
			env.put(AutomationUtils.DOCUMENT_UUID, docUuid);
			env.put(AutomationUtils.FOLDER_UUID, dstUuid);
			AutomationManager.getInstance().fireEvent(AutomationRule.EVENT_DOCUMENT_MOVE, AutomationRule.AT_PRE, env);
			
			NodeDocumentDAO.getInstance().move(docUuid, dstUuid);
			
			// AUTOMATION - POST
			AutomationManager.getInstance().fireEvent(AutomationRule.EVENT_DOCUMENT_MOVE, AutomationRule.AT_POST, env);
			
			// Activity log
			UserActivity.log(auth.getName(), "MOVE_DOCUMENT", docUuid, docPath, dstPath);
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
	public void copy(String token, String docPath, String dstPath) throws ItemExistsException, PathNotFoundException,
			AccessDeniedException, RepositoryException, IOException, AutomationException, DatabaseException,
			UserQuotaExceededException {
		log.debug("copy({}, {}, {})", new Object[] { token, docPath, dstPath });
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
			
			// Escape dangerous chars in name
			String docName = PathUtils.escape(PathUtils.getName(docPath));
			
			String docUuid = NodeBaseDAO.getInstance().getUuidFromPath(docPath);
			String dstUuid = NodeBaseDAO.getInstance().getUuidFromPath(dstPath);
			NodeDocument srcDocNode = NodeDocumentDAO.getInstance().findByPk(docUuid);
			NodeFolder dstFldNode = NodeFolderDAO.getInstance().findByPk(dstUuid);
			NodeDocument newDocNode = BaseDocumentModule.copy(auth.getName(), srcDocNode, dstPath, dstFldNode, docName);
			
			// Check subscriptions
			BaseNotificationModule.checkSubscriptions(dstFldNode, auth.getName(), "COPY_DOCUMENT", null);
			
			// Activity log
			UserActivity.log(auth.getName(), "COPY_DOCUMENT", newDocNode.getUuid(), docPath, dstPath);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
	}
	
	@Override
	public void restoreVersion(String token, String docPath, String versionId) throws PathNotFoundException,
			AccessDeniedException, LockException, RepositoryException, DatabaseException {
		log.debug("restoreVersion({}, {}, {})", new Object[] { token, docPath, versionId });
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
			
			String docUuid = NodeBaseDAO.getInstance().getUuidFromPath(docPath);
			NodeDocumentVersionDAO.getInstance().restoreVersion(docUuid, versionId);
			
			// Remove pdf & preview from cache
			CommonGeneralModule.cleanPreviewCache(docUuid);
			
			// Activity log
			UserActivity.log(auth.getName(), "RESTORE_DOCUMENT_VERSION", docUuid, docPath, versionId);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("restoreVersion: void");
	}
	
	@Override
	public void purgeVersionHistory(String token, String docPath) throws AccessDeniedException, PathNotFoundException,
			LockException, RepositoryException, DatabaseException {
		log.debug("purgeVersionHistory({}, {})", token, docPath);
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
			
			String docUuid = NodeBaseDAO.getInstance().getUuidFromPath(docPath);
			NodeDocumentVersionDAO.getInstance().purgeVersionHistory(docUuid);
			
			// Activity log
			UserActivity.log(auth.getName(), "PURGE_DOCUMENT_VERSION_HISTORY", docUuid, docPath, null);
		} catch (IOException e) {
			throw new RepositoryException(e.getMessage(), e);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("purgeVersionHistory: void");
	}
	
	@Override
	public List<Version> getVersionHistory(String token, String docPath) throws PathNotFoundException,
			RepositoryException, DatabaseException {
		log.debug("getVersionHistory({}, {})", token, docPath);
		List<Version> history = new ArrayList<Version>();
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String docUuid = NodeBaseDAO.getInstance().getUuidFromPath(docPath);
			List<NodeDocumentVersion> docVersions = NodeDocumentVersionDAO.getInstance().findByParent(docUuid);
			
			for (NodeDocumentVersion nDocVersion : docVersions) {
				history.add(BaseModule.getProperties(nDocVersion));
			}
			
			// Activity log
			UserActivity.log(auth.getName(), "GET_DOCUMENT_VERSION_HISTORY", docUuid, docPath, null);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getVersionHistory: {}", history);
		return history;
	}
	
	@Override
	public long getVersionHistorySize(String token, String docPath) throws RepositoryException, PathNotFoundException,
			DatabaseException {
		log.debug("getVersionHistorySize({}, {})", token, docPath);
		long versionHistorySize = 0;
		@SuppressWarnings("unused")
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String docUuid = NodeBaseDAO.getInstance().getUuidFromPath(docPath);
			List<NodeDocumentVersion> docVersions = NodeDocumentVersionDAO.getInstance().findByParent(docUuid);
			
			for (NodeDocumentVersion nDocVersion : docVersions) {
				versionHistorySize += nDocVersion.getSize();
			}
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getVersionHistorySize: {}", versionHistorySize);
		return versionHistorySize;
	}
	
	@Override
	public boolean isValid(String token, String docPath) throws PathNotFoundException, RepositoryException, DatabaseException {
		log.debug("isValid({}, {})", token, docPath);
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
			
			String docUuid = NodeBaseDAO.getInstance().getUuidFromPath(docPath);
			
			try {
				NodeDocumentDAO.getInstance().findByPk(docUuid);
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
