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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.core.Config;

public class VersionNumerationFactory {
	private static Logger log = LoggerFactory.getLogger(VersionNumerationFactory.class);
	private static VersionNumerationAdapter verNumAdapter = null;
	
	/**
	 * Singleton pattern for global Version Numeration Adapter.
	 */
	public static VersionNumerationAdapter getVersionNumerationAdapter() {
		if (verNumAdapter == null) {
			try {
				log.info("VersionNumerationAdapter: {}", Config.VERSION_NUMERATION_ADAPTER);
				Object object = Class.forName(Config.VERSION_NUMERATION_ADAPTER).newInstance();
				verNumAdapter = (VersionNumerationAdapter) object;
			} catch (ClassNotFoundException e) {
				//throw new PrincipalAdapterException(e.getMessage(), e);
			} catch (InstantiationException e) {
				//throw new PrincipalAdapterException(e.getMessage(), e);
			} catch (IllegalAccessException e) {
				//throw new PrincipalAdapterException(e.getMessage(), e);
			}
		}

		return verNumAdapter;
	}
}
