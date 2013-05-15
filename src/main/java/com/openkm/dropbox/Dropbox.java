package com.openkm.dropbox;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Account;
import com.dropbox.client2.DropboxAPI.DropboxFileInfo;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.RequestTokenPair;
import com.dropbox.client2.session.Session.AccessType;
import com.dropbox.client2.session.WebAuthSession;
import com.dropbox.client2.session.WebAuthSession.WebAuthInfo;
import com.openkm.core.DatabaseException;
import com.openkm.dao.bean.extension.DropboxToken;
import com.openkm.dao.extension.DropboxDAO;

/**
 * Dropbox
 * 
 * @author sochoa
 */
public class Dropbox {
	private static final String APP_KEY = "sqg92qs1ogoamfi";
	private static final String APP_SECRET = "x3020jx9hh7cgrg";
	private static final AccessType ACCESS_TYPE = AccessType.DROPBOX;
	
	private DropboxAPI<WebAuthSession> dropboxAPI;
	private WebAuthSession session;
	private AppKeyPair appKey;
	private WebAuthInfo info;
	private AccessTokenPair tokenPair;
	private String accessTokenKey;
	private String accessTokenSecret;
	
	/**
	 * Dropbox
	 */
	public Dropbox() throws DropboxUnlinkedException, DropboxServerException, DropboxIOException, DropboxException {
		appKey = new AppKeyPair(APP_KEY, APP_SECRET);
		session = new WebAuthSession(appKey, ACCESS_TYPE);
		dropboxAPI = new DropboxAPI<WebAuthSession>(session);
		info = session.getAuthInfo();
	}
	
	/**
	 * access
	 */
	public Account access(String usrId) throws DropboxUnlinkedException, DropboxServerException, DropboxIOException,
			DropboxException, DatabaseException {
		if (!session.isLinked()) {
			return null;
		}
		
		DropboxToken dbt = DropboxDAO.getInstance().findByPk(usrId);
		
		if (dbt != null) {
			accessTokenKey = dbt.getKey();
			accessTokenSecret = dbt.getSecret();
		}
		
		if (accessTokenKey != null && accessTokenSecret != null) {
			AccessTokenPair reAuthTokens = new AccessTokenPair(accessTokenKey, accessTokenSecret);
			dropboxAPI.getSession().setAccessTokenPair(reAuthTokens);
		} else {
			tokenPair = dropboxAPI.getSession().getAccessTokenPair();
			RequestTokenPair tokens = new RequestTokenPair(tokenPair.key, tokenPair.secret);
			dropboxAPI.getSession().retrieveWebAccessToken(tokens);
			
			// these two calls will retrive access tokens for future use
			accessTokenKey = session.getAccessTokenPair().key;
			accessTokenSecret = session.getAccessTokenPair().secret;
			dbt = new DropboxToken();
			dbt.setUser(usrId);
			dbt.setKey(accessTokenKey);
			dbt.setSecret(accessTokenSecret);
			DropboxDAO.getInstance().create(dbt);
		}
		
		return dropboxAPI.accountInfo();
	}
	
	/**
	 * uploadFile
	 */
	public void uploadFile(String path, InputStream inputStream, long length) throws DropboxUnlinkedException,
			DropboxServerException, DropboxIOException, DropboxException {
		dropboxAPI.putFile(path, inputStream, length, null, null);
	}
	
	/**
	 * downloadFile
	 */
	public DropboxFileInfo downloadFile(String path, OutputStream outputStream) throws DropboxUnlinkedException,
			DropboxServerException, DropboxPartialFileException, DropboxIOException, DropboxException {
		return dropboxAPI.getFile(path, null, outputStream, null);
	}
	
	/**
	 * search
	 */
	public List<Entry> search(String query) throws DropboxUnlinkedException, DropboxServerException, DropboxIOException,
			DropboxException {
		return dropboxAPI.search("/", query, 0, false);
	}
	
	/**
	 * getClildren
	 */
	public List<Entry> getClildren(String parentPath) throws DropboxUnlinkedException, DropboxServerException,
			DropboxIOException, DropboxException {
		return dropboxAPI.metadata(parentPath, 0, null, true, null).contents;
	}
	
	/**
	 * getRoot
	 */
	public Entry getRoot() throws DropboxUnlinkedException, DropboxServerException, DropboxIOException, DropboxException {
		return dropboxAPI.metadata("/", 0, null, true, null);
	}
	
	/**
	 * isChildren
	 */
	public boolean ischildren(Entry e) throws DropboxUnlinkedException, DropboxServerException, DropboxIOException,
			DropboxException {
		boolean children = false;
		
		if (e.isDir) {
			Entry entreis = dropboxAPI.metadata(e.path, 0, null, true, null);
			for (Entry entry : entreis.contents) {
				if (entry.isDir) {
					children = true;
					break;
				}
			}
		}
		
		return children;
	}
	
	/**
	 * createFolder
	 */
	public Entry createFolder(String path) throws DropboxUnlinkedException, DropboxServerException, DropboxIOException,
			DropboxException {
		return dropboxAPI.createFolder(path);
	}
	
	/**
	 * getInfo
	 */
	public WebAuthInfo getInfo() {
		return info;
	}
}