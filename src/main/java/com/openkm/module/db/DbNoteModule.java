package com.openkm.module.db;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import com.openkm.bean.Note;
import com.openkm.core.AccessDeniedException;
import com.openkm.core.Config;
import com.openkm.core.DatabaseException;
import com.openkm.core.LockException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.RepositoryException;
import com.openkm.dao.NodeBaseDAO;
import com.openkm.dao.NodeNoteDAO;
import com.openkm.dao.bean.NodeBase;
import com.openkm.dao.bean.NodeNote;
import com.openkm.module.NoteModule;
import com.openkm.module.db.base.BaseNoteModule;
import com.openkm.module.db.base.BaseNotificationModule;
import com.openkm.spring.PrincipalUtils;
import com.openkm.util.FormatUtil;
import com.openkm.util.UserActivity;

public class DbNoteModule implements NoteModule {
	private static Logger log = LoggerFactory.getLogger(DbNoteModule.class);
	
	@Override
	public Note add(String token, String nodePath, String text) throws LockException, PathNotFoundException,
			AccessDeniedException, RepositoryException, DatabaseException {
		log.debug("add({}, {}, {})", new Object[] { token, nodePath, text });
		Note newNote = null;
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
			
			String nodeUuid = NodeBaseDAO.getInstance().getUuidFromPath(nodePath);
			NodeBase node = NodeBaseDAO.getInstance().findByPk(nodeUuid);
			
			text = FormatUtil.sanitizeInput(text);
			NodeNote nNote = BaseNoteModule.create(nodeUuid, auth.getName(), text);
			newNote = BaseNoteModule.getProperties(nNote, nNote.getUuid());
			
			// Check subscriptions
			BaseNotificationModule.checkSubscriptions(node, auth.getName(), "ADD_NOTE", text);

			// Activity log
			UserActivity.log(auth.getName(), "ADD_NOTE", nodeUuid, nodePath, text);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}

		log.debug("add: {}", newNote);
		return newNote;
	}
	
	@Override
	public void delete(String token, String notePath) throws LockException, PathNotFoundException,
			AccessDeniedException, RepositoryException, DatabaseException {
		log.debug("delete({}, {})", token, notePath );
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
			
			String noteUuid = (notePath);
			NodeNote nNote = NodeNoteDAO.getInstance().findByPk(noteUuid);
			
			if (auth.getName().equals(nNote.getAuthor()) || PrincipalUtils.hasRole(Config.DEFAULT_ADMIN_ROLE)) {
				NodeNoteDAO.getInstance().delete(noteUuid);
			} else {
				throw new AccessDeniedException("Note can only be removed by its creator or " + Config.ADMIN_USER);
			}

			// Activity log
			UserActivity.log(auth.getName(), "DELETE_NOTE", noteUuid, notePath, null);
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
	public Note get(String token, String notePath) throws LockException, PathNotFoundException, AccessDeniedException,
			RepositoryException, DatabaseException {
		log.debug("get({}, {})", token, notePath);
		Note note = null;
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
			
			String noteUuid = (notePath);
			NodeNote nNote = NodeNoteDAO.getInstance().findByPk(noteUuid);
			note = BaseNoteModule.getProperties(nNote, nNote.getUuid());

			// Activity log
			UserActivity.log(auth.getName(), "GET_NOTE", noteUuid, notePath, null);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}

		log.debug("get: {}", note);
		return note;
	}
	
	@Override
	public String set(String token, String notePath, String text) throws LockException, PathNotFoundException,
			AccessDeniedException, RepositoryException, DatabaseException {
		log.debug("set({}, {})", token, notePath );
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
			
			String noteUuid = (notePath);
			NodeNote nNote = NodeNoteDAO.getInstance().findByPk(noteUuid);
			NodeBase node = NodeNoteDAO.getInstance().getParentNode(noteUuid);
			
			if (auth.getName().equals(nNote.getAuthor())) {
				text = FormatUtil.sanitizeInput(text);
				nNote.setText(text);
				NodeNoteDAO.getInstance().update(nNote);
			} else {
				throw new AccessDeniedException("Note can only be modified by its creator");
			}
			
			// Check subscriptions
			BaseNotificationModule.checkSubscriptions(node, auth.getName(), "SET_NOTE", text);

			// Activity log
			UserActivity.log(auth.getName(), "SET_NOTE", node.getUuid(), notePath, text);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}

		log.debug("set: {}", text);
		return text;
	}
	
	@Override
	public List<Note> list(String token, String nodePath) throws PathNotFoundException, RepositoryException,
			DatabaseException {
		log.debug("list({}, {})", token, nodePath);
		List<Note> childs = new ArrayList<Note>();
		Authentication auth = null, oldAuth = null;
		
		try {
			if (token == null) {
				auth = PrincipalUtils.getAuthentication();
			} else {
				oldAuth = PrincipalUtils.getAuthentication();
				auth = PrincipalUtils.getAuthenticationByToken(token);
			}
			
			String nodeUuid = NodeBaseDAO.getInstance().getUuidFromPath(nodePath);
			
			for (NodeNote nNote : NodeNoteDAO.getInstance().findByParent(nodeUuid)) {
				childs.add(BaseNoteModule.getProperties(nNote, nNote.getUuid()));
			}
			
			// Activity log
			UserActivity.log(auth.getName(), "LIST_NOTES", nodeUuid, nodePath, null);
		} catch (DatabaseException e) {
			throw e;
		} finally {
			if (token != null) {
				PrincipalUtils.setAuthentication(oldAuth);
			}
		}
				
		log.debug("list: {}", childs);
		return childs;
	}
}
