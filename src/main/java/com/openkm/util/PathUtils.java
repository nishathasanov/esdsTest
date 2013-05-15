/**
 *  OpenKM, Open Document Management System (http://www.openkm.com)
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

package com.openkm.util;

import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathUtils {
	private static Logger log = LoggerFactory.getLogger(PathUtils.class);
	
	/**
	 * Get parent node.
	 */
	public static String getParent(String path) {
		log.debug("getParent({})", path);
		int lastSlash = path.lastIndexOf('/');
		String ret = (lastSlash > 0)?path.substring(0, lastSlash):"/";
		log.debug("getParent: {}", ret);
		return ret;	
	}

	/**
	 * Get node name.
	 */
	public static String getName(String path) {
		log.debug("getName({})", path);
		String ret = path.substring(path.lastIndexOf('/') + 1);
		log.debug("getName: {}", ret);
		return ret;
	}
	
	/**
	 * Get path depth 
	 */
	public static int getDepth(String path) {
		return path.substring(1).split("/").length;
	}
	
	/**
	 * Get path context
	 */
	public static String getContext(String path) {
		int idx = path.indexOf('/', 1);
		return path.substring(0, idx < 0 ? path.length() : idx);
	}
	
	/**
	 * Eliminate dangerous chars in node name.
	 * TODO Keep on sync with uploader:com.openkm.applet.Util.escape(String)
	 * TODO Keep on sync with wsImporter:com.openkm.importer.Util.escape(String)
	 */
	public static String escape(String name) {
		log.debug("escape({})", name);
		String ret = name.replace('/', ' ');
		ret = ret.replace(':', ' ');
		ret = ret.replace('[', ' ');
		ret = ret.replace(']', ' ');
		ret = ret.replace('*', ' ');
		ret = ret.replace('\'', ' ');
		ret = ret.replace('"', ' ');
		ret = ret.replace('|', ' ');
		ret = ret.trim();
		
		// Fix XSS issues
		ret = Encode.forHtml(ret);
		
		log.debug("escape: {}", ret);
		return ret;
	}
	
	/**
	 * Fix context definition. For example "/okm:root" -> "okm_root"
	 */
	public static String fixContext(String context) {
		return context.substring(1).replace(':', '_');
	}
}
