package com.openkm.extension.frontend.client.service;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.RemoteService;
import com.openkm.extension.frontend.client.bean.GWTDropboxAccount;
import com.openkm.extension.frontend.client.bean.GWTDropboxEntry;
import com.openkm.extension.frontend.client.bean.GWTDropboxStatusListener;

/**
 * OKMDropboxServiceAsync
 * 
 * @author sochoa
 * 
 */
public interface OKMDropboxServiceAsync extends RemoteService {
	
	public void authorize(AsyncCallback<String> callback);
	
	public void access(AsyncCallback<GWTDropboxAccount> callback);
	
	public void exportDocument(String path, String uuid, AsyncCallback<?> callback);
	
	public void exportFolder(String path, String uuid, AsyncCallback<?> callback);
	
	public void search(String query, String category, AsyncCallback<List<GWTDropboxEntry>> callback);
	
	public void importDocument(GWTDropboxEntry gwtDropboxEntry, String path, AsyncCallback<?> callback);
	
	public void importFolder(GWTDropboxEntry gwtDropboxEntry, String path, AsyncCallback<?> callback);
	
	public void getRootDropbox(AsyncCallback<GWTDropboxEntry> callback);
	
	public void getChildren(String parentPath, AsyncCallback<List<GWTDropboxEntry>> callback);
	
	public void statusListener(AsyncCallback<List<GWTDropboxStatusListener>> callback);
}
