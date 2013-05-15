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

package com.openkm.module.db.stuff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.core.DatabaseException;

/**
 * Database Stuff
 * 
 * @author pavila
 */
public class DbUtils {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(DbUtils.class);
	
	/**
	 * Calculate user quota
	 * 
	 * @see com.openkm.module.jcr.stuff.JCRUtils
	 */
	public static long calculateQuota(String user) throws DatabaseException {
		// String qs = "/jcr:root//element(*, okm:document)[okm:content/@okm:author='" + session.getUserID() + "']";
		// Workspace workspace = session.getWorkspace();
		// QueryManager queryManager = workspace.getQueryManager();
		// Query query = queryManager.createQuery(qs, Query.XPATH);
		// QueryResult result = query.execute();
		long size = 0;
		
		// for (NodeIterator nit = result.getNodes(); nit.hasNext(); ) {
		// Node node = nit.nextNode();
		// Node contentNode = node.getNode(Document.CONTENT);
		// size += contentNode.getProperty(Document.SIZE).getLong();
		// }
		
		return size;
	}
}
