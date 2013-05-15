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

package com.openkm.extension.core;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.jackrabbit.api.XASession;

import com.openkm.bean.Folder;
import com.openkm.core.AccessDeniedException;
import com.openkm.core.DatabaseException;
import com.openkm.core.ItemExistsException;
import com.openkm.core.LockException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.Ref;
import com.openkm.core.RepositoryException;
import com.openkm.core.UserQuotaExceededException;

public interface FolderExtension extends Extension {
	/**
	 * Executed BEFORE folder creation.
	 */
	public void preCreate(Session session, Ref<Node> parentNode, Ref<Folder> fld) throws AccessDeniedException,
			RepositoryException, PathNotFoundException, ItemExistsException, DatabaseException, ExtensionException;
	
	/**
	 * Executed AFTER folder creation.
	 */
	public void postCreate(Session session, Ref<Node> parentNode, Ref<Node> fldNode) throws AccessDeniedException,
			RepositoryException, PathNotFoundException, ItemExistsException, DatabaseException, ExtensionException;
	
	/**
	 * Executed BEFORE folder delete.
	 */
	public void preDelete(Session session, String fldPath, Ref<Node> refFolderNode) throws AccessDeniedException,
			RepositoryException, PathNotFoundException, LockException, DatabaseException;
	
	/**
	 * Executed AFTER folder delete.
	 */
	public void postDelete(Session session, String fldPath, Ref<Node> refFolderNode) throws AccessDeniedException,
			RepositoryException, PathNotFoundException, LockException, DatabaseException;
	
	/**
	 * Executed BEFORE folder purge.
	 */
	public void prePurge(Session session, String fldPath, Ref<Node> refFolderNode) throws AccessDeniedException,
			RepositoryException, PathNotFoundException, DatabaseException;
	
	/**
	 * Executed AFTER folder purge.
	 */
	public void postPurge(Session session, String fldPath) throws AccessDeniedException, RepositoryException,
			PathNotFoundException, DatabaseException;
	
	/**
	 * Executed BEFORE folder purge.
	 */
	public void preRename(Session session, String fldPath, String newPath, Ref<Node> refFolderNode)
			throws AccessDeniedException, RepositoryException, PathNotFoundException, ItemExistsException,
			DatabaseException;
	
	/**
	 * Executed AFTER folder purge.
	 */
	public void postRename(Session session, String fldPath, String newPath, Ref<Node> refFolderNode)
			throws AccessDeniedException, RepositoryException, PathNotFoundException, ItemExistsException,
			DatabaseException;
	
	/**
	 * Executed BEFORE folder move.
	 */
	public void preMove(Session session, String fldPath, String dstNodePath) throws AccessDeniedException,
			RepositoryException, PathNotFoundException, ItemExistsException, DatabaseException;
	
	/**
	 * Executed AFTER folder move.
	 */
	public void postMove(Session session, String fldPath, String dstNodePath, Ref<Node> refDstFldNode)
			throws AccessDeniedException, RepositoryException, PathNotFoundException, ItemExistsException,
			DatabaseException;
	
	/**
	 * Executed BEFORE folder copy.
	 */
	public void preCopy(XASession session, Ref<Node> refSrcFolderNode, Ref<Node> refDstFolderNode)
			throws AccessDeniedException, RepositoryException, PathNotFoundException, ItemExistsException, IOException,
			DatabaseException, UserQuotaExceededException;
	
	/**
	 * Executed AFTER folder copy.
	 */
	public void postCopy(XASession session, Ref<Node> refSrcFolderNode, Ref<Node> refNewFolderNode)
			throws AccessDeniedException, RepositoryException, PathNotFoundException, ItemExistsException, IOException,
			DatabaseException, UserQuotaExceededException;
}