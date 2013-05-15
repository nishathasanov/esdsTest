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

package com.openkm.automation.action;

import java.io.File;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bsh.Interpreter;

import com.openkm.automation.Action;
import com.openkm.automation.AutomationUtils;
import com.openkm.dao.bean.NodeBase;

/**
 * ExecuteScripting
 * 
 * @author jllort
 *
 */
public class ExecuteScripting implements Action  {
	private static Logger log = LoggerFactory.getLogger(ExecuteScripting.class);
	
	@Override
	public void executePre(HashMap<String, Object> env, Object... params) {
		execute(env, params);
	}
	
	@Override
	public void executePost(HashMap<String, Object> env, Object... params) {
		execute(env, params);
	}
	
	/**
	 * execute
	 * 
	 * @param env ESDS API internal environment data.
	 * @param params Action configured parameters.
	 */
	private void execute(HashMap<String, Object> env, Object... params) {
		String script = AutomationUtils.getString(0, params);
		NodeBase node = AutomationUtils.getNode(env);
		String uuid = AutomationUtils.getUuid(env);
		File file = AutomationUtils.getFile(env);
		
		try {
			Interpreter i = new Interpreter();
			i.set("node", node);
			i.set("uuid", uuid);
			i.set("file", file);
			
			if (env.get(AutomationUtils.NODE_UUID) != null) {
				i.set(AutomationUtils.NODE_UUID, env.get(AutomationUtils.NODE_UUID));
			}
			
			if (env.get(AutomationUtils.NODE_PATH) != null) {
				i.set(AutomationUtils.NODE_PATH, env.get(AutomationUtils.NODE_PATH));
			}
			
			if (env.get(AutomationUtils.PROPERTY_GROUP_NAME) != null) {
				i.set(AutomationUtils.PROPERTY_GROUP_NAME, env.get(AutomationUtils.PROPERTY_GROUP_NAME));
			}
			
			if (env.get(AutomationUtils.PROPERTY_GROUP_PROPERTIES) != null) {
				i.set(AutomationUtils.PROPERTY_GROUP_PROPERTIES, env.get(AutomationUtils.PROPERTY_GROUP_PROPERTIES));
			}
			
			i.eval(script);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
}
