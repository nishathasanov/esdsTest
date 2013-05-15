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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.bean.Document;
import com.openkm.bean.Repository;
import com.openkm.bean.StatsInfo;
import com.openkm.core.DatabaseException;
import com.openkm.core.RepositoryException;
import com.openkm.module.StatsModule;
import com.openkm.module.jcr.stuff.JCRUtils;
import com.openkm.module.jcr.stuff.JcrSessionManager;

public class JcrStatsModule implements StatsModule {
	private static Logger log = LoggerFactory.getLogger(JcrStatsModule.class);
	
	private static String TAXONOMY_DOCUMENTS = "/jcr:root/"+Repository.ROOT+"//element(*,okm:document)";
	private static String TAXONOMY_FOLDERS = "/jcr:root/"+Repository.ROOT+"//element(*,okm:folder)";
	private static String TEMPLATES_DOCUMENTS = "/jcr:root/"+Repository.TEMPLATES+"//element(*,okm:document)";
	private static String TEMPLATES_FOLDERS = "/jcr:root/"+Repository.TEMPLATES+"//element(*,okm:folder)";
	private static String PERSONAL_DOCUMENTS = "/jcr:root/"+Repository.PERSONAL+"//element(*,okm:document)";
	private static String PERSONAL_FOLDERS = "/jcr:root/"+Repository.PERSONAL+"//element(*,okm:folder)";
	private static String TRASH_DOCUMENTS = "/jcr:root/"+Repository.TRASH+"//element(*,okm:document)";
	private static String TRASH_FOLDERS = "/jcr:root/"+Repository.TRASH+"//element(*,okm:folder)";
	
	@Override
	public StatsInfo getDocumentsByContext(String token) throws RepositoryException, DatabaseException {
		log.debug("getDocumentsByContext({})", token);
		StatsInfo si = new StatsInfo();
		double[] percents = new double[4];
		long[] sizes = new long[4];
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			Workspace workspace = session.getWorkspace();
			QueryManager queryManager = workspace.getQueryManager();
			long taxonomyDocuments = getCount(queryManager, TAXONOMY_DOCUMENTS);
			long personalDocuments = getCount(queryManager, PERSONAL_DOCUMENTS);
			long templatesDocuments = getCount(queryManager, TEMPLATES_DOCUMENTS);
			long trashDocuments = getCount(queryManager, TRASH_DOCUMENTS);
			long totalDocuments = taxonomyDocuments + personalDocuments + templatesDocuments + trashDocuments;
			si.setTotal(totalDocuments);
			
			// Fill sizes
			sizes[0] = taxonomyDocuments;
			sizes[1] = personalDocuments;
			sizes[2] = templatesDocuments;
			sizes[3] = trashDocuments;
			si.setSizes(sizes);
			
			// Compute percents
			percents[0] = (totalDocuments > 0) ? ((double) taxonomyDocuments / totalDocuments) : 0;
			percents[1] = (totalDocuments > 0) ? ((double) personalDocuments / totalDocuments) : 0;
			percents[2] = (totalDocuments > 0) ? ((double) templatesDocuments / totalDocuments) : 0;
			percents[3] = (totalDocuments > 0) ? ((double) trashDocuments / totalDocuments) : 0;
			si.setPercents(percents);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null)
				JCRUtils.logout(session);
		}
		
		log.debug("getDocumentsByContext: {}", si);
		return si;
	}
	
	@Override
	public StatsInfo getFoldersByContext(String token) throws RepositoryException, DatabaseException {
		log.debug("getFoldersByContext({})", token);
		StatsInfo si = new StatsInfo();
		double[] percents = new double[4];
		long[] sizes = new long[4];
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			Workspace workspace = session.getWorkspace();
			QueryManager queryManager = workspace.getQueryManager();
			long taxonomyFolders = getCount(queryManager, TAXONOMY_FOLDERS);
			long personalFolders = getCount(queryManager, PERSONAL_FOLDERS);
			long templatesFolders = getCount(queryManager, TEMPLATES_FOLDERS);
			long trashFolders = getCount(queryManager, TRASH_FOLDERS);
			long totalFolders = taxonomyFolders + personalFolders + templatesFolders + trashFolders;
			si.setTotal(totalFolders);
			
			// Fill sizes
			sizes[0] = taxonomyFolders;
			sizes[1] = personalFolders;
			sizes[2] = templatesFolders;
			sizes[3] = trashFolders;
			si.setSizes(sizes);
			
			// Compute percents
			percents[0] = (totalFolders > 0) ? ((double) taxonomyFolders / totalFolders) : 0;
			percents[1] = (totalFolders > 0) ? ((double) personalFolders / totalFolders) : 0;
			percents[2] = (totalFolders > 0) ? ((double) templatesFolders / totalFolders) : 0;
			percents[3] = (totalFolders > 0) ? ((double) trashFolders / totalFolders) : 0;
			si.setPercents(percents);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null)
				JCRUtils.logout(session);
		}
		
		log.debug("getFoldersByContext: {}", si);
		return si;
	}
	
	/**
	 * Get result node count.
	 */
	private long getCount(QueryManager queryManager, String statement) throws InvalidQueryException,
			javax.jcr.RepositoryException {
		Query query = queryManager.createQuery(statement, Query.XPATH);
		QueryResult result = query.execute();
		return result.getRows().getSize();
	}
	
	@Override
	public StatsInfo getDocumentsSizeByContext(String token) throws RepositoryException, DatabaseException {
		log.debug("getDocumentsSizeByContext({})", token);
		StatsInfo si = new StatsInfo();
		double[] percents = new double[4];
		long[] sizes = new long[4];
		Session session = null;
		
		try {
			if (token == null) {
				session = JCRUtils.getSession();
			} else {
				session = JcrSessionManager.getInstance().get(token);
			}
			
			Workspace workspace = session.getWorkspace();
			QueryManager queryManager = workspace.getQueryManager();
			long taxonomyDocumentSize = getSubtreeSize(queryManager, TAXONOMY_DOCUMENTS);
			long personalDocumentSize = getSubtreeSize(queryManager, PERSONAL_DOCUMENTS);
			long templatesDocumentSize = getSubtreeSize(queryManager, TEMPLATES_DOCUMENTS);
			long trashDocumentSize = getSubtreeSize(queryManager, TRASH_DOCUMENTS);
			long totalDocumentSize = taxonomyDocumentSize + personalDocumentSize + templatesDocumentSize + trashDocumentSize;
			si.setTotal(totalDocumentSize);
			
			// Fill sizes
			sizes[0] = taxonomyDocumentSize;
			sizes[1] = personalDocumentSize;
			sizes[2] = templatesDocumentSize;
			sizes[3] = trashDocumentSize;
			si.setSizes(sizes);
			
			// Compute percents
			percents[0] = (totalDocumentSize > 0) ? ((double) taxonomyDocumentSize / totalDocumentSize) : 0;
			percents[1] = (totalDocumentSize > 0) ? ((double) personalDocumentSize / totalDocumentSize) : 0;
			percents[2] = (totalDocumentSize > 0) ? ((double) templatesDocumentSize / totalDocumentSize) : 0;
			percents[3] = (totalDocumentSize > 0) ? ((double) trashDocumentSize / totalDocumentSize) : 0;
			si.setPercents(percents);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token == null)
				JCRUtils.logout(session);
		}
		
		log.debug("getDocumentsSizeByContext: {}", si);
		return si;
	}
	
	/**
	 * Get document node size.
	 */
	private long getSubtreeSize(QueryManager queryManager, String statement) throws InvalidQueryException,
			javax.jcr.RepositoryException {
		Query query = queryManager.createQuery(statement, Query.XPATH);
		QueryResult result = query.execute();
		long size = 0;
		
		for (NodeIterator nit = result.getNodes(); nit.hasNext();) {
			Node docNode = nit.nextNode();
			Node docContentNode = docNode.getNode(Document.CONTENT);
			size += docContentNode.getProperty(Document.SIZE).getLong();
		}
		
		return size;
	}
}
