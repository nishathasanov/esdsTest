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

package com.openkm.module.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import com.openkm.bean.Repository;
import com.openkm.bean.StatsInfo;
import com.openkm.core.DatabaseException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.RepositoryException;
import com.openkm.dao.NodeBaseDAO;
import com.openkm.dao.NodeDocumentDAO;
import com.openkm.dao.bean.NodeDocument;
import com.openkm.dao.bean.NodeFolder;
import com.openkm.module.StatsModule;
import com.openkm.spring.PrincipalUtils;

public class DbStatsModule implements StatsModule {
	private static Logger log = LoggerFactory.getLogger(DbStatsModule.class);
	
	@Override
	public StatsInfo getDocumentsByContext(String token) throws RepositoryException, DatabaseException {
		log.debug("getDocumentsByContext({})", token);
		StatsInfo si = new StatsInfo();
		double[] percents = new double[4];
		long[] sizes = new long[4];
		@SuppressWarnings("unused")
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String nodeType = NodeDocument.class.getSimpleName();
			long taxonomyDocuments = 0;
			long personalDocuments = 0;
			long templatesDocuments = 0;
			long trashDocuments = 0;
			
			taxonomyDocuments = NodeBaseDAO.getInstance().getSubtreeCount(nodeType, "/" + Repository.ROOT, 1);
			personalDocuments = NodeBaseDAO.getInstance().getSubtreeCount(nodeType, "/" + Repository.PERSONAL, 1);
			templatesDocuments = NodeBaseDAO.getInstance().getSubtreeCount(nodeType, "/" + Repository.TEMPLATES, 1);
			trashDocuments = NodeBaseDAO.getInstance().getSubtreeCount(nodeType, "/" + Repository.TRASH, 1);
			
			long totalDocuments =  taxonomyDocuments + personalDocuments + templatesDocuments + trashDocuments;
			si.setTotal(totalDocuments);
			
			// Fill sizes
			sizes[0] = taxonomyDocuments;
			sizes[1] = personalDocuments;
			sizes[2] = templatesDocuments;
			sizes[3] = trashDocuments;
			si.setSizes(sizes);
			
			// Compute percents
			percents[0] = (totalDocuments > 0)?((double) taxonomyDocuments / totalDocuments):0;
			percents[1] = (totalDocuments > 0)?((double) personalDocuments / totalDocuments):0;
			percents[2] = (totalDocuments > 0)?((double) templatesDocuments / totalDocuments):0;
			percents[3] = (totalDocuments > 0)?((double) trashDocuments / totalDocuments):0;
			si.setPercents(percents);
		} catch (PathNotFoundException e) {
			throw new RepositoryException(e.getMessage(), e);
		} catch (Exception e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
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
		@SuppressWarnings("unused")
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String nodeType = NodeFolder.class.getSimpleName();
			long taxonomyFolders = 0;
			long personalFolders = 0;
			long templatesFolders = 0;
			long trashFolders = 0;
			
			taxonomyFolders = NodeBaseDAO.getInstance().getSubtreeCount(nodeType, "/" + Repository.ROOT, 1);
			personalFolders = NodeBaseDAO.getInstance().getSubtreeCount(nodeType, "/" + Repository.PERSONAL, 2);
			templatesFolders = NodeBaseDAO.getInstance().getSubtreeCount(nodeType, "/" + Repository.TEMPLATES, 1);
			trashFolders = NodeBaseDAO.getInstance().getSubtreeCount(nodeType, "/" + Repository.TRASH, 2);	
			
			long totalFolders =  taxonomyFolders + personalFolders + templatesFolders + trashFolders; 
			si.setTotal(totalFolders);
			
			// Fill sizes
			sizes[0] = taxonomyFolders; 
			sizes[1] = personalFolders;
			sizes[2] = templatesFolders;
			sizes[3] = trashFolders;
			si.setSizes(sizes);
			
			// Compute percents
			percents[0] = (totalFolders > 0)?((double) taxonomyFolders / totalFolders):0;
			percents[1] = (totalFolders > 0)?((double) personalFolders / totalFolders):0;
			percents[2] = (totalFolders > 0)?((double) templatesFolders / totalFolders):0;
			percents[3] = (totalFolders > 0)?((double) trashFolders / totalFolders):0;
			si.setPercents(percents);
		} catch (PathNotFoundException e) {
			throw new RepositoryException(e.getMessage(), e);
		} catch (Exception e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}

		log.debug("getFoldersByContext: {}", si);
		return si;
	}
	
	@Override
	public StatsInfo getDocumentsSizeByContext(String token) throws RepositoryException, DatabaseException {
		log.debug("getDocumentsSizeByContext({})", token);
		StatsInfo si = new StatsInfo();
		double[] percents = new double[4];
		long[] sizes = new long[4];
		@SuppressWarnings("unused")
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			long taxonomyDocumentSize = 0;
			long personalDocumentSize = 0;
			long templatesDocumentSize = 0;
			long trashDocumentSize = 0;
			
			taxonomyDocumentSize = NodeDocumentDAO.getInstance().getSubtreeSize("/" + Repository.ROOT);
			personalDocumentSize = NodeDocumentDAO.getInstance().getSubtreeSize("/" + Repository.PERSONAL);
			templatesDocumentSize = NodeDocumentDAO.getInstance().getSubtreeSize("/" + Repository.TEMPLATES);
			trashDocumentSize = NodeDocumentDAO.getInstance().getSubtreeSize("/" + Repository.TRASH);
			
			long totalDocumentSize =  taxonomyDocumentSize + personalDocumentSize + templatesDocumentSize + trashDocumentSize;
			si.setTotal(totalDocumentSize);
			
			// Fill sizes
			sizes[0] = taxonomyDocumentSize;
			sizes[1] = personalDocumentSize;
			sizes[2] = templatesDocumentSize;
			sizes[3] = trashDocumentSize;
			si.setSizes(sizes);
			
			// Compute percents
			percents[0] = (totalDocumentSize > 0)?((double) taxonomyDocumentSize / totalDocumentSize):0;
			percents[1] = (totalDocumentSize > 0)?((double) personalDocumentSize / totalDocumentSize):0;
			percents[2] = (totalDocumentSize > 0)?((double) templatesDocumentSize / totalDocumentSize):0;
			percents[3] = (totalDocumentSize > 0)?((double) trashDocumentSize / totalDocumentSize):0;
			si.setPercents(percents);
		} catch (PathNotFoundException e) {
			throw new RepositoryException(e.getMessage(), e);
		} catch (Exception e) {
			throw new RepositoryException(e.getMessage(), e);
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}

		log.debug("getDocumentsSizeByContext: {}", si);
		return si;
	}
}
