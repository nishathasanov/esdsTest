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
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.core.AccessDeniedException;
import com.openkm.core.Config;
import com.openkm.core.DatabaseException;
import com.openkm.core.ItemExistsException;
import com.openkm.core.LockException;
import com.openkm.core.PathNotFoundException;
import com.openkm.dao.bean.NodeBase;
import com.openkm.dao.bean.NodeFolder;
import com.openkm.dao.bean.NodeMail;
import com.openkm.module.db.stuff.SecurityHelper;
import com.openkm.spring.PrincipalUtils;
import com.openkm.util.UserActivity;

public class NodeMailDAO {
	private static Logger log = LoggerFactory.getLogger(NodeMailDAO.class);
	private static NodeMailDAO single = new NodeMailDAO();
	
	private NodeMailDAO() {
	}
	
	public static NodeMailDAO getInstance() {
		return single;
	}
	
	/**
	 * Create node
	 */
	public void create(NodeMail nMail) throws PathNotFoundException, AccessDeniedException, ItemExistsException,
			DatabaseException {
		log.debug("create({})", nMail);
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			// Security Check
			NodeBase parentNode = (NodeBase) session.load(NodeBase.class, nMail.getParent());
			SecurityHelper.checkRead(parentNode);
			SecurityHelper.checkWrite(parentNode);
			
			// Check for same mail name in same parent
			NodeBaseDAO.getInstance().checkItemExistence(session, nMail.getParent(), nMail.getName());
			
			session.save(nMail);
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
	public List<NodeMail> findByParent(String parentUuid) throws PathNotFoundException, DatabaseException {
		log.debug("findByParent({})", parentUuid);
		String qs = "from NodeMail nm where nm.parent=:parent order by nm.name";
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
			List<NodeMail> ret = q.list();
			
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
	 * Find by path
	 */
	public NodeMail findByPk(String uuid) throws PathNotFoundException, DatabaseException {
		log.debug("findByPk({})", uuid);
		String qs = "from NodeMail nm where nm.uuid=:uuid";
		Session session = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Query q = session.createQuery(qs);
			q.setString("uuid", uuid);
			NodeMail nMail = (NodeMail) q.setMaxResults(1).uniqueResult();
			
			if (nMail == null) {
				throw new PathNotFoundException(uuid);
			}
			
			// Security Check
			SecurityHelper.checkRead(nMail);
			
			initialize(nMail);
			log.debug("findByPk: {}", nMail);
			return nMail;
		} catch (HibernateException e) {
			throw new DatabaseException(e.getMessage(), e);
		} finally {
			HibernateUtil.close(session);
		}
	}
	
	/**
	 * Check if this uuid represents a mail node.
	 * 
	 * Used in SearchDAO, and should exposed in other method should make Security Check
	 */
	public boolean isMail(FullTextSession ftSession, String uuid) throws HibernateException {
		log.debug("isMail({}, {})", ftSession, uuid);
		boolean ret = ftSession.get(NodeMail.class, uuid) instanceof NodeMail;
		log.debug("isMail: {}", ret);
		return ret;
	}
	
	/**
	 * Search nodes by category
	 */
	@SuppressWarnings("unchecked")
	public List<NodeMail> findByCategory(String catUuid) throws PathNotFoundException, DatabaseException {
		log.debug("findByCategory({})", catUuid);
		final String qs = "from NodeMail nm where :category in elements(nm.categories) order by nm.name";
		List<NodeMail> ret = new ArrayList<NodeMail>();
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
	public List<NodeMail> findByKeyword(String keyword) throws DatabaseException {
		log.debug("findByKeyword({})", keyword);
		final String qs = "from NodeMail nm where :keyword in elements(nm.keywords) order by nm.name";
		List<NodeMail> ret = new ArrayList<NodeMail>();
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
	public List<NodeMail> findByPropertyValue(String group, String property, String value) throws DatabaseException {
		log.debug("findByPropertyValue({}, {}, {})", property, value);
		String qs = "select nb from NodeMail nb join nb.properties nbp where nbp.group=:group and nbp.name=:property and nbp.value like :value";
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			Query q = session.createQuery(qs);
			q.setString("group", group);
			q.setString("property", property);
			q.setString("value", "%" + value + "%");
			List<NodeMail> ret = q.list();
			
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
		String qs = "from NodeMail nm where nm.parent=:parent";
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
	 * Rename mail
	 */
	public NodeMail rename(String uuid, String newName) throws PathNotFoundException, AccessDeniedException,
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
			NodeMail nMail = (NodeMail) session.load(NodeMail.class, uuid);
			SecurityHelper.checkRead(nMail);
			SecurityHelper.checkWrite(nMail);
			
			// Check for same folder name in same parent
			NodeBaseDAO.getInstance().checkItemExistence(session, nMail.getParent(), newName);
			
			nMail.setName(newName);
			session.update(nMail);
			initialize(nMail);
			HibernateUtil.commit(tx);
			log.debug("rename: {}", nMail);
			return nMail;
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
	 * Move mail
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
			NodeMail nMail = (NodeMail) session.load(NodeMail.class, uuid);
			SecurityHelper.checkRead(nMail);
			SecurityHelper.checkWrite(nMail);
			
			// Check for same folder name in same parent
			NodeBaseDAO.getInstance().checkItemExistence(session, dstUuid, nMail.getName());
			
			// Check if context changes
			if (!nDstFld.getContext().equals(nMail.getContext())) {
				nMail.setContext(nDstFld.getContext());
				
				// Need recursive context changes
				moveHelper(session, uuid, nDstFld.getContext());
			}
			
			nMail.setParent(dstUuid);
			session.update(nMail);
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
	 * Delete mail
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
			NodeMail nMail = (NodeMail) session.load(NodeMail.class, uuid);
			SecurityHelper.checkRead(nMail);
			SecurityHelper.checkWrite(nMail);
			
			// Test if already exists a mail with the same name in the trash
			String testName = name;
			
			for (int i=1; NodeBaseDAO.getInstance().testItemExistence(session, trashUuid, testName); i++) {
				// log.info("Trying with: {}", testName);
				testName = name + " (" + i + ")";
			}
			
			// Need recursive context changes
			moveHelper(session, uuid, nTrashFld.getContext());
			
			nMail.setContext(nTrashFld.getContext());
			nMail.setParent(trashUuid);
			nMail.setName(testName);
			session.update(nMail);
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
		}
	}
	
	/**
	 * Purge in depth
	 */
	public void purge(String uuid) throws PathNotFoundException, AccessDeniedException, LockException,
			DatabaseException, IOException {
		log.debug("purge({})", uuid);
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			// Security Check
			NodeMail nMail = (NodeMail) session.load(NodeMail.class, uuid);
			SecurityHelper.checkRead(nMail);
			SecurityHelper.checkDelete(nMail);
			
			purgeHelper(session, nMail);
			HibernateUtil.commit(tx);
			log.debug("purge: void");
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
	 * Purge in depth helper
	 */
	@SuppressWarnings("unchecked")
	public void purgeHelper(Session session, String parentUuid) throws PathNotFoundException, AccessDeniedException,
			LockException, IOException, DatabaseException, HibernateException {
		String qs = "from NodeMail nm where nm.parent=:parent";
		Query q = session.createQuery(qs);
		q.setString("parent", parentUuid);
		List<NodeMail> listMails = q.list();
		
		for (NodeMail nMail : listMails) {
			purgeHelper(session, nMail);
		}
	}
	
	/**
	 * Purge in depth helper
	 * 
	 * @see com.openkm.dao.NodeFolderDAO.purgeHelper(Session, NodeFolder, boolean)
	 */
	private void purgeHelper(Session session, NodeMail nMail) throws PathNotFoundException, AccessDeniedException,
			LockException, IOException, DatabaseException, HibernateException {
		String path = NodeBaseDAO.getInstance().getPathFromUuid(session, nMail.getUuid());
		String user = PrincipalUtils.getUser();
		
		// Security Check
		SecurityHelper.checkRead(nMail);
		SecurityHelper.checkDelete(nMail);
		
		// Delete children documents
		NodeDocumentDAO.getInstance().purgeHelper(session, nMail.getUuid());
		
		// Delete children notes
		NodeNoteDAO.getInstance().purgeHelper(session, nMail.getUuid());
		
		// Delete bookmarks
		BookmarkDAO.purgeBookmarksByNode(nMail.getUuid());
		
		// Delete the node itself
		session.delete(nMail);
		
		// Activity log
		UserActivity.log(user, "PURGE_MAIL", nMail.getUuid(), path, null);
	}
	
	/**
	 * Force initialization of a proxy
	 */
	public void initialize(NodeMail nMail) {
		if (nMail != null) {
			Hibernate.initialize(nMail);
			Hibernate.initialize(nMail.getTo());
			Hibernate.initialize(nMail.getCc());
			Hibernate.initialize(nMail.getBcc());
			Hibernate.initialize(nMail.getReply());
			Hibernate.initialize(nMail.getKeywords());
			Hibernate.initialize(nMail.getCategories());
			Hibernate.initialize(nMail.getSubscriptors());
			Hibernate.initialize(nMail.getUserPermissions());
			Hibernate.initialize(nMail.getRolePermissions());
		}
	}
	
	/**
	 * Force initialization of a proxy
	 */
	private void initialize(List<NodeMail> nMailList) {
		for (NodeMail nMail : nMailList) {
			initialize(nMail);
		}
	}
}
