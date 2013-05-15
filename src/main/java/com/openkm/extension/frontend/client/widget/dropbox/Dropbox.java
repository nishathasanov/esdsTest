package com.openkm.extension.frontend.client.widget.dropbox;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.openkm.extension.frontend.client.bean.GWTDropboxStatusListener;
import com.openkm.extension.frontend.client.service.OKMDropboxService;
import com.openkm.extension.frontend.client.service.OKMDropboxServiceAsync;
import com.openkm.frontend.client.extension.comunicator.GeneralComunicator;
import com.openkm.frontend.client.extension.comunicator.TabDocumentComunicator;
import com.openkm.frontend.client.extension.comunicator.TabFolderComunicator;
import com.openkm.frontend.client.extension.comunicator.TabMailComunicator;
import com.openkm.frontend.client.extension.event.HasDocumentEvent;
import com.openkm.frontend.client.extension.event.HasDocumentEvent.DocumentEventConstant;
import com.openkm.frontend.client.extension.event.HasFolderEvent;
import com.openkm.frontend.client.extension.event.HasFolderEvent.FolderEventConstant;
import com.openkm.frontend.client.extension.event.HasLanguageEvent;
import com.openkm.frontend.client.extension.event.HasLanguageEvent.LanguageEventConstant;
import com.openkm.frontend.client.extension.event.HasMailEvent;
import com.openkm.frontend.client.extension.event.HasMailEvent.MailEventConstant;
import com.openkm.frontend.client.extension.event.handler.DocumentHandlerExtension;
import com.openkm.frontend.client.extension.event.handler.FolderHandlerExtension;
import com.openkm.frontend.client.extension.event.handler.LanguageHandlerExtension;
import com.openkm.frontend.client.extension.event.handler.MailHandlerExtension;

/**
 * Dropbox
 * 
 * @author sochoa
 *
 */
public class Dropbox implements DocumentHandlerExtension, FolderHandlerExtension, MailHandlerExtension,
		LanguageHandlerExtension {
	
	private final OKMDropboxServiceAsync dropboxService = (OKMDropboxServiceAsync) GWT.create(OKMDropboxService.class);
	
	public static final int TAB_DOCUMENT = 0;
	public static final int TAB_FOLDER = 1;
	public static final int TAB_MAIL = 2;
	public static final int TAB_RECORD = 3;
	
	public static Dropbox singleton;
	private static final String UUID = "101fa1e6-4bf6-4e39-9124-88f44a474268";
	
	public SubMenuDropbox subMenuDropbox;
	public Status status;
	public ConfirmPopup confirmPopup;
	public SearchPopup searchPopup;
	private int selectedPanel = TAB_DOCUMENT;
	public AuthorizePopup authorizePopup;
	public FolderSelectPopup folderSelectPopup;
	private StatusListenerPopup statusListenerPopup;
	private boolean statusListener = false;
	/**
	 * Dropbox
	 */
	public Dropbox(List<String> uuidList) {
		if (isRegistered(uuidList)) {
			singleton = this;
			subMenuDropbox = new SubMenuDropbox();
			authorizePopup = new AuthorizePopup();
			authorizePopup.setStyleName("okm-Popup");
			authorizePopup.setWidth("500px");
			authorizePopup.setHeight("30px");
			status = new Status();
			status.setStyleName("okm-StatusPopup");
			confirmPopup = new ConfirmPopup();
			confirmPopup.setWidth("300px");
			confirmPopup.setHeight("50px");
			confirmPopup.setStyleName("okm-Popup");
			confirmPopup.addStyleName("okm-DisableSelect");
			searchPopup = new SearchPopup();
			searchPopup.setWidth("700px");
			searchPopup.setHeight("150px");
			searchPopup.setStyleName("okm-Popup");
			searchPopup.addStyleName("okm-DisableSelect");
			folderSelectPopup = new FolderSelectPopup();
			folderSelectPopup.setStyleName("okm-Popup");
			folderSelectPopup.addStyleName("okm-DisableSelect");
			statusListenerPopup = new StatusListenerPopup();
			statusListenerPopup.setStyleName("okm-Popup");
			statusListenerPopup.addStyleName("okm-DisableSelect");
			statusListenerPopup.setWidth("610");
			statusListenerPopup.setHeight("250");
		}
	}
	
	/**
	 * getExtensions
	 */
	public List<Object> getExtensions() {
		List<Object> extensions = new ArrayList<Object>();
		extensions.add(singleton);
		extensions.add(subMenuDropbox.getMenu());
		return extensions;
	}
	
	/**
	 * get
	 */
	public static Dropbox get() {
		return singleton;
	}
	
	/**
	 * getUuid
	 */
	public String getUuid() {
		switch (selectedPanel) {
			case TAB_DOCUMENT:
				return TabDocumentComunicator.getDocument().getUuid();
				
			case TAB_FOLDER:
				return TabFolderComunicator.getFolder().getUuid();
				
			case TAB_MAIL:
				return TabMailComunicator.getMail().getUuid();				
				
			default:
				return null;
		}
	}
	
	@Override
	public void onChange(DocumentEventConstant event) {
		if (event.equals(HasDocumentEvent.DOCUMENT_CHANGED)) {
			selectedPanel = TAB_DOCUMENT;
			subMenuDropbox.evaluateMenus();
		}
	}
	
	@Override
	public void onChange(FolderEventConstant event) {
		if (event.equals(HasFolderEvent.FOLDER_CHANGED)) {
			selectedPanel = TAB_FOLDER;
			subMenuDropbox.evaluateMenus();
		}
	}
	
	@Override
	public void onChange(MailEventConstant event) {
		if (event.equals(HasMailEvent.MAIL_CHANGED)) {
			selectedPanel = TAB_MAIL;
			subMenuDropbox.evaluateMenus();
		}
	}
	
	@Override
	public void onChange(LanguageEventConstant event) {
		if (event.equals(HasLanguageEvent.LANGUAGE_CHANGED)) {
			subMenuDropbox.langRefresh();
			authorizePopup.langRefresh();
			statusListenerPopup.langRefresh();
		}
	}
	
	/**
	 * getSelectedPanel
	 */
	public int getSelectedPanel() {
		return selectedPanel;
	}
	
	/**
	 * startStatusListener
	 */
	public void startStatusListener(int action) {
		statusListener = true;
		statusListenerPopup.reset(action);
		statusListenerPopup.center();
		runStatusListener();
	}
	
	/**
	 * runStatusListener
	 */
	public void runStatusListener() {
		if (statusListener) {
			dropboxService.statusListener(new AsyncCallback<List<GWTDropboxStatusListener>>() {
				@Override
				public void onSuccess(List<GWTDropboxStatusListener> result) {
					for (GWTDropboxStatusListener obj : result) {
						statusListenerPopup.add(obj);
					}
					statusListenerPopup.center();
					new Timer() {
						@Override
						public void run() {
							runStatusListener();
						}
					}.schedule(200);
				}
				
				@Override
				public void onFailure(Throwable caught) {
					GeneralComunicator.showError("status", caught);
				}
			});
		} else {
			// Execute last time to ensure all data list has been displayed
			dropboxService.statusListener(new AsyncCallback<List<GWTDropboxStatusListener>>() {
				@Override
				public void onSuccess(List<GWTDropboxStatusListener> result) {
					for (GWTDropboxStatusListener dsl : result) {
						statusListenerPopup.add(dsl);
					}
				}
				
				@Override
				public void onFailure(Throwable caught) {
					GeneralComunicator.showError("status", caught);
				}
			});
		}
	}
	
	/**
	 * stopStatusListener
	 */
	public void stopStatusListener() {
		statusListenerPopup.closeButton.setVisible(true);
		statusListener = false;
	}
	
	/**
	 * isRegistered
	 */
	public static boolean isRegistered(List<String> uuidList) {
		return uuidList.contains(UUID);
	}
}
