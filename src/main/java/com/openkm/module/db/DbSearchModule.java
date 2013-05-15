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

package com.openkm.module.db;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.WildcardQuery;
import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import com.openkm.bean.Document;
import com.openkm.bean.Folder;
import com.openkm.bean.Mail;
import com.openkm.bean.PropertyGroup;
import com.openkm.bean.QueryResult;
import com.openkm.bean.Repository;
import com.openkm.bean.ResultSet;
import com.openkm.bean.form.FormElement;
import com.openkm.bean.form.Input;
import com.openkm.bean.form.Select;
import com.openkm.bean.form.TextArea;
import com.openkm.bean.nr.NodeQueryResult;
import com.openkm.bean.nr.NodeResultSet;
import com.openkm.cache.UserNodeKeywordsManager;
import com.openkm.core.AccessDeniedException;
import com.openkm.core.Config;
import com.openkm.core.DatabaseException;
import com.openkm.core.ParseException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.RepositoryException;
import com.openkm.dao.DashboardDAO;
import com.openkm.dao.HibernateUtil;
import com.openkm.dao.NodeBaseDAO;
import com.openkm.dao.NodeDocumentDAO;
import com.openkm.dao.NodeFolderDAO;
import com.openkm.dao.NodeMailDAO;
import com.openkm.dao.QueryParamsDAO;
import com.openkm.dao.SearchDAO;
import com.openkm.dao.bean.NodeDocument;
import com.openkm.dao.bean.NodeFolder;
import com.openkm.dao.bean.NodeMail;
import com.openkm.dao.bean.QueryParams;
import com.openkm.dao.bean.cache.UserNodeKeywords;
import com.openkm.module.SearchModule;
import com.openkm.module.db.base.BaseDocumentModule;
import com.openkm.module.db.base.BaseFolderModule;
import com.openkm.module.db.base.BaseMailModule;
import com.openkm.spring.PrincipalUtils;
import com.openkm.util.FormUtils;
import com.openkm.util.ISO8601;
import com.openkm.util.PathUtils;
import com.openkm.util.UserActivity;

public class DbSearchModule implements SearchModule {
	private static Logger log = LoggerFactory.getLogger(DbSearchModule.class);
	private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("yyyyMMdd");
	
	@Override
	public List<QueryResult> findByContent(String token, String expression) throws IOException, ParseException,
			RepositoryException, DatabaseException {
		log.debug("findByContent({}, {})", token, expression);
		QueryParams params = new QueryParams();
		params.setContent(expression);
		List<QueryResult> ret = find(token, params);
		log.debug("findByContent: {}", ret);
		return ret;
	}
	
	@Override
	public List<QueryResult> findByName(String token, String expression) throws IOException, ParseException,
			RepositoryException, DatabaseException {
		log.debug("findByName({}, {})", token, expression);
		QueryParams params = new QueryParams();
		params.setName(expression);
		List<QueryResult> ret = find(token, params);
		log.debug("findByName: {}", ret);
		return ret;
	}
	
	@Override
	public List<QueryResult> findByKeywords(String token, Set<String> expression) throws IOException, ParseException,
			RepositoryException, DatabaseException {
		log.debug("findByKeywords({}, {})", token, expression);
		QueryParams params = new QueryParams();
		params.setKeywords(expression);
		List<QueryResult> ret = find(token, params);
		log.debug("findByKeywords: {}", ret);
		return ret;
	}
	
	@Override
	public List<QueryResult> find(String token, QueryParams params) throws IOException, ParseException, RepositoryException,
			DatabaseException {
		log.debug("find({}, {})", token, params);
		List<QueryResult> ret = findPaginated(token, params, 0, Config.MAX_SEARCH_RESULTS).getResults();
		log.debug("find: {}", ret);
		return ret;
	}
	
	@Override
	public ResultSet findPaginated(String token, QueryParams params, int offset, int limit) throws IOException, ParseException,
			RepositoryException, DatabaseException {
		log.debug("findPaginated({}, {}, {}, {})", new Object[] { token, params, offset, limit });
		Query query = null;
		
		if (params.getStatementQuery() != null && !params.getStatementQuery().equals("")) {
			// query = params.getStatementQuery();
		} else {
			query = prepareStatement(params);
		}
		
		ResultSet rs = findByStatementPaginated(token, query, offset, limit);
		log.debug("findPaginated: {}", rs);
		return rs;
	}
	
	/**
	 * Prepare statement
	 */
	public Query prepareStatement(QueryParams params) throws IOException, ParseException, RepositoryException,
			DatabaseException {
		log.debug("prepareStatement({})", params);
		BooleanQuery query = new BooleanQuery();
		
		// Clean params
		params.setName(params.getName() != null ? params.getName().trim() : "");
		params.setContent(params.getContent() != null ? params.getContent().trim() : "");
		params.setKeywords(params.getKeywords() != null ? params.getKeywords() : new HashSet<String>());
		params.setCategories(params.getCategories() != null ? params.getCategories() : new HashSet<String>());
		params.setMimeType(params.getMimeType() != null ? params.getMimeType().trim() : "");
		params.setAuthor(params.getAuthor() != null ? params.getAuthor().trim() : "");
		params.setPath(params.getPath() != null ? params.getPath().trim() : "");
		params.setMailSubject(params.getMailSubject() != null ? params.getMailSubject().trim() : "");
		params.setMailFrom(params.getMailFrom() != null ? params.getMailFrom().trim() : "");
		params.setMailTo(params.getMailTo() != null ? params.getMailTo().trim() : "");
		params.setProperties(params.getProperties() != null ? params.getProperties() : new HashMap<String, String>());
		
		// Domains
		boolean document = (params.getDomain() & QueryParams.DOCUMENT) != 0;
		boolean folder = (params.getDomain() & QueryParams.FOLDER) != 0;
		boolean mail = (params.getDomain() & QueryParams.MAIL) != 0;
		log.debug("doc={}, fld={}, mail={}", new Object[] { document, folder, mail });
		
		// Path to UUID conversion and in depth recursion
		List<String> pathInDepth = new ArrayList<String>();
		
		if (!params.getPath().equals("") && !params.getPath().equals("/" + Repository.ROOT)
				&& !params.getPath().equals("/" + Repository.CATEGORIES)
				&& !params.getPath().equals("/" + Repository.TEMPLATES) && !params.getPath().equals("/" + Repository.PERSONAL)
				&& !params.getPath().equals("/" + Repository.MAIL) && !params.getPath().equals("/" + Repository.TRASH)) {
			try {
				String uuid = NodeBaseDAO.getInstance().getUuidFromPath(params.getPath());
				log.debug("Path in depth: {} => {}", uuid, NodeBaseDAO.getInstance().getPathFromUuid(uuid));
				pathInDepth.add(uuid);
				
				for (String uuidChild : SearchDAO.getInstance().findFoldersInDepth(uuid)) {
					log.debug("Path in depth: {} => {}", uuidChild, NodeBaseDAO.getInstance().getPathFromUuid(uuidChild));
					pathInDepth.add(uuidChild);
				}
			} catch (PathNotFoundException e) {
				throw new RepositoryException("Path Not Found: " + e.getMessage());
			}
		}
		
		/**
		 * DOCUMENT
		 */
		if (document) {
			BooleanQuery queryDocument = new BooleanQuery();
			Term tEntity = new Term("_hibernate_class", NodeDocument.class.getCanonicalName());
			queryDocument.add(new TermQuery(tEntity), BooleanClause.Occur.MUST);
			
			if (!params.getContent().equals("")) {
				for (StringTokenizer st = new StringTokenizer(params.getContent(), " "); st.hasMoreTokens();) {
					Term t = new Term("text", st.nextToken().toLowerCase());
					queryDocument.add(new WildcardQuery(t), BooleanClause.Occur.MUST);
				}
			}
			
			if (!params.getName().equals("")) {
				if (!params.getName().contains("*") && !params.getName().contains("?")) {
					params.setName("*" + params.getName() + "*");
				}
				
				Term t = new Term("name", params.getName().toLowerCase());
				queryDocument.add(new WildcardQuery(t), BooleanClause.Occur.MUST);
			}
			
			if (!params.getPath().equals("")) {
				if (pathInDepth.isEmpty()) {
					Term t = new Term("context", PathUtils.fixContext(params.getPath()));
					queryDocument.add(new WildcardQuery(t), BooleanClause.Occur.MUST);
				} else {
					BooleanQuery parent = new BooleanQuery();
					
					for (String uuid : pathInDepth) {
						Term tChild = new Term("parent", uuid);
						parent.add(new TermQuery(tChild), BooleanClause.Occur.SHOULD);
					}
					
					queryDocument.add(parent, BooleanClause.Occur.MUST);
				}
			}
			
			if (!params.getMimeType().equals("")) {
				Term t = new Term("mimeType", params.getMimeType());
				queryDocument.add(new TermQuery(t), BooleanClause.Occur.MUST);
			}
			
			if (!params.getAuthor().equals("")) {
				Term t = new Term("author", params.getAuthor());
				queryDocument.add(new TermQuery(t), BooleanClause.Occur.MUST);
			}
			
			if (params.getLastModifiedFrom() != null && params.getLastModifiedTo() != null) {
				Date from = params.getLastModifiedFrom().getTime();
				String sFrom = DAY_FORMAT.format(from);
				Date to = params.getLastModifiedTo().getTime();
				String sTo = DAY_FORMAT.format(to);
				queryDocument.add(new TermRangeQuery("lastModified", sFrom, sTo, true, true), BooleanClause.Occur.MUST);
			}
			
			appendCommon(params, queryDocument);
			query.add(queryDocument, BooleanClause.Occur.SHOULD);
		}
		
		/**
		 * FOLDER
		 */
		if (folder) {
			BooleanQuery queryFolder = new BooleanQuery();
			Term tEntity = new Term("_hibernate_class", NodeFolder.class.getCanonicalName());
			queryFolder.add(new TermQuery(tEntity), BooleanClause.Occur.MUST);
			
			if (!params.getName().equals("")) {
				Term t = new Term("name", params.getName().toLowerCase());
				queryFolder.add(new WildcardQuery(t), BooleanClause.Occur.MUST);
			}
			
			if (!params.getPath().equals("")) {
				if (pathInDepth.isEmpty()) {
					Term t = new Term("context", PathUtils.fixContext(params.getPath()));
					queryFolder.add(new WildcardQuery(t), BooleanClause.Occur.MUST);
				} else {
					BooleanQuery parent = new BooleanQuery();
					
					for (String uuid : pathInDepth) {
						Term tChild = new Term("parent", uuid);
						parent.add(new TermQuery(tChild), BooleanClause.Occur.SHOULD);
					}
					
					queryFolder.add(parent, BooleanClause.Occur.MUST);
				}
			}
			
			appendCommon(params, queryFolder);
			query.add(queryFolder, BooleanClause.Occur.SHOULD);
		}
		
		/**
		 * MAIL
		 */
		if (mail) {
			BooleanQuery queryMail = new BooleanQuery();
			Term tEntity = new Term("_hibernate_class", NodeMail.class.getCanonicalName());
			queryMail.add(new TermQuery(tEntity), BooleanClause.Occur.MUST);
			
			if (!params.getPath().equals("")) {
				if (pathInDepth.isEmpty()) {
					Term t = new Term("context", PathUtils.fixContext(params.getPath()));
					queryMail.add(new WildcardQuery(t), BooleanClause.Occur.MUST);
				} else {
					BooleanQuery parent = new BooleanQuery();
					
					for (String uuid : pathInDepth) {
						Term tChild = new Term("parent", uuid);
						parent.add(new TermQuery(tChild), BooleanClause.Occur.SHOULD);
					}
					
					queryMail.add(parent, BooleanClause.Occur.MUST);
				}
			}
			
			if (!params.getContent().equals("")) {
				for (StringTokenizer st = new StringTokenizer(params.getContent(), " "); st.hasMoreTokens();) {
					Term t = new Term("content", st.nextToken().toLowerCase());
					queryMail.add(new WildcardQuery(t), BooleanClause.Occur.MUST);
				}
			}
			
			if (!params.getMailSubject().equals("")) {
				Term t = new Term("subject", params.getMailSubject().toLowerCase());
				queryMail.add(new WildcardQuery(t), BooleanClause.Occur.MUST);
			}
			
			if (!params.getMailFrom().equals("")) {
				Term t = new Term("from", params.getMailFrom().toLowerCase());
				queryMail.add(new WildcardQuery(t), BooleanClause.Occur.MUST);
			}
			
			if (!params.getMailTo().equals("")) {
				Term t = new Term("to", params.getMailTo().toLowerCase());
				queryMail.add(new WildcardQuery(t), BooleanClause.Occur.MUST);
			}
			
			if (!params.getMimeType().equals("")) {
				Term t = new Term("mimeType", params.getMimeType());
				queryMail.add(new TermQuery(t), BooleanClause.Occur.MUST);
			}
			
			appendCommon(params, queryMail);
			query.add(queryMail, BooleanClause.Occur.SHOULD);
		}
		
		log.debug("prepareStatement: {}", query.toString());
		return query;
	}
	
	/**
	 * Add common fields
	 */
	private void appendCommon(QueryParams params, BooleanQuery query) throws IOException, ParseException {
		if (!params.getKeywords().isEmpty()) {
			for (String keyword : params.getKeywords()) {
				Term t = new Term("keyword", keyword);
				query.add(new WildcardQuery(t), BooleanClause.Occur.MUST);
			}
		}
		
		if (!params.getCategories().isEmpty()) {
			for (String category : params.getCategories()) {
				Term t = new Term("category", category);
				query.add(new TermQuery(t), BooleanClause.Occur.MUST);
			}
		}
		
		if (!params.getProperties().isEmpty()) {
			Map<PropertyGroup, List<FormElement>> formsElements = FormUtils
					.parsePropertyGroupsForms(Config.PROPERTY_GROUPS_XML);
			
			for (Iterator<Entry<String, String>> it = params.getProperties().entrySet().iterator(); it.hasNext();) {
				Entry<String, String> ent = it.next();
				FormElement fe = FormUtils.getFormElement(formsElements, ent.getKey());
				
				if (fe != null && ent.getValue() != null) {
					String valueTrimmed = ent.getValue().trim().toLowerCase();
					
					if (!valueTrimmed.equals("")) {
						if (fe instanceof Select) {
							if (((Select) fe).getType().equals(Select.TYPE_SIMPLE)) {
								Term t = new Term(ent.getKey(), valueTrimmed);
								query.add(new TermQuery(t), BooleanClause.Occur.MUST);
							} else {
								String[] options = valueTrimmed.split(",");
								
								for (String option : options) {
									Term t = new Term(ent.getKey(), option);
									query.add(new TermQuery(t), BooleanClause.Occur.MUST);
								}
							}
						} else if (fe instanceof Input && ((Input) fe).getType().equals(Input.TYPE_DATE)) {
							String[] date = valueTrimmed.split(",");
							
							if (date.length == 2) {
								Calendar from = ISO8601.parseBasic(date[0]);
								Calendar to = ISO8601.parseBasic(date[1]);
								
								if (from != null && to != null) {
									String sFrom = DAY_FORMAT.format(from.getTime());
									String sTo = DAY_FORMAT.format(to.getTime());
									query.add(new TermRangeQuery(ent.getKey(), sFrom, sTo, true, true),
											BooleanClause.Occur.MUST);
								}
							}
						} else if (fe instanceof Input && ((Input) fe).getType().equals(Input.TYPE_TEXT)
								|| fe instanceof TextArea) {
							for (StringTokenizer st = new StringTokenizer(valueTrimmed, " "); st.hasMoreTokens();) {
								Term t = new Term(ent.getKey(), st.nextToken().toLowerCase());
								query.add(new WildcardQuery(t), BooleanClause.Occur.MUST);
							}
						} else {
							Term t = new Term(ent.getKey(), valueTrimmed);
							query.add(new WildcardQuery(t), BooleanClause.Occur.MUST);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Find by statement
	 */
	private ResultSet findByStatementPaginated(String token, Query query, int offset, int limit) throws RepositoryException,
			DatabaseException {
		log.debug("findByStatementPaginated({}, {}, {}, {}, {})", new Object[] { token, query, offset, limit });
		List<QueryResult> results = new ArrayList<QueryResult>();
		ResultSet rs = new ResultSet();
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			if (query != null) {
				NodeResultSet nrs = SearchDAO.getInstance().findByQuery(query, offset, limit);
				rs.setTotal(nrs.getTotal());
				
				for (NodeQueryResult nqr : nrs.getResults()) {
					QueryResult qr = new QueryResult();
					qr.setExcerpt(nqr.getExcerpt());
					qr.setScore((long) (100 * nqr.getScore()));
					
					if (nqr.getDocument() != null) {
						qr.setDocument(BaseDocumentModule.getProperties(auth.getName(), nqr.getDocument()));
					} else if (nqr.getFolder() != null) {
						qr.setFolder(BaseFolderModule.getProperties(auth.getName(), nqr.getFolder()));
					} else if (nqr.getMail() != null) {
						qr.setMail(BaseMailModule.getProperties(auth.getName(), nqr.getMail()));
					} else if (nqr.getAttachment() != null) {
						qr.setAttachment(BaseDocumentModule.getProperties(auth.getName(), nqr.getAttachment()));
					}
					
					results.add(qr);
				}
				
				rs.setResults(results);
			}
			
			// Activity log
			UserActivity.log(auth.getName(), "FIND_BY_STATEMENT_PAGINATED", null, null, offset + ", " + limit + ", " + query);
		} catch (PathNotFoundException e) {
			throw new RepositoryException(e.getMessage(), e);
		} catch (ParseException e) {
			throw new RepositoryException(e.getMessage(), e);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("findByStatementPaginated: {}", rs);
		return rs;
	}
	
	@Override
	public long saveSearch(String token, QueryParams params) throws AccessDeniedException, RepositoryException,
			DatabaseException {
		log.debug("saveSearch({}, {})", token, params);
		Authentication auth = null, oldAuth = null;
		long id = 0;
		
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
			
			params.setUser(auth.getName());
			id = QueryParamsDAO.create(params);
			
			// Activity log
			UserActivity.log(auth.getName(), "SAVE_SEARCH", params.getName(), null, params.toString());
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("saveSearch: {}", id);
		return id;
	}
	
	@Override
	public void updateSearch(String token, QueryParams params) throws AccessDeniedException, RepositoryException,
			DatabaseException {
		log.debug("updateSearch({}, {})", token, params);
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
			
			params.setUser(auth.getName());
			QueryParamsDAO.update(params);
			
			// Activity log
			UserActivity.log(auth.getName(), "UPDATE_SEARCH", params.getName(), null, params.toString());
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("updateSearch: void");
	}
	
	@Override
	public QueryParams getSearch(String token, int qpId) throws PathNotFoundException, RepositoryException, DatabaseException {
		log.debug("getSearch({}, {})", token, qpId);
		QueryParams qp = new QueryParams();
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			qp = QueryParamsDAO.findByPk(qpId);
			
			// If this is a dashboard user search, dates are used internally
			if (qp.isDashboard()) {
				qp.setLastModifiedFrom(null);
				qp.setLastModifiedTo(null);
			}
			
			// Activity log
			UserActivity.log(auth.getName(), "GET_SAVED_SEARCH", Integer.toString(qpId), null, qp.toString());
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getSearch: {}", qp);
		return qp;
	}
	
	@Override
	public List<QueryParams> getAllSearchs(String token) throws RepositoryException, DatabaseException {
		log.debug("getAllSearchs({})", token);
		List<QueryParams> ret = new ArrayList<QueryParams>();
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			List<QueryParams> qParams = QueryParamsDAO.findByUser(auth.getName());
			
			for (Iterator<QueryParams> it = qParams.iterator(); it.hasNext();) {
				QueryParams qp = it.next();
				
				if (!qp.isDashboard()) {
					ret.add(qp);
				}
			}
			
			// Activity log
			UserActivity.log(auth.getName(), "GET_ALL_SEARCHS", null, null, null);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getAllSearchs: {}", ret);
		return ret;
	}
	
	@Override
	public void deleteSearch(String token, long qpId) throws AccessDeniedException, RepositoryException, DatabaseException {
		log.debug("deleteSearch({}, {})", token, qpId);
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
			
			QueryParams qp = QueryParamsDAO.findByPk(qpId);
			QueryParamsDAO.delete(qpId);
			
			// Purge visited nodes table
			if (qp.isDashboard()) {
				DashboardDAO.deleteVisitedNodes(auth.getName(), qp.getName());
			}
			
			// Activity log
			UserActivity.log(auth.getName(), "DELETE_SAVED_SEARCH", Long.toString(qpId), null, null);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("deleteSearch: void");
	}
	
	@Override
	public Map<String, Integer> getKeywordMap(String token, List<String> filter) throws RepositoryException, DatabaseException {
		log.debug("getKeywordMap({}, {})", token, filter);
		Map<String, Integer> cloud = null;
		
		if (Config.USER_KEYWORDS_CACHE) {
			cloud = getKeywordMapCached(token, filter);
		} else {
			cloud = getKeywordMapLive(token, filter);
		}
		
		log.debug("getKeywordMap: {}", cloud);
		return cloud;
	}
	
	/**
	 * Get keyword map
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Integer> getKeywordMapLive(String token, List<String> filter) throws RepositoryException,
			DatabaseException {
		log.debug("getKeywordMapLive({}, {})", token, filter);
		String qs = "select elements(nb.keywords) from NodeBase nb";
		HashMap<String, Integer> cloud = new HashMap<String, Integer>();
		org.hibernate.Session hSession = null;
		Transaction tx = null;
		@SuppressWarnings("unused")
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			hSession = HibernateUtil.getSessionFactory().openSession();
			tx = hSession.beginTransaction();
			org.hibernate.Query hq = hSession.createQuery(qs);
			List<String> nodeKeywords = hq.list();
			
			if (filter != null && nodeKeywords.containsAll(filter)) {
				for (String keyword : nodeKeywords) {
					if (!filter.contains(keyword)) {
						Integer occurs = cloud.get(keyword) != null ? cloud.get(keyword) : 0;
						cloud.put(keyword, occurs + 1);
					}
				}
			}
			
			HibernateUtil.commit(tx);
		} catch (HibernateException e) {
			HibernateUtil.rollback(tx);
			throw new DatabaseException(e.getMessage(), e);
		} finally {
			HibernateUtil.close(hSession);
			
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getKeywordMapLive: {}", cloud);
		return cloud;
	}
	
	/**
	 * Get keyword map
	 */
	private Map<String, Integer> getKeywordMapCached(String token, List<String> filter) throws RepositoryException,
			DatabaseException {
		log.debug("getKeywordMapCached({}, {})", token, filter);
		HashMap<String, Integer> keywordMap = new HashMap<String, Integer>();
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			Collection<UserNodeKeywords> userDocKeywords = UserNodeKeywordsManager.get(auth.getName()).values();
			
			for (Iterator<UserNodeKeywords> kwIt = userDocKeywords.iterator(); kwIt.hasNext();) {
				Set<String> docKeywords = kwIt.next().getKeywords();
				
				if (filter != null && docKeywords.containsAll(filter)) {
					for (Iterator<String> itDocKeywords = docKeywords.iterator(); itDocKeywords.hasNext();) {
						String keyword = itDocKeywords.next();
						
						if (!filter.contains(keyword)) {
							Integer occurs = keywordMap.get(keyword) != null ? keywordMap.get(keyword) : 0;
							keywordMap.put(keyword, occurs + 1);
						}
					}
				}
			}
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getKeywordMapCached: {}", keywordMap);
		return keywordMap;
	}
	
	@Override
	public List<Document> getCategorizedDocuments(String token, String categoryId) throws RepositoryException,
			DatabaseException {
		log.debug("getCategorizedDocuments({}, {})", token, categoryId);
		List<Document> documents = new ArrayList<Document>();
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			for (NodeDocument nDoc : NodeDocumentDAO.getInstance().findByCategory(categoryId)) {
				documents.add(BaseDocumentModule.getProperties(auth.getName(), nDoc));
			}
		} catch (PathNotFoundException e) {
			throw new RepositoryException(e.getMessage(), e);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getCategorizedDocuments: {}", documents);
		return documents;
	}
	
	@Override
	public List<Folder> getCategorizedFolders(String token, String categoryId) throws RepositoryException, DatabaseException {
		log.debug("getCategorizedFolders({}, {})", token, categoryId);
		List<Folder> folders = new ArrayList<Folder>();
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			for (NodeFolder nFld : NodeFolderDAO.getInstance().findByCategory(categoryId)) {
				folders.add(BaseFolderModule.getProperties(auth.getName(), nFld));
			}
		} catch (PathNotFoundException e) {
			throw new RepositoryException(e.getMessage(), e);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getCategorizedFolders: {}", folders);
		return folders;
	}
	
	@Override
	public List<Mail> getCategorizedMails(String token, String categoryId) throws RepositoryException, DatabaseException {
		log.debug("getCategorizedMails({}, {})", token, categoryId);
		List<Mail> mails = new ArrayList<Mail>();
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			for (NodeMail nMail : NodeMailDAO.getInstance().findByCategory(categoryId)) {
				mails.add(BaseMailModule.getProperties(auth.getName(), nMail));
			}
		} catch (PathNotFoundException e) {
			throw new RepositoryException(e.getMessage(), e);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getCategorizedMails: {}", mails);
		return mails;
	}
	
	@Override
	public List<Document> getDocumentsByKeyword(String token, String keyword) throws RepositoryException, DatabaseException {
		log.debug("getDocumentsByKeyword({}, {})", token, keyword);
		List<Document> documents = new ArrayList<Document>();
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			for (NodeDocument nDoc : NodeDocumentDAO.getInstance().findByKeyword(keyword)) {
				documents.add(BaseDocumentModule.getProperties(auth.getName(), nDoc));
			}
		} catch (PathNotFoundException e) {
			throw new RepositoryException(e.getMessage(), e);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getDocumentsByKeyword: {}", documents);
		return documents;
	}
	
	@Override
	public List<Folder> getFoldersByKeyword(String token, String keyword) throws RepositoryException, DatabaseException {
		log.debug("getFoldersByKeyword({}, {})", token, keyword);
		List<Folder> folders = new ArrayList<Folder>();
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			for (NodeFolder nFld : NodeFolderDAO.getInstance().findByKeyword(keyword)) {
				folders.add(BaseFolderModule.getProperties(auth.getName(), nFld));
			}
		} catch (PathNotFoundException e) {
			throw new RepositoryException(e.getMessage(), e);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getFoldersByKeyword: {}", folders);
		return folders;
	}
	
	@Override
	public List<Mail> getMailsByKeyword(String token, String keyword) throws RepositoryException, DatabaseException {
		log.debug("getMailsByKeyword({}, {})", token, keyword);
		List<Mail> mails = new ArrayList<Mail>();
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			for (NodeMail nMail : NodeMailDAO.getInstance().findByKeyword(keyword)) {
				mails.add(BaseMailModule.getProperties(auth.getName(), nMail));
			}
		} catch (PathNotFoundException e) {
			throw new RepositoryException(e.getMessage(), e);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getMailsByKeyword: {}", mails);
		return mails;
	}
	
	@Override
	public List<Document> getDocumentsByPropertyValue(String token, String group, String property, String value)
			throws RepositoryException, DatabaseException {
		log.debug("getDocumentsByPropertyValue({}, {}, {}, {})", new Object[] { token, group, property, value });
		List<Document> documents = new ArrayList<Document>();
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			for (NodeDocument nDoc : NodeDocumentDAO.getInstance().findByPropertyValue(group, property, value)) {
				documents.add(BaseDocumentModule.getProperties(auth.getName(), nDoc));
			}
		} catch (PathNotFoundException e) {
			throw new RepositoryException(e.getMessage(), e);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getDocumentsByPropertyValue: {}", documents);
		return documents;
	}
	
	@Override
	public List<Folder> getFoldersByPropertyValue(String token, String group, String property, String value)
			throws RepositoryException, DatabaseException {
		log.debug("getFoldersByPropertyValue({}, {}, {}, {})", new Object[] { token, group, property, value });
		List<Folder> folders = new ArrayList<Folder>();
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			for (NodeFolder nFld : NodeFolderDAO.getInstance().findByPropertyValue(group, property, value)) {
				folders.add(BaseFolderModule.getProperties(auth.getName(), nFld));
			}
		} catch (PathNotFoundException e) {
			throw new RepositoryException(e.getMessage(), e);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getFoldersByPropertyValue: {}", folders);
		return folders;
	}
	
	@Override
	public List<Mail> getMailsByPropertyValue(String token, String group, String property, String value)
			throws RepositoryException, DatabaseException {
		log.debug("getMailsByPropertyValue({}, {}, {}, {})", new Object[] { token, group, property, value });
		List<Mail> mails = new ArrayList<Mail>();
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			for (NodeMail nMail : NodeMailDAO.getInstance().findByPropertyValue(group, property, value)) {
				mails.add(BaseMailModule.getProperties(auth.getName(), nMail));
			}
		} catch (PathNotFoundException e) {
			throw new RepositoryException(e.getMessage(), e);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getMailsByPropertyValue: {}", mails);
		return mails;
	}
	
	@Override
	public List<QueryResult> findSimpleQuery(String token, String statement) throws RepositoryException, DatabaseException {
		log.debug("findSimpleQuery({}, {})", token, statement);
		List<QueryResult> ret = findSimpleQueryPaginated(token, statement, 0, Config.MAX_SEARCH_RESULTS).getResults();
		log.debug("findSimpleQuery: {}", ret);
		return ret;
	}
	
	@Override
	public ResultSet findSimpleQueryPaginated(String token, String statement, int offset, int limit)
			throws RepositoryException, DatabaseException {
		log.debug("findSimpleQueryPaginated({}, {}, {}, {})", new Object[] { token, statement, offset, limit });
		List<QueryResult> results = new ArrayList<QueryResult>();
		ResultSet rs = new ResultSet();
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			if (statement != null && !statement.equals("")) {
				// Only search in Taxonomy
				statement = statement.concat(" AND context:okm_root");
				
				NodeResultSet nrs = SearchDAO.getInstance().findBySimpleQuery(statement, offset, limit);
				rs.setTotal(nrs.getTotal());
				
				for (NodeQueryResult nqr : nrs.getResults()) {
					QueryResult qr = new QueryResult();
					qr.setExcerpt(nqr.getExcerpt());
					qr.setScore((long) (100 * nqr.getScore()));
					
					if (nqr.getDocument() != null) {
						qr.setDocument(BaseDocumentModule.getProperties(auth.getName(), nqr.getDocument()));
					} else if (nqr.getFolder() != null) {
						qr.setFolder(BaseFolderModule.getProperties(auth.getName(), nqr.getFolder()));
					} else if (nqr.getMail() != null) {
						qr.setMail(BaseMailModule.getProperties(auth.getName(), nqr.getMail()));
					} else if (nqr.getAttachment() != null) {
						qr.setAttachment(BaseDocumentModule.getProperties(auth.getName(), nqr.getAttachment()));
					}
					
					results.add(qr);
				}
				
				rs.setResults(results);
			}
			
			// Activity log
			UserActivity.log(auth.getName(), "FIND_SIMPLE_QUERY_PAGINATED", null, null, offset + ", " + limit + ", "
					+ statement);
		} catch (PathNotFoundException e) {
			throw new RepositoryException(e.getMessage(), e);
		} catch (ParseException e) {
			throw new RepositoryException(e.getMessage(), e);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("findSimpleQueryPaginated: {}", rs);
		return rs;
	}
	
	@Override
	public ResultSet findMoreLikeThis(String token, String uuid, int maxResults) throws RepositoryException, DatabaseException {
		log.debug("findMoreLikeThis({}, {}, {})", new Object[] { token, uuid, maxResults });
		List<QueryResult> results = new ArrayList<QueryResult>();
		ResultSet rs = new ResultSet();
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			NodeResultSet nrs = SearchDAO.getInstance().moreLikeThis(uuid, maxResults);
			rs.setTotal(nrs.getTotal());
			
			for (NodeQueryResult nqr : nrs.getResults()) {
				QueryResult qr = new QueryResult();
				qr.setExcerpt(nqr.getExcerpt());
				qr.setScore((long) (100 * nqr.getScore()));
				
				if (nqr.getDocument() != null) {
					qr.setDocument(BaseDocumentModule.getProperties(auth.getName(), nqr.getDocument()));
				} else if (nqr.getFolder() != null) {
					qr.setFolder(BaseFolderModule.getProperties(auth.getName(), nqr.getFolder()));
				} else if (nqr.getMail() != null) {
					qr.setMail(BaseMailModule.getProperties(auth.getName(), nqr.getMail()));
				}
				
				results.add(qr);
			}
			
			rs.setResults(results);
			
			// Activity log
			UserActivity.log(auth.getName(), "FIND_MORE_LIKE_THIS", uuid, null, Integer.toString(maxResults));
		} catch (PathNotFoundException e) {
			throw new RepositoryException(e.getMessage(), e);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("findMoreLikeThis: {}", rs);
		return rs;
	}
}
