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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.bean.Permission;
import com.openkm.cache.UserItemsManager;
import com.openkm.core.AccessDeniedException;
import com.openkm.core.Config;
import com.openkm.core.DatabaseException;
import com.openkm.core.ItemExistsException;
import com.openkm.core.LockException;
import com.openkm.core.PathNotFoundException;
import com.openkm.dao.bean.NodeBase;
import com.openkm.dao.bean.NodeFolder;
import com.openkm.dao.bean.NodeMail;
import com.openkm.module.db.stuff.DbAccessManager;
import com.openkm.module.db.stuff.SecurityHelper;
import com.openkm.spring.PrincipalUtils;
import com.openkm.util.UserActivity;

public class NodeFolderDAO {
	private static Logger log = LoggerFactory.getLogger(NodeFolderDAO.class);
	private static NodeFolderDAO single = new NodeFolderDAO();
	
	private NodeFolderDAO() {
	}
	
	public static NodeFolderDAO getInstance() {
		return single;
	}
	
	/**
	 * Create base node
	 */
	public void createBase(NodeFolder nFolder) throws DatabaseException {
		log.debug("createBase({})", nFolder);
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			session.save(nFolder);
			HibernateUtil.commit(tx);
			log.debug("createBase: void");
		} catch (HibernateException e) {
			HibernateUtil.rollback(tx);
			throw new DatabaseException(e.getMessage(), e);
		} finally {
			HibernateUtil.close(session);
		}
	}
	
	/**
	 * Create node
	 */
	public void create(NodeFolder nFolder) throws PathNotFoundException, AccessDeniedException, ItemExistsException,
			DatabaseException {
		log.debug("create({})", nFolder);
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			// Security Check
			NodeBase parentNode = (NodeBase) session.load(NodeBase.class, nFolder.getParent());
			SecurityHelper.checkRead(parentNode);
			SecurityHelper.checkWrite(parentNode);
			
			// Check for same folder name in same parent
			NodeBaseDAO.getInstance().checkItemExistence(session, nFolder.getParent(), nFolder.getName());
			
			session.save(nFolder);
			HibernateUtil.commit(tx);
			log.debug("create: void");
		} catch (PathNotFoundException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (AccessDeniedException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (ItemExistsException e) {
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
	 * Find by parent
	 */
	@SuppressWarnings("unchecked")
	public List<NodeFolder> findByParent(String parentUuid) throws PathNotFoundException, DatabaseException {
		log.debug("findByParent({})", parentUuid);
		String qs = "from NodeFolder nf where nf.parent=:parent order by nf.name";
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			// Security Check
			if (!Config.ROOT_NODE_UUID.equals(parentUuid)) {
				NodeBase parentNode = (NodeBase) session.load(NodeBase.class, parentUuid);
				SecurityHelper.checkRead(parentNode);
			}
			
			Query q = session.createQuery(qs);
			q.setString("parent", parentUuid);
			List<NodeFolder> ret = q.list();
			
			// Security Check
			SecurityHelper.pruneNodeList(ret);
			
			initialize(ret);
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
	 * Find by pk
	 */
	public NodeFolder findByPk(String uuid) throws PathNotFoundException, DatabaseException {
		log.debug("findByPk({})", uuid);
		String qs = "from NodeFolder nf where nf.uuid=:uuid";
		Session session = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Query q = session.createQuery(qs);
			q.setString("uuid", uuid);
			NodeFolder nFld = (NodeFolder) q.setMaxResults(1).uniqueResult();
			
			if (nFld == null) {
				throw new PathNotFoundException(uuid);
			}
			
			// Security Check
			SecurityHelper.checkRead(nFld);
			
			initialize(nFld);
			log.debug("findByPk: {}", nFld);
			return nFld;
		} catch (HibernateException e) {
			throw new DatabaseException(e.getMessage(), e);
		} finally {
			HibernateUtil.close(session);
		}
	}
	
	/**
	 * Search nodes by category
	 */
	@SuppressWarnings("unchecked")
	public List<NodeFolder> findByCategory(String catUuid) throws PathNotFoundException, DatabaseException {
		log.debug("findByCategory({})", catUuid);
		final String qs = "from NodeFolder nf where :category in elements(nf.categories) order by nf.name";
		List<NodeFolder> ret = new ArrayList<NodeFolder>();
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			// Security Check
			NodeBase catNode = (NodeBase) session.load(NodeBase.class, catUuid);
			SecurityHelper.checkRead(catNode);
			
			Query q = session.createQuery(qs);
			q.setString("category", catUuid);
			ret = q.list();
			
			// Security Check
			SecurityHelper.pruneNodeList(ret);
			
			initialize(ret);
			HibernateUtil.commit(tx);
			log.debug("findByCategory: {}", ret);
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
	 * Search nodes by keyword
	 */
	@SuppressWarnings("unchecked")
	public List<NodeFolder> findByKeyword(String keyword) throws DatabaseException {
		log.debug("findByKeyword({})", keyword);
		final String qs = "from NodeFolder nf where :keyword in elements(nf.keywords) order by nf.name";
		List<NodeFolder> ret = new ArrayList<NodeFolder>();
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			Query q = session.createQuery(qs);
			q.setString("keyword", keyword);
			ret = q.list();
			
			// Security Check
			SecurityHelper.pruneNodeList(ret);
			
			initialize(ret);
			HibernateUtil.commit(tx);
			log.debug("findByKeyword: {}", ret);
			return ret;
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
	 * Search nodes by property value
	 */
	@SuppressWarnings("unchecked")
	public List<NodeFolder> findByPropertyValue(String group, String property, String value) throws DatabaseException {
		log.debug("findByPropertyValue({}, {}, {})", new Object[] { group, property, value });
		String qs = "select nb from NodeFolder nb join nb.properties nbp where nbp.group=:group and nbp.name=:property and nbp.value like :value";
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			Query q = session.createQuery(qs);
			q.setString("group", group);
			q.setString("property", property);
			q.setString("value", "%" + value + "%");
			List<NodeFolder> ret = q.list();
			
			// Security Check
			SecurityHelper.pruneNodeList(ret);
			
			initialize(ret);
			HibernateUtil.commit(tx);
			log.debug("findByPropertyValue: {}", ret);
			return ret;
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
	 * Check if folder has childs
	 */
	@SuppressWarnings("unchecked")
	public boolean hasChildren(String parentUuid) throws PathNotFoundException, DatabaseException {
		log.debug("hasChildren({})", parentUuid);
		String qs = "from NodeFolder nf where nf.parent=:parent";
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			// Security Check
			if (!Config.ROOT_NODE_UUID.equals(parentUuid)) {
				NodeBase parentNode = (NodeBase) session.load(NodeBase.class, parentUuid);
				SecurityHelper.checkRead(parentNode);
			}
			
			Query q = session.createQuery(qs);
			q.setString("parent", parentUuid);
			List<NodeFolder> nodeList = q.list();
			
			// Security Check
			SecurityHelper.pruneNodeList(nodeList);
			
			boolean ret = !nodeList.isEmpty();
			HibernateUtil.commit(tx);
			log.debug("hasChildren: {}", ret);
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
	 * Rename folder
	 */
	public NodeFolder rename(String uuid, String newName) throws PathNotFoundException, AccessDeniedException,
			ItemExistsException, DatabaseException {
		log.debug("rename({}, {})", uuid, newName);
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			// Security Check
			NodeBase parentNode = NodeBaseDAO.getInstance().getParentNode(session, uuid);
			SecurityHelper.checkRead(parentNode);
			SecurityHelper.checkWrite(parentNode);
			NodeFolder nFld = (NodeFolder) session.load(NodeFolder.class, uuid);
			SecurityHelper.checkRead(nFld);
			SecurityHelper.checkWrite(nFld);
			
			// Check for same folder name in same parent
			NodeBaseDAO.getInstance().checkItemExistence(session, nFld.getParent(), newName);
			
			nFld.setName(newName);
			session.update(nFld);
			initialize(nFld);
			HibernateUtil.commit(tx);
			log.debug("rename: {}", nFld);
			return nFld;
		} catch (PathNotFoundException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (AccessDeniedException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (ItemExistsException e) {
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
	 * Move folder
	 */
	public void move(String uuid, String dstUuid) throws PathNotFoundException, AccessDeniedException,
			ItemExistsException, DatabaseException {
		log.debug("move({}, {})", uuid, dstUuid);
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			// Security Check
			NodeFolder nDstFld = (NodeFolder) session.load(NodeFolder.class, dstUuid);
			SecurityHelper.checkRead(nDstFld);
			SecurityHelper.checkWrite(nDstFld);
			NodeFolder nFld = (NodeFolder) session.load(NodeFolder.class, uuid);
			SecurityHelper.checkRead(nFld);
			SecurityHelper.checkWrite(nFld);
			
			// Check if move to itself
			if (uuid.equals(dstUuid)) {
				String dstPath = NodeBaseDAO.getInstance().getPathFromUuid(dstUuid);
				throw new ItemExistsException(dstPath);
			}
			
			// Check for same folder name in same parent
			NodeBaseDAO.getInstance().checkItemExistence(session, dstUuid, nFld.getName());
			
			// Check if context changes
			if (!nDstFld.getContext().equals(nFld.getContext())) {
				nFld.setContext(nDstFld.getContext());
				
				// Need recursive context changes
				moveHelper(session, uuid, nDstFld.getContext());
			}
			
			nFld.setParent(dstUuid);
			session.update(nFld);
			HibernateUtil.commit(tx);
			log.debug("move: void");
		} catch (PathNotFoundException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (AccessDeniedException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (ItemExistsException e) {
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
	 * Delete folder
	 */
	public void delete(String name, String uuid, String trashUuid) throws PathNotFoundException,
			AccessDeniedException, DatabaseException {
		log.debug("delete({}, {}, {})", new Object[] { name, uuid, trashUuid });
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			// Security Check
			NodeFolder nTrashFld = (NodeFolder) session.load(NodeFolder.class, trashUuid);
			SecurityHelper.checkRead(nTrashFld);
			SecurityHelper.checkWrite(nTrashFld);
			NodeFolder nFld = (NodeFolder) session.load(NodeFolder.class, uuid);
			SecurityHelper.checkRead(nFld);
			SecurityHelper.checkWrite(nFld);
			
			// Test if already exists a folder with the same name in the trash
			String testName = name;
			
			for (int i=1; NodeBaseDAO.getInstance().testItemExistence(session, trashUuid, testName); i++) {
				// log.info("Trying with: {}", testName);
				testName = name + " (" + i + ")";
			}
			
			// Need recursive context changes
			moveHelper(session, uuid, nTrashFld.getContext());
			
			nFld.setContext(nTrashFld.getContext());
			nFld.setParent(trashUuid);
			nFld.setName(testName);
			session.update(nFld);
			HibernateUtil.commit(tx);
			log.debug("delete: void");
		} catch (PathNotFoundException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (AccessDeniedException e) {
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
	
	@SuppressWarnings("unchecked")
	private void moveHelper(Session session, String parentUuid, String newContext) throws HibernateException {
		String qs = "from NodeBase nf where nf.parent=:parent";
		Query q = session.createQuery(qs);
		q.setString("parent", parentUuid);
		
		for (NodeBase nBase : (List<NodeBase>) q.list()) {
			nBase.setContext(newContext);
			
			if (nBase instanceof NodeFolder || nBase instanceof NodeMail) {
				moveHelper(session, nBase.getUuid(), newContext);
			}
		}
	}
	
	/**
	 * Get categories from node
	 */
	public Set<NodeFolder> resolveCategories(Set<String> categories) throws DatabaseException {
		log.debug("resolveCategories({})", categories);
		Set<NodeFolder> ret = new HashSet<NodeFolder>();
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			DbAccessManager am = SecurityHelper.getAccessManager();
			
			for (String catUuid : categories) {
				NodeFolder nFld = (NodeFolder) session.load(NodeFolder.class, catUuid);
				
				// Security Check
				if (am.isGranted(nFld, Permission.READ)) {
					initialize(nFld);
					ret.add(nFld);
				}
			}
			
			log.debug("resolveCategories: {}", ret);
			return ret;
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
	 * Purge in depth. Respect the parameter deleteBase, it means if the node nFolder should be deleted itself. This
	 * parameter is present because this "purge" method is called from NrRepositoryModule.purgeTrash(String token) and
	 * NrFolderModule.purge(String token, String fldPath).
	 */
	public void purge(String uuid, boolean deleteBase) throws PathNotFoundException, AccessDeniedException,
			LockException, DatabaseException, IOException {
		log.debug("purgue({}, {})", uuid, deleteBase);
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			// Security Check
			NodeFolder nFld = (NodeFolder) session.load(NodeFolder.class, uuid);
			SecurityHelper.checkRead(nFld);
			SecurityHelper.checkDelete(nFld);
			
			purgeHelper(session, nFld, deleteBase);
			HibernateUtil.commit(tx);
			log.debug("purgue: void");
		} catch (PathNotFoundException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (AccessDeniedException e) {
			HibernateUtil.rollback(tx);
			throw e;
		} catch (IOException e) {
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
	 * Purge in depth helper.
	 */
	@SuppressWarnings("unchecked")
	private void purgeHelper(Session session, String parentUuid) throws PathNotFoundException, AccessDeniedException,
			LockException, IOException, DatabaseException, HibernateException {
		String qs = "from NodeFolder nf where nf.parent=:parent";
		Query q = session.createQuery(qs);
		q.setString("parent", parentUuid);
		List<NodeFolder> listFolders = q.list();
		
		for (NodeFolder nFld : listFolders) {
			purgeHelper(session, nFld, true);
		}
	}
	
	/**
	 * Purge in depth helper.
	 */
	private void purgeHelper(Session session, NodeFolder nFolder, boolean deleteBase) throws PathNotFoundException,
			AccessDeniedException, LockException, IOException, DatabaseException, HibernateException {
		String author = nFolder.getAuthor();
		
		// Security Check
		SecurityHelper.checkRead(nFolder);
		SecurityHelper.checkDelete(nFolder);
		
		// Delete children documents
		NodeDocumentDAO.getInstance().purgeHelper(session, nFolder.getUuid());
		
		// Delete children mails
		NodeMailDAO.getInstance().purgeHelper(session, nFolder.getUuid());
		
		// Delete children notes
		NodeNoteDAO.getInstance().purgeHelper(session, nFolder.getUuid());
		
		// Delete bookmarks
		BookmarkDAO.purgeBookmarksByNode(nFolder.getUuid());
		
		// Delete children folder
		purgeHelper(session, nFolder.getUuid());
		
		if (deleteBase) {
			String path = NodeBaseDAO.getInstance().getPathFromUuid(session, nFolder.getUuid());
			String user = PrincipalUtils.getUser();
			
			// Delete the node itself
			session.delete(nFolder);
			
			// Update user items size
			if (Config.USER_ITEM_CACHE) {
				UserItemsManager.decFolders(author, 1);
			}
			
			// Activity log
			UserActivity.log(user, "PURGE_FOLDER", nFolder.getUuid(), path, null);
		}
	}
	
	/**
	 * Force initialization of a proxy
	 */
	public void initialize(NodeFolder nFolder) {
		if (nFolder != null) {
			Hibernate.initialize(nFolder);
			Hibernate.initialize(nFolder.getKeywords());
			Hibernate.initialize(nFolder.getCategories());
			Hibernate.initialize(nFolder.getSubscriptors());
			Hibernate.initialize(nFolder.getUserPermissions());
			Hibernate.initialize(nFolder.getRolePermissions());
		}
	}
	
	/**
	 * Force initialization of a proxy
	 */
	private void initialize(List<NodeFolder> nFolderList) {
		for (NodeFolder nFolder : nFolderList) {
			initialize(nFolder);
		}
	}
}
