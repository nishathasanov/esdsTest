package com.openkm.extension.frontend.client.service;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.openkm.extension.frontend.client.bean.GWTDropboxAccount;
import com.openkm.extension.frontend.client.bean.GWTDropboxEntry;
import com.openkm.extension.frontend.client.bean.GWTDropboxStatusListener;
import com.openkm.frontend.client.OKMException;

/**
 * OKMDropboxService
 * 
 * @author sochoa
 * 
 */
@RemoteServiceRelativePath("../extension/Dropbox")
public interface OKMDropboxService extends RemoteService {
	
	public String authorize() throws OKMException;
	
	public GWTDropboxAccount access() throws OKMException;
	
	public void exportDocument(String path,String uuid) throws OKMException;
	
	public void exportFolder(String path,String uuid) throws OKMException;
	
	public List<GWTDropboxEntry> search(String query, String category) throws OKMException;
	
	public void importDocument(GWTDropboxEntry gwtDropboxEntry, String path) throws OKMException;
	
	public void importFolder(GWTDropboxEntry gwtDropboxEntry, String path) throws OKMException;
	
	public GWTDropboxEntry getRootDropbox() throws OKMException;
	
	public List<GWTDropboxEntry> getChildren(String parentPath) throws OKMException;
	
	public List<GWTDropboxStatusListener> statusListener() throws OKMException;
}
