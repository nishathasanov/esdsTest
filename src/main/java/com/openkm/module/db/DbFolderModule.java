/**
 * ESDS, Open Document Management System (http://www.openkm.com)
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import com.openkm.automation.AutomationException;
import com.openkm.automation.AutomationManager;
import com.openkm.automation.AutomationUtils;
import com.openkm.bean.ContentInfo;
import com.openkm.bean.Folder;
import com.openkm.bean.Repository;
import com.openkm.core.AccessDeniedException;
import com.openkm.core.Config;
import com.openkm.core.DatabaseException;
import com.openkm.core.ItemExistsException;
import com.openkm.core.LockException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.RepositoryException;
import com.openkm.core.UserQuotaExceededException;
import com.openkm.core.WorkflowException;
import com.openkm.dao.NodeBaseDAO;
import com.openkm.dao.NodeFolderDAO;
import com.openkm.dao.bean.AutomationRule;
import com.openkm.dao.bean.NodeFolder;
import com.openkm.extension.core.ExtensionException;
import com.openkm.module.FolderModule;
import com.openkm.module.db.base.BaseFolderModule;
import com.openkm.module.db.base.BaseScriptingModule;
import com.openkm.spring.PrincipalUtils;
import com.openkm.util.PathUtils;
import com.openkm.util.UserActivity;

public class DbFolderModule implements FolderModule {
	private static Logger log = LoggerFactory.getLogger(DbFolderModule.class);
	
	@Override
	public Folder create(String token, Folder fld) throws PathNotFoundException, ItemExistsException,
			AccessDeniedException, RepositoryException, DatabaseException, ExtensionException, AutomationException {
		log.debug("create({}, {})", token, fld);
		Folder newFolder = null;
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
			
			String name = PathUtils.getName(fld.getPath());
			String parentPath = PathUtils.getParent(fld.getPath());
			String parentUuid = NodeBaseDAO.getInstance().getUuidFromPath(parentPath);
			NodeFolder parentFolder = NodeFolderDAO.getInstance().findByPk(parentUuid);
			
			// Escape dangerous chars in name
			name = PathUtils.escape(name);
			
			if (!name.isEmpty()) {
				fld.setPath(parentPath + "/" + name);
				
				// AUTOMATION - PRE
				Map<String, Object> env = new HashMap<String, Object>();
				env.put(AutomationUtils.PARENT_UUID, parentUuid);
				env.put(AutomationUtils.PARENT_PATH, parentPath);
				env.put(AutomationUtils.PARENT_NODE, parentFolder);
				AutomationManager.getInstance().fireEvent(AutomationRule.EVENT_FOLDER_CREATE, AutomationRule.AT_PRE, env);
				parentFolder = (NodeFolder) env.get(AutomationUtils.PARENT_NODE);
				
				// Create node
				NodeFolder fldNode = BaseFolderModule.create(auth.getName(), parentFolder, name, fld.getCreated());
				
				// AUTOMATION - POST
				env.put(AutomationUtils.FOLDER_NODE, fldNode);
				AutomationManager.getInstance().fireEvent(AutomationRule.EVENT_FOLDER_CREATE, AutomationRule.AT_POST, env);
				
				// Set returned folder properties
				newFolder = BaseFolderModule.getProperties(auth.getName(), fldNode);
				
				// Check scripting
				BaseScriptingModule.checkScripts(auth.getName(), parentFolder.getUuid(), fldNode.getUuid(), "CREATE_FOLDER");
				
				// Activity log
				UserActivity.log(auth.getName(), "CREATE_FOLDER", fldNode.getUuid(), fld.getPath(), null);
			} else {
				throw new RepositoryException("Invalid folder name");
			}
		} catch (DatabaseException e) {
			throw e;
			// } catch (ExtensionException e) {
			// throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("create: {}", newFolder);
		return newFolder;
	}
	
	@Override
	public Folder getProperties(String token, String fldPath) throws PathNotFoundException, RepositoryException,
			DatabaseException {
		log.debug("getProperties({}, {})", token, fldPath);
		Folder fld = null;
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String fldUuid = NodeBaseDAO.getInstance().getUuidFromPath(fldPath);
			NodeFolder fldNode = NodeFolderDAO.getInstance().findByPk(fldUuid);
			fld = BaseFolderModule.getProperties(auth.getName(), fldNode);
			
			// Activity log
			UserActivity.log(auth.getName(), "GET_FOLDER_PROPERTIES", fldUuid, fldPath, null);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getProperties: {}", fld);
		return fld;
	}
	
	@Override
	public void delete(String token, String fldPath) throws LockException, PathNotFoundException,
			AccessDeniedException, RepositoryException, DatabaseException {
		log.debug("delete({}, {})", token, fldPath);
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
			
			String name = PathUtils.getName(fldPath);
			
			if (Repository.ROOT.equals(name) || Repository.CATEGORIES.equals(name) || Repository.THESAURUS.equals(name)
					|| Repository.TEMPLATES.equals(name) || Repository.PERSONAL.equals(name)
					|| Repository.MAIL.equals(name) || Repository.TRASH.equals(name)) {
				throw new AccessDeniedException("Can't delete a required node");
			}
			
			String fldUuid = NodeBaseDAO.getInstance().getUuidFromPath(fldPath);
			
			if (BaseFolderModule.hasLockedNodes(fldUuid)) {
				throw new LockException("Can't delete a folder with child locked nodes");
			}
			
			if (!BaseFolderModule.hasWriteAccess(fldUuid)) {
				throw new AccessDeniedException("Can't delete a folder with readonly nodes");
			}
			
			if (BaseFolderModule.hasWorkflowNodes(fldUuid)) {
				throw new LockException("Can't delete a folder with nodes used in a workflow");
			}
			
			if (fldPath.startsWith("/" + Repository.CATEGORIES) && NodeBaseDAO.getInstance().isCategoryInUse(fldUuid)) {
				throw new AccessDeniedException("Can't delete a category in use");
			}
			
			String userTrashPath = "/" + Repository.TRASH + "/" + auth.getName();
			String userTrashUuid = NodeBaseDAO.getInstance().getUuidFromPath(userTrashPath);
			
			NodeFolderDAO.getInstance().delete(name, fldUuid, userTrashUuid);
			
			// Check scripting
			String parentUuid = NodeBaseDAO.getInstance().getParentUuid(fldUuid);
			BaseScriptingModule.checkScripts(auth.getName(), parentUuid, fldUuid, "DELETE_FOLDER");
			
			// Activity log
			UserActivity.log(auth.getName(), "DELETE_FOLDER", fldUuid, fldPath, null);
		} catch (WorkflowException e) {
			throw new RepositoryException(e.getMessage());
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("delete: void");
	}
	
	@Override
	public void purge(String token, String fldPath) throws LockException, PathNotFoundException, AccessDeniedException,
			RepositoryException, DatabaseException {
		log.debug("purge({}, {})", token, fldPath);
		@SuppressWarnings("unused")
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
			
			String name = PathUtils.getName(fldPath);
			
			if (Repository.ROOT.equals(name) || Repository.CATEGORIES.equals(name) || Repository.THESAURUS.equals(name)
					|| Repository.TEMPLATES.equals(name) || Repository.PERSONAL.equals(name)
					|| Repository.MAIL.equals(name) || Repository.TRASH.equals(name)) {
				throw new AccessDeniedException("Can't delete a required node");
			}
			
			String fldUuid = NodeBaseDAO.getInstance().getUuidFromPath(fldPath);
			
			if (BaseFolderModule.hasLockedNodes(fldUuid)) {
				throw new LockException("Can't purge a folder with child locked nodes");
			}
			
			if (!BaseFolderModule.hasWriteAccess(fldUuid)) {
				throw new AccessDeniedException("Can't purge a folder with readonly nodes");
			}
			
			if (fldPath.startsWith("/" + Repository.CATEGORIES) && NodeBaseDAO.getInstance().isCategoryInUse(fldUuid)) {
				throw new AccessDeniedException("Can't purge a category in use");
			}
			
			NodeFolderDAO.getInstance().purge(fldUuid, true);
			
			// Check scripting
			// String parentUuid = NodeBaseDAO.getInstance().getParentUuid(fldUuid);
			// BaseScriptingModule.checkScripts(user, parentUuid, fldUuid, "PURGE_FOLDER");
			
			// Activity log - Already inside DAO
			// UserActivity.log(auth.getName(), "PURGE_FOLDER", fldUuid, fldPath, null);
		} catch (IOException e) {
			throw new RepositoryException(e.getMessage(), e);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("purge: void");
	}
	
	@Override
	public Folder rename(String token, String fldPath, String newName) throws PathNotFoundException,
			ItemExistsException, AccessDeniedException, RepositoryException, DatabaseException {
		log.debug("rename({}, {}, {})", new Object[] { token, fldPath, newName });
		Folder renamedFolder = null;
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
			
			String name = PathUtils.getName(fldPath);
			String fldUuid = NodeBaseDAO.getInstance().getUuidFromPath(fldPath);
			
			// Escape dangerous chars in name
			newName = PathUtils.escape(newName);
			
			if (newName != null && !newName.isEmpty() && !newName.equals(name)) {
				NodeFolder folderNode = NodeFolderDAO.getInstance().rename(fldUuid, newName);
				renamedFolder = BaseFolderModule.getProperties(auth.getName(), folderNode);
			} else {
				// Don't change anything
				NodeFolder folderNode = NodeFolderDAO.getInstance().findByPk(fldUuid);
				renamedFolder = BaseFolderModule.getProperties(auth.getName(), folderNode);
			}
			
			// Activity log
			UserActivity.log(auth.getName(), "RENAME_FOLDER", fldUuid, fldPath, newName);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("rename: {}", renamedFolder);
		return renamedFolder;
	}
	
	@Override
	public void move(String token, String fldPath, String dstPath) throws PathNotFoundException, ItemExistsException,
			AccessDeniedException, RepositoryException, DatabaseException {
		log.debug("move({}, {}, {})", new Object[] { token, fldPath, dstPath });
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
			
			String fldUuid = NodeBaseDAO.getInstance().getUuidFromPath(fldPath);
			String dstUuid = NodeBaseDAO.getInstance().getUuidFromPath(dstPath);
			NodeFolderDAO.getInstance().move(fldUuid, dstUuid);
			
			// Check scripting
			BaseScriptingModule.checkScripts(auth.getName(), dstUuid, fldUuid, "MOVE_FOLDER");
			
			// Activity log
			UserActivity.log(auth.getName(), "MOVE_FOLDER", fldUuid, fldPath, dstPath);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("move: void");
	}
	
	@Override
	public void copy(String token, String fldPath, String dstPath) throws PathNotFoundException, ItemExistsException,
			AccessDeniedException, RepositoryException, IOException, AutomationException, DatabaseException,
			UserQuotaExceededException {
		log.debug("copy({}, {}, {})", new Object[] { token, fldPath, dstPath });
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
			
			String fldUuid = NodeBaseDAO.getInstance().getUuidFromPath(fldPath);
			String dstUuid = NodeBaseDAO.getInstance().getUuidFromPath(dstPath);
			NodeFolder srcFolderNode = NodeFolderDAO.getInstance().findByPk(fldUuid);
			NodeFolder dstFolderNode = NodeFolderDAO.getInstance().findByPk(dstUuid);
			NodeFolder newFldNode = BaseFolderModule.copy(auth.getName(), srcFolderNode, dstFolderNode);
			
			// Activity log
			UserActivity.log(auth.getName(), "COPY_FOLDER", newFldNode.getUuid(), fldPath, dstPath);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
	}
	
	@Override
	@Deprecated
	public List<Folder> getChilds(String token, String fldPath) throws PathNotFoundException, RepositoryException,
			DatabaseException {
		return getChildren(token, fldPath);
	}
	
	@Override
	public List<Folder> getChildren(String token, String fldPath) throws PathNotFoundException, RepositoryException,
			DatabaseException {
		log.debug("getChildren({}, {})", token, fldPath);
		List<Folder> children = new ArrayList<Folder>();
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String fldUuid = NodeBaseDAO.getInstance().getUuidFromPath(fldPath);
			
			for (NodeFolder nFolder : NodeFolderDAO.getInstance().findByParent(fldUuid)) {
				children.add(BaseFolderModule.getProperties(auth.getName(), nFolder));
			}
			
			// Activity log
			UserActivity.log(auth.getName(), "GET_CHILDREN_FOLDERS", fldUuid, fldPath, null);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getChildren: {}", children);
		return children;
	}
	
	@Override
	public ContentInfo getContentInfo(String token, String fldPath) throws AccessDeniedException, RepositoryException,
			PathNotFoundException, DatabaseException {
		log.debug("getContentInfo({}, {})", token, fldPath);
		ContentInfo contentInfo = new ContentInfo();
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String fldUuid = NodeBaseDAO.getInstance().getUuidFromPath(fldPath);
			contentInfo = BaseFolderModule.getContentInfo(fldUuid);
			
			// Activity log
			UserActivity.log(auth.getName(), "GET_FOLDER_CONTENT_INFO", fldUuid, fldPath, contentInfo.toString());
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("getContentInfo: {}", contentInfo);
		return contentInfo;
	}
	
	@Override
	public boolean isValid(String token, String fldPath) throws PathNotFoundException, RepositoryException, DatabaseException {
		log.debug("isValid({}, {})", token, fldPath);
		boolean valid = true;
		@SuppressWarnings("unused")
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String fldUuid = NodeBaseDAO.getInstance().getUuidFromPath(fldPath);
			
			try {
				NodeFolderDAO.getInstance().findByPk(fldUuid);
			} catch (PathNotFoundException e) {
				valid = false;
			}
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
		
		log.debug("isValid: {}", valid);
		return valid;
	}
	
	@Override
	public String getPath(String token, String uuid) throws AccessDeniedException, RepositoryException,
			DatabaseException {
		try {
			return NodeBaseDAO.getInstance().getPathFromUuid(uuid);
		} catch (PathNotFoundException e) {
			throw new RepositoryException(e.getMessage(), e);
		}
	}
}
