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

package com.openkm.vernum;

import org.hibernate.Query;
import org.hibernate.Session;

import com.openkm.dao.bean.NodeDocument;
import com.openkm.dao.bean.NodeDocumentVersion;

/**
 * @author pavila
 */
public class PlainVersionNumerationAdapter implements VersionNumerationAdapter {

	@Override
	public String getInitialVersionNumber() {
		return "1";
	}

	@Override
	public String getNextVersionNumber(Session session, NodeDocument nDoc, NodeDocumentVersion nDocVer) {
		String versionNumber = nDocVer.getName();
		int nextVerNumber = Integer.parseInt(versionNumber);
		Query q = session.createQuery(qs);
		NodeDocumentVersion ndv = null;
		
		do {
			nextVerNumber++;
			q.setString("parent", nDoc.getUuid());
			q.setString("name", String.valueOf(nextVerNumber));
			ndv = (NodeDocumentVersion) q.setMaxResults(1).uniqueResult();
		} while (ndv != null);
		
		return String.valueOf(nextVerNumber);
	}
}
