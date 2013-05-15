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

package com.openkm.dao;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.cache.UserItemsManager;
import com.openkm.core.AccessDeniedException;
import com.openkm.core.Config;
import com.openkm.core.DatabaseException;
import com.openkm.core.LockException;
import com.openkm.core.PathNotFoundException;
import com.openkm.dao.bean.NodeDocument;
import com.openkm.dao.bean.NodeDocumentVersion;
import com.openkm.module.db.stuff.FsDataStore;
import com.openkm.module.db.stuff.LockHelper;
import com.openkm.module.db.stuff.SecurityHelper;
import com.openkm.vernum.VersionNumerationAdapter;
import com.openkm.vernum.VersionNumerationFactory;

public class NodeDocumentVersionDAO extends GenericDAO<NodeDocumentVersion, String> {
	private static Logger log = LoggerFactory.getLogger(NodeDocumentVersionDAO.class);
	private static NodeDocumentVersionDAO single = new NodeDocumentVersionDAO();
	
	private NodeDocumentVersionDAO() {
	}
	
	public static NodeDocumentVersionDAO getInstance() {
		return single;
	}
	
	/**
	 * Find by parent
	 */
	@SuppressWarnings("unchecked")
	public List<NodeDocumentVersion> findByParent(String docUuid) throws PathNotFoundException, DatabaseException {
		log.debug("findByParent({})", docUuid);
		String qs = "from NodeDocumentVersion ndv where ndv.parent=:parent order by ndv.created";
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			// Security Check
			NodeDocument nDoc = (NodeDocument) session.load(NodeDocument.class, docUuid);
			SecurityHelper.checkRead(nDoc);
			
			Query q = session.createQuery(qs);
			q.setString("parent", docUuid);
			List<NodeDocumentVersion> ret = q.list();
			HibernateUtil.commit(tx);
			log.debug("findByParent: {}", ret);
			return ret;
		} catch (PathNotFoundException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (DatabaseException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (HibernateException e) {
			HibernateUtil.rollback(tx);
			throw new DatabaseException(e.getMessage(), e);
		} finally {
			HibernateUtil.close(session);
		}
	}
	
	/**
	 * Find current document version
	 */
	public NodeDocumentVersion findCurrentVersion(String docUuid) throws PathNotFoundException, DatabaseException {
		log.debug("findCurrentVersion({})", docUuid);
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			// Security Check
			NodeDocument nDoc = (NodeDocument) session.load(NodeDocument.class, docUuid);
			SecurityHelper.checkRead(nDoc);
			
			NodeDocumentVersion currentVersion = findCurrentVersion(session, docUuid);
			HibernateUtil.commit(tx);
			log.debug("findCurrentVersion: {}", currentVersion);
			return currentVersion;
		} catch (PathNotFoundException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (DatabaseException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (HibernateException e) {
			HibernateUtil.rollback(tx);
			throw new DatabaseException(e.getMessage(), e);
		} finally {
			HibernateUtil.close(session);
		}
	}
	
	/**
	 * Find current document version
	 */
	public NodeDocumentVersion findCurrentVersion(Session session, String docUuid) throws HibernateException {
		log.debug("findCurrentVersion({})", docUuid);
		String qs = "from NodeDocumentVersion ndv where ndv.parent=:parent and ndv.current=:current";
		Query q = session.createQuery(qs);
		q.setString("parent", docUuid);
		q.setBoolean("current", true);
		NodeDocumentVersion currentVersion = (NodeDocumentVersion) q.setMaxResults(1).uniqueResult();
		return currentVersion;
	}
	
	/**
	 * Get document version content
	 * 
	 * @param docUuid Id of the document to get the content.
	 * This is used to enable the document preview.
	 */
	public InputStream getCurrentContentByParent(String docUuid) throws 
			PathNotFoundException, AccessDeniedException, DatabaseException, FileNotFoundException, IOException {
		log.debug("getContent({})", docUuid);
		String qs = "from NodeDocumentVersion ndv where ndv.parent=:parent and ndv.current=:current";
		Session session = null;
		Transaction tx = null;
		InputStream ret = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			// Security Check
			NodeDocument nDoc = (NodeDocument) session.load(NodeDocument.class, docUuid);
			SecurityHelper.checkRead(nDoc);
			
			Query q = session.createQuery(qs);
			q.setString("parent", docUuid);
			q.setBoolean("current", true);
			NodeDocumentVersion nDocVer = (NodeDocumentVersion) q.setMaxResults(1).uniqueResult();
			
			if (FsDataStore.DATASTORE_BACKEND_FS.equals(Config.REPOSITORY_DATASTORE_BACKEND)) {
				ret = FsDataStore.read(nDocVer.getUuid());
			} else {
				ret = new ByteArrayInputStream(nDocVer.getContent());
			}
			
			HibernateUtil.commit(tx);
			log.debug("getContent: {}", ret);
			return ret;
		} catch (PathNotFoundException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (DatabaseException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (HibernateException e) {
			HibernateUtil.rollback(tx);
			throw new DatabaseException(e.getMessage(), e);
		} finally {
			HibernateUtil.close(session);
		}
	}
	
	/**
	 * Get document version content
	 */
	public InputStream getVersionContentByParent(String docUuid, String name) throws PathNotFoundException,
			DatabaseException, FileNotFoundException, IOException {
		log.debug("getContent({})", docUuid);
		String qs = "from NodeDocumentVersion ndv where ndv.parent=:parent and ndv.name=:name";
		Session session = null;
		Transaction tx = null;
		InputStream ret = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			// Security Check
			NodeDocument nDoc = (NodeDocument) session.load(NodeDocument.class, docUuid);
			SecurityHelper.checkRead(nDoc);
			
			Query q = session.createQuery(qs);
			q.setString("parent", docUuid);
			q.setString("name", name);
			NodeDocumentVersion nDocVer = (NodeDocumentVersion) q.setMaxResults(1).uniqueResult();
			
			if (FsDataStore.DATASTORE_BACKEND_FS.equals(Config.REPOSITORY_DATASTORE_BACKEND)) {
				ret = FsDataStore.read(nDocVer.getUuid());
			} else {
				ret = new ByteArrayInputStream(nDocVer.getContent());
			}
			
			HibernateUtil.commit(tx);
			log.debug("getContent: {}", ret);
			return ret;
		} catch (PathNotFoundException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (DatabaseException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (HibernateException e) {
			HibernateUtil.rollback(tx);
			throw new DatabaseException(e.getMessage(), e);
		} finally {
			HibernateUtil.close(session);
		}
	}
	
	/**
	 * Create or update dummy version
	 */
	public NodeDocumentVersion checkin(String user, String comment, String docUuid, InputStream is, long size)
			throws IOException, PathNotFoundException, AccessDeniedException, LockException, DatabaseException {
		log.debug("checkin({}, {}, {}, {}, {})", new Object[] { user, comment, docUuid, is, size });
		String qs = "from NodeDocumentVersion ndv where ndv.parent=:parent and ndv.current=:current";
		NodeDocumentVersion newDocVersion = new NodeDocumentVersion();
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			// Security Check
			NodeDocument nDoc = (NodeDocument) session.load(NodeDocument.class, docUuid);
			SecurityHelper.checkRead(nDoc);
			SecurityHelper.checkWrite(nDoc);
			
			// Lock Check
			LockHelper.checkWriteLock(user, nDoc);
			
			Query q = session.createQuery(qs);
			q.setString("parent", docUuid);
			q.setBoolean("current", true);
			NodeDocumentVersion curDocVersion = (NodeDocumentVersion) q.setMaxResults(1).uniqueResult();
			VersionNumerationAdapter verNumAdapter = VersionNumerationFactory.getVersionNumerationAdapter();
			String nextVersionNumber = verNumAdapter.getNextVersionNumber(session, nDoc, curDocVersion);
			
			// Make current version obsolete
			curDocVersion.setCurrent(false);
			session.update(curDocVersion);
			
			// New document version
			newDocVersion.setUuid(UUID.randomUUID().toString());
			newDocVersion.setParent(docUuid);
			newDocVersion.setName(nextVersionNumber);
			newDocVersion.setAuthor(user);
			newDocVersion.setComment(comment);
			newDocVersion.setCurrent(true);
			newDocVersion.setCreated(Calendar.getInstance());
			newDocVersion.setSize(size);
			newDocVersion.setMimeType(curDocVersion.getMimeType());
			newDocVersion.setPrevious(curDocVersion.getUuid());
			
			// Persist file in datastore
			FsDataStore.persist(newDocVersion, is);
			
			session.save(newDocVersion);
			
			// Set document checkout status to false
			nDoc.setLastModified(newDocVersion.getCreated());
			nDoc.setCheckedOut(false);
			
			// Text extraction
			nDoc.setText("");
			nDoc.setTextExtracted(false);
			
			// Remove lock
			NodeDocumentDAO.getInstance().unlock(session, user, nDoc, false);
			
			session.update(nDoc);
			HibernateUtil.commit(tx);
			
			log.debug("checkin: {}", newDocVersion);
			return newDocVersion;
		} catch (PathNotFoundException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (AccessDeniedException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (DatabaseException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (LockException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (HibernateException e) {
			HibernateUtil.rollback(tx);
			
			// What happen when create fails? This datastore file should be deleted!
			FsDataStore.delete(newDocVersion.getUuid());
			throw new DatabaseException(e.getMessage(), e);
		} finally {
			HibernateUtil.close(session);
		}
	}
	
	/**
	 * Set version content.
	 */
	public void setContent(String docUuid, InputStream is, long size) throws IOException, PathNotFoundException,
			AccessDeniedException, LockException, DatabaseException {
		log.debug("setContent({}, {}, {})", new Object[] { docUuid, is, size });
		String qs = "from NodeDocumentVersion ndv where ndv.parent=:parent and ndv.current=:current";
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			// Security Check
			NodeDocument nDoc = (NodeDocument) session.load(NodeDocument.class, docUuid);
			SecurityHelper.checkRead(nDoc);
			SecurityHelper.checkWrite(nDoc);
			
			// Lock Check
			LockHelper.checkWriteLock(nDoc);
			
			Query q = session.createQuery(qs);
			q.setString("parent", docUuid);
			q.setBoolean("current", true);
			
			// Text extraction
			nDoc.setText("");
			nDoc.setTextExtracted(false);
			session.update(nDoc);
			
			// Update version content
			NodeDocumentVersion curDocVersion = (NodeDocumentVersion) q.setMaxResults(1).uniqueResult();
			curDocVersion.setText("");
			curDocVersion.setSize(size);
			session.update(curDocVersion);
			
			// Persist file in datastore
			FsDataStore.persist(curDocVersion, is);
			
			HibernateUtil.commit(tx);
			log.debug("setContent: void");
		} catch (PathNotFoundException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (AccessDeniedException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (DatabaseException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (LockException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (HibernateException e) {
			HibernateUtil.rollback(tx);
			throw new DatabaseException(e.getMessage(), e);
		} finally {
			HibernateUtil.close(session);
		}
	}
	
	/**
	 * Set a document version as current.
	 */
	public void restoreVersion(String docUuid, String versionId) throws PathNotFoundException, 
			AccessDeniedException, LockException, DatabaseException {
		log.debug("restoreVersion({}, {})", new Object[] { docUuid, versionId });
		String qsCurrent = "from NodeDocumentVersion ndv where ndv.parent=:parent and ndv.current=:current";
		String qsName = "from NodeDocumentVersion ndv where ndv.parent=:parent and ndv.name=:name";
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			// Security Check
			NodeDocument nDoc = (NodeDocument) session.load(NodeDocument.class, docUuid);
			SecurityHelper.checkRead(nDoc);
			SecurityHelper.checkWrite(nDoc);
			
			// Lock Check
			LockHelper.checkWriteLock(nDoc);
			
			Query qCurrent = session.createQuery(qsCurrent);
			qCurrent.setString("parent", docUuid);
			qCurrent.setBoolean("current", true);
			
			Query qName = session.createQuery(qsName);
			qName.setString("parent", docUuid);
			qName.setString("name", versionId);
			
			// Update current version
			NodeDocumentVersion curDocVersion = (NodeDocumentVersion) qCurrent.setMaxResults(1).uniqueResult();
			NodeDocumentVersion namDocVersion = (NodeDocumentVersion) qName.setMaxResults(1).uniqueResult();
			curDocVersion.setCurrent(false);
			namDocVersion.setCurrent(true);
			session.update(namDocVersion);
			session.update(curDocVersion);
			
			// Text extraction
			nDoc.setText(namDocVersion.getText());
			session.update(nDoc);
			
			HibernateUtil.commit(tx);
			log.debug("restoreVersion: void");
		} catch (PathNotFoundException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (AccessDeniedException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (DatabaseException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (LockException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (HibernateException e) {
			HibernateUtil.rollback(tx);
			throw new DatabaseException(e.getMessage(), e);
		} finally {
			HibernateUtil.close(session);
		}
	}
	
	/**
	 * Purge all non-current document version history nodes
	 */
	@SuppressWarnings("unchecked")
	public void purgeVersionHistory(String docUuid) throws PathNotFoundException, 
			AccessDeniedException, LockException, IOException, DatabaseException {
		log.debug("purgeVersionHistory({})", docUuid);
		String qs = "from NodeDocumentVersion ndv where ndv.parent=:parent and ndv.current=:current";
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			// Security Check
			NodeDocument nDoc = (NodeDocument) session.load(NodeDocument.class, docUuid);
			SecurityHelper.checkRead(nDoc);
			SecurityHelper.checkWrite(nDoc);
			
			// Lock Check
			LockHelper.checkWriteLock(nDoc);
			
			Query q = session.createQuery(qs);
			q.setString("parent", docUuid);
			q.setBoolean("current", false);
			
			// Remove non-current version nodes
			for (NodeDocumentVersion nDocVer : (List<NodeDocumentVersion>) q.list()) {
				String author = nDocVer.getAuthor();
				long size = nDocVer.getSize();
				
				if (FsDataStore.DATASTORE_BACKEND_FS.equals(Config.REPOSITORY_DATASTORE_BACKEND)) {
					FsDataStore.delete(nDocVer.getUuid());
				}
				
				session.delete(nDocVer);
				
				// Update user items size
				if (Config.USER_ITEM_CACHE) {
					UserItemsManager.decSize(author, size);
				}
			}
			
			HibernateUtil.commit(tx);
			log.debug("purgeVersionHistory: void");
		} catch (PathNotFoundException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (AccessDeniedException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (DatabaseException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (LockException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (HibernateException e) {
			HibernateUtil.rollback(tx);
			throw new DatabaseException(e.getMessage(), e);
		} finally {
			HibernateUtil.close(session);
		}
	}
	
	/**
	 * Purge in depth helper
	 */
	@SuppressWarnings("unchecked")
	public void purgeHelper(Session session, String parentUuid) throws HibernateException, IOException {
		String qs = "from NodeDocumentVersion ndv where ndv.parent=:parent";
		Query q = session.createQuery(qs);
		q.setString("parent", parentUuid);
		List<NodeDocumentVersion> listDocVersions = q.list();
		
		for (NodeDocumentVersion nDocVer : listDocVersions) {
			String author = nDocVer.getAuthor();
			long size = nDocVer.getSize();
			
			if (FsDataStore.DATASTORE_BACKEND_FS.equals(Config.REPOSITORY_DATASTORE_BACKEND)) {
				FsDataStore.delete(nDocVer.getUuid());
			}
			
			session.delete(nDocVer);
			
			// Update user items size
			if (Config.USER_ITEM_CACHE) {
				UserItemsManager.decSize(author, size);
			}
		}
	}
}
