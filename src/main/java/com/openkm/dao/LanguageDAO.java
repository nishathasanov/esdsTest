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

package com.openkm.dao;

import java.util.List;
import java.util.Set;

import org.hibernate.CacheMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.core.DatabaseException;
import com.openkm.dao.bean.Language;
import com.openkm.dao.bean.Translation;

/**
 * LanguageDAO
 * 
 * @author jllort
 * 
 */
public class LanguageDAO {
	private static Logger log = LoggerFactory.getLogger(LanguageDAO.class);
	
	private LanguageDAO() {
	}
	
	/**
	 * Find translations by pk
	 */
	public static Language findByPk(String id) throws DatabaseException {
		log.debug("findByPk({})", id);
		Session session = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Language ret = (Language) session.load(Language.class, id);
			Hibernate.initialize(ret);
			log.debug("findByPk: {}", ret);
			return ret;
		} catch (HibernateException e) {
			throw new DatabaseException(e.getMessage(), e);
		} finally {
			HibernateUtil.close(session);
		}
	}
	
	/**
	 * Find all languages
	 */
	public static List<Language> findAll() throws DatabaseException {
		return findAll(CacheMode.NORMAL);
	}
	
	/**
	 * Refresh 2nd level cache
	 */
	public static void refresh() throws DatabaseException {
		findAll(CacheMode.REFRESH);
	}
	
	/**
	 * Find all languages
	 * 
	 * @param cacheMode Execute language query with the designed cache mode.
	 */
	@SuppressWarnings("unchecked")
	private static List<Language> findAll(CacheMode cacheMode) throws DatabaseException {
		log.debug("findAll({})", cacheMode);
		String qs = "from Language lg order by lg.name asc";
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			session.setCacheMode(cacheMode);
			Query q = session.createQuery(qs);
			List<Language> ret = q.list();
			log.debug("findAll: {}", ret);
			return ret;
		} catch (HibernateException e) {
			HibernateUtil.rollback(tx);
			throw new DatabaseException(e.getMessage(), e);
		} finally {
			HibernateUtil.close(session);
		}
	}
	
	/**
	 * Delete
	 */
	public static void delete(String id) throws DatabaseException {
		log.debug("delete({})", id);
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			Language lang = (Language) session.load(Language.class, id);
			session.delete(lang);
			HibernateUtil.commit(tx);
		} catch (HibernateException e) {
			HibernateUtil.rollback(tx);
			throw new DatabaseException(e.getMessage(), e);
		} finally {
			HibernateUtil.close(session);
		}
		
		log.debug("delete: void");
	}
	
	/**
	 * Update language in database
	 */
	public static void update(Language lang) throws DatabaseException {
		log.debug("update({})", lang);
		String qs = "select lg.imageContent, lg.imageMime from Language lg where lg.id=:id";
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			if (lang.getImageContent() == null || lang.getImageContent().length() == 0) {
				Query q = session.createQuery(qs);
				q.setParameter("id", lang.getId());
				Object[] data = (Object[]) q.setMaxResults(1).uniqueResult();
				lang.setImageContent((String) data[0]);
				lang.setImageMime((String) data[1]);
			}
			
			session.update(lang);
			HibernateUtil.commit(tx);
		} catch (HibernateException e) {
			HibernateUtil.rollback(tx);
			throw new DatabaseException(e.getMessage(), e);
		} finally {
			HibernateUtil.close(session);
		}
		log.debug("update: void");
	}
	
	/**
	 * Create language in database
	 */
	public static void create(Language lang) throws DatabaseException {
		log.debug("create({})", lang);
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			session.save(lang);
			HibernateUtil.commit(tx);
		} catch (HibernateException e) {
			HibernateUtil.rollback(tx);
			throw new DatabaseException(e.getMessage(), e);
		} finally {
			HibernateUtil.close(session);
		}
		log.debug("create: void");
	}
	
	/**
	 * Get all language translations
	 */
	public static Set<Translation> findTransAll(String langId) throws DatabaseException {
		log.debug("findTransAll({})", langId);
		Session session = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Language lang = (Language) session.load(Language.class, langId);
			Set<Translation> trans = lang.getTranslations();
			log.debug("findTransAll: {}", trans);
			return trans;
		} catch (HibernateException e) {
			throw new DatabaseException(e.getMessage(), e);
		} finally {
			HibernateUtil.close(session);
		}
	}
	
	/**
	 * Get translation
	 */
	public static String getTranslation(String lang, String module, String key) throws DatabaseException {
		log.debug("getTranslation({}, {}, {})", new Object[] { module, lang, key });
		String qs = "select tr.text from Translation tr where tr.translationId.key=:key "
				+ "and tr.translationId.language=:lang and tr.translationId.module=:module";
		Session session = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Query q = session.createQuery(qs);
			q.setString("key", key);
			q.setString("module", module);
			q.setString("lang", lang);
			String ret = (String) q.setMaxResults(1).uniqueResult();
			
			log.debug("getTranslation: {}", ret);
			return ret;
		} catch (HibernateException e) {
			throw new DatabaseException(e.getMessage(), e);
		} finally {
			HibernateUtil.close(session);
		}
	}
}