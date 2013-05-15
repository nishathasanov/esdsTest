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

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.reader.ReaderProvider;
import org.hibernate.search.store.DirectoryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.bean.Permission;
import com.openkm.bean.nr.NodeQueryResult;
import com.openkm.bean.nr.NodeResultSet;
import com.openkm.core.Config;
import com.openkm.core.DatabaseException;
import com.openkm.core.ParseException;
import com.openkm.core.PathNotFoundException;
import com.openkm.dao.bean.NodeBase;
import com.openkm.dao.bean.NodeDocument;
import com.openkm.dao.bean.NodeFolder;
import com.openkm.dao.bean.NodeMail;
import com.openkm.module.db.stuff.DbAccessManager;
import com.openkm.module.db.stuff.SecurityHelper;

/**
 * Search results are filtered by com.openkm.module.db.stuff.ReadAccessFilterFactory, which limit the results only for
 * those which have read access.
 * 
 * @author pavila
 */
public class SearchDAO {
	private static Logger log = LoggerFactory.getLogger(SearchDAO.class);
	private static SearchDAO single = new SearchDAO();
	private static final int MAX_FRAGMENT_LEN = 256;
	public static final String SEARCH_LUCENE = "lucene";
	public static final String SEARCH_ACCESS_MANAGER_MORE = "am_more";
	public static final String SEARCH_ACCESS_MANAGER_WINDOW = "am_window";
	public static final String SEARCH_ACCESS_MANAGER_LIMITED = "am_limited";
	public static Analyzer analyzer = null;
	
	static {
		try {
			Class<?> Analyzer = Class.forName(Config.HIBERNATE_SEARCH_ANALYZER);
			
			if (Analyzer.getCanonicalName().startsWith("org.apache.lucene.analysis")) {
				Constructor<?> constructor = Analyzer.getConstructor(Config.LUCENE_VERSION.getClass());
				analyzer = (Analyzer) constructor.newInstance(Config.LUCENE_VERSION);
			} else {
				analyzer = (Analyzer) Analyzer.newInstance();
			}
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			analyzer = new StandardAnalyzer(Config.LUCENE_VERSION);
		}
		
		log.debug("Analyzer: {}", analyzer.getClass());
	}
	
	private SearchDAO() {
	}
	
	public static SearchDAO getInstance() {
		return single;
	}
	
	/**
	 * Search by query
	 */
	public NodeResultSet findByQuery(Query query, int offset, int limit) throws ParseException, DatabaseException {
		log.info("findByQuery({}, {}, {})", new Object[] { query, offset, limit });
		FullTextSession ftSession = null;
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			ftSession = Search.getFullTextSession(session);
			tx = ftSession.beginTransaction();
			
			NodeResultSet result = null;
			
			if (SEARCH_LUCENE.equals(Config.SECURITY_SEARCH_EVALUATION)) {
				result = runQueryLucene(ftSession, query, offset, limit);
			} else if (SEARCH_ACCESS_MANAGER_MORE.equals(Config.SECURITY_SEARCH_EVALUATION)) {
				result = runQueryAccessManagerMore(ftSession, query, offset, limit);
			} else if (SEARCH_ACCESS_MANAGER_WINDOW.equals(Config.SECURITY_SEARCH_EVALUATION)) {
				result = runQueryAccessManagerWindow(ftSession, query, offset, limit);
			} else if (SEARCH_ACCESS_MANAGER_LIMITED.equals(Config.SECURITY_SEARCH_EVALUATION)) {
				result = runQueryAccessManagerLimited(ftSession, query, offset, limit);
			}
			
			HibernateUtil.commit(tx);
			log.debug("findByQuery: {}", result);
			return result;
		} catch (IOException e) {
			HibernateUtil.rollback(tx);
			throw new DatabaseException(e.getMessage(), e);
		} catch (InvalidTokenOffsetsException e) {
			HibernateUtil.rollback(tx);
			throw new DatabaseException(e.getMessage(), e);
		} catch (HibernateException e) {
			HibernateUtil.rollback(tx);
			throw new DatabaseException(e.getMessage(), e);
		} finally {
			HibernateUtil.close(ftSession);
			HibernateUtil.close(session);
		}
	}
	
	/**
	 * Search by simple query
	 */
	public NodeResultSet findBySimpleQuery(String expression, int offset, int limit) throws ParseException,
			DatabaseException {
		log.info("findBySimpleQuery({}, {}, {})", new Object[] { expression, offset, limit });
		FullTextSession ftSession = null;
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			ftSession = Search.getFullTextSession(session);
			tx = ftSession.beginTransaction();
			
			QueryParser parser = new QueryParser(Config.LUCENE_VERSION, NodeDocument.TEXT_FIELD, analyzer);
			Query query = parser.parse(expression);
			NodeResultSet result = null;
			log.info("findBySimpleQuery.query: {}", query);
			
			if (SEARCH_LUCENE.equals(Config.SECURITY_SEARCH_EVALUATION)) {
				result = runQueryLucene(ftSession, query, offset, limit);
			} else if (SEARCH_ACCESS_MANAGER_MORE.equals(Config.SECURITY_SEARCH_EVALUATION)) {
				result = runQueryAccessManagerMore(ftSession, query, offset, limit);
			} else if (SEARCH_ACCESS_MANAGER_WINDOW.equals(Config.SECURITY_SEARCH_EVALUATION)) {
				result = runQueryAccessManagerWindow(ftSession, query, offset, limit);
			} else if (SEARCH_ACCESS_MANAGER_LIMITED.equals(Config.SECURITY_SEARCH_EVALUATION)) {
				result = runQueryAccessManagerLimited(ftSession, query, offset, limit);
			}
			
			HibernateUtil.commit(tx);
			log.debug("findBySimpleQuery: {}", result);
			return result;
		} catch (org.apache.lucene.queryParser.ParseException e) {
			HibernateUtil.rollback(tx);
			throw new ParseException(e.getMessage(), e);
		} catch (IOException e) {
			HibernateUtil.rollback(tx);
			throw new DatabaseException(e.getMessage(), e);
		} catch (InvalidTokenOffsetsException e) {
			HibernateUtil.rollback(tx);
			throw new DatabaseException(e.getMessage(), e);
		} catch (HibernateException e) {
			HibernateUtil.rollback(tx);
			throw new DatabaseException(e.getMessage(), e);
		} finally {
			HibernateUtil.close(ftSession);
			HibernateUtil.close(session);
		}
	}
	
	/**
	 * Security is evaluated by Lucene, so query result are already pruned. This means that every node
	 * should have its security (user and role) info stored in Lucene. This provides very quick search
	 * but security modifications need to be recursively applied to reach every document node in the
	 * repository. This may take several hours (or days) is big repositories.
	 */
	@SuppressWarnings("unchecked")
	private NodeResultSet runQueryLucene(FullTextSession ftSession, Query query, int offset, int limit)
			throws IOException, InvalidTokenOffsetsException, HibernateException {
		log.debug("runQueryLucene({}, {}, {}, {})", new Object[] { ftSession, query, offset, limit });
		List<NodeQueryResult> results = new ArrayList<NodeQueryResult>();
		NodeResultSet result = new NodeResultSet();
		FullTextQuery ftq = ftSession.createFullTextQuery(query, NodeDocument.class, NodeFolder.class, NodeMail.class);
		ftq.setProjection(FullTextQuery.SCORE, FullTextQuery.THIS);
		ftq.enableFullTextFilter("readAccess");
		QueryScorer scorer = new QueryScorer(query, NodeDocument.TEXT_FIELD);
		
		// Set limits
		ftq.setFirstResult(offset);
		ftq.setMaxResults(limit);
		
		// Highlight using a CSS style
		SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<span class='highlight'>", "</span>");
		Highlighter highlighter = new Highlighter(formatter, scorer);
		highlighter.setTextFragmenter(new SimpleSpanFragmenter(scorer, MAX_FRAGMENT_LEN));
		
		for (Iterator<Object[]> it = ftq.iterate(); it.hasNext();) {
			Object[] qRes = it.next();
			Float score = (Float) qRes[0];
			NodeBase nBase = (NodeBase) qRes[1];
			
			// Add result
			addResult(ftSession, results, highlighter, score, nBase);
		}
		
		result.setTotal(ftq.getResultSize());
		result.setResults(results);
		log.debug("runQueryLucene: {}", result);
		return result;
	}
	
	/**
	 * Security is not evaluate in Lucene but by AccessManager. This means that Lucene will return all the
	 * matched documents and this list need further prune by checking the READ permission in the AccessManager.
	 * If the returned document list is very big, maybe lots of documents will be pruned because the user has
	 * no read access and this would be a time consuming task.
	 * 
	 * This method will read and check document from the Lucene query result until reach a given offset. After
	 * that will add all the given document which the user have read access until the limit is reached. After
	 * that will check if there is another document more who the user can read.
	 */
	@SuppressWarnings("unchecked")
	private NodeResultSet runQueryAccessManagerMore(FullTextSession ftSession, Query query, int offset, int limit)
			throws IOException, InvalidTokenOffsetsException, DatabaseException, HibernateException {
		log.debug("runQueryAccessManagerMore({}, {}, {}, {})", new Object[] { ftSession, query, offset, limit });
		List<NodeQueryResult> results = new ArrayList<NodeQueryResult>();
		NodeResultSet result = new NodeResultSet();
		FullTextQuery ftq = ftSession.createFullTextQuery(query, NodeDocument.class, NodeFolder.class, NodeMail.class);
		ftq.setProjection(FullTextQuery.SCORE, FullTextQuery.THIS);
		ftq.enableFullTextFilter("readAccess");
		QueryScorer scorer = new QueryScorer(query, NodeDocument.TEXT_FIELD);
		int count = 0;
		
		// Highlight using a CSS style
		SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<span class='highlight'>", "</span>");
		Highlighter highlighter = new Highlighter(formatter, scorer);
		highlighter.setTextFragmenter(new SimpleSpanFragmenter(scorer, MAX_FRAGMENT_LEN));
		
		// Set limits
		Iterator<Object[]> it = ftq.iterate();
		DbAccessManager am = SecurityHelper.getAccessManager();
		
		// Bypass offset
		while (it.hasNext() && count < offset) {
			Object[] qRes = it.next();
			NodeBase nBase = (NodeBase) qRes[1];
			
			if (am.isGranted(nBase, Permission.READ)) {
				count++;
			}
		}
		
		// Read limit results
		while (it.hasNext() && results.size() < limit) {
			Object[] qRes = it.next();
			Float score = (Float) qRes[0];
			NodeBase nBase = (NodeBase) qRes[1];
			
			if (am.isGranted(nBase, Permission.READ)) {
				// Add result
				addResult(ftSession, results, highlighter, score, nBase);
			}
		}
		
		// Check if pending results
		count = results.size() + offset;
		
		while (it.hasNext() && count < offset + limit + 1) {
			Object[] qRes = it.next();
			NodeBase nBase = (NodeBase) qRes[1];
			
			if (am.isGranted(nBase, Permission.READ)) {
				count++;
			}
		}
		
		result.setTotal(count);
		result.setResults(results);
		log.debug("runQueryAccessManagerMore: {}", result);
		return result;
	}
	
	/**
	 * Security is not evaluate in Lucene but by AccessManager. This means that Lucene will return all the
	 * matched documents and this list need further prune by checking the READ permission in the AccessManager.
	 * If the returned document list is very big, maybe lots of documents will be pruned because the user has
	 * no read access and this would be a time consuming task.
	 * 
	 * This method will read and check document from the Lucene query result until reach a given offset. After
	 * that will add all the given document which the user have read access until the limit is reached. After
	 * that will check if there are more documents (2 * limit) the user can read.
	 */
	@SuppressWarnings("unchecked")
	private NodeResultSet runQueryAccessManagerWindow(FullTextSession ftSession, Query query, int offset, int limit)
			throws IOException, InvalidTokenOffsetsException, DatabaseException, HibernateException {
		log.debug("runQueryAccessManagerWindow({}, {}, {}, {})", new Object[] { ftSession, query, offset, limit });
		List<NodeQueryResult> results = new ArrayList<NodeQueryResult>();
		NodeResultSet result = new NodeResultSet();
		FullTextQuery ftq = ftSession.createFullTextQuery(query, NodeDocument.class, NodeFolder.class, NodeMail.class);
		ftq.setProjection(FullTextQuery.SCORE, FullTextQuery.THIS);
		ftq.enableFullTextFilter("readAccess");
		QueryScorer scorer = new QueryScorer(query, NodeDocument.TEXT_FIELD);
		int count = 0;
		
		// Highlight using a CSS style
		SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<span class='highlight'>", "</span>");
		Highlighter highlighter = new Highlighter(formatter, scorer);
		highlighter.setTextFragmenter(new SimpleSpanFragmenter(scorer, MAX_FRAGMENT_LEN));
		
		// Set limits
		Iterator<Object[]> it = ftq.iterate();
		DbAccessManager am = SecurityHelper.getAccessManager();
		
		// Bypass offset
		while (it.hasNext() && count < offset) {
			Object[] qRes = it.next();
			NodeBase nBase = (NodeBase) qRes[1];
			
			if (am.isGranted(nBase, Permission.READ)) {
				count++;
			}
		}
		
		// Read limit results
		while (it.hasNext() && results.size() < limit) {
			Object[] qRes = it.next();
			Float score = (Float) qRes[0];
			NodeBase nBase = (NodeBase) qRes[1];
			
			if (am.isGranted(nBase, Permission.READ)) {
				// Add result
				addResult(ftSession, results, highlighter, score, nBase);
			}
		}
		
		// Check if pending results
		count = results.size() + offset;
		
		while (it.hasNext() && count < offset + limit * 2) {
			Object[] qRes = it.next();
			NodeBase nBase = (NodeBase) qRes[1];
			
			if (am.isGranted(nBase, Permission.READ)) {
				count++;
			}
		}
		
		result.setTotal(count);
		result.setResults(results);
		log.debug("runQueryAccessManagerWindow: {}", result);
		return result;
	}
	
	/**
	 * Security is not evaluate in Lucene but by AccessManager. This means that Lucene will return all the
	 * matched documents and this list need further prune by checking the READ permission in the AccessManager.
	 * If the returned document list is very big, maybe lots of documents will be pruned because the user has
	 * no read access and this would be a time consuming task.
	 * 
	 * This method will read and check document from the Lucene query result until reach a given offset. After
	 * that will add all the given document which the user have read access until the limit is reached. After
	 * that will check if there are more documents (MAX_SEARCH_RESULTS) the user can read.
	 */
	@SuppressWarnings("unchecked")
	private NodeResultSet runQueryAccessManagerLimited(FullTextSession ftSession, Query query, int offset, int limit)
			throws IOException, InvalidTokenOffsetsException, DatabaseException, HibernateException {
		log.debug("runQueryAccessManagerLimited({}, {}, {}, {})", new Object[] { ftSession, query, offset, limit });
		List<NodeQueryResult> results = new ArrayList<NodeQueryResult>();
		NodeResultSet result = new NodeResultSet();
		FullTextQuery ftq = ftSession.createFullTextQuery(query, NodeDocument.class, NodeFolder.class, NodeMail.class);
		ftq.setProjection(FullTextQuery.SCORE, FullTextQuery.THIS);
		ftq.enableFullTextFilter("readAccess");
		QueryScorer scorer = new QueryScorer(query, NodeDocument.TEXT_FIELD);
		int count = 0;
		
		// Highlight using a CSS style
		SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<span class='highlight'>", "</span>");
		Highlighter highlighter = new Highlighter(formatter, scorer);
		highlighter.setTextFragmenter(new SimpleSpanFragmenter(scorer, MAX_FRAGMENT_LEN));
		
		// Set limits
		Iterator<Object[]> it = ftq.iterate();
		DbAccessManager am = SecurityHelper.getAccessManager();
		
		// Bypass offset
		while (it.hasNext() && count < offset) {
			Object[] qRes = it.next();
			NodeBase nBase = (NodeBase) qRes[1];
			
			if (am.isGranted(nBase, Permission.READ)) {
				count++;
			}
		}
		
		// Read limit results
		while (it.hasNext() && results.size() < limit) {
			Object[] qRes = it.next();
			Float score = (Float) qRes[0];
			NodeBase nBase = (NodeBase) qRes[1];
			
			if (am.isGranted(nBase, Permission.READ)) {
				// Add result
				addResult(ftSession, results, highlighter, score, nBase);
			}
		}
		
		// Check if pending results
		count = results.size() + offset;
		
		while (it.hasNext() && count < Config.MAX_SEARCH_RESULTS) {
			Object[] qRes = it.next();
			NodeBase nBase = (NodeBase) qRes[1];
			
			if (am.isGranted(nBase, Permission.READ)) {
				count++;
			}
		}
		
		result.setTotal(count);
		result.setResults(results);
		log.debug("Size: {}", results.size());
		log.debug("runQueryAccessManagerLimited: {}", result);
		return result;
	}
	
	/**
	 * Add result
	 */
	private void addResult(FullTextSession ftSession, List<NodeQueryResult> results,
			Highlighter highlighter, Float score, NodeBase nBase)
			throws IOException, InvalidTokenOffsetsException {
		NodeQueryResult qr = new NodeQueryResult();
		NodeDocument nDocument = null;
		NodeMail nMail = null;
		String excerpt = null;
		
		if (nBase instanceof NodeDocument) {
			nDocument = (NodeDocument) nBase;
			
			if (NodeMailDAO.getInstance().isMail(ftSession, nDocument.getParent())) {
				log.debug("NODE DOCUMENT - ATTACHMENT");
				qr.setAttachment(nDocument);
			} else {
				log.debug("NODE DOCUMENT");
				qr.setDocument(nDocument);
			}
		} else if (nBase instanceof NodeFolder) {
			log.debug("NODE FOLDER");
			NodeFolder nFld = (NodeFolder) nBase;
			qr.setFolder(nFld);
		} else if (nBase instanceof NodeMail) {
			log.debug("NODE MAIL");
			nMail = (NodeMail) nBase;
			qr.setMail(nMail);
		} else {
			log.warn("NODE UNKNOWN");
		}
		
		if (nDocument != null && nDocument.getText() != null) {
			excerpt = highlighter.getBestFragment(analyzer, NodeDocument.TEXT_FIELD, nDocument.getText());
		} else if (nMail != null && nMail.getContent() != null) {
			excerpt = highlighter.getBestFragment(analyzer, NodeMail.CONTENT_FIELD, nMail.getContent());
		}
		
		log.debug("Result: SCORE({}), EXCERPT({}), DOCUMENT({})", new Object[] { score, excerpt, nBase });
		qr.setScore(score);
		qr.setExcerpt(excerpt);
		
		if (qr.getDocument() != null) {
			NodeDocumentDAO.getInstance().initialize(qr.getDocument(), false);
			results.add(qr);
		} else if (qr.getFolder() != null) {
			NodeFolderDAO.getInstance().initialize(qr.getFolder());
			results.add(qr);
		} else if (qr.getMail() != null) {
			NodeMailDAO.getInstance().initialize(qr.getMail());
			results.add(qr);
		} else if (qr.getAttachment() != null) {
			NodeDocumentDAO.getInstance().initialize(qr.getAttachment(), false);
			results.add(qr);
		}
	}
	
	/**
	 * Find by parent in depth
	 * 
	 * TODO This cache should be for every user (no pass through access manager) and cleaned
	 * after a create, move or copy folder operation.
	 */
	public List<String> findFoldersInDepth(String parentUuid) throws PathNotFoundException, DatabaseException {
		log.debug("findFoldersInDepth({})", parentUuid);
		List<String> ret = null;
		
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			// Security Check
			NodeBase parentNode = (NodeBase) session.load(NodeBase.class, parentUuid);
			SecurityHelper.checkRead(parentNode);
			
			ret = findFoldersInDepthHelper(session, parentUuid);
			HibernateUtil.commit(tx);
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
		
		log.debug("findFoldersInDepth: {}", ret);
		return ret;
	}
	
	/**
	 * Find by parent in depth helper
	 */
	@SuppressWarnings("unchecked")
	private List<String> findFoldersInDepthHelper(Session session, String parentUuid) throws HibernateException,
			DatabaseException {
		log.debug("findFoldersInDepthHelper({}, {})", "session", parentUuid);
		List<String> ret = new ArrayList<String>();
		String qs = "from NodeFolder nf where nf.parent=:parent";
		org.hibernate.Query q = session.createQuery(qs);
		q.setString("parent", parentUuid);
		List<NodeFolder> results = q.list();
		
		// Security Check
		DbAccessManager am = SecurityHelper.getAccessManager();
		
		for (Iterator<NodeFolder> it = results.iterator(); it.hasNext();) {
			NodeFolder node = it.next();
			
			if (am.isGranted(node, Permission.READ)) {
				ret.add(node.getUuid());
				ret.addAll(findFoldersInDepthHelper(session, node.getUuid()));
			}
		}
		
		log.debug("findFoldersInDepthHelper: {}", ret);
		return ret;
	}
	
	/**
	 * Return a list of similar documents.
	 */
	public NodeResultSet moreLikeThis(String uuid, int maxResults) throws DatabaseException, PathNotFoundException {
		log.info("moreLikeThis({}, {})", new Object[] { uuid, maxResults });
		String[] moreLikeFields = new String[] { "text" };
		FullTextSession ftSession = null;
		Session session = null;
		Transaction tx = null;
		
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			ftSession = Search.getFullTextSession(session);
			tx = ftSession.beginTransaction();
			NodeResultSet result = new NodeResultSet();
			
			MoreLikeThis mlt = new MoreLikeThis(getReader(ftSession, NodeDocument.class));
			mlt.setFieldNames(moreLikeFields);
			mlt.setMaxQueryTerms(10);
			mlt.setMinDocFreq(1);
			mlt.setAnalyzer(analyzer);
			mlt.setMaxWordLen(8);
			mlt.setMinWordLen(7);
			mlt.setMinTermFreq(1);
			
			String str = NodeDocumentDAO.getInstance().getExtractedText(session, uuid);
			
			if (str != null && !str.isEmpty()) {
				StringReader sr = new StringReader(str);
				Query likeThisQuery = mlt.like(sr);
				
				BooleanQuery query = new BooleanQuery();
				query.add(likeThisQuery, Occur.SHOULD);
				query.add(new TermQuery(new Term("uuid", uuid)), Occur.MUST_NOT);
				log.info("moreLikeThis.Query: {}", query);
				
				if (SEARCH_LUCENE.equals(Config.SECURITY_SEARCH_EVALUATION)) {
					result = runQueryLucene(ftSession, query, 0, maxResults);
				} else if (SEARCH_ACCESS_MANAGER_MORE.equals(Config.SECURITY_SEARCH_EVALUATION)) {
					result = runQueryAccessManagerMore(ftSession, query, 0, maxResults);
				} else if (SEARCH_ACCESS_MANAGER_WINDOW.equals(Config.SECURITY_SEARCH_EVALUATION)) {
					result = runQueryAccessManagerWindow(ftSession, query, 0, maxResults);
				} else if (SEARCH_ACCESS_MANAGER_LIMITED.equals(Config.SECURITY_SEARCH_EVALUATION)) {
					result = runQueryAccessManagerLimited(ftSession, query, 0, maxResults);
				}
			} else {
				log.warn("Document has not text extracted: {}", uuid);
			}
			
			HibernateUtil.commit(tx);
			log.debug("moreLikeThis: {}", result);
			return result;
		} catch (IOException e) {
			HibernateUtil.rollback(tx);
			throw new DatabaseException(e.getMessage(), e);
		} catch (InvalidTokenOffsetsException e) {
			HibernateUtil.rollback(tx);
			throw new DatabaseException(e.getMessage(), e);
		} catch (HibernateException e) {
			HibernateUtil.rollback(tx);
			throw new DatabaseException(e.getMessage(), e);
		} finally {
			HibernateUtil.close(ftSession);
			HibernateUtil.close(session);
		}
	}
	
	/**
	 * Get Lucene index reader.
	 */
	@SuppressWarnings("rawtypes")
	private IndexReader getReader(FullTextSession session, Class entity) {
		SearchFactory searchFactory = session.getSearchFactory();
		DirectoryProvider provider = searchFactory.getDirectoryProviders(entity)[0];
		ReaderProvider readerProvider = searchFactory.getReaderProvider();
		return readerProvider.openReader(provider);
	}
}
