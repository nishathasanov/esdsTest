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

package com.openkm.module.jcr.base;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bsh.EvalError;
import bsh.Interpreter;

import com.openkm.bean.Document;
import com.openkm.bean.Folder;
import com.openkm.bean.Scripting;

public class BaseScriptingModule {
	private static Logger log = LoggerFactory.getLogger(BaseScriptingModule.class);
	
	/**
	 * Check for scripts and evaluate
	 * 
	 * @param node Node modified (Document or Folder)
	 * @param user User who generated the modification event
	 * @param eventType Type of modification event
	 */
	public static void checkScripts(Session session, Node scriptNode, Node eventNode, String eventType) {
		log.debug("checkScripts({}, {}, {}, {})", new Object[] { session, scriptNode, eventNode, eventType });

		try {
			checkScriptsHelper(session, scriptNode, eventNode, eventType);
		} catch (ValueFormatException e) {
			log.error(e.getMessage(), e);
		} catch (javax.jcr.PathNotFoundException e) {
			log.error(e.getMessage(), e);
		} catch (javax.jcr.RepositoryException e) {
			log.error(e.getMessage(), e);
		}

		log.debug("checkScripts: void");
	}

	/**
	 * Check script helper method for recursion.
	 */
	private static void checkScriptsHelper(Session session, Node scriptNode, Node eventNode,
			String eventType) throws javax.jcr.RepositoryException {
		log.debug("checkScriptsHelper({}, {}, {}, {})", new Object[] { session, scriptNode, eventNode,
				eventType });

		if (scriptNode.isNodeType(Folder.TYPE) || scriptNode.isNodeType(Document.TYPE)) {
			if (scriptNode.isNodeType(Scripting.TYPE)) {
				String code = scriptNode.getProperty(Scripting.SCRIPT_CODE).getString();

				// Evaluate script
				Interpreter i = new Interpreter();
				try {
					i.set("session", session);
					i.set("scriptNode", scriptNode);
					i.set("eventNode", eventNode);
					i.set("eventType", eventType);
					i.eval(code);
				} catch (EvalError e) {
					log.warn(e.getMessage(), e);
				}
			}

			// Check for script in parent node
			checkScriptsHelper(session, scriptNode.getParent(), eventNode, eventType);
		}

		log.debug("checkScriptsHelper: void");
	}
}
