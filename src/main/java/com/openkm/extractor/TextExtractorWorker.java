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

package com.openkm.extractor;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.core.Config;
import com.openkm.core.DatabaseException;
import com.openkm.core.PathNotFoundException;
import com.openkm.dao.NodeDocumentDAO;

/**
 * @author pavila
 */
public class TextExtractorWorker extends TimerTask {
	private static Logger log = LoggerFactory.getLogger(TextExtractorWorker.class);
	private static List<TextExtractorWork> inProgress = new ArrayList<TextExtractorWork>();
	private static volatile boolean running = false;
	
	/**
	 * Get in progress extraction works.
	 */
	public static List<TextExtractorWork> getInProgressWorks() throws DatabaseException {
		return inProgress;
	}
	
	/**
	 * Get pending extraction works.
	 */
	public static List<TextExtractorWork> getPendingWorks(int max) throws DatabaseException {
		return NodeDocumentDAO.getInstance().getPendingExtractions(max);
	}
	
	/**
	 * Get pending extraction work size.
	 */
	public static long getPendingSize() throws DatabaseException {
		return NodeDocumentDAO.getInstance().getPendingExtractionSize();
	}
	
	/**
	 * Document text extraction batch
	 */
	@Override
	public void run() {
		if (running) {
			log.warn("*** Text extraction already running ***");
		} else {
			running = true;
			log.debug("*** Begin text extraction ***");
			
			try {
				if (!Config.SYSTEM_READONLY) {
					processQueue(null, Config.MANAGED_TEXT_EXTRACTION_BATCH);
				} else {
					log.warn("*** Text extraction disabled because system is readonly ***");
				}
			} finally {
				running = false;
			}
			
			log.debug("*** End text extraction ***");
		}
	}
	
	/**
	 * Force text extraction of every document in the repository
	 */
	public void rebuildWorker(MassIndexerProgressMonitor monitor) throws PathNotFoundException, DatabaseException,
			InterruptedException {
		if (running) {
			log.warn("*** Text extraction already running ***");
		} else {
			running = true;
			log.debug("*** Begin massive text extraction ***");
			
			try {
				// Clear pending extraction queue
				NodeDocumentDAO.getInstance().resetAllPendingExtractionFlags();
				
				// Process queue
				while (NodeDocumentDAO.getInstance().hasPendingExtractions()) {
					processQueue(monitor, Config.MANAGED_TEXT_EXTRACTION_BATCH);
					Thread.sleep(750);
					System.gc();
				}
			} finally {
				running = false;
			}
			
			log.debug("*** End massive text extraction ***");
		}
	}
	
	/**
	 * Process text extraction pending queue
	 */
	private void processQueue(MassIndexerProgressMonitor monitor, int maxResults) {
		processSerial(monitor, maxResults);		
	}
	
	/**
	 * Process queue serial
	 */
	private void processSerial(MassIndexerProgressMonitor monitor, int maxResults) {
		log.debug("processSerial({}, {})", monitor, maxResults);
		
		try {
			for (TextExtractorWork work : NodeDocumentDAO.getInstance().getPendingExtractions(maxResults)) {
				log.info("processSerial.Working on {}", work);
				inProgress.add(work);
				NodeDocumentDAO.getInstance().textExtractorHelper(work);
				inProgress.clear();
				
				if (monitor != null) {
					monitor.documentsAdded(1);
				}
			}
		} catch (FileNotFoundException e) {
			log.warn(e.getMessage(), e);
		} catch (DatabaseException e) {
			log.warn(e.getMessage(), e);
		} finally {
			inProgress.clear();
		}
	}
}
